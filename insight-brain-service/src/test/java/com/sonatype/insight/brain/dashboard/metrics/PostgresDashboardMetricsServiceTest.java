/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import org.junit.experimental.categories.Category;

/**
 * Postgres parity for dashboard metrics Applications count (CLM-40927 AC-7).
 */
@PostgresTest
@Category(PostgresTestCategory.class)
public class PostgresDashboardMetricsServiceTest
    extends DashboardMetricsServiceTest
{
}
