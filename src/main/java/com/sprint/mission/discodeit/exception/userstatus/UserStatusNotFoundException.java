package com.sprint.mission.discodeit.exception.userstatus;

import com.sprint.mission.discodeit.exception.ErrorCode;
import java.util.Map;
import java.util.UUID;

class UserStatusNotFoundException extends UserStatusException {

  UserStatusNotFoundException(UUID userStatusId) {
    super(ErrorCode.USER_STATUS_NOT_FOUND,
        "UserStatus with id " + userStatusId + "not found",
        createDetails("userStatusId", userStatusId));
  }

  UserStatusNotFoundException(String message, Map<String, Object> details) {
    super(ErrorCode.USER_STATUS_NOT_FOUND, message, details);
  }
}
