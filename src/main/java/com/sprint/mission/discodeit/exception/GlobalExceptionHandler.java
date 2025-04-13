package com.sprint.mission.discodeit.exception;

import com.sprint.mission.discodeit.exception.binarycontent.BinaryContentException;
import com.sprint.mission.discodeit.exception.channel.ChannelException;
import com.sprint.mission.discodeit.exception.message.MessageException;
import com.sprint.mission.discodeit.exception.user.UserException;
import com.sprint.mission.discodeit.exception.userstatus.UserStatusException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(DiscodeitException.class)
  public ResponseEntity<ErrorResponse> handleDiscodeitException(DiscodeitException e) {
    HttpStatus status = getStatus(e.getErrorCode());
    return ResponseEntity
        .status(status)
        .body(ErrorResponse.from(e, status.value()));
  }

  //도메인별 예외 핸들러
  @ExceptionHandler(UserException.class)
  public ResponseEntity<ErrorResponse> handleUserException(UserException e) {
    HttpStatus status = getStatus(e.getErrorCode());
    return ResponseEntity
        .status(status)
        .body(ErrorResponse.from(e, status.value()));
  }

  @ExceptionHandler(ChannelException.class)
  public ResponseEntity<ErrorResponse> handleChannelException(ChannelException e) {
    HttpStatus status = getStatus(e.getErrorCode());
    return ResponseEntity
        .status(status)
        .body(ErrorResponse.from(e, status.value()));
  }

  @ExceptionHandler(MessageException.class)
  public ResponseEntity<ErrorResponse> handleMessageException(MessageException e) {
    HttpStatus status = getStatus(e.getErrorCode());
    return ResponseEntity
        .status(status)
        .body(ErrorResponse.from(e, status.value()));
  }

  @ExceptionHandler(BinaryContentException.class)
  public ResponseEntity<ErrorResponse> handleBinaryContentException(BinaryContentException e) {
    HttpStatus status = getStatus(e.getErrorCode());
    return ResponseEntity
        .status(status)
        .body(ErrorResponse.from(e, status.value()));
  }

  @ExceptionHandler(UserStatusException.class)
  public ResponseEntity<ErrorResponse> handleUserStatusException(UserStatusException e) {
    HttpStatus status = getStatus(e.getErrorCode());
    return ResponseEntity
        .status(status)
        .body(ErrorResponse.from(e, status.value()));
  }


  //입력값 검증 예외 처리
  @ExceptionHandler({MethodArgumentNotValidException.class})
  public ResponseEntity<ErrorResponse> handleValidationException(Exception e) {

    Map<String, Object> details = new HashMap<>();

    if (e instanceof MethodArgumentNotValidException validException) { /*
(pattern matching for instanceof) 타입 체크와 캐스팅 동시 수행 java16
          MethodArgumentNotValidException validException = (MethodArgumentNotValidException) e;
*/
      validException.getBindingResult()
          .getFieldErrors().forEach(error ->
              details.put(error.getField(), error.getDefaultMessage()));
    }

    ErrorResponse errorResponse = ErrorResponse.of(
        Instant.now(),
        ErrorCode.VALIDATION_ERROR.name(),
        "Validation failed",
        details,
        e.getClass().getSimpleName(),
        HttpStatus.BAD_REQUEST.value()
    );

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(errorResponse);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {

    ErrorResponse errorResponse = ErrorResponse.of(
        Instant.now(),
        ErrorCode.INTERNAL_SERVER_ERROR.name(),
        e.getMessage(),
        new HashMap<>(),
        e.getClass().getSimpleName(),
        HttpStatus.INTERNAL_SERVER_ERROR.value()
    );

    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(errorResponse);
  }

  //errorcode 별 http 상태코드 매핑
  private HttpStatus getStatus(ErrorCode errorCode) {

    return switch (errorCode) {
      case USER_NOT_FOUND, CHANNEL_NOT_FOUND, MESSAGE_NOT_FOUND,
           BINARY_CONTENT_NOT_FOUND, READ_STATUS_NOT_FOUND, USER_STATUS_NOT_FOUND ->
          HttpStatus.NOT_FOUND;

      case DUPLICATE_USER, READ_STATUS_ALREADY_EXISTS, USER_STATUS_ALREADY_EXISTS ->
          HttpStatus.CONFLICT;

      case INVALID_PASSWORD, VALIDATION_ERROR, PRIVATE_CHANNEL_UPDATE -> HttpStatus.BAD_REQUEST;

      case BINARY_CONTENT_STORAGE_ERROR, INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;

    };
  }
}
