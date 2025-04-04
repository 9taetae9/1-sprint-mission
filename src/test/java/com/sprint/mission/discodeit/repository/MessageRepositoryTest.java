package com.sprint.mission.discodeit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@EnableJpaAuditing
@ActiveProfiles("test")
class MessageRepositoryTest {

  @Autowired
  private MessageRepository messageRepository;

  @Autowired
  private TestEntityManager entityManager;

  private User author;
  private Channel channel;
  private List<Message> messages;

  @BeforeEach
  void setUp() {
    // 사용자 생성
    author = new User("testuser", "test@example.com", "password", null);
    new UserStatus(author, Instant.now());
    entityManager.persist(author);

    // 채널 생성
    channel = new Channel(ChannelType.PUBLIC, "test-channel", "Test Channel");
    entityManager.persist(channel);
    entityManager.flush();

    // 메시지 생성 - JPA가 자동으로 createdAt 필드 채워줌
    messages = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      Message message = new Message("Message " + i, channel, author, List.of());
      entityManager.persist(message);
      messages.add(message);
    }
    entityManager.flush();
  }

  @Test
  void findAllByChannelIdWithAuthor_성공_페이지네이션_적용() {
    // given
    Instant cursorTime = Instant.now().plus(1, ChronoUnit.MINUTES); // 현재 시간보다 미래
    Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));

    // when
    Slice<Message> result = messageRepository.findAllByChannelIdWithAuthor(
        channel.getId(), cursorTime, pageable);

    // then
    assertThat(result.getContent()).isNotEmpty(); // 메시지가 있어야 함
    assertThat(result.getContent().size()).isLessThanOrEqualTo(5); // 최대 5개까지

    // 콘텐츠로 메시지 구분 (타임스탬프 대체)
    if (result.getContent().size() >= 2) {
      for (int i = 0; i < result.getContent().size(); i++) {
        assertThat(result.getContent().get(i).getContent()).contains("Message ");
      }
    }
  }

  @Test
  void findAllByChannelIdWithAuthor_성공_커서시간_기준조회() {
    // 쿼리를 위한 커서 설정 (현재 시간 사용)
    Instant cursorTime = Instant.now();
    Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));

    // when, 커서 시간보다 이전의 메시지만 검색
    Slice<Message> result = messageRepository.findAllByChannelIdWithAuthor(
        channel.getId(), cursorTime, pageable);

    // then, 메시지가 검색되어야 함
    assertThat(result.getContent()).isNotEmpty();

    // 메시지가 비어있지 않다면 모든 메시지의 createdAt은 현재 시간보다 이전이어야 함
    for (Message message : result.getContent()) {
      assertThat(message.getCreatedAt()).isBefore(cursorTime);
    }
  }

  @Test
  void findLastMessageAtByChannelId_성공_최신메시지_시간반환() {
    // when
    Optional<Instant> lastMessageAt = messageRepository.findLastMessageAtByChannelId(
        channel.getId());

    // then
    assertThat(lastMessageAt).isPresent(); // 타임스탬프가 있어야 함
    assertThat(lastMessageAt.get()).isNotNull();

    // 현재 시간 이전이어야 함 (미래 시간 불가)
    assertThat(lastMessageAt.get()).isBefore(Instant.now().plus(1, ChronoUnit.SECONDS));
  }

  @Test
  void findLastMessageAtByChannelId_성공_빈채널인경우_Empty반환() {
    // given
    Channel emptyChannel = new Channel(ChannelType.PUBLIC, "empty-channel", "Empty Channel");
    entityManager.persist(emptyChannel);
    entityManager.flush();

    // when
    Optional<Instant> lastMessageAt = messageRepository.findLastMessageAtByChannelId(
        emptyChannel.getId());

    // then
    assertThat(lastMessageAt).isEmpty();
  }

  @Test
  void deleteAllByChannelId_성공_채널_모든메시지_삭제() {
    //given
    int initialCount = messageRepository.findAllByChannelIdWithAuthor(
        channel.getId(), Instant.now().plus(1, ChronoUnit.MINUTES),
        PageRequest.of(0, 100)).getContent().size();

    assertThat(initialCount).isEqualTo(10); //초기 10개 메시지

    //when
    int deletedCount = messageRepository.deleteAllByChannelId(channel.getId());

    //then
    assertThat(deletedCount).isEqualTo(10); //10개 메시지 삭제 되어야 함

    // 메시지가 실제로 삭제되었는지 확인
    Slice<Message> remainingMessages = messageRepository.findAllByChannelIdWithAuthor(
        channel.getId(), Instant.now().plus(1, ChronoUnit.MINUTES),
        PageRequest.of(0, 100));

    assertThat(remainingMessages.getContent()).isEmpty(); // 남은메시지 없어야 함
  }
}