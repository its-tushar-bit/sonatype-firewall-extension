/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import com.sonatype.insight.brain.product.license.InvalidLicenseException;

import org.junit.Test;
import org.mockito.InjectMocks;

public class ComponentRiskServiceNotLicensedTest
    extends AbstractServiceNotLicensedTest
{
  @InjectMocks
  private ComponentRiskService componentRiskService;

  @Test(expected = InvalidLicenseException.class)
  public void testGetComponentRisks_Unlicensed() {
    componentRiskService.getComponentRisks(null, null, null, null, null, 0);
  }
}
