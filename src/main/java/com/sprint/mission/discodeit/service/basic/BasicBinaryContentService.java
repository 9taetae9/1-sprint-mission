package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.mapper.BinaryContentMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.service.BinaryContentService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class BasicBinaryContentService implements BinaryContentService {

    private final BinaryContentRepository binaryContentRepository;
    private final BinaryContentMapper binaryContentMapper;
    private final BinaryContentStorage binaryContentStorage;

    @Transactional
    @Override
    public BinaryContentDto create(BinaryContentCreateRequest request) {
        String fileName = request.fileName();
        byte[] bytes = request.bytes();
        String contentType = request.contentType();

        log.info("Processing binary content creation: filename={}, contentType={}", fileName, contentType);
        log.debug("Binary content size: {} bytes", bytes.length);

        BinaryContent binaryContent = new BinaryContent(
                fileName,
                (long) bytes.length,
                contentType
        );
        binaryContentRepository.save(binaryContent);
        log.debug("Binary content metadata saved: binaryContentId={}", binaryContent.getId())
        ;
        try {
            binaryContentStorage.put(binaryContent.getId(), bytes);
        } catch (Exception e) {
            log.error("Error occurred while saving to storage: filename={}, error={}", fileName, e.getMessage(), e);
            throw e;
        }

        log.info("Binary content created successfully: binaryContentId={}, filename={}", binaryContent.getId(), fileName);
        return binaryContentMapper.toDto(binaryContent);
    }

    @Override
    public BinaryContentDto find(UUID binaryContentId) {
        log.debug("Finding binary content: binaryContentId={}", binaryContentId);

        return binaryContentRepository.findById(binaryContentId)
                .map(content -> {
                    log.debug("Binary content found: binaryContentId={}, filename={}, type={}, size={}",
                            binaryContentId, content.getFileName(), content.getContentType(), content.getSize());
                    return binaryContentMapper.toDto(content);
                })
                .orElseThrow(() -> {
                    log.warn("Binary content not found: binaryContentId={}", binaryContentId);
                    return new NoSuchElementException("BinaryContent with id " + binaryContentId + " not found");
                });
    }

    @Override
    public List<BinaryContentDto> findAllByIdIn(List<UUID> binaryContentIds) {
        log.debug("Finding all binary contents in list: count={}", binaryContentIds.size());

        List<BinaryContent> contents = binaryContentRepository.findAllById(binaryContentIds);

        if (contents.size() < binaryContentIds.size()) {
            log.warn("Some binary contents are not found: requested={}, found={}", binaryContentIds.size(), contents.size());
        }

        log.debug("Found multiple contents in list: count={}", contents.size());
        return contents.stream()
                .map(binaryContentMapper::toDto)
                .toList();
    }

    @Transactional
    @Override
    public void delete(UUID binaryContentId) {
        log.info("Processing binary content deletion: binaryContentId={}", binaryContentId);

        if (!binaryContentRepository.existsById(binaryContentId)) {
            log.warn("Binary content deletion failed: content not found - binaryContentId={}", binaryContentId);
            throw new NoSuchElementException("BinaryContent with id " + binaryContentId + " not found");
        }
        binaryContentRepository.deleteById(binaryContentId);
        log.info("Binary content deleted successfully: binaryContentId={} ", binaryContentId);
    }
}
