package com.sprint.mission.discodeit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicChannelUpdateRequest(
    @NotBlank(message = "Channel name is required")
    @Size(min = 2, max = 30, message = "Channel name length must be between 3 and 30")
    String newName,

    @Size(max = 255, message = "Description length must be less than 255")
    String newDescription
) {

}
