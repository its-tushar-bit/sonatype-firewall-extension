/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

import static java.util.stream.Collectors.toList;

/**
 * @since MIGRATE_PROXY_CONFIG
 */
@Entity
@Table(name = "proxy_configuration")
public class ProxyConfiguration
    implements HasStringId
{
  @Id
  @Column(name = "proxy_configuration_id")
  private String id;

  @Column(name = "hostname")
  private String hostname;

  @Column(name = "port")
  private int port;

  @Column(name = "username")
  private String username;

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

  public List<String> getExcludeHostsList() {
    if (excludeHosts == null) {
      return Collections.emptyList();
    }

    return Stream.of(excludeHosts.split("[,\\s]")).filter(proxy -> !proxy.isEmpty()).collect(toList());
  }
}
