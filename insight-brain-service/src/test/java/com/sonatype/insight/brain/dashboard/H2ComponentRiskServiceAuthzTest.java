/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2InMemoryTest;

@H2InMemoryTest
public class H2ComponentRiskServiceAuthzTest
    extends AbstractComponentRiskServiceAuthzTest
{
  @Inject
  private H2ComponentRiskService componentRiskService;

  @Override
  protected DashboardComponentRiskService getComponentRiskService() {
    return componentRiskService;
  }

  // The tests are in the parent class
}
