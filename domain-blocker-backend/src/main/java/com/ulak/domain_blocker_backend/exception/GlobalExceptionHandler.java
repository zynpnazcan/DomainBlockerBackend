package com.ulak.domain_blocker_backend.exception;
import com.ulak.domain_blocker_backend.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.time.LocalDateTime;


@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String errorKey = ex.getMessage();
        String logMessage;

        if ("ERROR_ALREADY_EXISTS".equals(errorKey)) {
            logMessage = "Mevcut Kayıt Denemesi: Bu domain zaten engellenmiş durumda.";
        } else if ("INVALID_DOMAIN_FORMAT".equals(errorKey)) {
            logMessage = "Geçersiz Format: Kullanıcı hatalı bir domain formatı girdi.";
        } else {
            logMessage = "Kullanıcı Hatası Detayı: " + errorKey;
        }
        log.warn(logMessage);
        ErrorResponse error = new ErrorResponse(errorKey, System.currentTimeMillis());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
    log.error("Sistemde beklenmedik hata oluştu",ex);
        ErrorResponse error = new ErrorResponse("Sunucu Hatası: " + ex.getMessage(), System.currentTimeMillis());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
