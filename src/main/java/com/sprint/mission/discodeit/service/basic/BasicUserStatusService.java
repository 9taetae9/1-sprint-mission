package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.UserStatusDto;
import com.sprint.mission.discodeit.dto.request.UserStatusCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.exception.user.UserExceptions;
import com.sprint.mission.discodeit.exception.userstatus.UserStatusExceptions;
import com.sprint.mission.discodeit.mapper.UserStatusMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.UserStatusService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class BasicUserStatusService implements UserStatusService {

  private final UserStatusRepository userStatusRepository;
  private final UserRepository userRepository;
  private final UserStatusMapper userStatusMapper;

  @Transactional
  @Override
  public UserStatusDto create(UserStatusCreateRequest request) {
    UUID userId = request.userId();

    log.info("Processing user status creation: userId={}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("User status creation failed: user not found - userId={}", userId);
          return UserExceptions.notFound(userId);
        });

    Optional.ofNullable(user.getStatus())
        .ifPresent(status -> {
          log.warn("User status creation failed: already exists - userId={}", userId);
          throw UserStatusExceptions.alreadyExists(userId);
        });

    Instant lastActiveAt = request.lastActiveAt();
    UserStatus userStatus = new UserStatus(user, lastActiveAt);
    userStatusRepository.save(userStatus);

    log.info("User status created successfully: statusId={}, userId={}", userStatus.getId(),
        userId);
    return userStatusMapper.toDto(userStatus);
  }

  @Override
  public UserStatusDto find(UUID userStatusId) {
    log.debug("Finding user status: statusId={}", userStatusId);

    return userStatusRepository.findById(userStatusId)
        .map(status -> {
          log.debug("User status found: statusId={}", userStatusId);
          return userStatusMapper.toDto(status);
        })
        .orElseThrow(() -> {
          log.warn("User status not found: statusId={}", userStatusId);
          return UserStatusExceptions.notFound(userStatusId);
        });
  }

  @Override
  public List<UserStatusDto> findAll() {
    log.debug("Finding all user statuses");

    List<UserStatus> statuses = userStatusRepository.findAll();
    log.debug("Found user statuses: {}", statuses.size());

    return statuses.stream()
        .map(userStatusMapper::toDto)
        .toList();
  }

  @Transactional
  @Override
  public UserStatusDto update(UUID userStatusId, UserStatusUpdateRequest request) {
    Instant newLastActiveAt = request.newLastActiveAt();

    log.info("Processing user status update: statusId={}, newLastActiveAt={}",
        userStatusId, newLastActiveAt);

    UserStatus userStatus = userStatusRepository.findById(userStatusId)
        .orElseThrow(
            () -> {
              log.warn("User status update failed: status not found - statusId={}", userStatusId);
              return UserStatusExceptions.notFound(userStatusId);
            });

    userStatus.update(newLastActiveAt);

    log.info("User status updated successfully: statusId={}", userStatusId);
    return userStatusMapper.toDto(userStatus);
  }

  @Transactional
  @Override
  public UserStatusDto updateByUserId(UUID userId, UserStatusUpdateRequest request) {
    Instant newLastActiveAt = request.newLastActiveAt();

    log.info("Processing user status update by userId: userId={}, newLastActiveAt={}",
        userId, newLastActiveAt);

    UserStatus userStatus = userStatusRepository.findByUserId(userId)
        .orElseThrow(
            () -> {
              log.warn("User status update failed: userId not found - userId={}", userId);
              return UserStatusExceptions.notFoundByuserId(userId);
            });
    userStatus.update(newLastActiveAt);

    log.info("User status updated successfully: statusId={}, userId={}",
        userStatus.getId(), userId);

    return userStatusMapper.toDto(userStatus);
  }

  @Transactional
  @Override
  public void delete(UUID userStatusId) {
    log.info("Processing user status deletion: statusId={}", userStatusId);

    if (!userStatusRepository.existsById(userStatusId)) {
      log.warn("User status deletion failed: status not found - statusId={}", userStatusId);
      throw UserStatusExceptions.notFound(userStatusId);
    }

    userStatusRepository.deleteById(userStatusId);
    log.info("User status deleted successfully: statusId={}", userStatusId);
  }
}
