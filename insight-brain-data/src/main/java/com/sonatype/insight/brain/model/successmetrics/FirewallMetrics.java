/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.successmetrics;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.169
 */
@Entity
@Table(name = "firewall_metrics")
public class FirewallMetrics
    implements HasStringId
{
  @Id
  @Column(name = "firewall_metrics_id")
  private String id;

  @Column(name = "metrics_date")
  private Date metricsDate;

  @Column(name = "metrics_name")
  @Enumerated(EnumType.STRING)
  private FirewallMetricsName metricsName;

  @Column(name = "metrics_value")
  private int metricsValue;

  @Column(name = "metrics_last_updated_at")
  private Date metricsLastUpdatedAt;

  public FirewallMetrics() {
  }

  public FirewallMetrics(
      Date metricsDate,
      FirewallMetricsName metricsName,
      int metricsValue)
  {
    this.metricsDate = metricsDate;
    this.metricsName = metricsName;
    this.metricsValue = metricsValue;
    this.metricsLastUpdatedAt = new Date();
  }

  public Date getMetricsDate() {
    return metricsDate;
  }

  public void setMetricsDate(Date metricsDate) {
    this.metricsDate = metricsDate;
  }

  public FirewallMetricsName getMetricsName() {
    return metricsName;
  }

  public void setMetricsName(final FirewallMetricsName metricsName) {
    this.metricsName = metricsName;
  }

  public int getMetricsValue() {
    return metricsValue;
  }

  public void setMetricsValue(final int metricsValue) {
    this.metricsValue = metricsValue;
  }

  public void incrementMetricsValue(final int value) {
    metricsValue += value;
  }

  public Date getMetricsLastUpdatedAt() {
    return metricsLastUpdatedAt;
  }

  public void setMetricsLastUpdatedAt(final Date metricsLastUpdatedAt) {
    this.metricsLastUpdatedAt = metricsLastUpdatedAt;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }
}
