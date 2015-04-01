/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.filter;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.11.0
 */
public class DashboardFilterDAO
    extends AbstractOperationalSqlDAO<DashboardFilter>
{

  public DashboardFilter getByUsername(TransactionContext tx, String username) {
    String sQuery = "SELECT entity FROM DashboardFilter entity WHERE entity.username=?1";
    return get(tx, sQuery, username);
  }

  public DashboardFilter getByUsername(String username) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsername(tx, username);
    }
  }

  public List<DashboardFilter> getAll() {
    String sQuery = "SELECT entity FROM DashboardFilter entity";
    return getList(sQuery);
  }
}
