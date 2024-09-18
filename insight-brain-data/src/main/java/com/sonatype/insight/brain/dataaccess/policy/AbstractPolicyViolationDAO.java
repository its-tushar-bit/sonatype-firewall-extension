/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationConstraintFacts;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * This is an interface but called "Abstract" to mirror the class hierarchy of AbstractPolicyViolation which is the
 * parent class to PolicyViolation and RepositoryPolicyViolation.
 */
public interface AbstractPolicyViolationDAO
{
  long getCountWhereConstraintFactsJsonNotNull();

  default void storeConstraints(
      final PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO,
      final TransactionContext tx,
      final AbstractPolicyViolation entity)
  {
    PolicyViolationConstraintFacts constraints = policyViolationConstraintFactsDAO
        .createIfNotExists(tx, entity.getConstraintFactsJson());
    entity.setConstraintFactsId(constraints.getId());
    entity.clearConstraintFactsJson();
  }
}
