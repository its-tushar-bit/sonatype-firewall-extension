/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * Configuration for CI integrations (e.g., GitHub Actions, GitLab CI).
 */
@Entity
@Table(name = "ci_integrations_config")
public class CiIntegrationsConfig
    implements HasStringId
{
  @Id
  @Column(name = "ci_integrations_config_id")
  private String id;

  @Column(name = "configuration_json")
  private String configurationJson;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "owner_type")
  private String ownerType;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "update_time")
  private Date updateTime;

  public CiIntegrationsConfig() {
  }

  public CiIntegrationsConfig(String ownerId, String ownerType, String configurationJson) {
    this.ownerId = ownerId;
    this.ownerType = ownerType;
    this.configurationJson = configurationJson;
    this.createTime = new Date();
    this.updateTime = new Date();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getConfigurationJson() {
    return configurationJson;
  }

  public void setConfigurationJson(String configurationJson) {
    this.configurationJson = configurationJson;
    this.updateTime = new Date();
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getOwnerType() {
    return ownerType;
  }

  public void setOwnerType(String ownerType) {
    this.ownerType = ownerType;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public Date getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }
}
