package com.epam.finaltask.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "application.security.jwt")
public class JwtProperties {

    private String secretKey = "dummySecretKeyForTests";

    private long expiration = 86_400_000L;

    @JsonProperty("refresh-token.expiration")

    private long refreshTokenExpiration = 172_800_000L;
}