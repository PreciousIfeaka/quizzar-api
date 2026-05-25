package com.quizzar.storage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "aws")
public class S3Properties {

    private String accessKey;
    private String secretKey;

    private final S3 s3 = new S3();

    @Setter
    @Getter
    public static class S3 {
        private String region;
        private String bucketName;
        private String endpoint;
        private int presignedUrlExpiryMinutes;

    }

}