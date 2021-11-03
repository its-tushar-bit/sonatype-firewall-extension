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
 * @since 1.127
 */
@Entity
@Table(name = "repository_client_configuration")
public class RepositoryClientConfiguration
    implements HasStringId
{
  @Id
  @Column(name = "repository_client_configuration_id")
  private String id;

  @Column(name = "connection_timeout")
  private int connectionTimeout = 30;

  @Column(name = "socket_timeout")
  private int socketTimeout = 60 * 2;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(final int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public int getSocketTimeout() {
    return socketTimeout;
  }

  public void setSocketTimeout(final int socketTimeout) {
    this.socketTimeout = socketTimeout;
  }
}
