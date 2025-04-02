package com.sprint.mission.discodeit.exception.binarycontent;

import com.sprint.mission.discodeit.exception.ErrorCode;
import java.util.HashMap;
import java.util.Map;

public class BinaryContentStorageException extends BinaryContentException {

  public BinaryContentStorageException(String fileName, Exception cause) {
    super(ErrorCode.BINARY_CONTENT_STORAGE_ERROR,
        "Error storing binary content: " + fileName + " - " + cause.getMessage(),
        createStorageErrorDetails(fileName, cause));
    initCause(cause);
  }

  private static Map<String, Object> createStorageErrorDetails(String fileName, Exception cause) {
    Map<String, Object> details = new HashMap<>();
    details.put("fileName", fileName);
    details.put("errorType", cause.getClass().getSimpleName());
    details.put("errorMessage", cause.getMessage());
    return details;
  }
}
