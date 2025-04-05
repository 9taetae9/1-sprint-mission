package com.sprint.mission.discodeit.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    entityManager.flush();
  }

  @Test
  void 사용자_생성_성공() throws Exception {
    // given
    UserCreateRequest request = new UserCreateRequest("testuser", "test@example.com", "password");
    String requestJson = objectMapper.writeValueAsString(request);

    MockMultipartFile userCreateRequest = new MockMultipartFile(
        "userCreateRequest",
        null,
        "application/json",
        requestJson.getBytes());

    // when & then
    mockMvc.perform(multipart("/api/users")
            .file(userCreateRequest)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("testuser"))
        .andExpect(jsonPath("$.email").value("test@example.com"));
  }

  @Test
  void 사용자_생성_실패_이메일_중복() throws Exception {
    // given - 기존 사용자 생성
    createTestUser("existinguser", "duplicate@example.com", "password");
    // 동일 이메일로 새 사용자 생성 시도
    UserCreateRequest request = new UserCreateRequest("newuser", "duplicate@example.com",
        "password");
    String requestJson = objectMapper.writeValueAsString(request);

    MockMultipartFile userCreateRequest = new MockMultipartFile(
        "userCreateRequest",
        null,
        "application/json",
        requestJson.getBytes());

    // when & then
    mockMvc.perform(multipart("/api/users")
            .file(userCreateRequest)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DUPLICATE_USER"));
  }

  @Test
  void 사용자_목록_조회_성공() throws Exception {
    // given
    createTestUser("user1", "user1@example.com", "password");
    createTestUser("user2", "user2@example.com", "password");

    entityManager.flush();
    entityManager.clear();

    // when & then
    mockMvc.perform(get("/api/users")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2));
  }


  @Test
  void 사용자_상태_업데이트_성공() throws Exception {
    // given: 사용자 생성 후 초기 상태 저장
    User user = createTestUser("updatetest", "update@example.com", "password");

    UserStatusUpdateRequest updateRequest = new UserStatusUpdateRequest(Instant.now());
    String updateJson = objectMapper.writeValueAsString(updateRequest);

    mockMvc.perform(patch("/api/users/{userId}/userStatus", user.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(updateJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lastActiveAt").exists());
  }

  @Test
  void 사용자_삭제_성공() throws Exception {
    // given
    User user = createTestUser("deleteuser", "delete@example.com", "password");
    UUID userId = user.getId();

    entityManager.flush();

    // when: 삭제 API 호출
    mockMvc.perform(delete("/api/users/{userId}", userId)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    // 현재 트랜잭션 커밋 후, 새 트랜잭션에서 검증
    TestTransaction.flagForCommit();
    TestTransaction.end();
    TestTransaction.start();
    assertFalse(userRepository.existsById(userId));
  }

  @Test
  void 사용자_삭제_실패_존재하지_않는_사용자() throws Exception {
    // given
    UUID nonExistentUserId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/users/{userId}", nonExistentUserId)
            .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
  }

  private User createTestUser(String username, String email, String password) {
    User user = new User(username, email, password, null);
    new UserStatus(user, Instant.now());
    userRepository.save(user);
    entityManager.flush();
    return user;
  }
}
