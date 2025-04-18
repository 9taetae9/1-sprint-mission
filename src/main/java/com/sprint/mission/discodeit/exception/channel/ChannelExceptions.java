package com.sprint.mission.discodeit.exception.channel;

import java.util.UUID;

public final class ChannelExceptions {

  private ChannelExceptions() {
  }

  public static ChannelException notFound(UUID channelId) {
    return new ChannelNotFoundException(channelId);
  }

  public static ChannelException privateChannelUpdate(UUID channelId) {
    return new PrivateChannelUpdateException(channelId);
  }
}
