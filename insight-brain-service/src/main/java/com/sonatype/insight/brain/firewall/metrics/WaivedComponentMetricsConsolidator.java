/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.firewall.metrics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.ApiFirewallMetricsService;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.brain.tenancy.TenantAwareFunction;
import com.sonatype.insight.brain.tenancy.TenantAwareSupplier;
import com.sonatype.insight.brain.utils.DateConverter;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.brain.utils.ExecutorThreadPools.ThreadPools;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.WAIVED_COMPONENTS;
import static java.util.stream.Collectors.toList;

@Named
public class WaivedComponentMetricsConsolidator
{
  private static final Logger log = LoggerFactory.getLogger(WaivedComponentMetricsConsolidator.class);

  private final PolicyWaiverDAO policyWaiverDAO;

  private final FirewallMetricsDAO firewallMetricsDAO;

  private final RepositoryDAO repositoryDAO;

  private final ApiFirewallMetricsService apiFirewallMetricsService;

  @Inject
  public WaivedComponentMetricsConsolidator(
      PolicyWaiverDAO policyWaiverDAO,
      FirewallMetricsDAO firewallMetricsDAO,
      RepositoryDAO repositoryDAO,
      ApiFirewallMetricsService apiFirewallMetricsService)
  {
    this.policyWaiverDAO = policyWaiverDAO;
    this.firewallMetricsDAO = firewallMetricsDAO;
    this.repositoryDAO = repositoryDAO;
    this.apiFirewallMetricsService = apiFirewallMetricsService;
  }

  public void consolidate() {
    if (!apiFirewallMetricsService.isValidProductLicense()) {
      log.debug("Skipping consolidation of Waived Components metrics due to Next-Gen Firewall not enabled");
      return;
    }
    log.info("Consolidating Waived Components metrics");
    long start = System.currentTimeMillis();
    List<Repository> repositories = repositoryDAO.getAll();
    Date mostRecentMetricDateFound = ObjectUtils.defaultIfNull(
        firewallMetricsDAO.getMostRecentLastUpdatedAtDateByName(WAIVED_COMPONENTS),
        DateConverter.toDate(LocalDate.now().minusMonths(12)));

    List<List<FirewallMetrics>> allMetrics = CompletableFuture.supplyAsync(new TenantAwareSupplier<>(() -> repositories
        .parallelStream()
        .map(new TenantAwareFunction<Repository, List<FirewallMetrics>>(repository -> {
          List<FirewallMetrics> repositoryMetrics = new ArrayList<>();

          Map<LocalDate, Long> results = policyWaiverDAO.getCountByOwnerIdAndDate(repository.getId(),
              mostRecentMetricDateFound);

          for (Entry<LocalDate, Long> entry : results.entrySet()) {
            repositoryMetrics.add(new FirewallMetrics(entry.getKey(), WAIVED_COMPONENTS,
                entry.getValue().intValue()));
          }
          return repositoryMetrics;
        }))
        .collect(toList())), ExecutorThreadPools.getInstance().getThreadPool(ThreadPools.GENERAL)).join();

    for (List<FirewallMetrics> metricsList : allMetrics) {
      for (FirewallMetrics fm : metricsList) {
        firewallMetricsDAO.insertUpdateFirewallMetrics(fm);
      }
    }
    log.info("Consolidated waived components in {} ms.", System.currentTimeMillis() - start);
  }
}
