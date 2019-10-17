/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.util.Date;
import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.75
 */
@Entity
@Table(name = "user_token")
public class UserToken
    implements HasStringId
{
  @Id
  @Column(name = "user_token_id")
  private String id;

  @Column(name = "username")
  private String username;

  @Column(name = "username_lowercase")
  private String usernameLowercase;

  @Column(name = "internal_user")
  private boolean isInternalUser;

  @Column(name = "user_code")
  private String userCode;

  @Column(name = "pass_code")
  private String passCode;

  @Column(name = "create_time")
  private Date createTime;

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
    this.username = username;
    usernameLowercase = UserToken.normalizeUsername(username);
  }

  public String getUsernameLowercase() {
    return usernameLowercase;
  }

  public boolean isInternalUser() {
    return isInternalUser;
  }

  public void setInternalUser(boolean isInternalUser) {
    this.isInternalUser = isInternalUser;
  }

  public String getUserCode() {
    return userCode;
  }

  public void setUserCode(String userCode) {
    this.userCode = userCode;
  }

  public String getPassCode() {
    return passCode;
  }

  public void setPassCode(String passCode) {
    this.passCode = passCode;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public static String normalizeUsername(String username) {
    if (username == null) {
      return null;
    }
    return username.toLowerCase(Locale.ENGLISH);
  }
}
