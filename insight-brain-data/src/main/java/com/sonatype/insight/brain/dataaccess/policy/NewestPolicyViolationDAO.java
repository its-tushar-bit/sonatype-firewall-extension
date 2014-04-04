/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.NewestPolicyViolation;

/**
 * @since 1.11
 */
public class NewestPolicyViolationDAO
    extends AbstractOperationalSqlDAO<NewestPolicyViolation>
{
  @Override
  protected NewestPolicyViolation getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM NewestPolicyViolation entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  @Override
  public void update(EntityManager em, NewestPolicyViolation entity) {
    throw new UnsupportedOperationException("The NewestPolicyViolation table does not support update operations");
  }
}
