package com.sprint.mission.discodeit.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ChannelApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ChannelRepository channelRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ReadStatusRepository readStatusRepository;

  @Autowired
  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    channelRepository.deleteAll();
    userRepository.deleteAll();
    entityManager.flush();
  }

  @Test
  void 공개_채널_생성_성공() throws Exception {
    // given
    PublicChannelCreateRequest request = new PublicChannelCreateRequest(
        "test-channel", "This is a test channel");

    // when & then
    mockMvc.perform(post("/api/channels/public")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("test-channel"))
        .andExpect(jsonPath("$.description").value("This is a test channel"))
        .andExpect(jsonPath("$.type").value("PUBLIC"));
  }


  @Test
  void 비공개_채널_생성_성공() throws Exception {
    // given - 참여자 사용자 생성
    User user1 = createTestUser("user1", "user1@example.com", "password");
    User user2 = createTestUser("user2", "user2@example.com", "password");

    PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(
        List.of(user1.getId(), user2.getId()));

    // when
    String response = mockMvc.perform(post("/api/channels/private")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("PRIVATE"))
        .andReturn().getResponse().getContentAsString();

    // then - DB에서 참여자 수 검증
    UUID channelId = UUID.fromString(objectMapper.readTree(response).get("id").asText());
    List<ReadStatus> readStatuses = readStatusRepository.findAllByChannelIdWithUser(channelId);
    assertEquals(2, readStatuses.size(), "참여자 수는 2명이어야 합니다.");
  }


  @Test
  void 비공개_채널_생성_실패_참여자_없음() throws Exception {
    // given
    PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(List.of());

    // when & then: VALIDATION_ERROR로 응답 확인
    mockMvc.perform(post("/api/channels/private")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.code").value("VALIDATION_ERROR"));  // INVALID_INPUT → VALIDATION_ERROR 수정
  }

  @Test
  void 채널_목록_조회_성공() throws Exception {
    // given
    User user = createTestUser("testuser", "test@example.com", "password");
    createTestChannel(ChannelType.PUBLIC, "channel1", "Channel 1 description");
    createTestChannel(ChannelType.PUBLIC, "channel2", "Channel 2 description");

    // when & then
    mockMvc.perform(get("/api/channels")
            .param("userId", user.getId().toString())
            .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void 채널_업데이트_성공() throws Exception {
    // given
    Channel channel = createTestChannel(ChannelType.PUBLIC, "old-name", "Old description");
    UUID channelId = channel.getId();
    entityManager.flush();

    PublicChannelUpdateRequest request = new PublicChannelUpdateRequest(
        "new-name", "New description");

    // when & then
    mockMvc.perform(patch("/api/channels/{channelId}", channelId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("new-name"))
        .andExpect(jsonPath("$.description").value("New description"));
  }

  @Test
  void 채널_업데이트_실패_존재하지_않는_채널() throws Exception {
    // given
    UUID nonExistentChannelId = UUID.randomUUID();
    PublicChannelUpdateRequest request = new PublicChannelUpdateRequest(
        "new-name", "New description");

    // when & then
    mockMvc.perform(patch("/api/channels/{channelId}", nonExistentChannelId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CHANNEL_NOT_FOUND"));
  }

  @Test
  void 채널_삭제_성공() throws Exception {
    // given
    Channel channel = createTestChannel(ChannelType.PUBLIC, "delete-channel",
        "Channel to be deleted");
    UUID channelId = channel.getId();
    entityManager.flush();

    // when
    mockMvc.perform(delete("/api/channels/{channelId}", channelId)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    // 현재 트랜잭션 커밋 후, 새 트랜잭션에서 검증
    TestTransaction.flagForCommit();
    TestTransaction.end();
    TestTransaction.start();
    assertFalse(channelRepository.existsById(channelId));
  }

  private User createTestUser(String username, String email, String password) {
    User user = new User(username, email, password, null);
    new UserStatus(user, Instant.now());
    userRepository.save(user);
    entityManager.flush();
    return user;
  }

  private Channel createTestChannel(ChannelType type, String name, String description) {
    Channel channel = new Channel(type, name, description);
    channelRepository.save(channel);
    entityManager.flush();
    return channel;
  }
}
