package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.channel.ChannelExceptions;
import com.sprint.mission.discodeit.exception.message.MessageExceptions;
import com.sprint.mission.discodeit.exception.user.UserExceptions;
import com.sprint.mission.discodeit.mapper.MessageMapper;
import com.sprint.mission.discodeit.mapper.PageResponseMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class BasicMessageService implements MessageService {

  private final MessageRepository messageRepository;
  //
  private final ChannelRepository channelRepository;
  private final UserRepository userRepository;
  private final MessageMapper messageMapper;
  private final BinaryContentStorage binaryContentStorage;
  private final BinaryContentRepository binaryContentRepository;
  private final PageResponseMapper pageResponseMapper;

  @Transactional
  @Override
  public MessageDto create(MessageCreateRequest messageCreateRequest,
      List<BinaryContentCreateRequest> binaryContentCreateRequests) {
    UUID channelId = messageCreateRequest.channelId();
    UUID authorId = messageCreateRequest.authorId();

    log.info("Processing message creation: channelId={}, authorId={}, number of attachments={}",
        channelId, authorId, binaryContentCreateRequests.size());

    try {
      Channel channel = channelRepository.findById(channelId)
          .orElseThrow(
              () -> {
                log.warn("Message creation failed: channel not found - channelId={}", channelId);
                return ChannelExceptions.notFound(channelId);
              });

      User author = userRepository.findById(authorId)
          .orElseThrow(
              () -> {
                log.warn("Message creation failed: user not found - userId={}", authorId);
                return UserExceptions.notFound(authorId);
              });

      List<BinaryContent> attachments = binaryContentCreateRequests.stream()
          .map(attachmentRequest -> {
            String fileName = attachmentRequest.fileName();
            String contentType = attachmentRequest.contentType();
            byte[] bytes = attachmentRequest.bytes();

            log.debug("Processing message attachment: filename={}, size={}, contentType={}",
                fileName, bytes.length, contentType);

            BinaryContent binaryContent = new BinaryContent(fileName, (long) bytes.length,
                contentType);
            binaryContentRepository.save(binaryContent);
            binaryContentStorage.put(binaryContent.getId(), bytes);
            return binaryContent;
          })
          .toList();

      String content = messageCreateRequest.content();
      log.debug("Message content length: {}", content.length());

      Message message = new Message(
          content,
          channel,
          author,
          attachments
      );

      messageRepository.save(message);

      log.info("Message created successfully: messageId={}, channelId={}", message.getId(),
          channelId);
      return messageMapper.toDto(message);
    } catch (Exception e) {
      log.error("Error occurred during message creation:channelId={}, authorId={}, error={}",
          channelId, authorId, e.getMessage(), e);
      throw e;
    }
  }

  @Transactional(readOnly = true)
  @Override
  public MessageDto find(UUID messageId) {
    log.debug("Finding message: messageId={}", messageId);
    return messageRepository.findById(messageId)
        .map(message -> {
          log.debug("Message found: messageId={}, channelId={}, authorId={}", messageId,
              message.getChannel().getId(), message.getAuthor().getId());
          return messageMapper.toDto(message);
        })
        .orElseThrow(
            () -> {
              log.warn("Message not found: messageId={}", messageId);
              return MessageExceptions.notFound(messageId);
            }
        );
  }

  @Transactional(readOnly = true)
  @Override
  public PageResponse<MessageDto> findAllByChannelId(UUID channelId, Instant createAt,
      Pageable pageable) {

    log.debug("Finding all messages: channelId={}, cursor={}, pageSize={}",
        channelId, createAt, pageable.getPageSize());

    Instant effectiveCreatedAt = Optional.ofNullable(createAt).orElse(Instant.now());
    Slice<MessageDto> slice = messageRepository.findAllByChannelIdWithAuthor(channelId,
            effectiveCreatedAt,
            pageable)
        .map(messageMapper::toDto);

    Instant nextCursor = null;
    if (!slice.getContent().isEmpty()) {
      nextCursor = slice.getContent().get(slice.getContent().size() - 1)
          .createdAt();
    }

    log.debug("Messages Found: {}, channelId={}, hasNext={}",
        slice.getContent().size(), channelId, slice.hasNext());

    return pageResponseMapper.fromSlice(slice, nextCursor);
  }

  @Transactional
  @Override
  public MessageDto update(UUID messageId, MessageUpdateRequest request) {
    log.info("Processing message update: messageId={}", messageId);
    String newContent = request.newContent();
    Message message = messageRepository.findById(messageId)
        .orElseThrow(
            () -> {
              log.warn("Message update failed: message not found - messageId={}", messageId);
              return MessageExceptions.notFound(messageId);
            });
    message.update(newContent);

    log.info("Message updated successfully: messageId={}", messageId);
    return messageMapper.toDto(message);
  }

  @Transactional
  @Override
  public void delete(UUID messageId) {
    log.info("Processing message deletion: messageId={}", messageId);
    if (!messageRepository.existsById(messageId)) {
      log.warn("Message deletion failed: message not found - messageId={}", messageId);
      throw MessageExceptions.notFound(messageId);
    }
    messageRepository.deleteById(messageId);

    log.info("Message deleted successfully: messageId={}", messageId);
  }
}
