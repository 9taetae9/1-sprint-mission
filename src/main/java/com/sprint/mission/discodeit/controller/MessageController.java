package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.controller.api.MessageApi;
import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/messages")
public class MessageController implements MessageApi {

    private final MessageService messageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageDto> create(
            @RequestPart("messageCreateRequest") MessageCreateRequest messageCreateRequest,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments
    ) {

        log.info("Starting message creation: channelId={}, authorId={}, number of attachments={}",
                messageCreateRequest.channelId(), messageCreateRequest.authorId(), attachments != null ? attachments.size() : 0);

        List<BinaryContentCreateRequest> attachmentRequests = Optional.ofNullable(attachments)
                .map(files -> files.stream()
                        .map(file -> {
                            try {
                                log.debug("processing content: filename={}, size={}, contentType={}",
                                        file.getOriginalFilename(), file.getContentType(), file.getBytes());
                                return new BinaryContentCreateRequest(
                                        file.getOriginalFilename(),
                                        file.getContentType(),
                                        file.getBytes()
                                );
                            } catch (IOException e) {
                                log.error("Error occurred while processing content: filename={}, error={}",
                                        file.getOriginalFilename(), e.getMessage(), e);
                                throw new RuntimeException(e);
                            }
                        })
                        .toList())
                .orElse(new ArrayList<>());
        MessageDto createdMessage = messageService.create(messageCreateRequest, attachmentRequests);

        log.info("Completed message creation: messageId={}, channelId={}", createdMessage.id(), createdMessage.channelId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdMessage);
    }

    @PatchMapping(path = "{messageId}")
    public ResponseEntity<MessageDto> update(@PathVariable("messageId") UUID messageId,
                                             @RequestBody MessageUpdateRequest request) {
        log.info("Starting message update: messageId={}", messageId);

        MessageDto updatedMessage = messageService.update(messageId, request);

        log.info("Completed message update: messageId={}", updatedMessage.id());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(updatedMessage);
    }

    @DeleteMapping(path = "{messageId}")
    public ResponseEntity<Void> delete(@PathVariable("messageId") UUID messageId) {
        log.info("Starting message deletion: messageId={}", messageId);
        messageService.delete(messageId);
        log.info("Completed message deletion: messageId={}", messageId);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @GetMapping
    public ResponseEntity<PageResponse<MessageDto>> findAllByChannelId(
            @RequestParam("channelId") UUID channelId,
            @RequestParam(value = "cursor", required = false) Instant cursor,
            @PageableDefault(
                    size = 50,
                    page = 0,
                    sort = "createdAt",
                    direction = Direction.DESC
            ) Pageable pageable) {

        log.debug("Finding all messages: channelId={}, cursor={}, pageSize={}", channelId, cursor, pageable.getPageSize());
        PageResponse<MessageDto> messages = messageService.findAllByChannelId(channelId, cursor,
                pageable);
        log.debug("Found messages successfully: channelId={}, resultSize={}, hasNext={}",
                channelId, messages.content().size(), messages.hasNext());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(messages);
    }
}
