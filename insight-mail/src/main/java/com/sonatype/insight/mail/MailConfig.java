/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mail;

import java.util.Arrays;

public class MailConfig
{
  private String hostname;

  private int port;

  private boolean disableSend;

  private boolean debug;

  private boolean ssl;

  private boolean tls;

  private String username;

  private char[] password;

  private String systemEmail;

  private String systemPersonal;

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

  public char[] getPassword() {
    return password;
  }

  public void setPassword(char[] password) {
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

  public void clearPassword() {
    if (password != null) {
      Arrays.fill(password, '0');
    }
  }
}
