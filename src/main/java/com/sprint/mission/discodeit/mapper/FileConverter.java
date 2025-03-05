package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class FileConverter {

  public Optional<BinaryContentCreateRequest> convertToBinaryRequest(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      log.debug("No file provided or file is empty");
      return Optional.empty();
    }

    try {
      return Optional.of(new BinaryContentCreateRequest(
          file.getOriginalFilename(),
          file.getContentType(),
          file.getBytes()
      ));
    } catch (IOException e) {
      log.error("File conversion failed: {}", file.getOriginalFilename(), e);
      throw new RuntimeException("File conversion failed", e);
    }
  }
}


