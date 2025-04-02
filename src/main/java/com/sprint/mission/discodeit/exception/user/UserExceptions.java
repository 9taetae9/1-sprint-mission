package com.sprint.mission.discodeit.exception.user;

import java.util.UUID;

public final class UserExceptions {

  private UserExceptions() {
  }

  public static UserNotFoundException notFound(UUID userId) {
    return new UserNotFoundException(userId);
  }

  public static UserNotFoundException notFound(String username) {
    return new UserNotFoundException(username);
  }

  public static UserAlreadyExistsException userNameAlreadyExists(String username) {
    return new UserAlreadyExistsException(username);
  }

  public static UserAlreadyExistsException emailAlreadyExists(String email) {
    return new UserAlreadyExistsException(email, true);
  }

  public static InvalidPasswordException invalidPassword(String username) {
    return new InvalidPasswordException(username);
  }
}
