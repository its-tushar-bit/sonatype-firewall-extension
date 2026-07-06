/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractAggregationSqlDAO;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReportData;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.aggregation.tables.SuccessMetricsReport.SUCCESS_METRICS_REPORT;

/**
 * @since 1.37
 */
@Named
@Singleton
public class SuccessMetricsReportDAO
    extends AbstractAggregationSqlDAO<SuccessMetricsReport>
{
  private final SuccessMetricsReportDataDAO successMetricsReportDataDAO;

  @Inject
  public SuccessMetricsReportDAO(
      final AggregationDataStore aggregationDataStore,
      final SuccessMetricsReportDataDAO successMetricsReportDataDAO)
  {
    super(aggregationDataStore);
    this.successMetricsReportDataDAO = successMetricsReportDataDAO;
  }

  public List<SuccessMetricsReport> getByUsername(String username) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SUCCESS_METRICS_REPORT)
          .where(SUCCESS_METRICS_REPORT.USERNAME.eq(username))
          .orderBy(SUCCESS_METRICS_REPORT.NAME)
          .fetchInto(SuccessMetricsReport.class);
    }
  }

  public SuccessMetricsReport getByUsernameAndName(String username, String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsernameAndName(tx, username, name);
    }
  }

  private SuccessMetricsReport getByUsernameAndName(TransactionContext tx, String username, String name) {
    // success metrics name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    return tx.dsl()
        .selectFrom(SUCCESS_METRICS_REPORT)
        .where(SUCCESS_METRICS_REPORT.USERNAME.eq(username))
        .and(SUCCESS_METRICS_REPORT.NAME_LOWERCASE_NO_WHITESPACE.eq(name))
        .fetchOneInto(SuccessMetricsReport.class);
  }

  @Override
  public int insert(TransactionContext tx, SuccessMetricsReport entity) {
    NameHelper.validate(entity.getName());
    if (getByUsernameAndName(tx, entity.getUsername(), entity.getName()) != null) {
      throw new BadRequestException(entity.getName() + " is already used as a name.");
    }

    if (entity.getCreateTime() == null) {
      entity.setCreateTime(new Date());
    }

    return super.insert(tx, entity);
  }

  @Override
  public int update(TransactionContext tx, SuccessMetricsReport entity) {
    throw new UnsupportedOperationException("SuccessMetricsReport does not support update operations.");
  }

  @Override
  public void delete(TransactionContext tx, SuccessMetricsReport entity) {
    SuccessMetricsReportData successMetricsReportData = successMetricsReportDataDAO
        .getById(entity.getId());

    if (successMetricsReportData != null) {
      successMetricsReportDataDAO.delete(tx, successMetricsReportData);
    }

    super.delete(tx, entity);
  }

  @Override
  public Table<?> getJooqTable() {
    return SUCCESS_METRICS_REPORT;
  }

  @Override
  public Class<SuccessMetricsReport> getEntityClass() {
    return SuccessMetricsReport.class;
  }
}
