/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.enterprisereporting;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.enterprisereporting.EnterpriseReportingFilter;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.EnterpriseReportingFilter.ENTERPRISE_REPORTING_FILTER;

@Named
@Singleton
public class EnterpriseReportingFilterDAO
    extends AbstractOperationalSqlDAO<EnterpriseReportingFilter>
{
  @Inject
  public EnterpriseReportingFilterDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<EnterpriseReportingFilter> getFiltersByUserId(String userId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(ENTERPRISE_REPORTING_FILTER)
          .where(ENTERPRISE_REPORTING_FILTER.USER_ID.eq(userId))
          .orderBy(ENTERPRISE_REPORTING_FILTER.FILTER_NAME)
          .fetch(this::toEntity);
    }
  }

  public EnterpriseReportingFilter getFilterByUserIdAndName(String userId, String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getFilterByUserIdAndName(tx, userId, name);
    }
  }

  public EnterpriseReportingFilter getFilterByUserIdAndName(TransactionContext tx, String userId, String name) {
    if (name == null) {
      throw new InvalidNameException("Filter name is required.");
    }
    List<EnterpriseReportingFilter> filters = tx.dsl()
        .selectFrom(ENTERPRISE_REPORTING_FILTER)
        .where(ENTERPRISE_REPORTING_FILTER.USER_ID.eq(userId))
        .and(DSL.lower(ENTERPRISE_REPORTING_FILTER.FILTER_NAME).eq(name.toLowerCase()))
        .fetch(this::toEntity);
    return filters.isEmpty() ? null : filters.get(0);
  }

  public EnterpriseReportingFilter getFilterByUserAndFilterId(String userId, String filterId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getFilterByUserAndFilterId(tx, userId, filterId);
    }
  }

  public EnterpriseReportingFilter getFilterByUserAndFilterId(TransactionContext tx, String userId, String filterId) {
    List<EnterpriseReportingFilter> filters = tx.dsl()
        .selectFrom(ENTERPRISE_REPORTING_FILTER)
        .where(ENTERPRISE_REPORTING_FILTER.ENTERPRISE_REPORTING_FILTER_ID.eq(filterId))
        .and(ENTERPRISE_REPORTING_FILTER.USER_ID.eq(userId))
        .fetch(this::toEntity);
    return filters.isEmpty() ? null : filters.get(0);
  }

  @Override
  public Table<?> getJooqTable() {
    return ENTERPRISE_REPORTING_FILTER;
  }

  @Override
  public Class<EnterpriseReportingFilter> getEntityClass() {
    return EnterpriseReportingFilter.class;
  }
}
