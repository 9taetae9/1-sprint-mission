package com.sprint.mission.discodeit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.exception.channel.ChannelExceptions;
import com.sprint.mission.discodeit.exception.message.MessageExceptions;
import com.sprint.mission.discodeit.exception.user.UserExceptions;
import com.sprint.mission.discodeit.service.MessageService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private MessageService messageService;

  @Test
  void 메시지_생성_성공() throws Exception {
    // given
    UUID messageId = UUID.randomUUID();
    UUID channelId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();

    MessageCreateRequest request = new MessageCreateRequest(
        "Hello, this is a test message", channelId, authorId);

    String requestJson = objectMapper.writeValueAsString(request);

    MockMultipartFile messageCreateRequest = new MockMultipartFile(
        "messageCreateRequest",
        null,
        "application/json",
        requestJson.getBytes());

    // 첨부 파일 생성
    MockMultipartFile attachment = new MockMultipartFile(
        "attachments",
        "test.txt",
        "text/plain",
        "Test content".getBytes());

    UserDto author = new UserDto(
        authorId, "testuser", "test@example.com", null, true);

    MessageDto expectedResponse = new MessageDto(
        messageId,
        Instant.now(),
        Instant.now(),
        "Hello, this is a test message",
        channelId,
        author,
        new ArrayList<>()
    );

    when(messageService.create(any(MessageCreateRequest.class), anyList()))
        .thenReturn(expectedResponse);

    // when & then
    mockMvc.perform(multipart("/api/messages")
            .file(messageCreateRequest)
            .file(attachment)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(messageId.toString()))
        .andExpect(jsonPath("$.content").value("Hello, this is a test message"))
        .andExpect(jsonPath("$.channelId").value(channelId.toString()))
        .andExpect(jsonPath("$.author.id").value(authorId.toString()))
        .andExpect(jsonPath("$.author.username").value("testuser"));
  }

  @Test
  void 메시지_생성_실패_존재하지_않는_채널() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();

    MessageCreateRequest request = new MessageCreateRequest(
        "Hello, this is a test message", channelId, authorId);

    String requestJson = objectMapper.writeValueAsString(request);

    MockMultipartFile messageCreateRequest = new MockMultipartFile(
        "messageCreateRequest",
        null,
        "application/json",
        requestJson.getBytes());

    // 서비스 예외 설정
    when(messageService.create(any(MessageCreateRequest.class), anyList()))
        .thenThrow(ChannelExceptions.notFound(channelId));

    // when & then
    mockMvc.perform(multipart("/api/messages")
            .file(messageCreateRequest)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CHANNEL_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Channel with id " + channelId + " not found"))
        .andExpect(jsonPath("$.details.channelId").value(channelId.toString()));
  }

  @Test
  void 메시지_생성_실패_존재하지_않는_사용자() throws Exception {
    UUID channelId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();

    MessageCreateRequest request = new MessageCreateRequest(
        "Hello, this is a test message", channelId, authorId);

    String requestJson = objectMapper.writeValueAsString(request);

    MockMultipartFile messageCreateRequest = new MockMultipartFile(
        "messageCreateRequest",
        null,
        "application/json",
        requestJson.getBytes()
    );

    //서비스
    when(messageService.create(any(MessageCreateRequest.class), anyList()))
        .thenThrow(UserExceptions.notFound(authorId));

    //when & then
    mockMvc.perform(multipart("/api/messages")
            .file(messageCreateRequest)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("User with id " + authorId + " not found"))
        .andExpect(jsonPath("$.details.userId").value(authorId.toString()));

  }

  @Test
  void 메시지_생성_실패_유효성_검증() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();

    // 빈 메시지(유효성 검증 실패)
    MessageCreateRequest request = new MessageCreateRequest("", channelId, authorId);

    String requestJson = objectMapper.writeValueAsString(request);

    MockMultipartFile messageCreateRequest = new MockMultipartFile(
        "messageCreateRequest",
        null,
        "application/json",
        requestJson.getBytes());

    // when & then
    mockMvc.perform(multipart("/api/messages")
            .file(messageCreateRequest)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details").isNotEmpty());
  }

  @Test
  void 메시지_업데이트_성공() throws Exception {
    // given
    UUID messageId = UUID.randomUUID();
    UUID channelId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();

    MessageUpdateRequest request = new MessageUpdateRequest("Updated message content");

    UserDto author = new UserDto(
        authorId, "testuser", "test@example.com", null, true);

    MessageDto expectedResponse = new MessageDto(
        messageId,
        Instant.now(),
        Instant.now(),
        "Updated message content",
        channelId,
        author,
        new ArrayList<>()
    );

    when(messageService.update(eq(messageId), any(MessageUpdateRequest.class)))
        .thenReturn(expectedResponse);

    // when & then
    mockMvc.perform(patch("/api/messages/{messageId}", messageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(messageId.toString()))
        .andExpect(jsonPath("$.content").value("Updated message content"))
        .andExpect(jsonPath("$.channelId").value(channelId.toString()));
  }

  @Test
  void 메시지_업데이트_실패_존재하지_않는_메시지() throws Exception {
    // given
    UUID messageId = UUID.randomUUID();
    MessageUpdateRequest request = new MessageUpdateRequest("Updated message content");

    when(messageService.update(eq(messageId), any(MessageUpdateRequest.class)))
        .thenThrow(MessageExceptions.notFound(messageId));

    // when & then
    mockMvc.perform(patch("/api/messages/{messageId}", messageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("MESSAGE_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Message with id " + messageId + " not found"))
        .andExpect(jsonPath("$.details.messageId").value(messageId.toString()));
  }

  @Test
  void 메시지_삭제_성공() throws Exception {
    // given
    UUID messageId = UUID.randomUUID();

    doNothing().when(messageService).delete(messageId);

    // when & then
    mockMvc.perform(delete("/api/messages/{messageId}", messageId))
        .andExpect(status().isNoContent());
  }

  @Test
  void 메시지_삭제_실패_존재하지_않는_메시지() throws Exception {
    // given
    UUID messageId = UUID.randomUUID();

    doThrow(MessageExceptions.notFound(messageId))
        .when(messageService).delete(messageId);

    // when & then
    mockMvc.perform(delete("/api/messages/{messageId}", messageId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("MESSAGE_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Message with id " + messageId + " not found"))
        .andExpect(jsonPath("$.details.messageId").value(messageId.toString()));
  }

  @Test
  void 채널_메시지_조회_성공() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();
    UUID messageId1 = UUID.randomUUID();
    UUID messageId2 = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();

    UserDto author = new UserDto(
        authorId, "testuser", "test@example.com", null, true);

    List<MessageDto> messages = List.of(
        new MessageDto(
            messageId1,
            Instant.now(),
            Instant.now(),
            "Message 1",
            channelId,
            author,
            new ArrayList<>()
        ),
        new MessageDto(
            messageId2,
            Instant.now().minusSeconds(60),
            Instant.now().minusSeconds(60),
            "Message 2",
            channelId,
            author,
            new ArrayList<>()
        )
    );

    PageResponse<MessageDto> pageResponse = new PageResponse<>(
        messages,
        messages.get(1).createdAt(),
        10,
        false,
        2L
    );

    when(messageService.findAllByChannelId(eq(channelId), nullable(Instant.class),
        any(Pageable.class)))
        .thenReturn(pageResponse);
    // when & then
    mockMvc.perform(get("/api/messages")
            .param("channelId", channelId.toString())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].id").value(messageId1.toString()))
        .andExpect(jsonPath("$.content[0].content").value("Message 1"))
        .andExpect(jsonPath("$.content[1].id").value(messageId2.toString()))
        .andExpect(jsonPath("$.content[1].content").value("Message 2"))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void 채널_메시지_조회_실패_존재하지_않는_채널() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();

    when(messageService.findAllByChannelId(eq(channelId), nullable(Instant.class),
        any(Pageable.class)))
        .thenThrow(ChannelExceptions.notFound(channelId));

    // when & then
    mockMvc.perform(get("/api/messages")
            .param("channelId", channelId.toString())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CHANNEL_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Channel with id " + channelId + " not found"))
        .andExpect(jsonPath("$.details.channelId").value(channelId.toString()));
  }

  @Test
  void 커서_전달된_채널_메시지_조회_성공() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();
    UUID messageId1 = UUID.randomUUID();
    UUID messageId2 = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();

    UserDto author = new UserDto(
        authorId, "testuser", "test@example.com", null, true);

    //메시지 생성 - 커서 기준보다 이전에 생성된 메시지들
    List<MessageDto> messages = List.of(
        new MessageDto(
            messageId1,
            Instant.now().minusSeconds(120),
            Instant.now().minusSeconds(120),
            "Message 1",
            channelId,
            author,
            new ArrayList<>()
        ),
        new MessageDto(
            messageId2,
            Instant.now().minusSeconds(240),
            Instant.now().minusSeconds(240),
            "Message 2",
            channelId,
            author,
            new ArrayList<>()
        )
    );

    // 커서
    Instant cursor = Instant.now().minusSeconds(60);

    PageResponse<MessageDto> pageResponse = new PageResponse<>(
        messages,
        messages.get(1).createdAt(),
        10,
        false,
        2L
    );

    when(messageService.findAllByChannelId(eq(channelId), eq(cursor), any(Pageable.class)))
        .thenReturn(pageResponse);

    // when & then
    mockMvc.perform(get("/api/messages")
            .param("channelId", channelId.toString())
            .param("cursor", cursor.toString())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].id").value(messageId1.toString()))
        .andExpect(jsonPath("$.content[0].content").value("Message 1"))
        .andExpect(jsonPath("$.content[1].id").value(messageId2.toString()))
        .andExpect(jsonPath("$.content[1].content").value("Message 2"))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalElements").value(2));
  }
}