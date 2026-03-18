/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import jakarta.inject.Inject;
import org.junit.experimental.categories.Category;

@PostgresTest
@Category({PostgresTestCategory.class, SlowTest.class})
public class PostgresComponentRiskServiceAuthzTest
    extends AbstractComponentRiskServiceAuthzTest
{
  @Inject
  private PostgresComponentRiskService componentRiskService;

  @Override
  protected DashboardComponentRiskService getComponentRiskService() {
    return componentRiskService;
  }

  // The tests are in the parent class
}
