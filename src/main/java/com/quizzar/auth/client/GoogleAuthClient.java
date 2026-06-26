package com.quizzar.auth.client;

import com.quizzar.auth.dto.GoogleUserInfo;
import com.quizzar.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleAuthClient {
    @Qualifier("googleWebClient")
    private final WebClient webClient;

    public GoogleUserInfo getUserInfo(String accessToken) {
        log.info("Fetching Google userinfo for provided access token");
        try {
            GoogleUserInfo userInfo = webClient.get()
                    .uri("/oauth2/v3/userinfo")
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .onStatus(HttpStatus.UNAUTHORIZED::equals, res ->
                            Mono.error(new UnauthorizedException("Invalid or expired Google access token")))
                    .onStatus(HttpStatus.FORBIDDEN::equals, res ->
                            Mono.error(new UnauthorizedException("Google access token does not have required scopes")))
                    .onStatus(HttpStatusCode::is5xxServerError, res ->
                            res.bodyToMono(String.class).defaultIfEmpty("").flatMap(body ->
                                    Mono.error(new UnauthorizedException("Google userinfo service error: " + sanitize(body)))))
                    .bodyToMono(GoogleUserInfo.class)
                    .block();
            return validate(userInfo);
        } catch (UnauthorizedException e) {
            throw e;
        } catch (WebClientResponseException e) {
            log.error("Google userinfo HTTP error {}: {}", e.getStatusCode(), sanitize(e.getResponseBodyAsString()));
            throw new UnauthorizedException("Google authentication failed (" + e.getStatusCode() + ")");
        } catch (Exception e) {
            log.error("Unexpected error calling Google userinfo: {}", e.getMessage());
            throw new UnauthorizedException("Unexpected error during Google authentication: " + e.getMessage());
        }
    }
    private GoogleUserInfo validate(GoogleUserInfo userInfo) {
        if (userInfo == null) {
            throw new UnauthorizedException("Google returned empty user info");
        }
        if (!userInfo.isEmailVerified()) {
            throw new UnauthorizedException("Google account email is not verified");
        }
        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            throw new UnauthorizedException("Google account has no email address");
        }
        return userInfo;
    }
    private String sanitize(String body) {
        if (body == null) return "null";
        return body.length() > 50 ? body.substring(0, 50) + "...[truncated]" : body;
    }

}
