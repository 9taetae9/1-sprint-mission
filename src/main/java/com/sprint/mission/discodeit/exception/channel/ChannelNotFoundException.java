package com.sprint.mission.discodeit.exception.channel;

import com.sprint.mission.discodeit.exception.ErrorCode;
import java.util.UUID;

class ChannelNotFoundException extends ChannelException {

  ChannelNotFoundException(UUID channelId) {
    super(ErrorCode.CHANNEL_NOT_FOUND,
        "Channel with id " + channelId + " not found",
        createDetails("channelId", channelId));
  }

}
