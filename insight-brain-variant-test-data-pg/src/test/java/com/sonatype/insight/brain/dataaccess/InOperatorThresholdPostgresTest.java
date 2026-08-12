/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.dataaccess.AbstractSqlDAO.POSTGRES_IN_OPERATOR_THRESHOLD;

@PostgresTest
@Category(PostgresTestCategory.class)
public class InOperatorThresholdPostgresTest
    extends InOperatorThresholdTest
{
  @Override
  protected int getInOperatorThreshold() {
    return POSTGRES_IN_OPERATOR_THRESHOLD;
  }
}
