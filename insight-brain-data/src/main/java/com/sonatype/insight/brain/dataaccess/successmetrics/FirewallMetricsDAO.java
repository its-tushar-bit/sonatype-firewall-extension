/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractAggregationSqlDAO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.dataaccess.TransactionContext;

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
}
