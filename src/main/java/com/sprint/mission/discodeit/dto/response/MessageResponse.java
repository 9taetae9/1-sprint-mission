package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.Message;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageResponse(
    UUID id,
    String content,
    List<UUID> attachmentIds,
    Instant creatAt,
    UUID channelId,
    UUID authorId
) {

  public static MessageResponse from(Message message) {
    return new MessageResponse(
        message.getId(),
        message.getContent(),
        message.getAttachmentIds(),
        message.getCreatedAt(),
        message.getChannelId(),
        message.getAuthorId()
    );
  }
}
