package com.quizzar.generation.controller;

import com.quizzar.auth.util.SecurityUtils;
import com.quizzar.common.dto.ApiResponse;
import com.quizzar.generation.dto.*;
import com.quizzar.generation.service.GenerationOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/v1/generate")
@RequiredArgsConstructor
@Tag(name = "AI Generation")
public class GenerationController {

    private final GenerationOrchestrationService orchestrationService;
    private final SecurityUtils securityUtils;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Generate quiz from uploaded document")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(encoding = @Encoding(name = "request", contentType = "application/json")))
    public ApiResponse<GenerationResponse> generateFromUpload(
            @RequestPart("request") GenerateFromUploadRequest request,
            @RequestPart("file") MultipartFile file) {
        String subject = securityUtils.getCurrentKeycloakSubject();
        return ApiResponse.ok(orchestrationService.generateFromUpload(request, file, subject));
    }

    @PostMapping("/paste")
    public ApiResponse<GenerationResponse> generateFromPaste(
            @RequestBody GenerateFromPasteRequest request) {
        String subject = securityUtils.getCurrentKeycloakSubject();
        return ApiResponse.ok(orchestrationService.generateFromPaste(request, subject));
    }

    @PostMapping(value = "/specs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Generate quiz from specs and optional syllabus")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(encoding = @Encoding(name = "request", contentType = "application/json")))
    public ApiResponse<GenerationResponse> generateFromSpecs(
            @RequestPart(value = "request") GenerateFromSpecsRequest request,
            @RequestPart(value = "syllabusFile", required = false) MultipartFile syllabusFile) {
        String subject = securityUtils.getCurrentKeycloakSubject();
        return ApiResponse.ok(orchestrationService.generateFromSpecs(request, syllabusFile, subject));
    }
}
