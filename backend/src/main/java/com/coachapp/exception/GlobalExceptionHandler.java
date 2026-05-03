package com.coachapp.exception;

import com.coachapp.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ApiResponse.failure("VALIDATION_ERROR", message);
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<?> handleAuthentication(AuthenticationException ex) {
        return ApiResponse.failure("UNAUTHORIZED", ex.getMessage());
    }

    @ExceptionHandler(InvitationExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    public ApiResponse<?> handleInvitationExpired(InvitationExpiredException ex) {
        return ApiResponse.failure("INVITATION_EXPIRED", ex.getMessage());
    }

    @ExceptionHandler(InvitationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<?> handleInvitationNotFound(InvitationNotFoundException ex) {
        return ApiResponse.failure("INVITATION_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InvitationAlreadyUsedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<?> handleInvitationAlreadyUsed(InvitationAlreadyUsedException ex) {
        return ApiResponse.failure("INVITATION_ALREADY_USED", ex.getMessage());
    }

    @ExceptionHandler(InvitationAlreadyPendingException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<?> handleInvitationAlreadyPending(InvitationAlreadyPendingException ex) {
        return ApiResponse.failure("INVITATION_ALREADY_PENDING", ex.getMessage());
    }

    @ExceptionHandler(TenantAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<?> handleTenantAlreadyExists(TenantAlreadyExistsException ex) {
        return ApiResponse.failure("TENANT_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<?> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return ApiResponse.failure("USER_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleUnexpected(Exception ex) {
        return ApiResponse.failure("INTERNAL_ERROR", "An unexpected error occurred");
    }
}
