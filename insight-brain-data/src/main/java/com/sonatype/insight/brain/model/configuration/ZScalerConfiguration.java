/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import com.sonatype.insight.brain.security.RotatableSecret;
import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "zscaler_configuration")
public class ZScalerConfiguration
    implements HasStringId
{
  @Id
  @Column(name = "zscaler_configuration_id")
  private String id;

  @Column(name = "username")
  private String username;

  @RotatableSecret
  @Column(name = "password")
  private String password;

  @Column(name = "hostname")
  private String hostname;

  @Column(name = "apikey")
  private String apikey;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(final String hostname) {
    this.hostname = hostname;
  }

  public String getApikey() {
    return apikey;
  }

  public void setApikey(final String apikey) {
    this.apikey = apikey;
  }
}
