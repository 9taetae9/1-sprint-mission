package com.sprint.mission.discodeit.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @Size(min = 3, max = 30, message = "Username length must be between 3 and 30")
    String newUsername,

    @Email(message = "Email should be valid")
    String newEmail,

    @Size(min = 8, message = "Password length must be at least 8")
    String newPassword
) {

}
