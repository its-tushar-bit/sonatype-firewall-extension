/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.enterprisereporting;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.enterprisereporting.EnterpriseReportingDefaultFilter;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.EnterpriseReportingDefaultFilter.ENTERPRISE_REPORTING_DEFAULT_FILTER;

@Named
@Singleton
public class EnterpriseReportingDefaultFilterDAO
    extends AbstractOperationalSqlDAO<EnterpriseReportingDefaultFilter>
{
  @Inject
  public EnterpriseReportingDefaultFilterDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public EnterpriseReportingDefaultFilter getDefaultFilterByUserId(String userId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getDefaultFilterByUserId(tx, userId);
    }
  }

  public EnterpriseReportingDefaultFilter getDefaultFilterByUserId(TransactionContext tx, String userId) {
    List<EnterpriseReportingDefaultFilter> filters = tx.dsl()
        .selectFrom(ENTERPRISE_REPORTING_DEFAULT_FILTER)
        .where(ENTERPRISE_REPORTING_DEFAULT_FILTER.USER_ID.eq(userId))
        .fetch(this::toEntity);
    return filters.isEmpty() ? null : filters.get(0);
  }

  @Override
  public Table<?> getJooqTable() {
    return ENTERPRISE_REPORTING_DEFAULT_FILTER;
  }

  @Override
  public Class<EnterpriseReportingDefaultFilter> getEntityClass() {
    return EnterpriseReportingDefaultFilter.class;
  }
}
