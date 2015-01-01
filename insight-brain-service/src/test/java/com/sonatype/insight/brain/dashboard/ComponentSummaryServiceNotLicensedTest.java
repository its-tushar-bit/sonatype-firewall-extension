/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import com.sonatype.insight.brain.product.license.InvalidLicenseException;

import org.junit.Test;
import org.mockito.InjectMocks;

public class ComponentSummaryServiceNotLicensedTest
    extends AbstractServiceNotLicensedTest
{
  @InjectMocks
  private ComponentSummaryService componentSummaryService;

  @Test(expected = InvalidLicenseException.class)
  public void testGetComponentSummary_Unlicensed() throws Exception {
    componentSummaryService.getComponentSummary(null, null, null);
  }
}
