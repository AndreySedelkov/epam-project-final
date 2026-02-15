package com.epam.finaltask.handler;

import com.epam.finaltask.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleValidationErrors(MethodArgumentNotValidException ex,
                                         Model model,
                                         HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing
                ));

        fillCommon(model, request, 400, "Bad Request", "Validation failed");
        model.addAttribute("fieldErrors", fieldErrors);
        return "error/400";
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleConstraintViolation(ConstraintViolationException ex,
                                            Model model,
                                            HttpServletRequest request) {
        fillCommon(model, request, 400, "Bad Request", ex.getMessage());
        return "error/400";
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public String handleBadCredentials(BadCredentialsException ex,
                                       Model model,
                                       HttpServletRequest request) {
        fillCommon(model, request, 401, "Unauthorized", "Invalid username or password");
        return "auth/sign-in";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex,
                                     Model model,
                                     HttpServletRequest request) {
        fillCommon(model, request, 403, "Forbidden",
                "You do not have permission to access this page.");
        return "error/403";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException ex,
                                 Model model,
                                 HttpServletRequest request) {
        fillCommon(model, request, 404, "Not Found", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public String handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                         Model model,
                                         HttpServletRequest request) {
        fillCommon(model, request, 405, "Method Not Allowed", ex.getMessage());
        return "error/405";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleAllOther(Exception ex,
                                 Model model,
                                 HttpServletRequest request) {
        log.error("🔴 Unhandled exception at {}:", request.getRequestURI(), ex);

        fillCommon(model, request, 500,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.");
        return "error/500";
    }


    private void fillCommon(Model model,
                            HttpServletRequest request,
                            int status,
                            String error,
                            String message) {
        model.addAttribute("timestamp", System.currentTimeMillis());
        model.addAttribute("status", status);
        model.addAttribute("error", error);
        model.addAttribute("message", message);
        model.addAttribute("path", request.getRequestURI());
    }
}