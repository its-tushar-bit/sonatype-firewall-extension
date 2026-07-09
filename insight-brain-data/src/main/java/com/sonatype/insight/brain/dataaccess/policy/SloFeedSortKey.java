/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;

import com.sonatype.insight.brain.model.policy.PolicyViolation;

import org.jooq.Field;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyViolation.POLICY_VIOLATION;

/**
 * Single source of truth for the SLO violation-feed sort key: the greatest of a violation's open/waive/fix/legacy
 * times, with {@code null} times treated as the epoch.
 * <p>
 * Its two members MUST stay in lock-step: {@link #of(PolicyViolation)} computes the key in memory (the value a caller
 * passes back as {@code updatedSince} to continue a walk) and {@link #field()} is the identical jOOQ expression the DAO
 * uses for both the keyset predicate and the {@code ORDER BY}. If they diverge — different columns, a different epoch
 * sentinel, or a different reduction — the frozen continuation point would shift relative to the ordering and rows
 * could be skipped, so the in-memory and SQL halves are deliberately kept side by side here rather than on the DAO.
 */
public final class SloFeedSortKey
{
  private SloFeedSortKey() {
    // static holder
  }

  /**
   * Computes the in-memory sort key for {@code violation}: the greatest of its open/waive/fix/legacy times, treating
   * {@code null} as the epoch. Paired with the row's id (as {@code afterViolationId}), this is the
   * {@code updatedSince} value a caller passes back to continue a cursor walk.
   * <p>
   * {@link Date} is millisecond-precision while the underlying TIMESTAMP columns are finer-grained; a sub-millisecond
   * component is truncated here, which can at most re-deliver a row in the same millisecond bucket (never skip one),
   * and callers already dedupe cursor walks by {@code violationId}.
   */
  public static Date of(final PolicyViolation violation) {
    long millis = 0L;
    if (violation.getOpenTime() != null) {
      millis = Math.max(millis, violation.getOpenTime().getTime());
    }
    if (violation.getWaiveTime() != null) {
      millis = Math.max(millis, violation.getWaiveTime().getTime());
    }
    if (violation.getFixTime() != null) {
      millis = Math.max(millis, violation.getFixTime().getTime());
    }
    if (violation.getLegacyViolationTime() != null) {
      millis = Math.max(millis, violation.getLegacyViolationTime().getTime());
    }
    return new Date(millis);
  }

  /**
   * The jOOQ counterpart of {@link #of(PolicyViolation)}: {@code GREATEST(COALESCE(open/waive/fix/legacy, epoch))}.
   * <p>
   * The epoch sentinel is the plain-SQL literal {@code TIMESTAMP '1970-01-01 00:00:00'} rather than a bound
   * {@link Date} so the expression renders byte-for-byte the same regardless of the JVM's default timezone. That keeps
   * it aligned with the Postgres expression index {@code policy_violation_app_stage_updated_idx} (defined in
   * {@code insight-brain-db}); a timezone-rendered bound literal could differ from the indexed expression and silently
   * bypass the index.
   */
  static Field<Date> field() {
    final Field<Date> epoch =
        DSL.field("timestamp '1970-01-01 00:00:00'", POLICY_VIOLATION.OPEN_TIME.getDataType());
    return DSL.greatest(
        DSL.coalesce(POLICY_VIOLATION.OPEN_TIME, epoch),
        DSL.coalesce(POLICY_VIOLATION.WAIVE_TIME, epoch),
        DSL.coalesce(POLICY_VIOLATION.FIX_TIME, epoch),
        DSL.coalesce(POLICY_VIOLATION.LEGACY_VIOLATION_TIME, epoch));
  }
}
