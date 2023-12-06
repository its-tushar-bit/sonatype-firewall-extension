/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.persistence.EntityExistsException;
import javax.persistence.LockModeType;
import javax.persistence.RollbackException;

import com.sonatype.insight.brain.dataaccess.AbstractAggregationSqlDAO;
import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
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

  public Date getMostRecentLastUpdatedAtDateByName(FirewallMetricsName metricName) {
    String sQuery = "SELECT MAX(entity.metricsLastUpdatedAt) " +
        " FROM FirewallMetrics entity" +
        " WHERE entity.metricsName=?1";

    return getSingle(Date.class, sQuery, metricName);
  }

  public void deleteRecordsOlderThanOneYear(FirewallMetricsName metricsName) {
    String sQuery = "SELECT entity " +
            "FROM FirewallMetrics entity WHERE " +
            "entity.metricsName = ?1 AND " +
            "entity.metricsDate < ?2";

    LocalDate oneYearAgoDate = LocalDate.now().minusMonths(12);
    List<FirewallMetrics> toBeDeleted = getList(sQuery,
        metricsName, oneYearAgoDate);
    toBeDeleted.forEach(this::delete);
  }

  @SuppressWarnings("unchecked")
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

  public FirewallMetrics insertUpdateFirewallMetrics(FirewallMetrics newFirewallMetrics) {
    FirewallMetrics resultFirewallMetrics;
    FirewallMetrics existingFirewallMetrics;
    String sQuery = "SELECT entity" +
        " FROM FirewallMetrics entity" +
        " WHERE entity.metricsDate=?1" +
        " AND entity.metricsName=?2";

    Query<FirewallMetrics> query =
        new Query<FirewallMetrics>(sQuery,
            newFirewallMetrics.getMetricsDate(), newFirewallMetrics.getMetricsName());
    // need a 'select for update' type query - this is how to do it in JPA
    query.setLockModeType(LockModeType.PESSIMISTIC_WRITE);

    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();

      existingFirewallMetrics = query.get(tx);
      if (existingFirewallMetrics != null) {
        int newFirewallMetricsValue = newFirewallMetrics.getMetricsValue() + existingFirewallMetrics.getMetricsValue();
        existingFirewallMetrics.setMetricsValue(newFirewallMetricsValue);
        existingFirewallMetrics.setMetricsLastUpdatedAt(new Date());
        update(tx, existingFirewallMetrics);
        resultFirewallMetrics = existingFirewallMetrics;
      }
      else {
        insert(tx, newFirewallMetrics);
        resultFirewallMetrics = newFirewallMetrics;
      }

      tx.commit();
    }
    catch (RollbackException e) {
      if (e.getCause() instanceof EntityExistsException) {
        return insertUpdateFirewallMetrics(newFirewallMetrics);
      }
      throw e;
    }
    return resultFirewallMetrics;
  }

  private static FirewallMetricsName getFirewallMetricsName(String firewallMetricsName) {
    return FirewallMetricsName.valueOf(firewallMetricsName);
  }

  private static ApiFirewallMetricsResultDTO getTotalFirewallMetricsValueAndLatestUpdatedTime(
      int firewallMetricsValue,
      Date latestUpdatedTime)
  {
    return new ApiFirewallMetricsResultDTO(firewallMetricsValue, latestUpdatedTime);
  }

  public LocalDate getEarliestMetricDateByName(FirewallMetricsName metricsName) {
    String sQuery = "SELECT MIN(entity.metricsDate)" + //
        " FROM FirewallMetrics entity" + //
        " WHERE entity.metricsName = ?1";
    return getSingle(LocalDate.class, sQuery, metricsName);
  }
}
