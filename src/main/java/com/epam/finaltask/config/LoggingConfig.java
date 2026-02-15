package com.epam.finaltask.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;


@Configuration
public class LoggingConfig {

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter f = new CommonsRequestLoggingFilter();
        f.setIncludeClientInfo(true);
        f.setIncludeQueryString(true);
        f.setIncludeHeaders(false);
        f.setIncludePayload(false);
        f.setBeforeMessagePrefix("→ REQUEST  : ");
        f.setAfterMessagePrefix("← RESPONSE : ");
        return f;
    }
}