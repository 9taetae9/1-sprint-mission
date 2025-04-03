package com.sprint.mission.discodeit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BinaryContentCreateRequest(
    @NotBlank(message = "File name is required")
    String fileName,

    @NotBlank(message = "Content type is required")
    String contentType,

    @NotNull(message = "File content is required")
    byte[] bytes
) {

}
