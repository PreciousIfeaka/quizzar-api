package com.quizzar.generation.controller;

import com.quizzar.auth.util.SecurityUtils;
import com.quizzar.common.dto.ApiResponse;
import com.quizzar.generation.dto.*;
import com.quizzar.generation.service.GenerationOrchestrationService;
import com.quizzar.storage.dto.PresignedUrlResponse;
import com.quizzar.storage.service.S3StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/generate")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Quiz Generation")
public class GenerationController {

    private final GenerationOrchestrationService orchestrationService;
    private final S3StorageService s3StorageService;
    private final SecurityUtils securityUtils;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Generate quiz from S3 uploaded document key and metadata")
    public ApiResponse<GenerationResponse> generateFromUpload(
            @RequestPart("request") @Valid GenerateFromUploadRequest request,
            @RequestPart("file") MultipartFile file) {
        UUID teacherId = securityUtils.getCurrentTeacherId();
        log.info("Generating quiz from upload for teacher: {}", teacherId);
        return ApiResponse.ok(orchestrationService.generateFromUpload(request, file, teacherId));
    }

    @PostMapping("/paste")
    @Operation(summary = "Generate quiz from pasted text")
    public ApiResponse<GenerationResponse> generateFromPaste(
            @Valid @RequestBody GenerateFromPasteRequest request) {
        UUID teacherId = securityUtils.getCurrentTeacherId();
        log.info("Generating quiz from pasted text for teacher: {}", teacherId);
        return ApiResponse.ok(orchestrationService.generateFromPaste(request, teacherId));
    }

    @PostMapping(value = "/specs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Generate quiz from specs and optional S3 uploaded syllabus key")
    public ApiResponse<GenerationResponse> generateFromSpecs(
            @RequestPart("request") @Valid GenerateFromSpecsRequest request,
            @RequestPart(value = "syllabusFile", required = false) MultipartFile syllabusFile) {
        UUID teacherId = securityUtils.getCurrentTeacherId();
        log.info("Generating quiz from specs for teacher: {}", teacherId);
        return ApiResponse.ok(orchestrationService.generateFromSpecs(request, syllabusFile, teacherId));
    }

    @PostMapping("/presigned-url")
    @Operation(summary = "Get S3 presigned upload URL for a quiz document or syllabus")
    public ApiResponse<PresignedUrlResponse> getDocumentUploadUrl(
            @RequestParam("filename") String filename,
            @RequestParam("contentType") String contentType) {
        UUID teacherId = securityUtils.getCurrentTeacherId();
        log.info("Received presigned document upload URL request for teacher: {} with filename: {} and contentType: {}",
                teacherId, filename, contentType);
        PresignedUrlResponse response = s3StorageService.generatePresignedUploadUrl(teacherId, filename, contentType,
                false);
        return ApiResponse.ok(response);
    }
}
