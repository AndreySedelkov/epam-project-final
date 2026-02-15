package com.epam.finaltask.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class GlobalRequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        String method = request.getMethod();
        String uri    = request.getRequestURI();

        log.info(">>> {} {}", method, uri);
        try {
            chain.doFilter(request, response);
        } catch (Exception ex) {
            log.error(">< Exception processing {} {}:", method, uri, ex);
            throw ex;
        }
        log.info("<<< {} {} status={}", method, uri, response.getStatus());
    }
}