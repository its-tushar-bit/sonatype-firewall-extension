/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.firewall.metrics;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiFirewallMetricsService;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.utils.DateConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuarantinedComponentMetricsConsolidator
{
  private static final Logger log = LoggerFactory.getLogger(QuarantinedComponentMetricsConsolidator.class);

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final FirewallMetricsDAO firewallMetricsDAO;

  private final ApiFirewallMetricsService apiFirewallMetricsService;

  public boolean disableForTesting;

  @Inject
  public QuarantinedComponentMetricsConsolidator(
      RepositoryComponentDAO repositoryComponentDAO,
      FirewallMetricsDAO firewallMetricsDAO,
      ApiFirewallMetricsService apiFirewallMetricsService
  )
  {
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.firewallMetricsDAO = firewallMetricsDAO;
    this.apiFirewallMetricsService = apiFirewallMetricsService;
  }

  public void consolidate() {
    try {
      apiFirewallMetricsService.checkProductLicense();
    }
    catch (InvalidLicenseException invalidLicenseException) {
      // If invalid license, do nothing
      log.debug("Skipping Consolidation of Quarantined Components metrics due to an " +
          "invalid firewall license");
      return;
    }
    log.info("Consolidating Quarantined Components metrics");
    long start = System.currentTimeMillis();

    firewallMetricsDAO.deleteRecordsOlderThanOneYear(FirewallMetricsName
        .COMPONENTS_QUARANTINED);
    Date mostRecentMetricDateFound = firewallMetricsDAO.getMostRecentLastUpdatedAtDateByName(
        FirewallMetricsName.COMPONENTS_QUARANTINED);

    Map<LocalDate, Long> quarantinedComponentsCount;
    if (mostRecentMetricDateFound != null) {
      quarantinedComponentsCount = repositoryComponentDAO
          .getConsolidatedQuarantinedComponentsMetricByDate(mostRecentMetricDateFound);

    }
    else {
      quarantinedComponentsCount = repositoryComponentDAO
          .getConsolidatedQuarantinedComponentsMetricByDate(
              DateConverter.toDate(
                  LocalDate.now().minusMonths(12)
              )
          );
    }
    List<FirewallMetrics> firewallMetricsToInsertOrUpdate =
        quarantinedComponentsCount.entrySet().stream().map( entry -> new FirewallMetrics(
            entry.getKey(),
            FirewallMetricsName.COMPONENTS_QUARANTINED,
            entry.getValue().intValue()
        )).collect(Collectors.toList());
    for (FirewallMetrics fm : firewallMetricsToInsertOrUpdate) {
      firewallMetricsDAO.insertUpdateFirewallMetrics(fm);
    }

    log.info("Updated component categories in {} ms.", System.currentTimeMillis() - start);
  }
}
