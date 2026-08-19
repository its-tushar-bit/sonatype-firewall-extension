/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.searchindex;

import java.util.Date;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "search_index_estate_snapshot")
public class SearchIndexEstateSnapshot
    implements HasStringId
{
  public static final String CURRENT_ID = "CURRENT";

  @Id
  @Column(name = "search_index_estate_snapshot_id")
  private String id = CURRENT_ID;

  @Column(name = "application_count", nullable = false)
  private long applicationCount;

  @Column(name = "violation_count", nullable = false)
  private long violationCount;

  @Column(name = "component_count")
  private Long componentCount;

  @Column(name = "eta_low_minutes")
  private Integer etaLowMinutes;

  @Column(name = "eta_high_minutes")
  private Integer etaHighMinutes;

  @Column(name = "advanced_search_enabled", nullable = false)
  private boolean advancedSearchEnabled;

  @Column(name = "captured_at", nullable = false)
  private Date capturedAt;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public long getApplicationCount() {
    return applicationCount;
  }

  public void setApplicationCount(final long applicationCount) {
    this.applicationCount = applicationCount;
  }

  public long getViolationCount() {
    return violationCount;
  }

  public void setViolationCount(final long violationCount) {
    this.violationCount = violationCount;
  }

  public Long getComponentCount() {
    return componentCount;
  }

  public void setComponentCount(final Long componentCount) {
    this.componentCount = componentCount;
  }

  public Integer getEtaLowMinutes() {
    return etaLowMinutes;
  }

  public void setEtaLowMinutes(final Integer etaLowMinutes) {
    this.etaLowMinutes = etaLowMinutes;
  }

  public Integer getEtaHighMinutes() {
    return etaHighMinutes;
  }

  public void setEtaHighMinutes(final Integer etaHighMinutes) {
    this.etaHighMinutes = etaHighMinutes;
  }

  public boolean isAdvancedSearchEnabled() {
    return advancedSearchEnabled;
  }

  public void setAdvancedSearchEnabled(final boolean advancedSearchEnabled) {
    this.advancedSearchEnabled = advancedSearchEnabled;
  }

  public Date getCapturedAt() {
    return capturedAt;
  }

  public void setCapturedAt(final Date capturedAt) {
    this.capturedAt = capturedAt;
  }
}
