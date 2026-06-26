package com.quizzar.storage.service;

import com.quizzar.common.exception.S3OperationException;
import com.quizzar.storage.config.S3Properties;
import com.quizzar.storage.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    public String uploadFile(MultipartFile file, UUID teacherId, UUID quizId) {
        String key = buildS3Key(teacherId, quizId, file.getOriginalFilename());
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getS3().getBucketName())
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .serverSideEncryption(ServerSideEncryption.AES256)
                .build();
            
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Uploaded file to S3: {}", key);
            return key;
        } catch (Exception e) {
            throw new S3OperationException("Failed to upload file to S3: " + e.getMessage());
        }
    }

    public String generatePresignedUrl(String s3Key) {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(properties.getS3().getPresignedUrlExpiryMinutes()))
                .getObjectRequest(GetObjectRequest.builder()
                    .bucket(properties.getS3().getBucketName())
                    .key(s3Key)
                    .build())
                .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            throw new S3OperationException("Failed to generate presigned URL: " + e.getMessage());
        }
    }

    public void deleteFiles(List<String> s3Keys) {
        if (s3Keys == null || s3Keys.isEmpty()) return;
        try {
            List<ObjectIdentifier> objectIds = s3Keys.stream()
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .toList();
            
            DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                .bucket(properties.getS3().getBucketName())
                .delete(Delete.builder().objects(objectIds).build())
                .build();
            
            s3Client.deleteObjects(deleteRequest);
            log.info("Deleted {} files from S3", s3Keys.size());
        } catch (Exception e) {
            throw new S3OperationException("Failed to delete files from S3: " + e.getMessage());
        }
    }

    public PresignedUrlResponse generatePresignedUploadUrl(UUID teacherId, String filename, String contentType, boolean isAvatar) {
        String safeFilename = filename == null ? "file" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String prefix = isAvatar ? "avatar" : "documents";
        String s3Key = String.format("teachers/%s/%s/%s_%s", teacherId, prefix, UUID.randomUUID(), safeFilename);
        
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(properties.getS3().getBucketName())
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .putObjectRequest(putObjectRequest)
                    .build();

            String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
            return new PresignedUrlResponse(uploadUrl, s3Key);
        } catch (Exception e) {
            throw new S3OperationException("Failed to generate presigned upload URL: " + e.getMessage());
        }
    }

    public byte[] downloadFile(String s3Key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.getS3().getBucketName())
                    .key(s3Key)
                    .build();
            return s3Client.getObjectAsBytes(request).asByteArray();
        } catch (Exception e) {
            throw new S3OperationException("Failed to download file from S3: " + e.getMessage());
        }
    }

    private String buildS3Key(UUID teacherId, UUID quizId, String filename) {
        String safeFilename = filename == null ? "file" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return String.format("teachers/%s/quizzes/%s/documents/%s_%s",
            teacherId, quizId, UUID.randomUUID(), safeFilename);
    }
}
