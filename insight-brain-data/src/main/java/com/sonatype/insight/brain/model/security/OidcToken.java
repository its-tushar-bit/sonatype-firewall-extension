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

@Entity
@Table(name = "oidc_token")
public class OidcToken
    implements HasStringId
{
  @Id
  @Column(name = "oidc_token_id")
  private String id;

  @Column(name = "oidc_token")
  private String token;

  @Column(name = "registration_time")
  private Date registrationTime;

  public OidcToken() {
  }

  public OidcToken(String token) {
    this(token, new Date());
  }

  public OidcToken(String token, Date registrationTime) {
    this.token = token;
    this.registrationTime = registrationTime;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getToken() {
    return token;
  }

  public void setToken(final String token) {
    this.token = token;
  }

  public Date getRegistrationTime() {
    return registrationTime;
  }

  public void setRegistrationTime(final Date registrationTime) {
    this.registrationTime = registrationTime;
  }
}
