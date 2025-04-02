package com.sprint.mission.discodeit.exception.userstatus;

import com.sprint.mission.discodeit.exception.ErrorCode;
import java.util.UUID;

class UserStatusAlreadyExistsException extends UserStatusException {

  UserStatusAlreadyExistsException(UUID userId) {
    super(ErrorCode.USER_STATUS_ALREADY_EXISTS,
        "UserStatus for user with id " + userId + "already exists",
        createDetails("userId", userId));
  }
}
