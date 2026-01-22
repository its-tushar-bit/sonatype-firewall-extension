/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.enterprisereporting;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.enterprisereporting.EnterpriseReportingDefaultFilter;
import com.sonatype.insight.dataaccess.TransactionContext;

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
    String sQuery = "SELECT entity FROM EnterpriseReportingDefaultFilter entity " +
        "WHERE entity.id=?1";
    List<EnterpriseReportingDefaultFilter> filters = getList(tx, sQuery, userId);
    return filters.isEmpty() ? null : filters.get(0);
  }
}
