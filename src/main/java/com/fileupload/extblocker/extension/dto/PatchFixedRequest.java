package com.fileupload.extblocker.extension.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PatchFixedRequest(
        @JsonProperty("is_blocked") boolean isBlocked
) {
}
