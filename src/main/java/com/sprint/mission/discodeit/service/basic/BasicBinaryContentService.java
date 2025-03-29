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

        log.info("** binary content 생성 시작 ** filename: {}, type: {}", fileName, contentType);
        log.debug("binary content size: {} bytes", bytes.length);

        BinaryContent binaryContent = new BinaryContent(
                fileName,
                (long) bytes.length,
                contentType
        );
        binaryContentRepository.save(binaryContent);
        log.debug("binary content 메타 데이터 저장 완료 id: {}", binaryContent.getId());

        try {
            binaryContentStorage.put(binaryContent.getId(), bytes);
        } catch (Exception e) {
            log.error("스토리지 저장 실패! 파일명: {}", fileName, e);
            throw e;
        }

        log.info("** binary content 생성 완료 ** id: {}", binaryContent.getId());
        return binaryContentMapper.toDto(binaryContent);
    }

    @Override
    public BinaryContentDto find(UUID binaryContentId) {
        log.debug("binary contetent 조회 시도 id: {}", binaryContentId);

        return binaryContentRepository.findById(binaryContentId)
                .map(content -> {
                    log.debug("binary content 조회 성공 filename: {}, type: {}, size: {}",
                            content.getFileName(), content.getContentType(), content.getSize());
                    return binaryContentMapper.toDto(content);
                })
                .orElseThrow(() -> {
                    log.warn("binary content 조회 실패 id: {}", binaryContentId);
                    return new NoSuchElementException("BinaryContent with id " + binaryContentId + " not found");
                });
    }

    @Override
    public List<BinaryContentDto> findAllByIdIn(List<UUID> binaryContentIds) {
        log.debug("binary contents 리스트로 조회 시도 id 개수: {}", binaryContentIds.size());

        List<BinaryContent> contents = binaryContentRepository.findAllById(binaryContentIds);

        if (contents.size() < binaryContentIds.size()) {
            log.warn("요청된 binary content {}개 중 {}개 조회 성공", binaryContentIds.size(), contents.size());
        }

        return contents.stream()
                .map(binaryContentMapper::toDto)
                .toList();
//        return binaryContentRepository.findAllById(binaryContentIds).stream()
//                .map(binaryContentMapper::toDto)
//                .toList();
    }

    @Transactional
    @Override
    public void delete(UUID binaryContentId) {
        log.info("binary content 삭제 시도 id: {}", binaryContentId);

        if (!binaryContentRepository.existsById(binaryContentId)) {
            log.warn("존재 하지 않는 binary content 삭제 시도 id: {}", binaryContentId);
            throw new NoSuchElementException("BinaryContent with id " + binaryContentId + " not found");
        }

        binaryContentRepository.deleteById(binaryContentId);
        log.info("binary content 삭제 성공 id: {}", binaryContentId);
    }
}
