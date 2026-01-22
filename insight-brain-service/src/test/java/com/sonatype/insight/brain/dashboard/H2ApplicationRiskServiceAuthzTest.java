/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import jakarta.inject.Inject;

@Category(SlowTest.class)
public class H2ApplicationRiskServiceAuthzTest
    extends AbstractApplicationRiskServiceAuthzTest
{
  @Inject
  private H2ApplicationRiskService applicationRiskService;

  @Override
  protected ApplicationRiskService getApplicationRiskService() {
    return applicationRiskService;
  }
}
