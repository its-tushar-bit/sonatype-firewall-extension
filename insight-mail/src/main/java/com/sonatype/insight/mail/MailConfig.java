/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mail;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.sisu.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class MailConfig
{
  private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

  private String hostname;

  private int port;

  private boolean disableSend;

  private boolean debug;

  private boolean ssl;

  private boolean tls;

  private String username;

  private String password;

  private String systemEmail;

  private String systemPersonal;

  @Inject
  public MailConfig(@Named("${mail.hostname}") String hostname, @Named("${mail.port}") int port,
      @Named("${mail.disablesend:-false}") boolean disableSend, @Named("${mail.debug:-false}") boolean debug,
      @Named("${mail.ssl:-false}") boolean ssl, @Named("${mail.tls:-false}") boolean tls,
      @Named("${mail.username}") @Nullable String username, @Named("${mail.password}") @Nullable String password,
      @Named("${mail.systemEmail}") @Nullable String systemEmail,
      @Named("${mail.systemPersonal}") @Nullable String systemPersonal)
  {
    this.hostname = hostname;
    this.port = port;
    this.disableSend = disableSend;
    this.debug = debug;
    this.ssl = ssl;
    this.tls = tls;
    this.username = username;
    this.password = password;
    this.systemEmail = systemEmail;
    this.systemPersonal = systemPersonal;
    log.debug("Loaded mail configuration");
  }

  protected MailConfig() {
    // so we can re-use this config outside sisu
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getHostname() {
    return hostname;
  }

  public int getPort() {
    return port;
  }

  public boolean isDisableSend() {
    return disableSend;
  }

  public boolean isDebug() {
    return debug;
  }

  public boolean isSsl() {
    return ssl;
  }

  public void setSsl(boolean ssl) {
    this.ssl = ssl;
  }

  public boolean isTls() {
    return tls;
  }

  public void setTls(boolean tls) {
    this.tls = tls;
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

  public String getSystemEmail() {
    return systemEmail;
  }

  public void setSystemEmail(String systemEmail) {
    this.systemEmail = systemEmail;
  }

  public String getSystemPersonal() {
    return systemPersonal;
  }

  public void setSystemPersonal(String systemPersonal) {
    this.systemPersonal = systemPersonal;
  }
}
