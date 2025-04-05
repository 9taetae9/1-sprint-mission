package com.sprint.mission.discodeit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.exception.channel.ChannelExceptions;
import com.sprint.mission.discodeit.service.ChannelService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChannelController.class)
class ChannelControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ChannelService channelService;

  @Test
  void 공개_채널_생성_성공() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();
    PublicChannelCreateRequest request = new PublicChannelCreateRequest(
        "test-channel", "This is a test channel");

    ChannelDto expectedResponse = new ChannelDto(
        channelId,
        ChannelType.PUBLIC,
        "test-channel",
        "This is a test channel",
        new ArrayList<>(),
        Instant.now());

    when(channelService.create(any(PublicChannelCreateRequest.class))).thenReturn(expectedResponse);

    // when & then
    mockMvc.perform(post("/api/channels/public")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(channelId.toString()))
        .andExpect(jsonPath("$.type").value("PUBLIC"))
        .andExpect(jsonPath("$.name").value("test-channel"))
        .andExpect(jsonPath("$.description").value("This is a test channel"));
  }

  @Test
  void 비공개_채널_생성_성공() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();
    UUID user1Id = UUID.randomUUID();
    UUID user2Id = UUID.randomUUID();

    PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(
        List.of(user1Id, user2Id));

    List<UserDto> participants = List.of(
        new UserDto(user1Id, "user1", "user1@example.com", null, true),
        new UserDto(user2Id, "user2", "user2@example.com", null, false)
    );

    ChannelDto expectedResponse = new ChannelDto(
        channelId,
        ChannelType.PRIVATE,
        null,
        null,
        participants,
        Instant.now());

    when(channelService.create(any(PrivateChannelCreateRequest.class))).thenReturn(
        expectedResponse);

    // when & then
    mockMvc.perform(post("/api/channels/private")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(channelId.toString()))
        .andExpect(jsonPath("$.type").value("PRIVATE"))
        .andExpect(jsonPath("$.participants").isArray())
        .andExpect(jsonPath("$.participants.length()").value(2))
        .andExpect(jsonPath("$.participants[0].id").value(user1Id.toString()))
        .andExpect(jsonPath("$.participants[1].id").value(user2Id.toString()));
  }

  @Test
  void 채널_업데이트_실패_비공개_채널_업데이트_시도() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();
    PublicChannelUpdateRequest request = new PublicChannelUpdateRequest(
        "updated-channel", "Updated description");

    when(channelService.update(eq(channelId), any(PublicChannelUpdateRequest.class)))
        .thenThrow(ChannelExceptions.privateChannelUpdate(channelId));

    // when & then
    mockMvc.perform(patch("/api/channels/{channelId}", channelId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PRIVATE_CHANNEL_UPDATE"))
        .andExpect(jsonPath("$.message").value(
            "Private channel with id " + channelId + " cannot be updated"))
        .andExpect(jsonPath("$.details.channelId").value(channelId.toString()));
  }

  @Test
  @DisplayName("채널 삭제 성공")
  void 채널_삭제_성공() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();

    doNothing().when(channelService).delete(channelId);

    // when & then
    mockMvc.perform(delete("/api/channels/{channelId}", channelId))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("채널 삭제 실패 - 존재하지 않는 채널")
  void 채널_삭제_실패_존재하지_않는_채널() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();

    doThrow(ChannelExceptions.notFound(channelId))
        .when(channelService).delete(channelId);

    // when & then
    mockMvc.perform(delete("/api/channels/{channelId}", channelId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CHANNEL_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Channel with id " + channelId + " not found"))
        .andExpect(jsonPath("$.details.channelId").value(channelId.toString()));
  }

  @Test
  void 사용자별_채널_목록_조회_성공() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID channel1Id = UUID.randomUUID();
    UUID channel2Id = UUID.randomUUID();

    List<ChannelDto> channels = List.of(
        new ChannelDto(channel1Id, ChannelType.PUBLIC, "public-channel", "Description",
            new ArrayList<>(), Instant.now()),
        new ChannelDto(channel2Id, ChannelType.PRIVATE, null, null, new ArrayList<>(),
            Instant.now())
    );

    when(channelService.findAllByUserId(userId)).thenReturn(channels);

    // when & then
    mockMvc.perform(get("/api/channels")
            .param("userId", userId.toString())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(channel1Id.toString()))
        .andExpect(jsonPath("$[0].type").value("PUBLIC"))
        .andExpect(jsonPath("$[0].name").value("public-channel"))
        .andExpect(jsonPath("$[1].id").value(channel2Id.toString()))
        .andExpect(jsonPath("$[1].type").value("PRIVATE"));
  }
}