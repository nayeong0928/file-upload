package com.fileupload.extblocker.upload.dto;

public record UploadSuccessResponse(
        String storedFilename,
        String originalFilename,
        String detectedExtension
) {
}
