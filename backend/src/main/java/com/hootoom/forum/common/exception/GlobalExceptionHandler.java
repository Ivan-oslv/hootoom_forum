package com.hootoom.forum.common.exception;

import com.hootoom.forum.common.api.ErrorResponse;
import com.hootoom.forum.common.api.ValidationError;
import com.hootoom.forum.common.web.RequestIdFilter;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(error(exception.getCode(), exception.getMessage(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<ValidationError> details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toValidationError)
                .toList();
        return ResponseEntity.badRequest()
                .body(error("VALIDATION_ERROR", "请求参数不正确", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<ValidationError> details = exception.getConstraintViolations().stream()
                .map(violation -> new ValidationError(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(error("VALIDATION_ERROR", "请求参数不正确", details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("unhandled request exception: errorCode=INTERNAL_ERROR", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("INTERNAL_ERROR", "服务器暂时无法处理请求", List.of()));
    }

    private ValidationError toValidationError(FieldError error) {
        return new ValidationError(error.getField(), error.getDefaultMessage());
    }

    private ErrorResponse error(String code, String message, List<ValidationError> details) {
        return new ErrorResponse(code, message, details, MDC.get(RequestIdFilter.MDC_REQUEST_ID));
    }
}
