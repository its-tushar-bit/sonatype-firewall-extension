/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.brain.security.RotatableSecret;
import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.annotation.JsonIgnore;

import static java.util.stream.Collectors.toList;

/**
 * @since 1.84
 */
@Entity
@Table(name = "proxy_server_configuration")
public class ProxyServerConfiguration
    implements HasStringId
{
  @Id
  @Column(name = "proxy_server_configuration_id")
  private String id;

  @Column(name = "hostname")
  private String hostname;

  @Column(name = "port")
  private int port;

  @Column(name = "username")
  private String username;

  @RotatableSecret
  @Column(name = "password")
  private char[] password;

  @Column(name = "exclude_hosts")
  private String excludeHosts;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
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

  public char[] getPassword() {
    return password;
  }

  public void setPassword(final char[] password) {
    this.password = password;
  }

  public String getExcludeHosts() {
    return excludeHosts;
  }

  public void setExcludeHosts(String excludeHosts) {
    this.excludeHosts = excludeHosts;
  }

  @JsonIgnore
  public List<String> getExcludeHostsList() {
    if (excludeHosts == null) {
      return Collections.emptyList();
    }

    return Stream.of(excludeHosts.split("[,\\s]")).filter(proxy -> !proxy.isEmpty()).collect(toList());
  }
}
