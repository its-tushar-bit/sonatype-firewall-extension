/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyWaiverReason.POLICY_WAIVER_REASON;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;

@Named
@Singleton
public class PolicyWaiverReasonDAO
    extends AbstractOperationalSqlDAO<PolicyWaiverReason>
{
  @Inject
  public PolicyWaiverReasonDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public List<PolicyWaiverReason> getAll(TransactionContext tx) {
    return tx.dsl()
        .selectFrom(POLICY_WAIVER_REASON)
        .orderBy(POLICY_WAIVER_REASON.SORT_ORDER.asc().nullsFirst(),
            POLICY_WAIVER_REASON.REASON_TEXT.asc())
        .fetchInto(PolicyWaiverReason.class);
  }

  @Override
  public Table<?> getJooqTable() {
    return POLICY_WAIVER_REASON;
  }

  // Returns all waivers reasons as a convenient lookup map;
  // Call this once, outside any loops, so we don't go the db over and over;
  // The number of waivers reasons is very small and should always be very small;
  // We can get all of them once per request, keep them in memory, and just look them up id from this map;
  public Map<String, PolicyWaiverReason> getPolicyWaiverReasonIdToPolicyWaiverReasonMap() {
    return getAll()
        .stream()
        .collect(toMap(PolicyWaiverReason::getId, identity(), (existing, replacement) -> existing));
  }

  public List<PolicyWaiverReason> getAllByIds(List<String> policyWaiverReasonIds) {
    if (policyWaiverReasonIds == null || policyWaiverReasonIds.isEmpty()) {
      return java.util.Collections.emptyList();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_WAIVER_REASON)
          .where(POLICY_WAIVER_REASON.WAIVER_REASON_ID.in(policyWaiverReasonIds))
          .fetchInto(PolicyWaiverReason.class);
    }
  }

  public PolicyWaiverReason getByReasonText(String reasontext) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_WAIVER_REASON)
          .where(POLICY_WAIVER_REASON.REASON_TEXT.eq(reasontext))
          .fetchOneInto(PolicyWaiverReason.class);
    }
  }

  @Override
  public Class<PolicyWaiverReason> getEntityClass() {
    return PolicyWaiverReason.class;
  }
}
