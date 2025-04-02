package com.sprint.mission.discodeit.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  //user
  USER_NOT_FOUND("User not found"),
  DUPLICATE_USER("User already exists"),
  INVALID_PASSWORD("Wrong password"),

  //channel
  CHANNEL_NOT_FOUND("Channel not found"),
  PRIVATE_CHANNEL_UPDATE("Private channel cannot be updated"),

  //message
  MESSAGE_NOT_FOUND("Message not found"),
  //binarycontent
  BINARY_CONTENT_NOT_FOUND("Binary content not found"),
  BINARY_CONTENT_STORAGE_ERROR("Error storing binary content"),

  // readstatus
  READ_STATUS_NOT_FOUND("Read status not found"),
  READ_STATUS_ALREADY_EXISTS("Read status already exists"),

  // userstatus
  USER_STATUS_NOT_FOUND("User status not found"),
  USER_STATUS_ALREADY_EXISTS("User status already exists"),

  // basic
  VALIDATION_ERROR("Validation error"),
  INTERNAL_SERVER_ERROR("Internal server error");

  private final String message;
}
