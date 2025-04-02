package com.sprint.mission.discodeit.exception.user;

import com.sprint.mission.discodeit.exception.ErrorCode;

public class UserAlreadyExistsException extends UserException {

  public UserAlreadyExistsException(String username) {
    super(ErrorCode.DUPLICATE_USER,
        "User with username " + username + " already exists",
        createDetails("username", username));
  }

  public UserAlreadyExistsException(String email, boolean isEmail) {
    super(ErrorCode.DUPLICATE_USER,
        "User with email " + email + " already exists",
        createDetails("email", email));
  }
}
