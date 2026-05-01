/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.scanhealth;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * Configuration for Scan Health features (e.g., fail on zero components).
 */
@Entity
@Table(name = "scan_health_config")
public class ScanHealthConfig
    implements HasStringId
{
  @Id
  @Column(name = "scan_health_config_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "owner_type")
  private String ownerType;

  @Column(name = "configuration_json")
  private String configurationJson;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "update_time")
  private Date updateTime;

  public ScanHealthConfig() {
  }

  public ScanHealthConfig(final String ownerId, final String ownerType, final String configurationJson) {
    this.ownerId = ownerId;
    this.ownerType = ownerType;
    this.configurationJson = configurationJson;
    final Date now = new Date();
    this.createTime = now;
    this.updateTime = now;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(final String ownerId) {
    this.ownerId = ownerId;
  }

  public String getOwnerType() {
    return ownerType;
  }

  public void setOwnerType(final String ownerType) {
    this.ownerType = ownerType;
  }

  public String getConfigurationJson() {
    return configurationJson;
  }

  public void setConfigurationJson(final String configurationJson) {
    this.configurationJson = configurationJson;
    this.updateTime = new Date();
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(final Date createTime) {
    this.createTime = createTime;
  }

  public Date getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(final Date updateTime) {
    this.updateTime = updateTime;
  }
}
