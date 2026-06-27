/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Postgres flavor of {@link RepositoryComponentDAOTest}.
 *
 * <p>
 * Exists because the H2 and Postgres branches of
 * {@link RepositoryComponentDAO#getMonitoringEligiblePage} diverge: H2 uses a 3-pass
 * GROUP-BY + self-JOIN dedup, while Postgres uses a {@code NOT EXISTS} anti-join driven by the
 * {@code repository_component_dedup_keyset_idx} composite index on
 * {@code (repository_id, hash, time DESC, repository_component_id DESC)} — one index, dual use
 * (outer driver scan + inner anti-join probe). The cursor predicate
 * {@code (time, repository_component_id) < (cursor.time, cursor.id)} — expressed via jOOQ
 * {@code DSL.row(...).lessThan(DSL.row(...))} — must produce identical row order, dedup, and
 * no-skip-under-concurrent-insert behavior on both dialects (CLM-41005 acceptance criterion).
 *
 * <p>
 * Without this Postgres-specific run the anti-join semantics and the row-value comparison
 * planner behavior are never exercised against a real Postgres instance — a regression in either
 * (e.g. wrong tuple-comparison semantics, planner falling back to a sequential scan) would ship
 * undetected.
 *
 * @since 1.206 (CLM-41005)
 */
@PostgresTest
@Category(PostgresTestCategory.class)
public class RepositoryComponentDAOPostgresTest
    extends RepositoryComponentDAOTest
{
  /**
   * Inherited from the parent test class but specific to the H2 dialect — it asserts
   * {@code isDatabaseEmbedded() == true} which is false under Postgres. The Postgres equivalent
   * is the sibling {@code testDeleteByRepositoryId_Postgres} in the parent, which IS exercised
   * here. Override with {@link Ignore} so the Postgres run does not pick up the H2-only check.
   */
  @Test
  @Ignore("H2-only: covered by testDeleteByRepositoryId_Postgres under this Postgres flavor")
  @Override
  public void testDeleteByRepositoryId_H2() {
  }
}
