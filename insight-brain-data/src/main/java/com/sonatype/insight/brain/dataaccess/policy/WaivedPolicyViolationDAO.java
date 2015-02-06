/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.WaivedPolicyViolation;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.12
 */
public class WaivedPolicyViolationDAO
    extends AbstractOperationalSqlDAO<WaivedPolicyViolation>
{
  @Override
  public WaivedPolicyViolation getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM WaivedPolicyViolation entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  @Override
  public void update(TransactionContext tx, WaivedPolicyViolation entity) {
    throw new UnsupportedOperationException("The WaivedPolicyViolation table does not support update operations.");
  }
}
