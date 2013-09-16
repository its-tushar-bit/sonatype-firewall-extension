/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.brain.model.NameHelper;
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

  @Id
  @Column(name = "user_id")
  private String id;

  @Column(name = "username")
  private String username;

  @Column(name = "username_lowercase")
  private String usernameLowercase;

  @Column(name = "password_hash")
  private char[] passwordHash;

  @Column(name = "first_name")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  @Column(name = "email")
  private String email;

  public User() {
  }

  public User(String username, String firstName, String lastName) {
    setUsername(username);
    this.firstName = firstName;
    this.lastName = lastName;
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
    usernameLowercase = NameHelper.normalize(username);
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

  public char[] getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(char[] passwordHash) {
    this.passwordHash = passwordHash;
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
}
