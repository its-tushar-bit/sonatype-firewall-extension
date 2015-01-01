/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ComponentDetailsServiceLicenseTest
{
  @InjectMocks
  private ComponentDetailService componentDetailsService;

  @Mock
  private CLMLicenseManager licenseManager;

  @Before
  public void setup() {
    when(licenseManager.hasDashboard()).thenReturn(false);
  }

  @Test(expected = InvalidLicenseException.class)
  public void testGetApplicationDetailsByHash_Unlicensed() {
    componentDetailsService.getApplicationDetailsByHash("some-hash");
  }

  @Test(expected = InvalidLicenseException.class)
  public void testGetComponentNameByHash_Unlicensed() {
    componentDetailsService.getComponentNameByHash("some-hash");
  }
}
