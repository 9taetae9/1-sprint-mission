package com.sprint.mission.discodeit.service.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.base.BaseUpdatableEntity;
import com.sprint.mission.discodeit.exception.channel.ChannelException;
import com.sprint.mission.discodeit.exception.channel.ChannelExceptions;
import com.sprint.mission.discodeit.mapper.ChannelMapper;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BasicChannelServiceTest {

  @Mock
  private ChannelRepository channelRepository;
  @Mock
  private ReadStatusRepository readStatusRepository;
  @Mock
  private MessageRepository messageRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private ChannelMapper channelMapper;

  @InjectMocks
  private BasicChannelService channelService;

  @Test
  void create_공개채널_성공() {
    // given
    PublicChannelCreateRequest request = new PublicChannelCreateRequest("general",
        "General discussion channel");

    Channel savedChannel = new Channel(ChannelType.PUBLIC, request.name(), request.description());
    UUID channelId = UUID.randomUUID();
    setId(savedChannel, channelId);

    // ChannelDto를 생성하는 대신 Mock 응답을 설정
    ChannelDto expectedDto = mockChannelDto();

    given(channelRepository.save(any(Channel.class))).willReturn(savedChannel);
    given(channelMapper.toDto(any(Channel.class))).willReturn(expectedDto);

    // when
    ChannelDto result = channelService.create(request);

    // then
    assertThat(result).isEqualTo(expectedDto);
    then(channelRepository).should(times(1)).save(any(Channel.class));
  }

  @Test
  void create_비공개채널_성공() {
    // given
    UUID user1Id = UUID.randomUUID();
    UUID user2Id = UUID.randomUUID();
    List<UUID> participantIds = Arrays.asList(user1Id, user2Id);

    PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(participantIds);

    Channel savedChannel = new Channel(ChannelType.PRIVATE, null, null);
    UUID channelId = UUID.randomUUID();
    setId(savedChannel, channelId);

    User user1 = new User("user1", "user1@example.com", "password", null);
    setId(user1, user1Id);
    User user2 = new User("user2", "user2@example.com", "password", null);
    setId(user2, user2Id);

    List<User> users = Arrays.asList(user1, user2);

    // ChannelDto를 생성하는 대신 Mock 응답을 설정
    ChannelDto expectedDto = mockChannelDto();

    given(channelRepository.save(any(Channel.class))).willReturn(savedChannel);
    given(userRepository.findAllById(participantIds)).willReturn(users);
    given(channelMapper.toDto(any(Channel.class))).willReturn(expectedDto);

    // when
    ChannelDto result = channelService.create(request);

    // then
    assertThat(result).isEqualTo(expectedDto);
    then(channelRepository).should(times(1)).save(any(Channel.class));
    then(readStatusRepository).should(times(1)).saveAll(any());
  }

  @Test
  void create_비공개채널_일부사용자_존재하지_않음() {
    // given
    UUID user1Id = UUID.randomUUID();
    UUID user2Id = UUID.randomUUID();
    List<UUID> participantIds = Arrays.asList(user1Id, user2Id);

    PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(participantIds);

    Channel savedChannel = new Channel(ChannelType.PRIVATE, null, null);
    UUID channelId = UUID.randomUUID();
    setId(savedChannel, channelId);

    User user1 = new User("user1", "user1@example.com", "password", null);
    setId(user1, user1Id);

    List<User> users = Arrays.asList(user1); // 요청된 참가자 중 한 명만 존재

    ChannelDto expectedDto = mockChannelDto();

    given(channelRepository.save(any(Channel.class))).willReturn(savedChannel);
    given(userRepository.findAllById(participantIds)).willReturn(users);
    given(channelMapper.toDto(any(Channel.class))).willReturn(expectedDto);

    // when
    ChannelDto result = channelService.create(request);

    // then
    assertThat(result).isEqualTo(expectedDto);
    then(channelRepository).should(times(1)).save(any(Channel.class));
    then(readStatusRepository).should(times(1)).saveAll(any());
  }

  @Test
  void find_성공() {
    // given
    UUID channelId = UUID.randomUUID();
    Channel channel = new Channel(ChannelType.PUBLIC, "일반", "개발-자료 채널");
    setId(channel, channelId);

    ChannelDto expectedDto = mockChannelDto();

    given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));
    given(channelMapper.toDto(any(Channel.class))).willReturn(expectedDto);

    // when
    ChannelDto result = channelService.find(channelId);

    // then
    assertThat(result).isEqualTo(expectedDto);
  }

  @Test
  void find_실패_존재하지_않는_채널() {
    // given
    UUID channelId = UUID.randomUUID();
    given(channelRepository.findById(channelId)).willReturn(Optional.empty());

    // when, then
    ChannelException channelException = ChannelExceptions.notFound(channelId);
    assertThatThrownBy(() -> channelService.find(channelId))
        .hasSameClassAs(channelException);
  }

  @Test
  void findAllByUserId_성공() {
    // given
    UUID userId = UUID.randomUUID();

    UUID publicChannelId = UUID.randomUUID();
    Channel publicChannel = new Channel(ChannelType.PUBLIC, "general", "announcement channel");
    setId(publicChannel, publicChannelId);

    UUID privateChannelId = UUID.randomUUID();
    Channel privateChannel = new Channel(ChannelType.PRIVATE, null, null);
    setId(privateChannel, privateChannelId);

    User user = new User("username", "email@example.com", "password", null);
    setId(user, userId);

    ReadStatus readStatus = new ReadStatus(user, privateChannel, Instant.now());

    List<ReadStatus> readStatuses = List.of(readStatus);
    List<Channel> channels = List.of(publicChannel, privateChannel);

    // Mock ChannelDto 응답 설정
    ChannelDto publicChannelDto = mockChannelDto();
    ChannelDto privateChannelDto = mockChannelDto();

    given(readStatusRepository.findAllByUserId(userId)).willReturn(readStatuses);
    given(channelRepository.findAllByTypeOrIdIn(ChannelType.PUBLIC,
        List.of(privateChannelId))).willReturn(channels);

    // 순차적으로 반환하도록 설정
    given(channelMapper.toDto(channels.get(0))).willReturn(publicChannelDto);
    given(channelMapper.toDto(channels.get(1))).willReturn(privateChannelDto);

    // when
    List<ChannelDto> result = channelService.findAllByUserId(userId);

    // then
    assertThat(result).hasSize(2);
    assertThat(result).contains(publicChannelDto, privateChannelDto);
  }

  @Test
  void update_성공() {
    // given
    UUID channelId = UUID.randomUUID();
    PublicChannelUpdateRequest request = new PublicChannelUpdateRequest("new general",
        "Updated general channel");

    Channel channel = new Channel(ChannelType.PUBLIC, "general", "discussion channel");
    setId(channel, channelId);

    ChannelDto expectedDto = mockChannelDto();

    given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));
    given(channelMapper.toDto(any(Channel.class))).willReturn(expectedDto);

    // when
    ChannelDto result = channelService.update(channelId, request);

    // then
    assertThat(result).isEqualTo(expectedDto);
    assertThat(channel.getName()).isEqualTo(request.newName());
    assertThat(channel.getDescription()).isEqualTo(request.newDescription());
  }

  @Test
  void update_실패_존재하지_않는_채널() {
    // given
    UUID channelId = UUID.randomUUID();
    PublicChannelUpdateRequest request = new PublicChannelUpdateRequest("new name",
        "new description");

    given(channelRepository.findById(channelId)).willReturn(Optional.empty());

    // when, then
    ChannelException channelException = ChannelExceptions.notFound(channelId);
    assertThatThrownBy(() -> channelService.update(channelId, request))
        .hasSameClassAs(channelException);
  }

  @Test
  void update_실패_비공개채널_수정() {
    // given
    UUID channelId = UUID.randomUUID();
    PublicChannelUpdateRequest request = new PublicChannelUpdateRequest("new general",
        "Updated general channel");

    Channel channel = new Channel(ChannelType.PRIVATE, null, null);
    setId(channel, channelId);

    given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));

    // when, then
    ChannelException channelException = ChannelExceptions.privateChannelUpdate(channelId);
    assertThatThrownBy(() -> channelService.update(channelId, request))
        .hasSameClassAs(channelException);
  }

  @Test
  void delete_성공() {
    // given
    UUID channelId = UUID.randomUUID();
    given(channelRepository.existsById(channelId)).willReturn(true);
    given(messageRepository.deleteAllByChannelId(channelId)).willReturn(5); // message 5개 삭제
    given(readStatusRepository.deleteAllByChannelId(channelId)).willReturn(3); // read status 3개 삭제

    // when
    channelService.delete(channelId);

    // then
    then(messageRepository).should(times(1)).deleteAllByChannelId(channelId);
    then(readStatusRepository).should(times(1)).deleteAllByChannelId(channelId);
    then(channelRepository).should(times(1)).deleteById(channelId);
  }

  @Test
  void delete_실패_존재하지_않는_채널() {
    // given
    UUID channelId = UUID.randomUUID();
    given(channelRepository.existsById(channelId)).willReturn(false);

    // when, then
    ChannelException channelException = ChannelExceptions.notFound(channelId);
    assertThatThrownBy(() -> channelService.delete(channelId))
        .hasSameClassAs(channelException);

    then(messageRepository).should(times(0)).deleteAllByChannelId(channelId);
    then(readStatusRepository).should(times(0)).deleteAllByChannelId(channelId);
    then(channelRepository).should(times(0)).deleteById(channelId);
  }

  // ChannelDto mock 객체 생성
  private ChannelDto mockChannelDto() {
    UUID id = UUID.randomUUID();
    return new ChannelDto(
        id,
        ChannelType.PUBLIC,
        "mockChannelName",
        "mockChannelDescription",
        List.of(),
        Instant.now()
    );
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