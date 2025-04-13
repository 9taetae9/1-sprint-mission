package com.sprint.mission.discodeit.exception.userstatus;

import com.sprint.mission.discodeit.exception.DiscodeitException;
import java.util.UUID;

public final class UserStatusExceptions {

  private UserStatusExceptions() {
  }

  public static UserStatusException notFound(UUID userStatusId) {
    return new UserStatusNotFoundException(userStatusId);
  }

  public static UserStatusException notFoundByuserId(UUID userId) {
    return new UserStatusNotFoundException("UserStatus with userId " + userId + " not found",
        DiscodeitException.createDetails("userId", userId));
  }

  public static UserStatusException alreadyExists(UUID userId) {
    return new UserStatusAlreadyExistsException(userId);
  }
}
