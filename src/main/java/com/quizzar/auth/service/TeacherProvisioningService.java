package com.quizzar.auth.service;

import com.quizzar.cache.config.CacheConfig;
import com.quizzar.teacher.entity.Teacher;
import com.quizzar.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TeacherProvisioningService {

    private final TeacherRepository teacherRepository;

    @Transactional
    @CacheEvict(value = CacheConfig.TEACHER_PROFILE, key = "#jwt.subject")
    public Teacher provisionTeacher(Jwt jwt) {
        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String name = (givenName != null ? givenName : "") + " " + (familyName != null ? familyName : "");
        String finalName = name.trim();

        return teacherRepository.findByKeycloakSubject(subject)
            .map(teacher -> {
                boolean updated = false;
                if (!email.equals(teacher.getEmail())) {
                    teacher.setEmail(email);
                    updated = true;
                }
                if (!finalName.isEmpty() && !finalName.equals(teacher.getName())) {
                    teacher.setName(finalName);
                    updated = true;
                }
                if (updated) {
                    log.info("Updated teacher profile for subject: {}", subject);
                    return teacherRepository.save(teacher);
                }
                return teacher;
            })
            .orElseGet(() -> {
                log.info("Provisioning new teacher for subject: {}", subject);
                Teacher newTeacher = Teacher.builder()
                    .keycloakSubject(subject)
                    .email(email)
                    .name(finalName.isEmpty() ? "Teacher" : finalName)
                    .build();
                return teacherRepository.save(newTeacher);
            });
    }
}
