package com.sprint.mission.discodeit.exception.message;

import java.util.UUID;

public final class MessageExceptions {

  private MessageExceptions() {
  }

  public static MessageNotFoundException notFound(UUID messageId) {
    return new MessageNotFoundException(messageId);
  }
}
