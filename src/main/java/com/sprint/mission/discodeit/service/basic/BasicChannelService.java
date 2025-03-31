package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.mapper.ChannelMapper;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.ChannelService;

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
public class BasicChannelService implements ChannelService {

    private final ChannelRepository channelRepository;

    private final ReadStatusRepository readStatusRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChannelMapper channelMapper;

    @Transactional
    @Override
    public ChannelDto create(PublicChannelCreateRequest request) {
        String name = request.name();
        String description = request.description();

        log.info("Processing public channel creation: name={}", name);
        log.debug("channel description: {}", description);

        Channel channel = new Channel(ChannelType.PUBLIC, name, description);
        channelRepository.save(channel);

        log.info("Public channel created successfully: channelId={}, name={}", channel.getId(), name);
        return channelMapper.toDto(channel);
    }

    @Transactional
    @Override
    public ChannelDto create(PrivateChannelCreateRequest request) {
        log.info("Processing private channel creation: number of participants={}", request.participantIds().size());
        log.debug("Participant IDs: {}", request.participantIds());

        Channel channel = new Channel(ChannelType.PRIVATE, null, null);
        channelRepository.save(channel);

        List<ReadStatus> readStatuses = userRepository.findAllById(request.participantIds()).stream()
                .map(user -> new ReadStatus(user, channel, channel.getCreatedAt()))
                .toList();

        if (readStatuses.size() < request.participantIds().size()) {
            log.warn("Some participants do not exist: requested number={}, actual joined={}",
                    request.participantIds().size(), readStatuses.size());
        }

        readStatusRepository.saveAll(readStatuses);

        log.info("Private channel created successfully: channelId={}, number of participants={}", channel.getId(), readStatuses.size());

        return channelMapper.toDto(channel);
    }

    @Transactional(readOnly = true)
    @Override
    public ChannelDto find(UUID channelId) {
        log.debug("Finding channel: channelId={}", channelId);

        return channelRepository.findById(channelId)
                .map(channel -> {
                    log.debug("Channel found: channelId={}, type={}, name={}",
                            channelId, channel.getType(), channel.getName());
                    return channelMapper.toDto(channel);
                })
                .orElseThrow(
                        () -> {
                            log.warn("Channel not found: channelId={}", channelId);
                            return new NoSuchElementException("Channel with id " + channelId + " not found");
                        });
    }

    @Transactional(readOnly = true)
    @Override
    public List<ChannelDto> findAllByUserId(UUID userId) {
        log.debug("Finding all channel: userId={}", userId);
        List<UUID> mySubscribedChannelIds = readStatusRepository.findAllByUserId(userId).stream()
                .map(ReadStatus::getChannel)
                .map(Channel::getId)
                .toList();

        log.debug("Number of found private channels: {}", mySubscribedChannelIds.size());

        List<Channel> channels = channelRepository.findAllByTypeOrIdIn(ChannelType.PUBLIC, mySubscribedChannelIds);

        log.debug("Number of found total channels(private, public channels): {}", channels);
        return channels.stream()
                .map(channelMapper::toDto)
                .toList();
    }

    @Transactional
    @Override
    public ChannelDto update(UUID channelId, PublicChannelUpdateRequest request) {
        String newName = request.newName();
        String newDescription = request.newDescription();

        log.info("Processing channel update: channelId={}, newName={}", channelId, newName);
        log.debug("New channel description: {}", newDescription);

        try {
            Channel channel = channelRepository.findById(channelId)
                    .orElseThrow(() -> {
                        log.warn("Channel update failed: channel not found - channelId={}", channelId);
                        return new NoSuchElementException("Channel with id " + channelId + " not found");
                    });

            if (channel.getType().equals(ChannelType.PRIVATE)) {
                log.warn("Channel update failed: cannot update private channel - channelId={}", channelId);
                throw new IllegalArgumentException("Private channel cannot be updated");
            }

            channel.update(newName, newDescription);
            log.info("Channel updated successfully: channelId={}", channelId);
            return channelMapper.toDto(channel);
        } catch (Exception e) {
            log.error("Error occurred during channel update: channelId={}, error={}", channelId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    @Override
    public void delete(UUID channelId) {

        log.info("Processing channel deletion: channelId={}", channelId);

        if (!channelRepository.existsById(channelId)) {
            log.warn("Channel deletion failed: channel not found - channelId={}", channelId);
            throw new NoSuchElementException("Channel with id " + channelId + " not found");
        }

        try {
            int messageCount = messageRepository.deleteAllByChannelId(channelId);
            log.debug("Channel messages deleted: {}, channelId={}", messageCount, channelId);

            int readStatusCount = readStatusRepository.deleteAllByChannelId(channelId);
            log.debug("ReadStatus deleted: {}, channelId={}", readStatusCount, channelId);

            channelRepository.deleteById(channelId);

            log.info("Channel deleted successfully: channelId={}", channelId);
        } catch (Exception e) {
            log.error("Error occurred during channel deletion: channelId={}, error={}", channelId, e.getMessage(), e);
            throw e;
        }
    }
}
