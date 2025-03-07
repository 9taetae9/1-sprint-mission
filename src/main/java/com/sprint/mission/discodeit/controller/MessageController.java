package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.docs.MessageSwagger;
import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.dto.data.Pageable;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.MessageResponse;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.mapper.FileConverter;
import com.sprint.mission.discodeit.service.BinaryContentService;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.basic.BasicMessageService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController implements MessageSwagger {

  // implements MessageSwagger
  private final FileConverter fileConverter;
  private final BasicMessageService messageService;
  private final UserService userService;
  private final BinaryContentService binaryContentService;

  @PostMapping(consumes = "multipart/form-data")
  public ResponseEntity<MessageDto> create(
      @RequestPart("messageCreateRequest") MessageCreateRequest request,
      @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments
  ) {
    List<BinaryContentCreateRequest> attachmentRequests = FileConverter.getAttachmentRequests(
        attachments);
    Message message = messageService.create(request, attachmentRequests);

    UserDto authorDto = userService.find(message.getAuthorId());

    List<BinaryContent> binaryContents = binaryContentService.findAllByIdIn(
        message.getAttachmentIds());
    List<BinaryContentDto> binaryContentDtos = binaryContents.stream()
        .map(BinaryContentDto::from)
        .toList();

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(MessageDto.from(message, authorDto, binaryContentDtos));
  }


  @PatchMapping(value = "/{messageId}")
  public ResponseEntity<MessageDto> update(
      @PathVariable UUID messageId,
      @RequestBody MessageUpdateRequest request
  ) {
    Message message = messageService.update(messageId, request);
    UserDto author = userService.find(message.getAuthorId());

    List<BinaryContent> binaryContents = binaryContentService.findAllByIdIn(
        message.getAttachmentIds());
    List<BinaryContentDto> binaryContentDtos = binaryContents.stream()
        .map(BinaryContentDto::from)
        .toList();

    return ResponseEntity.status(HttpStatus.OK)
        .body(MessageDto.from(message, author, binaryContentDtos));
  }

  @DeleteMapping(value = "/{messageId}")
  public ResponseEntity<Void> delete(
      @PathVariable UUID messageId
  ) {
    messageService.delete(messageId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  public ResponseEntity<PageResponse> findMessagesByChannel(
      @RequestParam UUID channelId,
      @RequestParam Pageable pageable
  ) {
    List<MessageResponse> messages = messageService.findAllByChannelId(channelId)
        .stream().map(MessageResponse::from).toList();
    return ResponseEntity.ok(null);
  }

}