/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import org.junit.experimental.categories.Category;

@PostgresTest
@Category({PostgresTestCategory.class, SlowTest.class})
public class SourceControlUserActivityPostgresqlDAOTest
    extends SourceControlUserActivityDAOTest
{
}
