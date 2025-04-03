package com.sprint.mission.discodeit.service.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.base.BaseUpdatableEntity;
import com.sprint.mission.discodeit.exception.user.UserException;
import com.sprint.mission.discodeit.exception.user.UserExceptions;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BasicUserServiceTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private UserMapper userMapper;
  @Mock
  private BinaryContentRepository binaryContentRepository;
  @Mock
  private BinaryContentStorage binaryContentStorage;

  @InjectMocks
  private BasicUserService userService;


  @Test
  void create_성공_프로필_없음() {
    //given
    UserCreateRequest request = new UserCreateRequest("testuser", "test@test.com",
        "password123");
    Optional<BinaryContentCreateRequest> profileRequest = Optional.empty();

    User requestedUser = new User(request.username(), request.email(), request.password(), null);
    UUID userId = UUID.randomUUID();
    setId(requestedUser, userId);

    UserDto expectedDto = new UserDto(userId, request.username(), request.email(), null, true);

    given(userRepository.existsByEmail(request.email())).willReturn(false);
    given(userRepository.existsByUsername(request.username())).willReturn(false);

    given(userRepository.save(any(User.class))).willReturn(requestedUser);
    given(userMapper.toDto(any(User.class))).willReturn(expectedDto);

    //when
    UserDto result = userService.create(request, profileRequest);

    //then
    assertThat(result).isEqualTo(expectedDto);
    then(userRepository).should(times(1)).save(any(User.class));

  }

  @Test
  void create_성공_프로필_있음() {
    //given
    UserCreateRequest request = new UserCreateRequest("testuser", "test@test.com",
        "password123");
    BinaryContentCreateRequest profileContent = new BinaryContentCreateRequest(
        "profile.jpg", "image/jpeg", new byte[]{1, 2, 3});
    Optional<BinaryContentCreateRequest> profileRequest = Optional.of(profileContent);

    BinaryContent binaryContent = new BinaryContent(profileContent.fileName(),
        (long) profileContent.bytes().length, "image/jpeg");
    UUID profileId = UUID.randomUUID();

    User requestedUser = new User(request.username(), request.email(), request.password(),
        binaryContent);
    UUID userId = UUID.randomUUID();
    setId(requestedUser, userId);

    UserDto expectedDto = new UserDto(userId, request.username(), request.email(), null, true);

    given(userRepository.existsByEmail(request.email())).willReturn(false);
    given(userRepository.existsByUsername(request.username())).willReturn(false);

//    given(binaryContentRepository.save(any(BinaryContent.class))).willReturn(binaryContent);
    given(binaryContentRepository.save(any(BinaryContent.class))).willAnswer(invocation -> {
      BinaryContent savedContent = invocation.getArgument(0);
      setId(savedContent, profileId);
      return savedContent;
    });

    given(userRepository.save(any(User.class))).willReturn(requestedUser);
    given(userMapper.toDto(any(User.class))).willReturn(expectedDto);

    //when
    UserDto result = userService.create(request, profileRequest);

    //then
    assertThat(result).isEqualTo(expectedDto);
    then(binaryContentRepository).should(times(1)).save(any(BinaryContent.class));
    then(binaryContentStorage).should(times(1)).put(profileId, profileContent.bytes());
    then(userRepository).should(times(1)).save(any(User.class));

  }

  @Test
  void create_실패_사용자명_중복() {
    //given
    UserCreateRequest request = new UserCreateRequest("testuser", "test@test.com",
        "password123");
    Optional<BinaryContentCreateRequest> profileRequest = Optional.empty();

    //리포지토리 동작 mocking
    given(userRepository.existsByEmail(request.email())).willReturn(false);
    given(userRepository.existsByUsername(request.username())).willReturn(true);

    //팩토리 메서드가 반환하는 구체적 예외 타입 검증(방법1)
    UserException exception = UserExceptions.userNameAlreadyExists(request.username());
    assertThatThrownBy(() -> userService.create(request, profileRequest))
        .hasSameClassAs(exception);

    // 에러코드로 간접 검증(방법2)
//    assertThatThrownBy(() -> userService.create(request, profileRequest))
//        .isInstanceOf(UserException.class)
//        .satisfies(ex -> {
//          UserException userException = (UserException) ex;
//          assertThat(userException.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_USER);
//        });

    then(userRepository).should(times(0)).save(any(User.class));
  }

  @Test
  void update_성공() {
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("newusername", "newemail@test.com",
        "newpassword123");
    Optional<BinaryContentCreateRequest> profileRequest = Optional.empty();

    User presentUser = new User("testuser", "test@test.com", "password123", null);
    setId(presentUser, userId);

    UserDto expectedDto = new UserDto(userId, request.newUsername(), request.newEmail(), null,
        true);

    given(userRepository.findById(userId)).willReturn(Optional.of(presentUser));
    given(userRepository.existsByEmail(request.newEmail())).willReturn(false); //디폴트 false
    given(userRepository.existsByUsername(request.newUsername())).willReturn(false);
    given(userMapper.toDto(presentUser)).willReturn(expectedDto);

    //when
    UserDto result = userService.update(userId, request, profileRequest);

    //then
    assertThat(result).isEqualTo(expectedDto);
  }

  @Test
  void update_실패_존재하지_않는_사용자() {
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("newusername", "newemail@test.com",
        "newpassword123");
    Optional<BinaryContentCreateRequest> profileRequest = Optional.empty();

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    //when, then
    UserException userException = UserExceptions.notFound(userId);
    assertThatThrownBy(() -> userService.update(userId, request, profileRequest))
        .hasSameClassAs(userException);
  }

  @Test
  void delete_성공() {
    UUID userId = UUID.randomUUID();
    given(userRepository.existsById(userId)).willReturn(true);

    userService.delete(userId);

    then(userRepository).should(times(1)).deleteById(userId);
  }

  @Test
  void delete_실패_존재하지_않는_사용자() {
    UUID userId = UUID.randomUUID();
    given(userRepository.existsById(userId)).willReturn(false);

    UserException userException = UserExceptions.notFound(userId);
    assertThatThrownBy(() -> userService.delete(userId))
        .hasSameClassAs(userException);

    then(userRepository).should(times(0)).deleteById(userId);
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