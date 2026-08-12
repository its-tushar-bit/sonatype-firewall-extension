/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import jakarta.inject.Inject;

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
