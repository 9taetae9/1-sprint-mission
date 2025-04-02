package com.sprint.mission.discodeit.exception.readstatus;

import com.sprint.mission.discodeit.exception.ErrorCode;
import java.util.UUID;

class ReadStatusNotFoundException extends ReadStatusException {

  ReadStatusNotFoundException(UUID readStatusId) {
    super(ErrorCode.READ_STATUS_NOT_FOUND,
        "ReadStatus with id " + readStatusId + " not found",
        createDetails("readStatusId", readStatusId));
  }

}
