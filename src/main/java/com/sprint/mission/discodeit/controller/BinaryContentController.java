package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.docs.BinaryContentSwagger;
import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.service.BinaryContentService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/binaryContents") //binary-contents로 변경, 요구사항 맞추기위해 Camel 로 수정
@RequiredArgsConstructor
public class BinaryContentController implements BinaryContentSwagger {

  private final BinaryContentService binaryContentService;

  @GetMapping(value = "/{binaryContentId}")
  public ResponseEntity<BinaryContentDto> find(
      @PathVariable UUID binaryContentId
  ) {
    BinaryContent binaryContent = binaryContentService.find(binaryContentId);
    return ResponseEntity.ok(BinaryContentDto.from(binaryContent));
  }

  @GetMapping
  public ResponseEntity<List<BinaryContentDto>> batch(
      @RequestParam("binaryContentIds") List<UUID> ids
  ) {
    List<BinaryContentDto> contents = binaryContentService.findAllByIdIn(ids)
        .stream().map(BinaryContentDto::from).toList();
    return ResponseEntity.ok()
        .body(contents);
  }

  @GetMapping("/{binaryContentId}/download")
  public ResponseEntity<byte[]> download(
      @PathVariable("binaryContentId") UUID binaryContentId
  ) {
    BinaryContent binaryContent = binaryContentService.find(binaryContentId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(binaryContent.getContentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + binaryContent.getFileName() + "\"")
        .contentLength(binaryContent.getSize())
        .body(binaryContent.getBytes());
  }
}
