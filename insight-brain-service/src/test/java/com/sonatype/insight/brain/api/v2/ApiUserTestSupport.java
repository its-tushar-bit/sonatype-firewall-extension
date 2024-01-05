/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.UUID;

import com.sonatype.insight.brain.api.v2.dto.ApiUserDTO;
import com.sonatype.insight.brain.model.security.User;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiUserTestSupport
{
  public static ApiUserDTO createUserDTOToAdd() {
    ApiUserDTO userDTO = new ApiUserDTO();
    String random = UUID.randomUUID().toString().replace("-", "");
    userDTO.username = "username_" + random;
    userDTO.password = "password_" + random;
    userDTO.firstName = "firstName_" + random;
    userDTO.lastName = "lastName_" + random;
    userDTO.email = "email_" + random + "@domain";
    return userDTO;
  }

  public static ApiUserDTO createUserDTOToUpdate(User user) {
    ApiUserDTO userDTO = new ApiUserDTO();
    userDTO.username = user.getUsername();
    // exclude password
    String random = UUID.randomUUID().toString().replace("-", "");
    userDTO.firstName = random + "_" + user.getFirstName();
    userDTO.lastName = random + "_" + user.getLastName();
    userDTO.email = random + "_" + user.getEmail();
    return userDTO;
  }

  public static void assertEqualExceptNullDTOPassword(User user, ApiUserDTO userDTO) {
    assertThat(userDTO.username).isEqualTo(user.getUsername());
    assertThat(userDTO.password).isNull();
    assertThat(userDTO.firstName).isEqualTo(user.getFirstName());
    assertThat(userDTO.lastName).isEqualTo(user.getLastName());
    assertThat(userDTO.email).isEqualTo(user.getEmail());
  }

  public static void assertEqualIgnoringPassword(ApiUserDTO inputUserDTO, ApiUserDTO outputUserDTO) {
    assertThat(outputUserDTO.username).isEqualTo(inputUserDTO.username);
    assertThat(outputUserDTO.firstName).isEqualTo(inputUserDTO.firstName);
    assertThat(outputUserDTO.lastName).isEqualTo(inputUserDTO.lastName);
    assertThat(outputUserDTO.email).isEqualTo(inputUserDTO.email);
  }

  public static void assertMatchingUser(ApiUserDTO inputUserDTO, User user) {
    assertThat(inputUserDTO.firstName).isEqualTo(user.getFirstName());
    assertThat(inputUserDTO.lastName).isEqualTo(user.getLastName());
    assertThat(inputUserDTO.email).isEqualTo(user.getEmail());
  }
}
