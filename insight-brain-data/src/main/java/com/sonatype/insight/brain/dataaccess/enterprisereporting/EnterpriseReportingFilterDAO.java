/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.enterprisereporting;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.enterprisereporting.EnterpriseReportingFilter;
import com.sonatype.insight.dataaccess.TransactionContext;

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
    String sQuery = "SELECT entity FROM EnterpriseReportingFilter entity " +
        "WHERE entity.userId=?1 " +
        "ORDER BY entity.filterName";
    return getList(sQuery, userId);
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
    String sQuery = "SELECT entity FROM EnterpriseReportingFilter entity " +
        "WHERE entity.userId=?1 AND LOWER(entity.filterName)=?2";
    List<EnterpriseReportingFilter> filters = getList(tx, sQuery, userId, name.toLowerCase());
    return filters.isEmpty() ? null : filters.get(0);
  }

  public EnterpriseReportingFilter getFilterByUserAndFilterId(String userId, String filterId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getFilterByUserAndFilterId(tx, userId, filterId);
    }
  }

  public EnterpriseReportingFilter getFilterByUserAndFilterId(TransactionContext tx, String userId, String filterId) {
    String sQuery = "SELECT entity FROM EnterpriseReportingFilter entity " +
            "WHERE entity.id=?1 AND entity.userId=?2";
    List<EnterpriseReportingFilter> filters = getList(tx, sQuery, filterId, userId);
    return filters.isEmpty() ? null : filters.get(0);
  }
}
