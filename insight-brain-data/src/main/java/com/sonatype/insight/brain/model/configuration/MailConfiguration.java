/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since MIGRATE_MAIL_CONFIG
 */
@Entity
@Table(name = "mail_configuration")
public class MailConfiguration
    implements HasStringId
{
  @Id
  @Column(name = "mail_configuration_id")
  private String id;

  @Column(name = "hostname")
  private String hostname;

  @Column(name = "port")
  private int port;

  @Column(name = "username")
  private String username;

  @Column(name = "password")
  private String password;

  @Column(name = "ssl_enabled")
  private boolean sslEnabled;

  @Column(name = "start_tls_enabled")
  private boolean startTlsEnabled;

  @Column(name = "system_email")
  private String systemEmail;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isSslEnabled() {
    return sslEnabled;
  }

  public void setSslEnabled(boolean sslEnabled) {
    this.sslEnabled = sslEnabled;
  }

  public boolean isStartTlsEnabled() {
    return startTlsEnabled;
  }

  public void setStartTlsEnabled(boolean startTlsEnabled) {
    this.startTlsEnabled = startTlsEnabled;
  }

  public String getSystemEmail() {
    return systemEmail;
  }

  public void setSystemEmail(String systemEmail) {
    this.systemEmail = systemEmail;
  }
}
