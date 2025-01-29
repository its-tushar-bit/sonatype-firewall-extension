/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.crowd;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.brain.security.RotatableSecret;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.134
 */
@Entity
@Table(name = "crowd_configuration")
public class CrowdConfiguration
    implements HasStringId
{
  @Id
  @Column(name = "crowd_configuration_id")
  private String id;

  @Column(name = "server_url")
  private String serverUrl;

  @Column(name = "application_name")
  private String applicationName;

  @RotatableSecret
  @Column(name = "application_password")
  private char[] applicationPassword;

  public CrowdConfiguration() {
  }

  public CrowdConfiguration(String serverUrl, String applicationName, char[] applicationPassword) {
    this.serverUrl = serverUrl;
    this.applicationName = applicationName;
    this.applicationPassword = applicationPassword;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public void setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  public char[] getApplicationPassword() {
    return applicationPassword;
  }

  public void setApplicationPassword(char[] applicationPassword) {
    this.applicationPassword = applicationPassword;
  }
}
