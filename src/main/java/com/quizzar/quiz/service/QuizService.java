package com.quizzar.quiz.service;

import com.quizzar.cache.config.CacheConfig;
import com.quizzar.common.dto.PageResponse;
import com.quizzar.common.exception.QuizNotFoundException;
import com.quizzar.common.exception.QuizOwnershipException;
import com.quizzar.document.entity.UploadedDocument;
import com.quizzar.document.repository.UploadedDocumentRepository;
import com.quizzar.quiz.dto.*;
import com.quizzar.quiz.entity.Quiz;
import com.quizzar.quiz.repository.QuizRepository;
import com.quizzar.quiz.util.QuizMapper;
import com.quizzar.storage.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final UploadedDocumentRepository uploadedDocumentRepository;
    private final S3StorageService s3StorageService;
    
    @Value("${quizzar.base-url}") 
    private String baseUrl;

    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheConfig.QUIZ_LIST,
            key = "#keycloakSubject + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()"
    )
    public PageResponse<QuizSummaryResponse> getAllQuizzes(String keycloakSubject, Pageable pageable) {
        Page<QuizSummaryResponse> quizzes = quizRepository.findAllByTeacherKeycloakSubject(keycloakSubject, pageable)
            .map(quiz -> QuizSummaryResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .quizCode(quiz.getQuizCode())
                    .timingMode(quiz.getTimingMode())
                .questionCount(quiz.getQuestions().size())
                .createdAt(quiz.getCreatedAt())
                .build());

        return PageResponse.<QuizSummaryResponse>builder()
                .content(quizzes.getContent())
                .pageNumber(quizzes.getNumber())
                .pageSize(quizzes.getSize())
                .totalElements(quizzes.getTotalElements())
                .totalPages(quizzes.getTotalPages())
                .last(quizzes.isLast())
                .build();
    }

    @Cacheable(value = CacheConfig.QUIZ_DETAIL, key = "#quizId")
    @Transactional(readOnly = true)
    public QuizResponse getQuizById(UUID quizId, String keycloakSubject) {
        Quiz quiz = this.findQuizWithOwnershipCheck(quizId, keycloakSubject);
        return QuizMapper.toDetailResponse(quiz);
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.QUIZ_DETAIL, key = "#quizId"),
            @CacheEvict(value = CacheConfig.PUBLIC_QUIZ, key = "#quizId"),
            @CacheEvict(value = CacheConfig.QUIZ_LIST, allEntries = true),
            @CacheEvict(value = CacheConfig.ANALYTICS, key = "#quizId"),
            @CacheEvict(value = CacheConfig.ANALYTICS, key = "'summary-' + #keycloakSubject")
    })
    public void deleteQuiz(UUID quizId, String keycloakSubject) {
        Quiz quiz = this.findQuizWithOwnershipCheck(quizId, keycloakSubject);
        
        List<String> s3Keys = uploadedDocumentRepository.findAllByQuizId(quizId)
            .stream().map(UploadedDocument::getS3Key).toList();
        
        if (!s3Keys.isEmpty()) {
            s3StorageService.deleteFiles(s3Keys);
        }

        quiz.getSessions().clear();
        quizRepository.saveAndFlush(quiz);
        
        quizRepository.delete(quiz);
        log.info("Deleted quiz {} and {} S3 documents", quizId, s3Keys.size());
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.QUIZ_DETAIL, key = "#quizId"),
            @CacheEvict(value = CacheConfig.PUBLIC_QUIZ, key = "#quizId"),
            @CacheEvict(value = CacheConfig.QUIZ_LIST, allEntries = true),
            @CacheEvict(value = CacheConfig.ANALYTICS, key = "#quizId"),
            @CacheEvict(value = CacheConfig.ANALYTICS, key = "'summary-' + #keycloakSubject")
    })
    public QuizResponse updateQuiz(UUID quizId, UpdateQuizRequest request, String keycloakSubject) {
        Quiz quiz = this.findQuizWithOwnershipCheck(quizId, keycloakSubject);
        
        if (request.getTitle() != null) quiz.setTitle(request.getTitle());
        if (request.getDescription() != null) quiz.setDescription(request.getDescription());
        if (request.getTimingMode() != null) quiz.setTimingMode(request.getTimingMode());
        if (request.getTimerValueSeconds() != null) quiz.setTimerValueSeconds(request.getTimerValueSeconds());
        
        return QuizMapper.toDetailResponse(quizRepository.save(quiz));
    }

    @Transactional(readOnly = true)
    public QuizLinkResponse getQuizLink(UUID quizId, String keycloakSubject) {
        Quiz quiz = this.findQuizWithOwnershipCheck(quizId, keycloakSubject);
        return new QuizLinkResponse(quiz.getQuizCode(), baseUrl + "/public/quiz/" + quiz.getQuizCode());
    }

    @CacheEvict(value = {CacheConfig.QUIZ_DETAIL, CacheConfig.PUBLIC_QUIZ}, key = "#quizId")
    public QuizLinkResponse regenerateQuizCode(UUID quizId, String keycloakSubject) {
        Quiz quiz = this.findQuizWithOwnershipCheck(quizId, keycloakSubject);
        quiz.setQuizCode(this.generateQuizCode());
        quiz = quizRepository.save(quiz);
        return new QuizLinkResponse(quiz.getQuizCode(), baseUrl + "/public/quiz/" + quiz.getQuizCode());
    }

    public Quiz findQuizWithOwnershipCheck(UUID quizId, String keycloakSubject) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new QuizNotFoundException("Quiz not found: " + quizId));
        if (!quiz.getTeacher().getKeycloakSubject().equals(keycloakSubject)) {
            throw new QuizOwnershipException("You do not have permission to access this quiz");
        }
        return quiz;
    }

    public String generateQuizCode() {
        String code;
        do {
            code = RandomStringUtils
                    .secure()
                    .nextAlphanumeric(8)
                    .toUpperCase();
        } while (quizRepository.existsByQuizCode(code));
        return code;
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.PUBLIC_QUIZ, key = "#quizCode")
    public PublicQuizResponse getPublicQuiz(String quizCode) {
        Quiz quiz = quizRepository.findByQuizCode(quizCode)
            .orElseThrow(() -> new QuizNotFoundException("Quiz not found"));
        return QuizMapper.toPublicResponse(quiz);
    }
}
