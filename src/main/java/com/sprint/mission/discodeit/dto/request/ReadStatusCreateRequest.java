package com.sprint.mission.discodeit.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record ReadStatusCreateRequest(
    @NotNull(message = "User ID is required")
    UUID userId,

    @NotNull(message = "Channel ID is required")
    UUID channelId,

    @NotNull(message = "Last read time is required")
    Instant lastReadAt
) {

}
