/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationConstraintFacts;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.jooq.UpdatableRecord;
import org.jooq.impl.DSL;

public abstract class AbstractPolicyViolationDAO<T extends AbstractPolicyViolation>
    extends AbstractOperationalSqlDAO<T>
{
  private final PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO;

  protected AbstractPolicyViolationDAO(
      OperationalDataStore operationalDataStore,
      PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO)
  {
    super(operationalDataStore);
    this.policyViolationConstraintFactsDAO = policyViolationConstraintFactsDAO;
  }

  public T getByIdWithConstraintFacts(String id) {
    T entity = getById(id);
    if (entity != null) {
      loadConstraintFacts(Collections.singletonList(entity));
    }
    return entity;
  }

  public long getCountWhereDeprecatedConstraintFactsJsonNotNull() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectCount()
          .from(getJooqTable())
          .where(DSL.field("constraint_facts_json").isNotNull())
          .fetchOne(0, Long.class);
    }
  }

  /**
   * Loads the constraint facts from the db for a collection of policy violations in one db round trip.
   * In order to reduce db round trips, it is important to call this method on collections of policy violations, not on
   * individual policy violations.
   * We don't expose a loadConstraintFacts method for a single violation on purpose - to make developers think twice
   * about optimizing db round trips :).
   * In order to reduce memory consumption, it is important to call this method only when constraint facts are actually
   * needed and only for the policy violations for which constraint facts are needed.
   *
   * For ex, a REST endpoint that loads a large number of policy violations and returns a smaller number of policy
   * violations can load the constraint facts only for the result set (if constraint facts are not needed for the
   * internal processing).
   */
  public void loadConstraintFacts(Collection<T> policyViolations) {
    if (CollectionUtils.isEmpty(policyViolations)) {
      return;
    }

    List<T> policyViolationsToLoad = policyViolations.stream()
        .filter(policyViolation -> !policyViolation.constraintFactsAreLoaded()
            && policyViolation.getConstraintFactsId() != null)
        .toList();
    Set<String> constraintFactsIds = policyViolationsToLoad.stream()
        .map(AbstractPolicyViolation::getConstraintFactsId) //
        .collect(Collectors.toSet());
    if (CollectionUtils.isEmpty(constraintFactsIds)) {
      return;
    }

    Map<String, PolicyViolationConstraintFacts> constraintFactsById =
        policyViolationConstraintFactsDAO.getByIds(constraintFactsIds)
            .stream()
            .collect(Collectors.toMap(PolicyViolationConstraintFacts::getId, Function.identity()));
    policyViolationsToLoad.forEach(policyViolation -> {
      try {
        policyViolation.setConstraintFacts(Arrays.asList(
            JsonUtils.parse(constraintFactsById.get(policyViolation.getConstraintFactsId()).getConstraintFactsJson(),
                ConstraintFact[].class)));
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }

  @Override
  protected UpdatableRecord<?> fromEntity(final UpdatableRecord<?> record, final T entity) {
    record.from(entity);
    // Fix: record.from() uses Java bean introspection and maps the computed getConstraintFactsJson() getter
    // to the constraint_facts_json column. We need the actual deprecatedConstraintFactsJson field value instead.
    var field = getJooqTable().field("constraint_facts_json", String.class);
    if (field != null) {
      record.set(field, entity.getDeprecatedConstraintFactsJson());
    }
    return record;
  }

  protected void storeConstraints(final AbstractPolicyViolation entity) {
    // If constraint facts are already stored (identified by their ID) and not loaded in memory,
    // skip re-storing them. Constraint facts are immutable and identified by a hash of their JSON.
    // This allows update() to work with entities that were loaded from the database without
    // requiring the constraint facts to be explicitly loaded first.
    if (entity.getConstraintFactsId() != null && !entity.constraintFactsAreLoaded()) {
      return;
    }
    PolicyViolationConstraintFacts constraints =
        policyViolationConstraintFactsDAO.createIfNotExists(entity.getConstraintFactsJson());
    entity.setConstraintFactsId(constraints.getId());
    entity.setDeprecatedConstraintFactsJson(null);
  }
}
