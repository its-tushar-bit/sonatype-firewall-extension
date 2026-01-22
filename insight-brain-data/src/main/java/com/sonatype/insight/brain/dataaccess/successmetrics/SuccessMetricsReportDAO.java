/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractAggregationSqlDAO;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReportData;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

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

  @Override
  public SuccessMetricsReport getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM SuccessMetricsReport entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<SuccessMetricsReport> getByUsername(String username) {
    String sQuery = "SELECT entity FROM SuccessMetricsReport entity" + //
        " WHERE entity.username=?1" +
        " ORDER BY entity.name";
    return getList(sQuery, username);
  }

  public SuccessMetricsReport getByUsernameAndName(String username, String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsernameAndName(tx, username, name);
    }
  }

  private SuccessMetricsReport getByUsernameAndName(TransactionContext tx, String username, String name) {
    // success metrics name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM SuccessMetricsReport entity" +
        " WHERE entity.username=?1 AND entity.nameLowercaseNoWhitespace=?2";
    return get(tx, sQuery, username, name);
  }

  @Override
  public void insert(TransactionContext tx, SuccessMetricsReport successMetrics) {
    NameHelper.validate(successMetrics.getName());
    if (getByUsernameAndName(tx, successMetrics.getUsername(), successMetrics.getName()) != null) {
      throw new BadRequestException(successMetrics.getName() + " is already used as a name.");
    }

    if (successMetrics.getCreateTime() == null) {
      successMetrics.setCreateTime(new Date());
    }

    super.insert(tx, successMetrics);
  }

  @Override
  public void update(TransactionContext tx, SuccessMetricsReport successMetrics) {
    throw new UnsupportedOperationException("SuccessMetricsReport does not support update operations.");
  }

  @Override
  public void delete(TransactionContext tx, SuccessMetricsReport successMetricsReport) {
    SuccessMetricsReportData successMetricsReportData = successMetricsReportDataDAO
        .getById(successMetricsReport.getId());

    if (successMetricsReportData != null) {
      successMetricsReportDataDAO.delete(tx, successMetricsReportData);
    }

    super.delete(tx, successMetricsReport);
  }

  public List<SuccessMetricsReport> getAll() {
    String sQuery = "SELECT entity FROM SuccessMetricsReport entity";
    return getList(sQuery);
  }
}
