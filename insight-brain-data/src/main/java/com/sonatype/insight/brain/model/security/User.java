/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.util.Locale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.7
 */
@Entity
@Table(name = "user")
public class User
    implements HasStringId
{
  public static final String ADMIN_USERNAME = "admin";

  public static final String INTERNAL_REALM_ID = "Internal";

  @Id
  @Column(name = "user_id")
  private String id;

  @Column(name = "username")
  private String username;

  @Column(name = "username_lowercase")
  private String usernameLowercase;

  @Column(name = "password")
  private String password;

  @Column(name = "first_name")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  @Column(name = "email")
  private String email;

  public User() {
  }

  public User(String username, String password, String firstName, String lastName, String email) {
    setUsername(username);
    this.password = password;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    usernameLowercase = normalizeUsername(username);
    this.username = username;
  }

  public String getUsernameLowercase() {
    return usernameLowercase;
  }

  /**
   * This method is defined here only to trick jackson into "thinking" that it de-serialized the value of the
   * usernameLowercase field. If this method is not defined, jackson will set/access the
   * usernameLowercase field directly via reflection, possibly setting it to an incorrect value.
   *
   * @deprecated This method should not be used explicitly.
   */
  @Deprecated
  @SuppressWarnings("unused")
  private void setUsernameLowercase(String usernameLowercase) {
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String calculateDisplayName() {
    return firstName + " " + lastName;
  }

  public static String normalizeUsername(String username) {
    if (username == null) {
      return null;
    }
    return username.toLowerCase(Locale.ENGLISH);
  }
}
