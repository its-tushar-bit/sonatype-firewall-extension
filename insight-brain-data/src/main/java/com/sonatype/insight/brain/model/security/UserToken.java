/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

  @Column(name = "user_code")
  private String userCode;

  @Column(name = "pass_code")
  private String passCode;

  /**
   * The id of the realm that authenticated the user that created this user token.
   *
   * @see UserPrincipal#realmId
   */
  @Column(name = "realm_id")
  private String realmId;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "last_access_time")
  private Date lastAccessTime;

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
  }

  public boolean isInternalUser() {
    return User.INTERNAL_REALM_ID.equals(realmId);
  }

  public boolean isSsoUser() {
    return SamlUser.SAML_REALM_ID.equals(realmId) || OAuth2User.OAUTH2_REALM_ID.equals(realmId);
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

  public String getRealmId() {
    return realmId;
  }

  public void setRealmId(String realmId) {
    this.realmId = realmId;
  }

  public Date getLastAccessTime() {
    return lastAccessTime;
  }

  public void setLastAccessTime(Date lastAccessTime) {
    this.lastAccessTime = lastAccessTime;
  }
}
