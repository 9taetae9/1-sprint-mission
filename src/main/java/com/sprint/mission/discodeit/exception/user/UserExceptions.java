package com.sprint.mission.discodeit.exception.user;

import java.util.UUID;

public final class UserExceptions {

  private UserExceptions() {
  }

  public static UserException notFound(UUID userId) {
    return new UserNotFoundException(userId);
  }

  public static UserException notFound(String username) {
    return new UserNotFoundException(username);
  }

  public static UserException userNameAlreadyExists(String username) {
    return new UserAlreadyExistsException(username);
  }

  public static UserException emailAlreadyExists(String email) {
    return new UserAlreadyExistsException(email, true);
  }

  public static UserException invalidPassword(String username) {
    return new InvalidPasswordException(username);
  }
}
