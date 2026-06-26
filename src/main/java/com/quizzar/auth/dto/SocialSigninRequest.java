package com.quizzar.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SocialSigninRequest {
    private String name;
    private String email;
    private boolean emailVerified;
}
