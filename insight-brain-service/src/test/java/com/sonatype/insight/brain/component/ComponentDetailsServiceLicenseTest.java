/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import com.sonatype.insight.brain.features.LicensedFeature;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.when;

public class ComponentDetailsServiceLicenseTest
{
  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @InjectMocks
  private ComponentDetailService componentDetailsService;

  @Mock
  private CLMLicenseManager licenseManager;

  @Before
  public void setup() {
    when(licenseManager.hasFeature(LicensedFeature.DASHBOARD)).thenReturn(false);
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
