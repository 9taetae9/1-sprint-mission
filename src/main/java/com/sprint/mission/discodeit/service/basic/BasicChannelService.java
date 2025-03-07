package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.dto.response.ReadStatusResponse;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.ChannelService;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class BasicChannelService implements ChannelService {

  private final ChannelRepository channelRepository;
  //
  private final ReadStatusRepository readStatusRepository;
  private final MessageRepository messageRepository;
  private final UserRepository userRepository;
  private final BinaryContentRepository binaryContentRepository;
  private final UserStatusRepository userStatusRepository;

  @Override
  public Channel create(PublicChannelCreateRequest request) {
    String name = request.name();
    String description = request.description();
    Channel channel = new Channel(ChannelType.PUBLIC, name, description);

    return channelRepository.save(channel);
  }

  @Override
  public Channel create(PrivateChannelCreateRequest request) {
    Channel channel = new Channel(ChannelType.PRIVATE, null, null);
    Channel createdChannel = channelRepository.save(channel);

    request.participantIds().stream()
        .map(userId -> new ReadStatus(userId, createdChannel.getId(), channel.getCreatedAt()))
        .forEach(readStatusRepository::save);

    return createdChannel;
  }

  @Override
  public ChannelDto find(UUID channelId) {
    Channel channel = channelRepository.findById(channelId)
        .orElseThrow(
            () -> new NoSuchElementException("Channel with id " + channelId + " not found"));

    Instant lastMessageAt = getLastMessageTime(channel.getId());

    List<UserDto> participants = readStatusRepository.findAllByChannelId(channel.getId())
        .stream()
        .map(ReadStatus::getUserId)
        .map(userId -> {
          User user = userRepository.findById(userId)
              .orElseThrow(() -> new NoSuchElementException("User not found"));

          // 사용자 프로필 이미지 조회
          BinaryContent profile = binaryContentRepository.findById(user.getProfileId()).get();

          Optional<UserStatus> userStatusOpt = userStatusRepository.findByUserId(userId);
          Boolean isOnline;
          if (userStatusOpt.isPresent()) {
            isOnline = userStatusOpt.get().isOnline();
          } else {
            isOnline = false;
          }

          return UserDto.from(user, profile, isOnline);
        })
        .collect(Collectors.toList());

    return ChannelDto.from(channel, participants, lastMessageAt);

  }

  @Override
  public List<ChannelDto> findAllByUserId(UUID userId) {

    List<UUID> userSubscribedChannelIds = readStatusRepository.findAllByUserId(userId).stream()
        .map(ReadStatus::getChannelId)
        .toList();
//특정 사용자가 볼 수 있는 모든 채널 목록을 조회 => 개인 채널 + 공개 채널

    return channelRepository.findAll().stream()
        .filter(channel ->
            channel.getType().equals(ChannelType.PUBLIC)
                || userSubscribedChannelIds.contains(channel.getId()))
        .map(this::toDto).toList();
  }

  @Override
  public Channel update(UUID channelId, PublicChannelUpdateRequest request) {
    String newName = request.newName();
    String newDescription = request.newDescription();
    Channel channel = channelRepository.findById(channelId)
        .orElseThrow(
            () -> new NoSuchElementException("Channel with id " + channelId + " not found"));
    if (channel.getType().equals(ChannelType.PRIVATE)) {
      throw new IllegalArgumentException("Private channel cannot be updated");
    }
    channel.update(newName, newDescription);
    return channelRepository.save(channel);
  }

  @Override
  public void delete(UUID channelId) {
    Channel channel = channelRepository.findById(channelId)
        .orElseThrow(
            () -> new NoSuchElementException("Channel with id " + channelId + " not found"));

    messageRepository.deleteAllByChannelId(channel.getId());
    readStatusRepository.deleteAllByChannelId(channel.getId());

    channelRepository.deleteById(channelId);
  }

  private List<UUID> getParticipantIds(Channel channel) {
    List<UUID> participantIds = List.of();
    if (channel.getType().equals(ChannelType.PRIVATE)) {
      participantIds = readStatusRepository.findAllByChannelId(channel.getId())
          .stream()
          .map(ReadStatus::getUserId)
          .toList();
    }
    return participantIds;
  }

  private Instant getLastMessageTime(UUID channelId) {
    Instant lastMessageAt = messageRepository.findAllByChannelId(channelId)
        .stream()
        .max(Comparator.comparing(Message::getCreatedAt))
        .map(Message::getCreatedAt)
        .orElse(Instant.MIN);
    return lastMessageAt;
  }

  private ChannelDto toDto(Channel channel) {
    // 1. 마지막 메시지 시간 조회
    Instant lastMessageAt = messageRepository.findAllByChannelId(channel.getId())
        .stream()
        .sorted(Comparator.comparing(Message::getCreatedAt).reversed())
        .map(Message::getCreatedAt)
        .limit(1)
        .findFirst()
        .orElse(Instant.MIN);

    List<UUID> participantIds = new ArrayList<>();
    if (channel.getType() == ChannelType.PRIVATE) {
      readStatusRepository.findAllByChannelId(channel.getId())
          .stream()
          .map(ReadStatus::getUserId)
          .forEach(participantIds::add);
    }
    return new ChannelDto(
        channel.getId(),
        channel.getType(),
        channel.getName(),
        channel.getDescription(),
        participantIds,
        lastMessageAt
    );
  }
}
