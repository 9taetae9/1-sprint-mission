package com.sprint.mission.discodeit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.exception.user.UserExceptions;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.UserStatusService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private UserStatusService userStatusService;

  @Test
  void 사용자_생성_성공() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UserCreateRequest request = new UserCreateRequest("testuser", "test@example.com", "password");
    UserDto expectedResponse = new UserDto(userId, "testuser", "test@example.com", null, false);

    // JSON 문자열로 변환
    String requestJson = objectMapper.writeValueAsString(request);

    // MockMultipartFile 생성
    MockMultipartFile userCreateRequest = new MockMultipartFile(
        "userCreateRequest",
        null,
        "application/json",
        requestJson.getBytes());

    // 프로필 이미지 생성 (빈 파일)
    MockMultipartFile profile = new MockMultipartFile(
        "profile",
        "profile.jpg",
        "image/jpeg",
        new byte[0]);

    when(userService.create(any(UserCreateRequest.class), any())).thenReturn(expectedResponse);

    // when & then
    mockMvc.perform(multipart("/api/users").file(userCreateRequest)
            .file(profile)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.username").value("testuser"))
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.online").value(false));
  }

  @Test
  void 사용자_생성_실패_이메일_중복() throws Exception {
    // given
    UserCreateRequest request = new UserCreateRequest("testuser", "duplicate@example.com",
        "password");

    String requestJson = objectMapper.writeValueAsString(request);

    MockMultipartFile userCreateRequest = new MockMultipartFile(
        "userCreateRequest",
        "",
        "application/json",
        requestJson.getBytes());

    // userService.create- 이메일 중복 예외
    when(userService.create(any(UserCreateRequest.class), any()))
        .thenThrow(UserExceptions.emailAlreadyExists("duplicate@example.com"));

    // when & then
    mockMvc.perform(multipart("/api/users")
            .file(userCreateRequest)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DUPLICATE_USER"))
        .andExpect(
            jsonPath("$.message").value("User with email duplicate@example.com already exists"));
  }

  @Test
  void 사용자_생성_실패_유효성_검증() throws Exception {
    // given
    // 올바르지 않은 이메일 형식
    UserCreateRequest request = new UserCreateRequest("", "invalid-email", "");

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
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details").isNotEmpty());
  }

  @Test
  void 사용자_상태_업데이트_실패_존재하지_않음() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UserStatusUpdateRequest request = new UserStatusUpdateRequest(Instant.now());

    when(userStatusService.updateByUserId(eq(userId), any(UserStatusUpdateRequest.class)))
        .thenThrow(UserExceptions.notFound(userId));

    // when & then
    mockMvc.perform(patch("/api/users/{userId}/userStatus", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("User with id " + userId + " not found"))
        .andExpect(jsonPath("$.details.userId").value(userId.toString()));
  }

  @Test
  void 모든_사용자_조회_성공() throws Exception {
    // given
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();

    BinaryContentDto profile = new BinaryContentDto(
        UUID.randomUUID(), "profile.jpg", 1024L, "image/jpeg");

    List<UserDto> users = List.of(
        new UserDto(userId1, "user1", "user1@example.com", null, true),
        new UserDto(userId2, "user2", "user2@example.com", profile, false)
    );

    when(userService.findAll()).thenReturn(users);

    // when & then
    mockMvc.perform(get("/api/users")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(userId1.toString()))
        .andExpect(jsonPath("$[0].username").value("user1"))
        .andExpect(jsonPath("$[0].profile").isEmpty())
        .andExpect(jsonPath("$[0].online").value(true))
        .andExpect(jsonPath("$[1].id").value(userId2.toString()))
        .andExpect(jsonPath("$[1].username").value("user2"))
        .andExpect(jsonPath("$[1].profile").isNotEmpty())
        .andExpect(jsonPath("$[1].profile.fileName").value("profile.jpg"))
        .andExpect(jsonPath("$[1].online").value(false));
  }

  @Test
  void 사용자_삭제_실패_존재하지_않음() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    // userService.delete 호출 시 예외 발생하도록 설정
    doThrow(UserExceptions.notFound(userId))
        .when(userService).delete(userId);

    // when & then
    mockMvc.perform(delete("/api/users/{userId}", userId)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("User with id " + userId + " not found"))
        .andExpect(jsonPath("$.details.userId").value(userId.toString()));
  }
}