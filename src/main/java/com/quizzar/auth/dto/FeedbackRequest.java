package com.quizzar.auth.dto;

import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class FeedbackRequest {
    private String text;

    @URL(
            protocol = "https",
            host = "dailyfoods3bucket.s3.amazonaws.com",
            message = "Image URL must originate from secure S3 bucket"
    )
    private String imageUrl;
}
