package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class FileConverter {

  public Optional<BinaryContentDto> convertToBinaryRequest(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      log.debug("No file provided or file is empty");
      return Optional.empty();
    }

    return Optional.of(new BinaryContentDto(
        UUID.randomUUID(),
        file.getName(),
        file.getSize(),
        file.getContentType()
    ));
  }

  public static Optional<BinaryContentCreateRequest> resolveProfileRequest(
      MultipartFile profileFile) {
    if (profileFile.isEmpty()) {
      return Optional.empty();
    } else {
      try {
        BinaryContentCreateRequest binaryContentCreateRequest = new BinaryContentCreateRequest(
            profileFile.getOriginalFilename(),
            profileFile.getContentType(),
            profileFile.getBytes()
        );
        return Optional.of(binaryContentCreateRequest);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }


  public static List<BinaryContentCreateRequest> getAttachmentRequests(
      List<MultipartFile> attachments) {
    return Optional.ofNullable(attachments)
        .map(files -> files.stream()
            .map(file -> {
              try {
                return new BinaryContentCreateRequest(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes()
                );
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
            .toList())
        .orElse(new ArrayList<>());
  }
}


