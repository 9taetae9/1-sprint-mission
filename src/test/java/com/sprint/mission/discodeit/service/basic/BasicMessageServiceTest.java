package com.sprint.mission.discodeit.service.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.base.BaseUpdatableEntity;
import com.sprint.mission.discodeit.exception.channel.ChannelException;
import com.sprint.mission.discodeit.exception.channel.ChannelExceptions;
import com.sprint.mission.discodeit.exception.message.MessageException;
import com.sprint.mission.discodeit.exception.message.MessageExceptions;
import com.sprint.mission.discodeit.exception.user.UserException;
import com.sprint.mission.discodeit.exception.user.UserExceptions;
import com.sprint.mission.discodeit.mapper.MessageMapper;
import com.sprint.mission.discodeit.mapper.PageResponseMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
class BasicMessageServiceTest {

  @Mock
  private MessageRepository messageRepository;
  @Mock
  private ChannelRepository channelRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private MessageMapper messageMapper;
  @Mock
  private BinaryContentStorage binaryContentStorage;
  @Mock
  private BinaryContentRepository binaryContentRepository;
  @Mock
  private PageResponseMapper pageResponseMapper;

  @InjectMocks
  private BasicMessageService messageService;

  @Test
  void create_성공_첨부파일_없음() {
    // given
    UUID channelId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    String content = "Test message content";

    MessageCreateRequest request = new MessageCreateRequest(content, channelId, authorId);
    List<BinaryContentCreateRequest> attachmentRequests = Collections.emptyList();

    Channel channel = new Channel(ChannelType.PUBLIC, "general", "General channel");
    setId(channel, channelId);

    User author = new User("testuser", "test@test.com", "password", null);
    setId(author, authorId);

    Message savedMessage = new Message(content, channel, author, Collections.emptyList());
    UUID messageId = UUID.randomUUID();
    setId(savedMessage, messageId);

    Instant now = Instant.now();
    UserDto authorDto = new UserDto(authorId, author.getUsername(), author.getEmail(), null, true);

    MessageDto expectedDto = new MessageDto(
        messageId,
        now,
        now,
        content,
        channelId,
        authorDto,
        Collections.emptyList()
    );

    given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));
    given(userRepository.findById(authorId)).willReturn(Optional.of(author));
    given(messageRepository.save(any(Message.class))).willReturn(savedMessage);
    given(messageMapper.toDto(any(Message.class))).willReturn(expectedDto);

    // when
    MessageDto result = messageService.create(request, attachmentRequests);

    // then
    assertThat(result).isEqualTo(expectedDto);
    then(channelRepository).should(times(1)).findById(channelId);
    then(userRepository).should(times(1)).findById(authorId);
    then(messageRepository).should(times(1)).save(any(Message.class));
  }

  @Test
  void create_성공_첨부파일_있음() {
    // given
    UUID channelId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    String content = "Test message with attachment";

    MessageCreateRequest request = new MessageCreateRequest(content, channelId, authorId);

    BinaryContentCreateRequest attachmentRequest = new BinaryContentCreateRequest(
        "test.txt", "text/plain", new byte[]{1, 2, 3});
    List<BinaryContentCreateRequest> attachmentRequests = List.of(attachmentRequest);

    Channel channel = new Channel(ChannelType.PUBLIC, "general", "General channel");
    setId(channel, channelId);

    User author = new User("testuser", "test@test.com", "password", null);
    setId(author, authorId);

    BinaryContent binaryContent = new BinaryContent(
        attachmentRequest.fileName(), (long) attachmentRequest.bytes().length,
        attachmentRequest.contentType());
    UUID attachmentId = UUID.randomUUID();
    setId(binaryContent, attachmentId);

    List<BinaryContent> attachments = List.of(binaryContent);

    Message savedMessage = new Message(content, channel, author, attachments);
    UUID messageId = UUID.randomUUID();
    setId(savedMessage, messageId);

    Instant now = Instant.now();
    UserDto authorDto = new UserDto(authorId, author.getUsername(), author.getEmail(), null, true);
    BinaryContentDto attachmentDto = new BinaryContentDto(
        attachmentId,
        attachmentRequest.fileName(),
        (long) attachmentRequest.bytes().length,
        attachmentRequest.contentType()
    );

    MessageDto expectedDto = new MessageDto(
        messageId,
        now,
        now,
        content,
        channelId,
        authorDto,
        List.of(attachmentDto)
    );

    given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));
    given(userRepository.findById(authorId)).willReturn(Optional.of(author));

    given(binaryContentRepository.save(any(BinaryContent.class))).willAnswer(invocation -> {
      BinaryContent savedContent = invocation.getArgument(0);
      setId(savedContent, attachmentId);
      return savedContent;
    });

    given(messageRepository.save(any(Message.class))).willReturn(savedMessage);
    given(messageMapper.toDto(any(Message.class))).willReturn(expectedDto);

    // when
    MessageDto result = messageService.create(request, attachmentRequests);

    // then
    assertThat(result).isEqualTo(expectedDto);
    then(channelRepository).should(times(1)).findById(channelId);
    then(userRepository).should(times(1)).findById(authorId);
    then(binaryContentRepository).should(times(1)).save(any(BinaryContent.class));
    then(binaryContentStorage).should(times(1)).put(attachmentId, attachmentRequest.bytes());
    then(messageRepository).should(times(1)).save(any(Message.class));
  }

  @Test
  void create_실패_채널_없음() {
    // given
    UUID channelId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    String content = "Test message content";

    MessageCreateRequest request = new MessageCreateRequest(content, channelId, authorId);
    List<BinaryContentCreateRequest> attachmentRequests = Collections.emptyList();

    given(channelRepository.findById(channelId)).willReturn(Optional.empty());

    // when, then
    ChannelException exception = ChannelExceptions.notFound(channelId);
    assertThatThrownBy(() -> messageService.create(request, attachmentRequests))
        .hasSameClassAs(exception);

    then(messageRepository).should(times(0)).save(any(Message.class));
  }

  @Test
  void create_실패_사용자_없음() {
    // given
    UUID channelId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    String content = "Test message content";

    MessageCreateRequest request = new MessageCreateRequest(content, channelId, authorId);
    List<BinaryContentCreateRequest> attachmentRequests = Collections.emptyList();

    Channel channel = new Channel(ChannelType.PUBLIC, "general", "General channel");
    setId(channel, channelId);

    given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));
    given(userRepository.findById(authorId)).willReturn(Optional.empty());

    // when, then
    UserException exception = UserExceptions.notFound(authorId);
    assertThatThrownBy(() -> messageService.create(request, attachmentRequests))
        .hasSameClassAs(exception);

    then(messageRepository).should(times(0)).save(any(Message.class));
  }

  @Test
  void find_성공() {
    // given
    UUID messageId = UUID.randomUUID();
    UUID channelId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    String content = "Test message content";

    Channel channel = new Channel(ChannelType.PUBLIC, "general", "General channel");
    setId(channel, channelId);

    User author = new User("testuser", "test@test.com", "password", null);
    setId(author, authorId);

    Message message = new Message(content, channel, author, Collections.emptyList());
    setId(message, messageId);

    Instant now = Instant.now();
    UserDto authorDto = new UserDto(authorId, author.getUsername(), author.getEmail(), null, true);

    MessageDto expectedDto = new MessageDto(
        messageId,
        now,
        now,
        content,
        channelId,
        authorDto,
        Collections.emptyList()
    );

    given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
    given(messageMapper.toDto(any(Message.class))).willReturn(expectedDto);

    // when
    MessageDto result = messageService.find(messageId);

    // then
    assertThat(result).isEqualTo(expectedDto);
  }

  @Test
  void find_실패_메시지_없음() {
    // given
    UUID messageId = UUID.randomUUID();
    given(messageRepository.findById(messageId)).willReturn(Optional.empty());

    // when, then
    MessageException exception = MessageExceptions.notFound(messageId);
    assertThatThrownBy(() -> messageService.find(messageId))
        .hasSameClassAs(exception);
  }

  @Test
  void findAllByChannelId_성공() {
    // given
    UUID channelId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    Instant createdAt = Instant.now();
    Pageable pageable = PageRequest.of(0, 10);

    Channel channel = new Channel(ChannelType.PUBLIC, "general", "General channel");
    setId(channel, channelId);

    User author = new User("testuser", "test@test.com", "password", null);
    setId(author, authorId);

    UserDto authorDto = new UserDto(authorId, author.getUsername(), author.getEmail(), null, true);

    MessageDto dto1 = new MessageDto(
        UUID.randomUUID(),
        createdAt.minusSeconds(10),
        createdAt.minusSeconds(10),
        "Message 1",
        channelId,
        authorDto,
        Collections.emptyList()
    );

    MessageDto dto2 = new MessageDto(
        UUID.randomUUID(),
        createdAt,
        createdAt,
        "Message 2",
        channelId,
        authorDto,
        Collections.emptyList()
    );

    List<MessageDto> messageDtos = Arrays.asList(dto1, dto2);
    SliceImpl<MessageDto> slice = new SliceImpl<>(messageDtos, pageable, false);

    PageResponse<MessageDto> expectedResponse = new PageResponse<>(
        messageDtos,       // content
        createdAt,         // nextCursor
        messageDtos.size(),// size
        false,             // hasNext
        null               // totalElements
    );

    Message mockMessage1 = mock(Message.class);
    Message mockMessage2 = mock(Message.class);
    given(messageRepository.findAllByChannelIdWithAuthor(channelId, createdAt, pageable))
        .willReturn(new SliceImpl<>(Arrays.asList(mockMessage1, mockMessage2), pageable, false));
    given(messageMapper.toDto(any(Message.class))).willReturn(dto1, dto2);
    given(pageResponseMapper.fromSlice(eq(slice), any(Instant.class))).willReturn(expectedResponse);

    // when
    PageResponse<MessageDto> result = messageService.findAllByChannelId(channelId, createdAt,
        pageable);

    // then
    assertThat(result).isEqualTo(expectedResponse);
  }

  @Test
  void update_성공() {
    // given
    UUID messageId = UUID.randomUUID();
    UUID channelId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    String oldContent = "Original content";
    String newContent = "Updated content";

    MessageUpdateRequest request = new MessageUpdateRequest(newContent);

    Channel channel = new Channel(ChannelType.PUBLIC, "general", "General channel");
    setId(channel, channelId);

    User author = new User("testuser", "test@test.com", "password", null);
    setId(author, authorId);

    Message message = new Message(oldContent, channel, author, Collections.emptyList());
    setId(message, messageId);

    Instant now = Instant.now();
    UserDto authorDto = new UserDto(authorId, author.getUsername(), author.getEmail(), null, true);

    MessageDto expectedDto = new MessageDto(
        messageId,
        now,
        now,
        newContent,
        channelId,
        authorDto,
        Collections.emptyList()
    );

    given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
    given(messageMapper.toDto(any(Message.class))).willReturn(expectedDto);

    // when
    MessageDto result = messageService.update(messageId, request);

    // then
    assertThat(result).isEqualTo(expectedDto);
    assertThat(message.getContent()).isEqualTo(newContent);
  }

  @Test
  void update_실패_메시지_없음() {
    // given
    UUID messageId = UUID.randomUUID();
    MessageUpdateRequest request = new MessageUpdateRequest("Updated content");

    given(messageRepository.findById(messageId)).willReturn(Optional.empty());

    // when, then
    MessageException exception = MessageExceptions.notFound(messageId);
    assertThatThrownBy(() -> messageService.update(messageId, request))
        .hasSameClassAs(exception);
  }

  @Test
  void delete_성공() {
    // given
    UUID messageId = UUID.randomUUID();
    given(messageRepository.existsById(messageId)).willReturn(true);

    // when
    messageService.delete(messageId);

    // then
    then(messageRepository).should(times(1)).deleteById(messageId);
  }

  @Test
  void delete_실패_메시지_없음() {
    // given
    UUID messageId = UUID.randomUUID();
    given(messageRepository.existsById(messageId)).willReturn(false);

    // when, then
    MessageException exception = MessageExceptions.notFound(messageId);
    assertThatThrownBy(() -> messageService.delete(messageId))
        .hasSameClassAs(exception);

    then(messageRepository).should(times(0)).deleteById(messageId);
  }

  private void setId(Object entity, UUID id) {
    try {
      if (entity instanceof BaseUpdatableEntity) {
        Field idField = entity.getClass().getSuperclass().getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
      } else {
        Field idField = entity.getClass().getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("엔티티 ID 설정 중 오류 발생", e);
    }
  }
}