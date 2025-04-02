package com.sprint.mission.discodeit.exception.message;

import com.sprint.mission.discodeit.exception.ErrorCode;
import java.util.UUID;

class MessageNotFoundException extends MessageException {

  MessageNotFoundException(UUID messageId) {
    super(ErrorCode.MESSAGE_NOT_FOUND,
        "Message with id " + messageId + " not found",
        createDetails("messageId", messageId));
  }
}
