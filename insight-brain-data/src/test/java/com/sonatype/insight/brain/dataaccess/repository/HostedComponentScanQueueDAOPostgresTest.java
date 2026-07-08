/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import org.junit.experimental.categories.Category;

/**
 * Postgres flavor of {@link HostedComponentScanQueueDAOTest} — re-runs the suite against real Postgres so the
 * CLM-42122 purge-vs-acquire concurrency tests exercise {@code FOR UPDATE SKIP LOCKED} (H2 only approximates it).
 */
@PostgresTest
@Category(PostgresTestCategory.class)
public class HostedComponentScanQueueDAOPostgresTest
    extends HostedComponentScanQueueDAOTest
{
}
