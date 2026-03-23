/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationConstraintFacts;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyViolationConstraintFacts.POLICY_VIOLATION_CONSTRAINT_FACTS;

@Named
@Singleton
public class PolicyViolationConstraintFactsDAO
    extends AbstractOperationalSqlDAO<PolicyViolationConstraintFacts>
{
  @Inject
  public PolicyViolationConstraintFactsDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public PolicyViolationConstraintFacts createIfNotExists(final String constraintFactsJson) {
    String hash = AbstractPolicyViolation.calculateConstraintFactsId(constraintFactsJson);
    PolicyViolationConstraintFacts constraints = getById(hash);

    if (constraints != null) {
      return constraints;
    }
    else {
      constraints = new PolicyViolationConstraintFacts(hash, constraintFactsJson);
      insert(constraints);

      return constraints;
    }
  }

  @Override
  public void update(final TransactionContext tx, final PolicyViolationConstraintFacts entity) {
    throw new UnsupportedOperationException("Constraints are immutable");
  }

  public List<PolicyViolationConstraintFacts> getByIds(Set<String> constraintFactsIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return getListWithSqlInClause(constraintFactsIds,
          inClauseValuesPartition -> tx.dsl()
              .selectFrom(POLICY_VIOLATION_CONSTRAINT_FACTS)
              .where(POLICY_VIOLATION_CONSTRAINT_FACTS.POLICY_VIOLATION_CONSTRAINT_FACTS_ID.in(inClauseValuesPartition))
              .fetch(this::toEntity));
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return POLICY_VIOLATION_CONSTRAINT_FACTS;
  }

  @Override
  public Class<PolicyViolationConstraintFacts> getEntityClass() {
    return PolicyViolationConstraintFacts.class;
  }
}
