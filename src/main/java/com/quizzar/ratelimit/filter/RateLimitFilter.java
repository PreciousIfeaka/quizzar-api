package com.quizzar.ratelimit.filter;

import com.quizzar.ratelimit.config.RateLimitConfig;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String bucketKey = resolveBucketKey(request, path);
        BucketConfiguration config = resolveBucketConfig(path);
        
        BucketProxy bucket = proxyManager.builder().build(bucketKey, () -> config);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            response.addHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            response.setStatus(429);
            response.addHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"success\":false,\"error\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests. Retry after " 
                + retryAfterSeconds + " seconds.\"}"
            );
        }
    }
    
    private String resolveBucketKey(HttpServletRequest request, String path) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return "user:" + jwtAuth.getToken().getSubject() + ":" + (path.startsWith("/api/v1/generate") ? "ai" : "general");
        }
        return "ip:" + getClientIp(request);
    }
    
    private BucketConfiguration resolveBucketConfig(String path) {
        if (path.startsWith("/api/v1/generate")) {
            return BucketConfiguration.builder().addLimit(RateLimitConfig.aiEndpointBandwidth()).build();
        } else if (path.startsWith("/public/quiz") && path.endsWith("/submit")) {
            return BucketConfiguration.builder().addLimit(RateLimitConfig.publicSubmitBandwidth()).build();
        } else if (path.startsWith("/public")) {
            return BucketConfiguration.builder().addLimit(RateLimitConfig.publicEndpointBandwidth()).build();
        }
        return BucketConfiguration.builder().addLimit(RateLimitConfig.generalEndpointBandwidth()).build();
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
