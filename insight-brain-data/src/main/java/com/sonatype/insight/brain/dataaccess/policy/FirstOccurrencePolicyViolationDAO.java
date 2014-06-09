/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.FirstOccurrencePolicyViolation;

/**
 * @since 1.11
 */
public class FirstOccurrencePolicyViolationDAO
    extends AbstractOperationalSqlDAO<FirstOccurrencePolicyViolation>
{
  @Override
  public FirstOccurrencePolicyViolation getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM FirstOccurrencePolicyViolation entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  @Override
  public void update(EntityManager em, FirstOccurrencePolicyViolation entity) {
    throw new UnsupportedOperationException(
        "The FirstOccurrencePolicyViolation table does not support update operations");
  }
}
