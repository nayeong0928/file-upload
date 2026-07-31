package com.fileupload.extblocker.extension.dto;

import com.fileupload.extblocker.extension.BlockedExtension;

public record CustomExtensionResponse(
        Long id,
        String extension
) {
    public static CustomExtensionResponse from(BlockedExtension entity) {
        return new CustomExtensionResponse(entity.getId(), entity.getExtension());
    }
}
