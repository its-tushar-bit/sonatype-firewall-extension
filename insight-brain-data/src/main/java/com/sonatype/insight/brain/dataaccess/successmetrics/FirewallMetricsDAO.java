/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.time.LocalDate;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.LockModeType;
import jakarta.persistence.RollbackException;

import com.sonatype.insight.brain.dataaccess.AbstractAggregationSqlDAO;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.dataaccess.TransactionContext;

import static java.util.stream.Collectors.toMap;

/**
 * @since 1.169
 */
@Named
@Singleton
public class FirewallMetricsDAO
    extends AbstractAggregationSqlDAO<FirewallMetrics>
{
  @Inject
  public FirewallMetricsDAO(AggregationDataStore aggregationDataStore) {
    super(aggregationDataStore);
  }

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

  @SuppressWarnings("unchecked")
  public Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> getMetricsValueByName() {
    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = "SELECT entity.metricsName," +
          " SUM(entity.metricsValue) as total_metrics_value," +
          " MAX(entity.metricsLastUpdatedAt) as metrics_last_updated_at" +
          " FROM FirewallMetrics entity" +
          " WHERE ((entity.metricsName IN ?1)" +
          " OR (entity.metricsName NOT IN ?1 AND entity.metricsDate >= ?2))" +
          " GROUP BY entity.metricsName";

      Set<FirewallMetricsName> firewallMetricsNamesForAllTime =
          EnumSet.of(FirewallMetricsName.SUPPLY_CHAIN_ATTACKS_BLOCKED, FirewallMetricsName.NAMESPACE_ATTACKS_BLOCKED);
      LocalDate oneYearAgoDate = LocalDate.now().minusMonths(12);

      jakarta.persistence.Query query = tx.createQuery(sQuery);
      query.setParameter(1, firewallMetricsNamesForAllTime);
      query.setParameter(2, oneYearAgoDate);

      return ((Stream<Object[]>) query.getResultStream()) //
          .collect(toMap(row -> getFirewallMetricsName(row[0].toString()),
              row -> getTotalFirewallMetricsValueAndLatestUpdatedTime(((Number) row[1]).intValue(), (Date) row[2])));
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
        new Query<>(sQuery,
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
