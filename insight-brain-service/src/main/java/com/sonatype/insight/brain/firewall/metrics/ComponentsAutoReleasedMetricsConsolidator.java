/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.firewall.metrics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.ApiFirewallMetricsService;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.brain.utils.DateConverter;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.brain.utils.ExecutorThreadPools.ThreadPools;

import com.sonatype.insight.brain.tenancy.TenantAwareFunction;
import com.sonatype.insight.brain.tenancy.TenantAwareSupplier;

import org.apache.commons.lang3.ObjectUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.COMPONENTS_AUTO_RELEASED;
import static java.util.stream.Collectors.toList;

@Named
public class ComponentsAutoReleasedMetricsConsolidator
{
  private static final Logger log = LoggerFactory.getLogger(ComponentsAutoReleasedMetricsConsolidator.class);

  private final RepositoryDAO repositoryDAO;

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private final FirewallMetricsDAO firewallMetricsDAO;

  private final ApiFirewallMetricsService apiFirewallMetricsService;

  public boolean disableForTesting;

  @Inject
  public ComponentsAutoReleasedMetricsConsolidator(
      RepositoryDAO repositoryDAO,
      ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      FirewallMetricsDAO firewallMetricsDAO,
      ApiFirewallMetricsService apiFirewallMetricsService)
  {
    this.repositoryDAO = repositoryDAO;
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
    this.firewallMetricsDAO = firewallMetricsDAO;
    this.apiFirewallMetricsService = apiFirewallMetricsService;
  }

  public void consolidate() {
    if (!apiFirewallMetricsService.isValidProductLicense()) {
      log.debug("Skipping Consolidation of Auto-released Components metrics due to Next-Gen Firewall not enabled");
      return;
    }

    log.info("Consolidating Auto-released Components metrics");

    long start = System.currentTimeMillis();

    List<Repository> repositories = repositoryDAO.getAll();

    Date mostRecentMetricDateFound = ObjectUtils.defaultIfNull(
        firewallMetricsDAO.getMostRecentLastUpdatedAtDateByName(COMPONENTS_AUTO_RELEASED),
        DateConverter.toDate(LocalDate.now().minusMonths(12)));

    List<List<FirewallMetrics>> allMetrics = CompletableFuture.supplyAsync(new TenantAwareSupplier<>(() -> repositories
        .parallelStream()
        .map(new TenantAwareFunction<Repository, List<FirewallMetrics>>(repository -> {
          List<FirewallMetrics> repositoryMetrics = new ArrayList<>();

          Map<LocalDate, Long> autoReleasedComponentsCount = proxyRepositoryComponentDAO
              .getAutoReleaseQuarantinedCountByRepositoryIdAndDate(
                  repository.getId(), mostRecentMetricDateFound, true);

          for (Entry<LocalDate, Long> entry : autoReleasedComponentsCount.entrySet()) {
            repositoryMetrics.add(new FirewallMetrics(entry.getKey(), COMPONENTS_AUTO_RELEASED,
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

    log.info("Consolidation of Auto-released Components metrics done in {} ms.", System.currentTimeMillis() - start);
  }
}
