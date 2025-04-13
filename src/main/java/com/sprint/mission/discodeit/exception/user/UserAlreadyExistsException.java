package com.sprint.mission.discodeit.exception.user;

import com.sprint.mission.discodeit.exception.ErrorCode;

class UserAlreadyExistsException extends UserException {

  UserAlreadyExistsException(String username) {
    super(ErrorCode.DUPLICATE_USER,
        "User with username " + username + " already exists",
        createDetails("username", username));
  }

  UserAlreadyExistsException(String email, boolean isEmail) {
    super(ErrorCode.DUPLICATE_USER,
        "User with email " + email + " already exists",
        createDetails("email", email));
  }
}
