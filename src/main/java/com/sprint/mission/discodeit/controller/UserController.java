package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.controller.api.UserApi;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.data.UserStatusDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.UserStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController implements UserApi {

    private final UserService userService;
    private final UserStatusService userStatusService;

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @Override
    public ResponseEntity<UserDto> create(
            @RequestPart("userCreateRequest") UserCreateRequest userCreateRequest,
            @RequestPart(value = "profile", required = false) MultipartFile profile
    ) {
        log.info("Starting user creation: username={}, email={}",
                userCreateRequest.username(), userCreateRequest.email());

        Optional<BinaryContentCreateRequest> profileRequest = Optional.ofNullable(profile)
                .flatMap(this::resolveProfileRequest);
        UserDto createdUser = userService.create(userCreateRequest, profileRequest);

        log.info("Completed user creation: userId={}, username={}",
                createdUser.id(), createdUser.username());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdUser);
    }

    @PatchMapping(
            path = "{userId}",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}
    )
    @Override
    public ResponseEntity<UserDto> update(
            @PathVariable("userId") UUID userId,
            @RequestPart("userUpdateRequest") UserUpdateRequest userUpdateRequest,
            @RequestPart(value = "profile", required = false) MultipartFile profile
    ) {
        log.info("Starting user update: userId={}, newUsername={}",
                userId, userUpdateRequest.newUsername());

        Optional<BinaryContentCreateRequest> profileRequest = Optional.ofNullable(profile)
                .flatMap(this::resolveProfileRequest);
        UserDto updatedUser = userService.update(userId, userUpdateRequest, profileRequest);

        log.info("Completed user update: userId={}", updatedUser.id());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(updatedUser);
    }

    @DeleteMapping(path = "{userId}")
    @Override
    public ResponseEntity<Void> delete(@PathVariable("userId") UUID userId) {
        log.info("Starting user deletion: userId={}", userId);

        userService.delete(userId);

        log.info("Completed user deletion: userId={}", userId);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @GetMapping
    @Override
    public ResponseEntity<List<UserDto>> findAll() {
        log.debug("Starting user findAll operation");

        List<UserDto> users = userService.findAll();

        log.debug("Completed user findAll: retrieved {} users", users.size());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(users);
    }

    @PatchMapping(path = "{userId}/userStatus")
    @Override
    public ResponseEntity<UserStatusDto> updateUserStatusByUserId(@PathVariable("userId") UUID userId,
                                                                  @RequestBody UserStatusUpdateRequest request) {

        log.info("Starting user status update: userId={}, newLastActive={}", userId, request.newLastActiveAt());

        UserStatusDto updatedUserStatus = userStatusService.updateByUserId(userId, request);

        log.info("Completed user status update: userId={}, statusId={}", userId, updatedUserStatus.id());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(updatedUserStatus);
    }

    private Optional<BinaryContentCreateRequest> resolveProfileRequest(MultipartFile profileFile) {
        log.debug("Resolving profile request: filename={}, size={}, contentType={}",
                profileFile.getOriginalFilename(),
                profileFile.getSize(),
                profileFile.getContentType());

        if (profileFile.isEmpty()) {
            log.debug("Empty profile!");
            return Optional.empty();
        } else {
            try {
                BinaryContentCreateRequest binaryContentCreateRequest = new BinaryContentCreateRequest(
                        profileFile.getOriginalFilename(),
                        profileFile.getContentType(),
                        profileFile.getBytes()
                );
                log.debug("Profile request resolved successfully: filename={}",
                        binaryContentCreateRequest.fileName());
                return Optional.of(binaryContentCreateRequest);
            } catch (IOException e) {
                log.error("Error resolving profile request: filename={}, error={}",
                        profileFile.getOriginalFilename(), e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }
}
