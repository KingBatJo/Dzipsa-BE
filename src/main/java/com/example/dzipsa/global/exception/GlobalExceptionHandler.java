package com.example.dzipsa.global.exception;

import com.example.dzipsa.global.exception.dto.ErrorResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ApiCode code = e.getCode();
        log.warn("[GlobalExceptionHandler] BusinessException code={}, message={}", code.getCode(), e.resolvedMessage());
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(new ErrorResponse(code.getCode(), e.resolvedMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : "잘못된 요청입니다.")
                .orElse(CommonErrorCode.BAD_REQUEST.getMessage());
        ApiCode code = CommonErrorCode.BAD_REQUEST;
        log.warn("[GlobalExceptionHandler] MethodArgumentNotValidException message={}", message);
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(new ErrorResponse(code.getCode(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception e) {
        ApiCode code = CommonErrorCode.SERVER_ERROR;
        log.error("[GlobalExceptionHandler] 예상치 못한 예외", e);
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(new ErrorResponse(code.getCode(), code.getMessage()));
    }
}
