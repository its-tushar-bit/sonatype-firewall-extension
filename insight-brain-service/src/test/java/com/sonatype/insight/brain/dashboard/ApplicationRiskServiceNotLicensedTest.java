/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;

import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;

import org.junit.Test;
import org.mockito.InjectMocks;

public class ApplicationRiskServiceNotLicensedTest
    extends AbstractServiceNotLicensedTest
{
  @InjectMocks
  private ApplicationRiskService applicationRiskService;

  @Test(expected = InvalidLicenseException.class)
  public void testGetApplicationRisks_Unlicensed() {
    applicationRiskService.getApplicationRisks(null, null, Collections.singleton(DevelopStageType.ID), null, null, null,
        null, 0);
  }
}
