/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.time.LocalDate;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.ApiFirewallMetricsService;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;

import com.google.common.eventbus.Subscribe;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class RequestSafeComponentsAutoSelectMetricEventHandler
    implements Managed
{
  private static final Logger log = LoggerFactory
      .getLogger(RequestSafeComponentsAutoSelectMetricEventHandler.class);

  private final AsyncEventBus eventBus;

  private final ApiFirewallMetricsService firewallMetricsService;

  @Inject
  public RequestSafeComponentsAutoSelectMetricEventHandler(
      AsyncEventBus eventBus,
      ApiFirewallMetricsService firewallMetricsService)
  {
    this.eventBus = eventBus;
    this.firewallMetricsService = firewallMetricsService;
  }

  @Override
  public void start() throws Exception {
    eventBus.register(this);
  }

  @Override
  public void stop() throws Exception {
    eventBus.unregister(this);
  }

  @Subscribe
  public void onSafeComponentsAutoSelectMetricRequested(
      @SuppressWarnings("unused") RequestSafeComponentsAutoSelectMetricEvent event)
  {
    if (!firewallMetricsService.isValidProductLicense()) {
      log.debug("Firewall Metrics not collected due to Next-Gen Firewall not enabled");
      return;
    }

    FirewallMetrics firewallMetrics = new FirewallMetrics(LocalDate.now(),
        FirewallMetricsName.SAFE_VERSIONS_SELECTED_AUTOMATICALLY, 1);
    firewallMetricsService.incrementFirewallMetrics(firewallMetrics);

    log.info("Request of safe components auto-selected for Firewall Metrics saved");
  }
}
