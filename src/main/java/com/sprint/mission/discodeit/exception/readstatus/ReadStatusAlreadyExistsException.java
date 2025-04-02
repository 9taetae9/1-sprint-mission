package com.sprint.mission.discodeit.exception.readstatus;

import com.sprint.mission.discodeit.exception.ErrorCode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReadStatusAlreadyExistsException extends ReadStatusException {

  public ReadStatusAlreadyExistsException(UUID userId, UUID channelId) {
    super(ErrorCode.READ_STATUS_ALREADY_EXISTS,
        "ReadStatus with userId " + userId + " and channelId " + channelId + " already exists",
        createReadStatusDetails(userId, channelId));
  }

  private static Map<String, Object> createReadStatusDetails(UUID userId, UUID channelId) {
    Map<String, Object> details = new HashMap<>();
    details.put("userId", userId);
    details.put("channelId", channelId);
    return details;
  }
}
