/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationConstraintFacts;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class PolicyViolationConstraintFactsDAO
    extends AbstractOperationalSqlDAO<PolicyViolationConstraintFacts>
{
  @Inject
  public PolicyViolationConstraintFactsDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public PolicyViolationConstraintFacts createIfNotExists(
      final TransactionContext tx,
      final String constraintFactsJson)
  {
    String hash = AbstractPolicyViolation.calculateConstraintFactsId(constraintFactsJson);
    PolicyViolationConstraintFacts constraints = getById(tx, hash);

    if (constraints != null) {
      return constraints;
    }
    else {
      constraints = new PolicyViolationConstraintFacts(hash, constraintFactsJson);
      insert(tx, constraints);

      return getByIdNotNull(tx, hash);
    }
  }

  @Override
  public void update(final TransactionContext tx, final PolicyViolationConstraintFacts entity) {
    throw new UnsupportedOperationException("Constraints are immutable");
  }
}
