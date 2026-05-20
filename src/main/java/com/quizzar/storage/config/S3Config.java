package com.quizzar.storage.config;

import java.net.URI;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@Slf4j
public class S3Config {

    private final S3Properties properties;

    public S3Config(S3Properties properties) {
        this.properties = properties;
    }

    @Bean
    public ApplicationRunner createBucketIfNotExists(S3Client s3Client, S3Properties properties) {
        return args -> {
            String bucket = properties.getS3().getBucketName();
            try {
                s3Client.headBucket(r -> r.bucket(bucket));
            } catch (NoSuchBucketException e) {
                s3Client.createBucket(r -> r.bucket(bucket));
                log.info("Created S3 bucket: {}", bucket);
            }
        };
    }

    @Bean
    public S3Client s3Client() {

        Region region = Region.of(properties.getS3().getRegion());

        String endpoint = properties.getS3().getEndpoint();

        // LOCALSTACK
        if (endpoint != null && !endpoint.isBlank()) {

            return S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .region(region)
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(
                                            properties.getAccessKey(),
                                            properties.getSecretKey()
                                    )
                            )
                    )
                    .serviceConfiguration(
                            S3Configuration.builder()
                                    .pathStyleAccessEnabled(true)
                                    .build()
                    )
                    .build();
        }

        // REAL AWS
        return S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {

        Region region = Region.of(properties.getS3().getRegion());

        String endpoint = properties.getS3().getEndpoint();

        // LOCALSTACK
        if (endpoint != null && !endpoint.isBlank()) {

            return S3Presigner.builder()
                    .endpointOverride(URI.create(endpoint))
                    .region(region)
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(
                                            properties.getAccessKey(),
                                            properties.getSecretKey()
                                    )
                            )
                    )
                    .build();
        }

        // REAL AWS
        return S3Presigner.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }
}