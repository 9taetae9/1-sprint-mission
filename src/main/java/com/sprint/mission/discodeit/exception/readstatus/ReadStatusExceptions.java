package com.sprint.mission.discodeit.exception.readstatus;

import java.util.UUID;

public final class ReadStatusExceptions {

  private ReadStatusExceptions() {
  }

  public static ReadStatusException notFound(UUID readStatusId) {
    return new ReadStatusNotFoundException(readStatusId);
  }

  public static ReadStatusException alreadyExists(UUID userId, UUID channelId) {
    return new ReadStatusAlreadyExistsException(userId, channelId);
  }
}
