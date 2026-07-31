package com.fileupload.extblocker.extension;

import com.fileupload.extblocker.extension.dto.ErrorResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = ExtensionController.class)
public class ExtensionExceptionHandler {

    @ExceptionHandler(ExtensionValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ExtensionValidationException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();
        return ResponseEntity.status(status).body(new ErrorResponse(ex.getReason()));
    }
}
