/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.List;

import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.collections4.CollectionUtils;
import org.quartz.JobExecutionContext;

/**
 * For paginated telemetry collectors we send a single telemetry object with a page worth of data as a list in the
 * 'data-list' attribute
 */
public abstract class PaginatedTelemetryCollectorImpl
    implements PaginatedTelemetryCollector
{
  public static final String DATA_LIST = "data-list";

  private final TenantReference<Integer> currentPageNumber = new TenantReference<>(() -> 1);

  private final TenantReference<Boolean> hasMoreData = new TenantReference<>(() -> true);

  private final int pageSize;

  private final TelemetryPurpose telemetryPurpose;

  protected PaginatedTelemetryCollectorImpl(TelemetryPurpose telemetryPurpose, int pageSize) {
    this.telemetryPurpose = telemetryPurpose;
    this.pageSize = pageSize;
  }

  @Override
  public List<TelemetryData> collectAllData() {
    throw new UnsupportedOperationException("collectAllData is not supported for paginated telemetry collectors");
  }

  @Override
  public TelemetryData collectData() {
    throw new UnsupportedOperationException("collectData is not supported for paginated telemetry collectors");
  }

  @Override
  public List<TelemetryData> collectAllData(JobExecutionContext jobExecutionContext) {
    throw new UnsupportedOperationException("collectAllData is not supported for paginated telemetry collectors");
  }

  @Override
  public TelemetryData collectData(JobExecutionContext jobExecutionContext) {
    throw new UnsupportedOperationException("collectData is not supported for paginated telemetry collectors");
  }

  @Override
  public TelemetryData firstPage() {
    currentPageNumber.set(1);
    hasMoreData.set(true);
    return nextPage();
  }

  @Override
  public boolean hasMoreData() {
    return hasMoreData.get();
  }

  @Override
  public TelemetryData nextPage() {
    if (Boolean.FALSE.equals(hasMoreData.get())) {
      throw new IllegalStateException("trying to read beyond last page of data");
    }

    List<?> data = collectData(currentPageNumber.get());

    hasMoreData.set(!CollectionUtils.isEmpty(data));
    currentPageNumber.set(currentPageNumber.get() + 1);

    return new TelemetryData(telemetryPurpose)
        .put(DATA_LIST, CollectionUtils.isEmpty(data) ? List.of() : data);
  }

  protected abstract List<?> collectData(int pageNumber);

  protected int getPageSize() {
    return pageSize;
  }
}
