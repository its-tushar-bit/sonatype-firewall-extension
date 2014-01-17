/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.validation.PortRange;

public class ProxyConfig
{
  @JsonProperty
  private String hostname;

  @PortRange(min = 1)
  @JsonProperty
  private int port = 80;

  @JsonProperty
  private String username;

  @JsonProperty
  private String password;

  public String getHostname() {
    return hostname;
  }

  public int getPort() {
    return port;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public void setHostname(final String hostname) {
    this.hostname = hostname;
  }

  public void setPort(final int port) {
    this.port = port;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
