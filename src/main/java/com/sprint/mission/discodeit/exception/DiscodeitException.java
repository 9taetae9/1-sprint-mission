package com.sprint.mission.discodeit.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class DiscodeitException extends RuntimeException {

  private final Instant timestamp;
  private final ErrorCode errorCode;
  private final Map<String, Object> details;

  public DiscodeitException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.timestamp = Instant.now();
    this.errorCode = errorCode;
    this.details = new HashMap<>();
  }

  public DiscodeitException(ErrorCode errorCode, String message) {
    super(message);
    this.timestamp = Instant.now();
    this.errorCode = errorCode;
    this.details = new HashMap<>();
  }

  public DiscodeitException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode.getMessage());
    this.timestamp = Instant.now();
    this.errorCode = errorCode;
    this.details = details;
  }

  public DiscodeitException(ErrorCode errorCode, String message, Map<String, Object> details) {
    super(message);
    this.timestamp = Instant.now();
    this.errorCode = errorCode;
    this.details = details;
  }


  /**
   * @param key
   * @param value
   * @return details: 예외 발생 상황에 대한 추가정보를 저장하기 위한 속성
   */
  public static Map<String, Object> createDetails(String key, Object value) {
    Map<String, Object> details = new HashMap<>();
    details.put(key, value);
    return details;
  }
}
