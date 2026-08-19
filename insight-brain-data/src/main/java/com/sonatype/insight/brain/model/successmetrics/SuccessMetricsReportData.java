/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.successmetrics;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.39
 */
@Entity
@Table(name = "success_metrics_report_data")
public class SuccessMetricsReportData
    implements HasStringId
{
  @Id
  @Column(name = "success_metrics_report_data_id")
  private String id;

  @Column(name = "last_updated")
  private Date lastUpdated;

  @Column(name = "included_application_ids_json")
  private String includedApplicationIdsJson;

  @Column(name = "month_count")
  private int monthCount;

  @Column(name = "active_application_count")
  private int activeApplicationCount;

  @Column(name = "chart_data_json")
  private String chartDataJson;

  public SuccessMetricsReportData() {
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public int getMonthCount() {
    return monthCount;
  }

  public void setMonthCount(int monthCount) {
    this.monthCount = monthCount;
  }

  public int getActiveApplicationCount() {
    return activeApplicationCount;
  }

  public void setActiveApplicationCount(int activeApplicationCount) {
    this.activeApplicationCount = activeApplicationCount;
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public Set<String> getIncludedApplicationIds() {
    try {
      @SuppressWarnings("unchecked")
      Set<String> retval = JsonUtils.parse(includedApplicationIdsJson, Set.class);

      return retval;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void setIncludedApplicationIds(Set<String> includedApplicationIds) {
    this.includedApplicationIdsJson = JsonUtils.format(includedApplicationIds);
  }

  public void setChartDataJson(String chartDataJson) {
    this.chartDataJson = chartDataJson;
  }

  public String getChartDataJson() {
    return chartDataJson;
  }
}
