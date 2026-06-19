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
 * Postgres flavor of {@link OnConflictRaceIntegrationTest} — exercises the
 * {@code ON CONFLICT (idempotency_key) WHERE (idempotency_key IS NOT NULL) DO NOTHING}
 * predicate against a real Postgres planner. The base class's H2 fixture only covers
 * the savepoint/absorb branch.
 *
 * @since 1.205 (CLM-40771)
 */
@PostgresTest
@Category({PostgresTestCategory.class, SlowTest.class})
public class OnConflictRacePostgresIntegrationTest
    extends OnConflictRaceIntegrationTest
{
}
