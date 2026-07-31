package com.fileupload.extblocker.upload;

import com.fileupload.extblocker.upload.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = FileUploadController.class)
public class FileUploadExceptionHandler {

    @ExceptionHandler(UploadRejectedException.class)
    public ResponseEntity<ErrorResponse> handleRejected(UploadRejectedException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }
}
