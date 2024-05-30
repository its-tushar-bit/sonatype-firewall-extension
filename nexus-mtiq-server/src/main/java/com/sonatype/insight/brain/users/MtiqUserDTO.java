/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import java.util.Objects;

import com.sonatype.insight.brain.security.SsoUser;

public class MtiqUserDTO
{
  private String username;

  private String firstName;

  private String lastName;

  private String email;

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(final String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(final String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(final String email) {
    this.email = email;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MtiqUserDTO that = (MtiqUserDTO) o;
    return Objects.equals(firstName, that.firstName) &&
        Objects.equals(lastName, that.lastName) && Objects.equals(email, that.email);
  }

  @Override
  public int hashCode() {
    return Objects.hash(firstName, lastName, email);
  }

  static MtiqUserDTO ssoUserToMtiqUser(final SsoUser user) {
    MtiqUserDTO mtiqUser = new MtiqUserDTO();
    mtiqUser.setUsername(user.getUsername());
    mtiqUser.setFirstName(user.getFirstName());
    mtiqUser.setLastName(user.getLastName());
    mtiqUser.setEmail(user.getEmail());
    return mtiqUser;
  }
}
