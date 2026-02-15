package com.epam.finaltask.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class ControllerLoggingAspect {

    @Pointcut("within(com.epam.finaltask.controller..*)")
    public void controllerPackage() {}

    @Before("controllerPackage() && @annotation(requestMapping)")
    public void logBefore(JoinPoint jp, RequestMapping requestMapping) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest req = attrs.getRequest();

        log.debug(
                "[{} {}] >>> Enter {}.{}(), params: {}",
                req.getMethod(),
                req.getRequestURI(),
                jp.getSignature().getDeclaringType().getSimpleName(),
                jp.getSignature().getName(),
                Arrays.toString(jp.getArgs())
        );
    }

    @AfterReturning(pointcut = "controllerPackage()", returning = "ret")
    public void logAfter(JoinPoint jp, Object ret) {
        log.debug(
                "{}.{}() >>> returned {}",
                jp.getSignature().getDeclaringType().getSimpleName(),
                jp.getSignature().getName(),
                (ret != null ? ret.getClass().getSimpleName() : "null")
        );
    }

    @AfterThrowing(pointcut = "controllerPackage()", throwing = "ex")
    public void logError(JoinPoint jp, Throwable ex) {
        log.error(
                "Exception in {}.{}(): {}",
                jp.getSignature().getDeclaringType().getSimpleName(),
                jp.getSignature().getName(),
                ex.getMessage(),
                ex
        );
    }
}