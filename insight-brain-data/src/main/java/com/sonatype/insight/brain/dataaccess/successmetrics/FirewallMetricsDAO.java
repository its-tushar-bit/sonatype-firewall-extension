/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.time.LocalDate;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractAggregationSqlDAO;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.aggregation.tables.FirewallMetrics.FIREWALL_METRICS;

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

  public Date getMostRecentLastUpdatedAtDateByName(FirewallMetricsName metricName) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(DSL.max(FIREWALL_METRICS.METRICS_LAST_UPDATED_AT))
          .from(FIREWALL_METRICS)
          .where(FIREWALL_METRICS.METRICS_NAME.eq(metricName.name()))
          .fetchOneInto(Date.class);
    }
  }

  public Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> getMetricsValueByName() {
    try (TransactionContext tx = createTransactionContext()) {
      Set<FirewallMetricsName> firewallMetricsNamesForAllTime =
          EnumSet.of(FirewallMetricsName.SUPPLY_CHAIN_ATTACKS_BLOCKED, FirewallMetricsName.NAMESPACE_ATTACKS_BLOCKED);
      LocalDate oneYearAgoDate = LocalDate.now().minusMonths(12);

      // Convert enum set to string list for the query
      List<String> allTimeMetricsNames = firewallMetricsNamesForAllTime.stream()
          .map(FirewallMetricsName::name)
          .toList();

      Result<Record3<String, Integer, Date>> results = tx.dsl()
          .select(
              FIREWALL_METRICS.METRICS_NAME,
              DSL.sum(FIREWALL_METRICS.METRICS_VALUE).cast(Integer.class),
              DSL.max(FIREWALL_METRICS.METRICS_LAST_UPDATED_AT))
          .from(FIREWALL_METRICS)
          .where(FIREWALL_METRICS.METRICS_NAME.in(allTimeMetricsNames)
              .or(FIREWALL_METRICS.METRICS_NAME.notIn(allTimeMetricsNames)
                  .and(FIREWALL_METRICS.METRICS_DATE.greaterOrEqual(oneYearAgoDate))))
          .groupBy(FIREWALL_METRICS.METRICS_NAME)
          .fetch();

      Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> resultMap = new HashMap<>();
      for (Record3<String, Integer, Date> row : results) {
        FirewallMetricsName metricsName = getFirewallMetricsName(row.value1());
        Integer totalValue = row.value2();
        Date lastUpdatedAt = row.value3();
        resultMap.put(metricsName, getTotalFirewallMetricsValueAndLatestUpdatedTime(
            totalValue != null ? totalValue : 0, lastUpdatedAt));
      }
      return resultMap;
    }
  }

  public FirewallMetrics insertUpdateFirewallMetrics(FirewallMetrics newFirewallMetrics) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();

      // Select for update to lock the row
      FirewallMetrics existingFirewallMetrics = tx.dsl()
          .selectFrom(FIREWALL_METRICS)
          .where(FIREWALL_METRICS.METRICS_DATE.eq(newFirewallMetrics.getMetricsDate()))
          .and(FIREWALL_METRICS.METRICS_NAME.eq(
              newFirewallMetrics.getMetricsName() != null ? newFirewallMetrics.getMetricsName().name() : null))
          .forUpdate()
          .fetchOneInto(FirewallMetrics.class);

      FirewallMetrics resultFirewallMetrics;
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
      return resultFirewallMetrics;
    }
    catch (DataAccessException e) {
      // Handle unique constraint violation by retrying (race condition on insert)
      if (e.getMessage() != null && e.getMessage().contains("Unique")) {
        return insertUpdateFirewallMetrics(newFirewallMetrics);
      }
      throw e;
    }
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
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(DSL.min(FIREWALL_METRICS.METRICS_DATE))
          .from(FIREWALL_METRICS)
          .where(FIREWALL_METRICS.METRICS_NAME.eq(metricsName.name()))
          .fetchOneInto(LocalDate.class);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return FIREWALL_METRICS;
  }

  @Override
  public Class<FirewallMetrics> getEntityClass() {
    return FirewallMetrics.class;
  }
}
