package com.sprint.mission.discodeit.exception.userstatus;

import com.sprint.mission.discodeit.exception.ErrorCode;
import java.util.UUID;

public class UserStatusAlreadyExistsException extends UserStatusException {

  public UserStatusAlreadyExistsException(UUID userId) {
    super(ErrorCode.USER_STATUS_ALREADY_EXISTS,
        "UserStatus for user with id " + userId + "already exists",
        createDetails("userId", userId));
  }
}
