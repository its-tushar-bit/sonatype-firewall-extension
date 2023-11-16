/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.sonatype.insight.brain.dataaccess.AbstractAggregationSqlDAO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;

import com.sonatype.insight.dataaccess.TransactionContext;

import static java.util.stream.Collectors.toMap;

/**
 * @since 1.169
 */
public class FirewallMetricsDAO
    extends AbstractAggregationSqlDAO<FirewallMetrics>
{
  @Override
  public FirewallMetrics getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM FirewallMetrics entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<FirewallMetrics> getAll() {
    String sQuery = "SELECT entity FROM FirewallMetrics entity";
    return getList(sQuery);
  }

  public Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> getMetricsValueByName() {
    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = "SELECT entity.metricsName," +
          " SUM(entity.metricsValue) as total_metrics_value," +
          " MAX(entity.metricsLastUpdatedAt) as metrics_last_updated_at" +
          " FROM FirewallMetrics entity" +
          " GROUP BY entity.metricsName";

      javax.persistence.Query query = tx.createQuery(sQuery);
      return ((Stream<Object[]>) query.getResultStream()) //
          .collect(toMap(row -> getFirewallMetricsName(row[0].toString()),
              row -> getTotalFirewallMetricsValueAndLatestUpdatedTime(((Number)row[1]).intValue(), (Date)row[2])));
    }
  }

  private static FirewallMetricsName getFirewallMetricsName(String firewallMetricsName) {
    return FirewallMetricsName.valueOf(firewallMetricsName);
  }

  private static ApiFirewallMetricsResultDTO getTotalFirewallMetricsValueAndLatestUpdatedTime(int firewallMetricsValue,
                                                                                      Date latestUpdatedTime)
  {
    return new ApiFirewallMetricsResultDTO(firewallMetricsValue, latestUpdatedTime);
  }
}
