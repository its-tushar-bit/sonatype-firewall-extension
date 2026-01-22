/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.apache.shiro.util.CollectionUtils;

@Named
@Singleton
public class TelemetryQueue
{
  private final TenantReference<List<TelemetryData>> telemetryData = new TenantReference<>(ArrayList::new);

  // we're using a provider here to avoid circular dependency issues
  private final Provider<TelemetrySender> telemetrySenderProvider;

  @Inject
  public TelemetryQueue(Provider<TelemetrySender> telemetrySenderProvider) {
    this.telemetrySenderProvider = telemetrySenderProvider;
  }

  public void add(TelemetryData telemetryData) {
    this.telemetryData.get().add(telemetryData);
  }

  public void flush() {
    var dataList = telemetryData.get();
    if (CollectionUtils.isEmpty(dataList)) {
      return;
    }

    // we need to send a copy of the list since we clear the original immediately after this call
    telemetrySenderProvider.get().send(new ArrayList<>(dataList));
    dataList.clear();
  }
}
