package com.sprint.mission.discodeit.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record PrivateChannelCreateRequest(
    @NotNull(message = "Participant IDs are required")
    @NotEmpty(message = "At least one participant ID is required")
    List<UUID> participantIds
) {

}
