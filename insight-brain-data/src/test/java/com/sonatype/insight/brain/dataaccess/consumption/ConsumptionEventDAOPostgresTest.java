/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.consumption;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import org.junit.experimental.categories.Category;

/**
 * Postgres flavor of {@link ConsumptionEventDAOTest}.
 *
 * <p>
 * Exists because the H2 and Postgres branches of {@link ConsumptionEventDAO#recordEvent}
 * diverge: H2 uses a savepoint + integrity-violation absorption, Postgres uses
 * {@code ON CONFLICT (idempotency_key) WHERE (idempotency_key IS NOT NULL) DO NOTHING}.
 * Without this Postgres-specific run, the {@code ON CONFLICT} predicate clause is never
 * exercised against a real Postgres planner — and a missing predicate would cause every
 * insert to fail with "no unique or exclusion constraint matching the ON CONFLICT
 * specification" (caught manually during CLM-40771 verification, would otherwise have
 * shipped as a Critical regression).
 *
 * @since 1.205 (CLM-40771)
 */
@PostgresTest
@Category({PostgresTestCategory.class, SlowTest.class})
public class ConsumptionEventDAOPostgresTest
    extends ConsumptionEventDAOTest
{
}
