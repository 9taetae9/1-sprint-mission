package com.sprint.mission.discodeit.exception.binarycontent;

import java.util.UUID;

public class BinaryContentExceptions {

  private BinaryContentExceptions() {
  }

  public static BinaryContentException notFound(UUID binaryContentId) {
    return new BinaryContentNotFoundException(binaryContentId);
  }

  public static BinaryContentException storageException(String fileName, Exception cause) {
    return new BinaryContentStorageException(fileName, cause);
  }
}
