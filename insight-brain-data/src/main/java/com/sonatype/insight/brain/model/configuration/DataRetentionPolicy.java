/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.63
 */
@Entity
@Table(name = "data_retention_policy")
public class DataRetentionPolicy
    implements HasStringId
{
  public static final String CONTEXT_ID_CONTINUOUS_MONITORING = "continuous-monitoring";

  public static final String CONTEXT_ID_SUCCESS_METRICS = "success-metrics";

  @Id
  @Column(name = "data_retention_policy_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "context_id")
  private String contextId;

  @Column(name = "purging_enabled")
  private boolean purgingEnabled;

  @Column(name = "max_count")
  private Integer maxCount;

  @Column(name = "max_age_in_days")
  private Integer maxAgeInDays;

  public DataRetentionPolicy() {
  }

  public DataRetentionPolicy(String ownerId, String contextId) {
    setOwnerId(ownerId);
    setContextId(contextId);
  }

  public DataRetentionPolicy(
      String ownerId,
      String contextId,
      boolean purgingEnabled,
      Integer maxCount,
      Integer maxAgeInDays)
  {
    setOwnerId(ownerId);
    setContextId(contextId);
    setPurgingEnabled(purgingEnabled);
    setMaxCount(maxCount);
    setMaxAgeInDays(maxAgeInDays);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getContextId() {
    return contextId;
  }

  public void setContextId(String contextId) {
    this.contextId = contextId;
  }

  public boolean isPurgingEnabled() {
    return purgingEnabled;
  }

  public void setPurgingEnabled(boolean purgingEnabled) {
    this.purgingEnabled = purgingEnabled;
  }

  public Integer getMaxCount() {
    return maxCount;
  }

  public void setMaxCount(Integer maxCount) {
    this.maxCount = maxCount;
  }

  public Integer getMaxAgeInDays() {
    return maxAgeInDays;
  }

  public void setMaxAgeInDays(Integer maxAgeInDays) {
    this.maxAgeInDays = maxAgeInDays;
  }
}
