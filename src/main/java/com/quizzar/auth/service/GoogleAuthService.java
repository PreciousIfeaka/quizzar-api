package com.quizzar.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.quizzar.auth.dto.SocialSigninRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
public class GoogleAuthService {
    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthService(@Value("${google.client-id}") String clientId) {
        verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public SocialSigninRequest verifyToken(String idTokenString) {
        try {
            GoogleIdToken googleIdToken = verifier.verify(idTokenString);
            if (googleIdToken != null) {
                GoogleIdToken.Payload payload = googleIdToken.getPayload();

                String givenName = payload.get("given_name") != null ? (String) payload.get("given_name") : "";
                String familyName = payload.get("family_name") != null ? (String) payload.get("family_name") : "";

                String fallbackName = (givenName + " " + familyName).trim();

                String name = payload.get("name") != null ? (String) payload.get("name") : fallbackName;

                if (name.isEmpty()) {
                    name = "Google User";
                }

                return SocialSigninRequest.builder()
                        .email(payload.getEmail())
                        .emailVerified(payload.getEmailVerified())
                        .name(name).build();
            }
        } catch (Exception e) {
            log.error("Google token verification failed: {}", e.getMessage());
        }
        return null;
    }
}
