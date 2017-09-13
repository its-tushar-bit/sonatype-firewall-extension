/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractAggregationSqlDAO;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetrics;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.36
 */
public class SuccessMetricsDAO
    extends AbstractAggregationSqlDAO<SuccessMetrics>
{
  @Override
  public SuccessMetrics getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM SuccessMetrics entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<SuccessMetrics> getByUsername(String username) {
    String sQuery = "SELECT entity FROM SuccessMetrics entity" + //
        " WHERE entity.username=?1" +
        " ORDER BY entity.name";
    return getList(sQuery, username);
  }

  public SuccessMetrics getByUsernameAndName(String username, String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsernameAndName(tx, username, name);
    }
  }

  private SuccessMetrics getByUsernameAndName(TransactionContext tx, String username, String name) {
    // success metrics name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM SuccessMetrics entity" +
        " WHERE entity.username=?1 AND entity.nameLowercaseNoWhitespace=?2";
    return get(tx, sQuery, username, name);
  }

  @Override
  public void insert(TransactionContext tx, SuccessMetrics successMetrics) {
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
  public void update(TransactionContext tx, SuccessMetrics successMetrics) {
    throw new UnsupportedOperationException("SuccessMetrics does not support update operations.");
  }
}
