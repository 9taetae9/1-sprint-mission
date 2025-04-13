package com.sprint.mission.discodeit.exception.channel;

import com.sprint.mission.discodeit.exception.ErrorCode;
import java.util.UUID;

class PrivateChannelUpdateException extends ChannelException {

  PrivateChannelUpdateException(UUID channelId) {
    super(ErrorCode.PRIVATE_CHANNEL_UPDATE,
        "Private channel with id " + channelId + " cannot be updated",
        createDetails("channelId", channelId));
  }
}
