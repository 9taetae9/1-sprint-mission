package com.sprint.mission.discodeit.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class MessageApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ChannelRepository channelRepository;

  @Autowired
  private MessageRepository messageRepository;

  @Autowired
  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    messageRepository.deleteAll();
    channelRepository.deleteAll();
    userRepository.deleteAll();
    entityManager.flush();
  }

  @Test
  void 메시지_생성_성공() throws Exception {
    // given
    User author = createTestUser("testuser", "test@example.com");
    Channel channel = createTestChannel(ChannelType.PUBLIC, "test-channel");

    // when & then
    MockMultipartFile jsonPart = new MockMultipartFile(
        "messageCreateRequest",
        null,
        "application/json",
        objectMapper.writeValueAsBytes(
            new MessageCreateRequest("Test content", channel.getId(), author.getId())
        )
    );

    mockMvc.perform(multipart("/api/messages")
            .file(jsonPart)
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.content").value("Test content"))
        .andExpect(jsonPath("$.channelId").value(channel.getId().toString()))
        .andExpect(jsonPath("$.author.id").value(author.getId().toString()));
  }

  @Test
  void 메시지_업데이트_성공() throws Exception {
    // given: 사용자, 채널 생성
    User author = new User("updateuser", "update@example.com", "password", null);
    new UserStatus(author, Instant.now());
    userRepository.save(author);

    Channel channel = new Channel(ChannelType.PUBLIC, "update-channel", "Update channel");
    channelRepository.save(channel);

    entityManager.flush();
    entityManager.clear();

    // 메시지 생성 API 호출
    MessageCreateRequest createRequest = new MessageCreateRequest(
        "Original message", channel.getId(), author.getId());
    String createRequestJson = objectMapper.writeValueAsString(createRequest);

    MockMultipartFile messageCreateRequest = new MockMultipartFile(
        "messageCreateRequest",
        null,
        "application/json",
        createRequestJson.getBytes());

    MvcResult result = mockMvc.perform(
            multipart(
                "/api/messages")
                .file(messageCreateRequest)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().isCreated())
        .andReturn();

    String createResponse = result.getResponse().getContentAsString();
    UUID messageId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());

    // 메시지 업데이트 요청
    MessageUpdateRequest updateRequest = new MessageUpdateRequest("Updated message content");

    // when & then
    mockMvc.perform(patch("/api/messages/{messageId}", messageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.id")
                .value(messageId.toString()))
        .andExpect(
            jsonPath("$.content")
                .value("Updated message content"));
  }

  @Test
  void 메시지_삭제_성공() throws Exception {
    // given
    User author = createTestUser("testuser", "test@example.com");
    Channel channel = createTestChannel(ChannelType.PUBLIC, "test-channel");
    UUID messageId = createTestMessage(channel.getId(), author.getId(), "To be deleted");

    // when
    mockMvc.perform(delete("/api/messages/{messageId}", messageId))
        .andExpect(status().isNoContent());

    // then
    TestTransaction.flagForCommit();
    TestTransaction.end();
    TestTransaction.start();
    assertFalse(messageRepository.existsById(messageId));
  }

  @Test
  void 존재하지_않는_메시지_삭제_실패() throws Exception {
    // given
    UUID nonExistentId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/messages/{messageId}", nonExistentId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("MESSAGE_NOT_FOUND"));
  }


  @Test
  void 채널_메시지_조회_성공() throws Exception {
    // given: 사용자와 채널 생성
    User author = new User("listuser", "list@example.com", "password", null);
    new UserStatus(author, Instant.now());
    userRepository.save(author);

    Channel channel = new Channel(ChannelType.PUBLIC, "list-channel", "List channel");
    channelRepository.save(channel);

    entityManager.flush();
    entityManager.clear();

    // 메시지 생성
    for (int i = 0; i < 3; i++) {
      MessageCreateRequest createRequest = new MessageCreateRequest(
          "Message " + i, channel.getId(), author.getId());
      String createRequestJson = objectMapper.writeValueAsString(createRequest);

      MockMultipartFile messageCreateRequest = new MockMultipartFile(
          "messageCreateRequest",
          null,
          "application/json",
          createRequestJson.getBytes());

      mockMvc.perform(multipart(
              "/api/messages")
              .file(messageCreateRequest)
              .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
          .andExpect(status().isCreated());
    }

    // when & then
    mockMvc.perform(get("/api/messages")
            .param("channelId", channel.getId().toString())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.content")
                .isArray())
        .andExpect(jsonPath(
            "$.content.length()").value(3));
  }

  @Test
  void 채널_메시지_페이징_조회_성공() throws Exception {
    // given
    User author = createTestUser("testuser", "test@example.com");
    Channel channel = createTestChannel(ChannelType.PUBLIC, "test-channel");

    // 메시지 15개 생성
    for (int i = 1; i <= 15; i++) {
      createTestMessage(channel.getId(), author.getId(), "Message " + i);
    }

    // when & then
    mockMvc.perform(get("/api/messages")
            .param("channelId", channel.getId().toString())
            .param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(5))
        .andExpect(jsonPath("$.hasNext").value(true));
  }

  @Test
  void 유효하지_않은_메시지_생성_요청() throws Exception {
    // given
    User author = createTestUser("testuser", "test@example.com");
    Channel channel = createTestChannel(ChannelType.PUBLIC, "test-channel");

    // 내용없는 메시지 생성 시도
    MessageCreateRequest invalidRequest = new MessageCreateRequest("", channel.getId(),
        author.getId());

    MockMultipartFile jsonPart = new MockMultipartFile(
        "messageCreateRequest",
        null,
        "application/json",
        objectMapper.writeValueAsBytes(invalidRequest)
    );

    // when & then
    mockMvc.perform(multipart("/api/messages")
            .file(jsonPart)
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  private User createTestUser(String username, String email) {
    User user = new User(username, email, "password", null);
    new UserStatus((user), Instant.now());
    userRepository.save(user);
    entityManager.flush();
    return user;
  }

  private Channel createTestChannel(ChannelType type, String name) {
    Channel channel = new Channel(type, name, "Test Channel");
    channelRepository.save(channel);
    entityManager.flush();
    return channel;
  }

  private UUID createTestMessage(UUID channelId, UUID authorId, String content) throws Exception {
    MessageCreateRequest request = new MessageCreateRequest(content, channelId, authorId);
    MockMultipartFile jsonPart = new MockMultipartFile(
        "messageCreateRequest",
        null,
        "application/json",
        objectMapper.writeValueAsBytes(request)
    );

    MvcResult result = mockMvc.perform(multipart("/api/messages")
            .file(jsonPart)
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isCreated())
        .andReturn();

    return UUID.fromString(
        objectMapper.readTree(result.getResponse().getContentAsString())
            .get("id").asText()
    );
  }
}
