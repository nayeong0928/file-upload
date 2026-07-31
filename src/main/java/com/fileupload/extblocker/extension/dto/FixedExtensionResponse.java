package com.fileupload.extblocker.extension.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fileupload.extblocker.extension.BlockedExtension;

public record FixedExtensionResponse(
        String extension,
        @JsonProperty("is_blocked") boolean isBlocked
) {
    public static FixedExtensionResponse from(BlockedExtension entity) {
        return new FixedExtensionResponse(entity.getExtension(), entity.isBlocked());
    }
}
