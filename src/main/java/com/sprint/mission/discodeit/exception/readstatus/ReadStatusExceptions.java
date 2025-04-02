package com.sprint.mission.discodeit.exception.readstatus;

import java.util.UUID;

public class ReadStatusExceptions {

  private ReadStatusExceptions() {
  }

  public static ReadStatusNotFoundException notFound(UUID readStatusId) {
    return new ReadStatusNotFoundException(readStatusId);
  }

  public static ReadStatusAlreadyExistsException alreadyExists(UUID userId, UUID channelId) {
    return new ReadStatusAlreadyExistsException(userId, channelId);
  }
}
