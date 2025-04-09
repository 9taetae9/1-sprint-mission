package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.controller.api.ChannelApi;
import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.service.ChannelService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/channels")
public class ChannelController implements ChannelApi {

  private final ChannelService channelService;

  @PostMapping(path = "public")
  public ResponseEntity<ChannelDto> create(@Valid @RequestBody PublicChannelCreateRequest request) {
    log.info("Starting public channel creation: name={}", request.name());
    log.debug("Public channel description: {}", request.description());

    ChannelDto createdChannel = channelService.create(request);

    log.info("Completed public channel creation: channelId={}, name={}", createdChannel.id(),
        createdChannel.name());
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(createdChannel);
  }

  @PostMapping(path = "private")
  public ResponseEntity<ChannelDto> create(
      @Valid @RequestBody PrivateChannelCreateRequest request) {
    log.info("Starting private channel creation: number of participants={}",
        request.participantIds().size());
    log.debug("Private channel participant Ids: {}", request.participantIds());

    ChannelDto createdChannel = channelService.create(request);

    log.info("Completed private channel creation: channelId={}", createdChannel.id());
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(createdChannel);
  }

  @PatchMapping(path = "{channelId}")
  public ResponseEntity<ChannelDto> update(@PathVariable("channelId") UUID channelId,
      @Valid @RequestBody PublicChannelUpdateRequest request) {

    log.info("Starting public channel update: channelId={}, newName={}", channelId,
        request.newName());
    log.debug("public channel update: new description={}", request.newDescription());

    ChannelDto updatedChannel = channelService.update(channelId, request);

    log.info("Completed public channel update: channelId={}", updatedChannel.id());
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(updatedChannel);
  }

  @DeleteMapping(path = "{channelId}")
  public ResponseEntity<Void> delete(@PathVariable("channelId") UUID channelId) {
    log.info("Starting channel deletion: channelId={}", channelId);

    channelService.delete(channelId);

    log.info("Completed channel deletion: channelId={}", channelId);
    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }

  @GetMapping
  public ResponseEntity<List<ChannelDto>> findAll(@RequestParam("userId") UUID userId) {

    log.debug("Starting findAll channels: userId={}", userId);

    List<ChannelDto> channels = channelService.findAllByUserId(userId);
    log.debug("Completed findAll channels: userId={}, channels found={}", userId, channels.size());
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(channels);
  }
}
