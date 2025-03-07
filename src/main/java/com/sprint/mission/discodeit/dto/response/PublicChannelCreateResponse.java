package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import java.time.Instant;
import java.util.UUID;

public record PublicChannelCreateResponse(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    ChannelType type,
    String name,
    String description
) {

  public static PublicChannelCreateResponse from(Channel channel) {
    return new PublicChannelCreateResponse(
        channel.getId(),
        channel.getCreatedAt(),
        channel.getUpdatedAt(),
        channel.getType(),
        channel.getName(),
        channel.getDescription()
    );
  }
}