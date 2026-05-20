/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.consumption;

import java.util.Objects;

import com.sonatype.insight.model.HasStringId;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Configuration for consumption limits at the organization level.
 *
 * @since 1.204
 */
@Entity
@Table(name = "consumption_limit_config")
public class ConsumptionLimitConfig
    implements HasStringId
{
  @Id
  @Column(name = "id")
  private String id;

  @Column(name = "org_id", nullable = false)
  private String orgId;

  @Nullable
  @Column(name = "monthly_limit")
  private Long monthlyLimit;

  @Column(name = "warning_threshold_pct")
  private int warningThresholdPct = 80;

  @Column(name = "enforcement_mode")
  private EnforcementMode enforcementMode = EnforcementMode.SOFT;

  public ConsumptionLimitConfig() {
    // Default constructor for serialization/deserialization
  }

  public ConsumptionLimitConfig(String orgId) {
    this.orgId = orgId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getOrgId() {
    return orgId;
  }

  public void setOrgId(String orgId) {
    this.orgId = orgId;
  }

  @Nullable
  public Long getMonthlyLimit() {
    return monthlyLimit;
  }

  public void setMonthlyLimit(@Nullable Long monthlyLimit) {
    if (monthlyLimit != null && monthlyLimit <= 0) {
      throw new IllegalArgumentException(
          "monthlyLimit must be positive, got: " + monthlyLimit);
    }
    this.monthlyLimit = monthlyLimit;
  }

  public int getWarningThresholdPct() {
    return warningThresholdPct;
  }

  public void setWarningThresholdPct(int warningThresholdPct) {
    if (warningThresholdPct < 0 || warningThresholdPct > 100) {
      throw new IllegalArgumentException(
          "warningThresholdPct must be between 0 and 100, got: " + warningThresholdPct);
    }
    this.warningThresholdPct = warningThresholdPct;
  }

  public EnforcementMode getEnforcementMode() {
    return enforcementMode;
  }

  public void setEnforcementMode(EnforcementMode enforcementMode) {
    this.enforcementMode = Objects.requireNonNull(enforcementMode, "enforcementMode");
  }
}
