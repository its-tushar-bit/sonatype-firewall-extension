/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.ApiFirewallMetricsService;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;

import com.google.common.eventbus.Subscribe;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class CreateRepositoryPolicyViolationsEventHandler
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(CreateRepositoryPolicyViolationsEventHandler.class);

  private final AsyncEventBus eventBus;

  private final ApiFirewallMetricsService firewallMetricsService;

  @Inject
  public CreateRepositoryPolicyViolationsEventHandler(
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
  public void onRepositoryPolicyViolationsCreated(CreateRepositoryPolicyViolationsEvent event) {
    if (!firewallMetricsService.isValidProductLicense()) {
      log.debug("Invalid product license to create Firewall Metrics");
      return;
    }

    if (CollectionUtils.isEmpty(event.repositoryPolicyViolations)) {
      log.debug("No repository policy violations to process");
      return;
    }

    long start = System.currentTimeMillis();
    log.info("Start processing repository policy violations for Firewall Metrics");

    Map<LocalDate, FirewallMetrics> namespaceAttacksBlockedMetrics = new HashMap<>();
    Map<LocalDate, FirewallMetrics> supplyChainAttacksBlockedMetrics = new HashMap<>();

    for (RepositoryPolicyViolation repositoryPolicyViolation : event.repositoryPolicyViolations) {
      firewallMetricsService.checkFirewallMetricsInRepositoryPolicyViolation(repositoryPolicyViolation,
          namespaceAttacksBlockedMetrics, supplyChainAttacksBlockedMetrics);
    }

    namespaceAttacksBlockedMetrics.values().stream().forEach(firewallMetricsService::incrementFirewallMetrics);
    supplyChainAttacksBlockedMetrics.values().stream().forEach(firewallMetricsService::incrementFirewallMetrics);

    log.info("Finished processing repository policy violations for Firewall Metrics in {} ms",
        System.currentTimeMillis() - start);
  }
}
