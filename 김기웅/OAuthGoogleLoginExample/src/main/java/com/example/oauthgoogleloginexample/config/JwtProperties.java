package com.example.oauthgoogleloginexample.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "spring.jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private long accessTokenValiditySeconds;
    private long refreshTokenValiditySeconds;

    @PostConstruct
    void validate() {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT secret must be configured");
        }
        if (accessTokenValiditySeconds <= 0 || refreshTokenValiditySeconds <= 0) {
            throw new IllegalStateException("JWT token validity must be positive");
        }
    }
}
