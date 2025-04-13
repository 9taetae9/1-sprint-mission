package com.sprint.mission.discodeit.exception.message;

import com.sprint.mission.discodeit.exception.DiscodeitException;
import com.sprint.mission.discodeit.exception.ErrorCode;
import java.util.Map;

public class MessageException extends DiscodeitException {

  public MessageException(ErrorCode errorCode) {
    super(errorCode);
  }

  public MessageException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  public MessageException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public MessageException(ErrorCode errorCode, String message, Map<String, Object> details) {
    super(errorCode, message, details);
  }
}
