/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "repository_manager")
public class RepositoryManager
    implements HasStringId
{
  @Id
  @Column(name = "repository_manager_id")
  private String id;

  @Column(name = "instance_id")
  private String instanceId;

  @Column(name = "user_agent")
  private String userAgent;

  @Column(name = "configured")
  private boolean configured;

  @Column(name = "configure_time")
  private Date configureTime;

  public RepositoryManager() {
  }

  public RepositoryManager(String instanceId) {
    this.instanceId = instanceId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public boolean isConfigured() {
    return configured;
  }

  public void setConfigured(boolean configured) {
    this.configured = configured;
  }

  public Date getConfigureTime() {
    return configureTime;
  }

  public void setConfigureTime(Date configureTime) {
    this.configureTime = configureTime;
  }
}
