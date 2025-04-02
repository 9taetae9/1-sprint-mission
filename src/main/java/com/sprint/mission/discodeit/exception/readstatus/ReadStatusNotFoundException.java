package com.sprint.mission.discodeit.exception.readstatus;

import com.sprint.mission.discodeit.exception.ErrorCode;
import java.util.UUID;

public class ReadStatusNotFoundException extends ReadStatusException {

  public ReadStatusNotFoundException(UUID readStatusId) {
    super(ErrorCode.READ_STATUS_NOT_FOUND,
        "ReadStatus with id " + readStatusId + " not found",
        createDetails("readStatusId", readStatusId));
  }

}
