package com.sprint.mission.discodeit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@EnableJpaAuditing
@ActiveProfiles("test")
class ChannelRepositoryTest {

  @Autowired
  private ChannelRepository channelRepository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  void findAllByTypeOrIdIn_모든공개채널_반환() {
    // given
    Channel publicChannel1 = new Channel(ChannelType.PUBLIC, "public-channel-1",
        "Public Channel 1");
    Channel publicChannel2 = new Channel(ChannelType.PUBLIC, "public-channel-2",
        "Public Channel 2");
    Channel privateChannel = new Channel(ChannelType.PRIVATE, null, null);

    entityManager.persist(publicChannel1);
    entityManager.persist(publicChannel2);
    entityManager.persist(privateChannel);
    entityManager.flush();

    // when
    List<Channel> channels = channelRepository.findAllByTypeOrIdIn(ChannelType.PUBLIC, List.of());

    // then
    assertThat(channels).hasSize(2);
    assertThat(channels).extracting("type").containsOnly(ChannelType.PUBLIC);
    assertThat(channels).extracting("name")
        .containsExactlyInAnyOrder("public-channel-1", "public-channel-2");
  }

  @Test
  void findAllByTypeOrIdIn_비공개채널_ID목록에_해당되는_비공개채널만_반환() {
    // given
    Channel publicChannel = new Channel(ChannelType.PUBLIC, "public-channel", "Public Channel");
    Channel privateChannel1 = new Channel(ChannelType.PRIVATE, null, null);
    Channel privateChannel2 = new Channel(ChannelType.PRIVATE, null, null);
    Channel privateChannel3 = new Channel(ChannelType.PRIVATE, null, null);

    entityManager.persist(publicChannel);
    entityManager.persist(privateChannel1);
    entityManager.persist(privateChannel2);
    entityManager.persist(privateChannel3);
    entityManager.flush();

    // when 비공개 채널 조회 목록(1,2) //3은 목록에 없음
    List<UUID> privateChannelIds = Arrays.asList(privateChannel1.getId(), privateChannel2.getId());
    List<Channel> channels = channelRepository.findAllByTypeOrIdIn(ChannelType.PUBLIC,
        privateChannelIds);

    // then
    assertThat(channels).hasSize(3); // public 1개, private 2개

    // 모든 공개 채널 확인
    assertThat(channels).filteredOn(c -> c.getType() == ChannelType.PUBLIC).hasSize(1);

    //
    List<Channel> privateChannels = channels.stream()
        .filter(c -> c.getType() == ChannelType.PRIVATE)
        .toList();

    assertThat(privateChannels).hasSize(2);
    assertThat(privateChannels).extracting("id")
        .containsExactlyInAnyOrder(privateChannel1.getId(), privateChannel2.getId());
  }

  @Test
  void findAllByTypeOrIdIn_공개채널과_지정된비공개채널_모두_반환() {
    // given
    Channel publicChannel1 = new Channel(ChannelType.PUBLIC, "public-channel-1",
        "Public Channel 1");
    Channel publicChannel2 = new Channel(ChannelType.PUBLIC, "public-channel-2",
        "Public Channel 2");
    Channel privateChannel1 = new Channel(ChannelType.PRIVATE, null, null);
    Channel privateChannel2 = new Channel(ChannelType.PRIVATE, null, null);

    entityManager.persist(publicChannel1);
    entityManager.persist(publicChannel2);
    entityManager.persist(privateChannel1);
    entityManager.persist(privateChannel2);
    entityManager.flush();

    // when
    List<UUID> privateChannelIds = List.of(privateChannel1.getId());
    List<Channel> channels = channelRepository.findAllByTypeOrIdIn(ChannelType.PUBLIC,
        privateChannelIds);

    // then
    assertThat(channels).hasSize(3); // public 2개, private 1개

    // 공개 채널 검증
    List<Channel> publicChannels = channels.stream()
        .filter(c -> c.getType() == ChannelType.PUBLIC)
        .toList();
    assertThat(publicChannels).hasSize(2);

    // 비공개 체널 검증
    List<Channel> privateChannels = channels.stream()
        .filter(c -> c.getType() == ChannelType.PRIVATE)
        .toList();
    assertThat(privateChannels).hasSize(1);
    assertThat(privateChannels.get(0).getId()).isEqualTo(privateChannel1.getId());
  }
}