/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.firewall.metrics;

import java.time.LocalDate;
import java.util.Date;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.ApiFirewallMetricsService;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.brain.utils.DateConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.COMPONENTS_QUARANTINED;

@Named
public class QuarantinedComponentMetricsConsolidator
{
  private static final Logger log = LoggerFactory.getLogger(QuarantinedComponentMetricsConsolidator.class);

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private final FirewallMetricsDAO firewallMetricsDAO;

  private final ApiFirewallMetricsService apiFirewallMetricsService;

  public boolean disableForTesting;

  @Inject
  public QuarantinedComponentMetricsConsolidator(
      ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      FirewallMetricsDAO firewallMetricsDAO,
      ApiFirewallMetricsService apiFirewallMetricsService)
  {
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
    this.firewallMetricsDAO = firewallMetricsDAO;
    this.apiFirewallMetricsService = apiFirewallMetricsService;
  }

  public void consolidate() {
    if (!apiFirewallMetricsService.isValidProductLicense()) {
      log.debug("Skipping consolidation of Quarantined Components metrics due to an invalid firewall license");
      return;
    }

    log.info("Consolidating Quarantined Components metrics");
    long start = System.currentTimeMillis();

    Date mostRecentMetricDateFound = firewallMetricsDAO.getMostRecentLastUpdatedAtDateByName(COMPONENTS_QUARANTINED);

    Map<LocalDate, Long> quarantinedComponentsCount;
    if (mostRecentMetricDateFound != null) {
      quarantinedComponentsCount =
          proxyRepositoryComponentDAO.getConsolidatedQuarantinedComponentsMetricByDate(mostRecentMetricDateFound);

    }
    else {
      quarantinedComponentsCount = proxyRepositoryComponentDAO
          .getConsolidatedQuarantinedComponentsMetricByDate(
              DateConverter.toDate(
                  LocalDate.now().minusMonths(12)));
    }

    quarantinedComponentsCount.entrySet()
        .stream()
        .map(entry -> new FirewallMetrics(entry.getKey(), COMPONENTS_QUARANTINED, entry.getValue().intValue()))
        .forEach(firewallMetricsDAO::insertUpdateFirewallMetrics);

    log.info("Consolidation of Quarantined Components metrics done in {} ms.", System.currentTimeMillis() - start);
  }
}
