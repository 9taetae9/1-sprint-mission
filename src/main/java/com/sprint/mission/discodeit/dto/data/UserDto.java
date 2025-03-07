package com.sprint.mission.discodeit.dto.data;

import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import java.time.Instant;
import java.util.UUID;

public record UserDto(
    UUID id,
    String username,
    String email,
    BinaryContentDto profile,
    boolean online
) {

  public static UserDto from(User user, BinaryContent binaryContent, boolean online) {

    return new UserDto(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        binaryContent == null ? null : BinaryContentDto.from(binaryContent),
        online
    );
  }
}

