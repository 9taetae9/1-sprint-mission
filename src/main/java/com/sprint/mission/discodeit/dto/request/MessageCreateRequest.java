package com.sprint.mission.discodeit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MessageCreateRequest(
    @NotBlank(message = "Content is required")
    String content,

    @NotNull(message = "Channel ID is required")
    UUID channelId,

    @NotNull(message = "Author ID is required")
    UUID authorId
) {

}
