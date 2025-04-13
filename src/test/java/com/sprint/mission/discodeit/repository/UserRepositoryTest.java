package com.sprint.mission.discodeit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@EnableJpaAuditing
@ActiveProfiles("test")
class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  void findByUsername_존재하는_사용자명일때_사용자_반환() {
    // given
    String username = "testuser";
    User user = new User(username, "test@example.com", "password", null);
    new UserStatus(user, Instant.now());
    entityManager.persist(user);
    entityManager.flush();

    // when
    Optional<User> foundUser = userRepository.findByUsername(username);

    // then
    assertThat(foundUser).isPresent();
    assertThat(foundUser.get().getUsername()).isEqualTo(username);
  }

  @Test
  void findByUsername_존재하지_않는_사용자명일때_Empty_반환() {
    // given
    String username = "nonexistentuser";

    // when
    Optional<User> foundUser = userRepository.findByUsername(username);

    // then
    assertThat(foundUser).isEmpty();
  }

  @Test
  void existsByEmail_존재하는_이메일일때_true_반환() {
    // given
    String email = "test@example.com";
    User user = new User("testuser", email, "password", null);
    entityManager.persist(user);
    entityManager.flush();

    // when
    boolean exists = userRepository.existsByEmail(email);

    // then
    assertThat(exists).isTrue();
  }

  @Test
  void existsByEmail_존재하지_않는_이메일일때_false_반환() {
    // given
    String email = "nonexistent@example.com";

    // when
    boolean exists = userRepository.existsByEmail(email);

    // then
    assertThat(exists).isFalse();
  }

  @Test
  void findAllWithProfileAndStatus_모든사용자_프로필과_상태정보_포함하여_반환() {
    // given
    User user1 = new User("user1", "user1@example.com", "password", null);
    User user2 = new User("user2", "user2@example.com", "password", null);

    // userstatus-user 연결
    new UserStatus(user1, Instant.now());
    new UserStatus(user2, Instant.now());

    // user2에 프로필 설정
    BinaryContent profile = new BinaryContent("profile.jpg", 1024L, "image/jpeg");
    entityManager.persist(profile);
    user2.update("user2", "user2@example.com", "password", profile);

    entityManager.persist(user1);
    entityManager.persist(user2);
    entityManager.flush();

    // when
    List<User> users = userRepository.findAllWithProfileAndStatus();

    // then
    assertThat(users).hasSize(2);
    assertThat(users.get(0).getStatus()).isNotNull();
    assertThat(users.get(1).getStatus()).isNotNull();

    // user2 조회 (프로필존재)
    User userWithProfile = users.stream()
        .filter(u -> u.getUsername().equals("user2"))
        .findFirst()
        .orElseThrow();

    assertThat(userWithProfile.getProfile()).isNotNull();
    assertThat(userWithProfile.getProfile().getFileName()).isEqualTo("profile.jpg");
  }
}