/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Map;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * @since 1.65
 */
public class DatabaseConfig
{
  @NotNull
  @Pattern(regexp = "postgresql")
  private String type;

  @NotNull
  private String hostname;

  @Min(1)
  @Max(65535)
  private Integer port;

  @NotNull
  private String name;

  @NotNull
  private String username;

  @NotNull
  private String password;

  @Min(1)
  private Integer maxConnections;

  private Map<String, String> parameters;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public Map<String, String> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  public Integer getMaxConnections() {
    return maxConnections;
  }

  public void setMaxConnections(Integer maxConnections) {
    this.maxConnections = maxConnections;
  }
}
