package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.ReadStatusDto;
import com.sprint.mission.discodeit.dto.request.ReadStatusCreateRequest;
import com.sprint.mission.discodeit.dto.request.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.mapper.ReadStatusMapper;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.ReadStatusService;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class BasicReadStatusService implements ReadStatusService {

    private final ReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final ReadStatusMapper readStatusMapper;

    @Transactional
    @Override
    public ReadStatusDto create(ReadStatusCreateRequest request) {
        UUID userId = request.userId();
        UUID channelId = request.channelId();

        log.info("Processing readStatus creation: userId={}, channelId={}", userId, channelId);

        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> {
                            log.warn("ReadStatus creation failed: user not found - userId={}", userId);
                            return new NoSuchElementException("User with id " + userId + " does not exist");
                        });
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(
                        () -> {
                            log.warn("ReadStatus creation failed: channel not found - channelId={}", channelId);
                            return new NoSuchElementException("Channel with id " + channelId + " does not exist");
                        }
                );

        if (readStatusRepository.existsByUserIdAndChannelId(user.getId(), channel.getId())) {
            log.warn("ReadStatus creation failed: user and channel already exist - userId={}, channelId={}", userId, channelId);
            throw new IllegalArgumentException(
                    "ReadStatus with userId " + userId + " and channelId " + channelId + " already exists");
        }

        Instant lastReadAt = request.lastReadAt();
        ReadStatus readStatus = new ReadStatus(user, channel, lastReadAt);
        readStatusRepository.save(readStatus);

        log.info("ReadStatus created successfully: readStatusId={}, userId={}, channelId={}", readStatus.getId(), userId, channelId);
        return readStatusMapper.toDto(readStatus);
    }

    @Override
    public ReadStatusDto find(UUID readStatusId) {
        log.debug("Finding ReadStatus: readStatusId={}", readStatusId);
        return readStatusRepository.findById(readStatusId)
                .map(
                        readStatus -> {
                            log.debug("ReadStatus found: readStatusId={}, userId={}, channelId={}", readStatus.getId(), readStatus.getUser().getId(), readStatus.getChannel().getId());
                            return readStatusMapper.toDto(readStatus);
                        }
                )
                .orElseThrow(
                        () -> {
                            log.warn("ReadStatus not found: readStatusId={}", readStatusId);
                            return new NoSuchElementException("ReadStatus with id " + readStatusId + " not found");
                        });
    }

    @Override
    public List<ReadStatusDto> findAllByUserId(UUID userId) {
        log.debug("Finding all readStatus: userId={}", userId);

        return readStatusRepository.findAllByUserId(userId).stream()
                .map(readStatusMapper::toDto)
                .toList();
    }

    @Transactional
    @Override
    public ReadStatusDto update(UUID readStatusId, ReadStatusUpdateRequest request) {
        Instant newLastReadAt = request.newLastReadAt();

        log.info("Processing readStatus update: readStatusId={}, newLastReadAt={}", readStatusId, newLastReadAt);

        ReadStatus readStatus = readStatusRepository.findById(readStatusId)
                .orElseThrow(
                        () -> {
                            log.warn("ReadStatus update failed: not found - readStatusId={}", readStatusId);
                            return new NoSuchElementException("ReadStatus with id " + readStatusId + " not found");
                        });

        readStatus.update(newLastReadAt);

        log.info("ReadStatus updated successfully: readStatusId={}", readStatusId);
        return readStatusMapper.toDto(readStatus);
    }

    @Transactional
    @Override
    public void delete(UUID readStatusId) {
        log.info("Processing readStatus deletion: readStatusId={}", readStatusId);
        if (!readStatusRepository.existsById(readStatusId)) {
            log.warn("ReadStatus deletion failed: not found - readStatusId={}", readStatusId);
            throw new NoSuchElementException("ReadStatus with id " + readStatusId + " not found");
        }
        readStatusRepository.deleteById(readStatusId);
        log.info("Read status deleted successfully: readStatusId={}", readStatusId);
    }
}
