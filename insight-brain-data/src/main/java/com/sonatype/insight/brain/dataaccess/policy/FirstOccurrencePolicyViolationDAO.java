/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.FirstOccurrencePolicyViolation;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.11
 */
public class FirstOccurrencePolicyViolationDAO
    extends AbstractOperationalSqlDAO<FirstOccurrencePolicyViolation>
{
  @Override
  public FirstOccurrencePolicyViolation getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM FirstOccurrencePolicyViolation entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  @Override
  public void update(TransactionContext tx, FirstOccurrencePolicyViolation entity) {
    throw new UnsupportedOperationException(
        "The FirstOccurrencePolicyViolation table does not support update operations");
  }
}
