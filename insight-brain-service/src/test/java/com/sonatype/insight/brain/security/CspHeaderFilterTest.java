/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService;
import com.sonatype.insight.brain.service.Configuration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class CspHeaderFilterTest
{
  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock
  private Configuration configuration;

  @Mock
  private EnterpriseReportingService enterpriseReportingService;

  private CspHeaderFilter cspHeaderFilter;

  @Before
  public void before() {
    cspHeaderFilter = new CspHeaderFilter(configuration, enterpriseReportingService);
  }

  @Test
  public void testGetFrameSrc_Valid_FeatureEnabled() {
    when(enterpriseReportingService.getBaseUrl()).thenReturn("https://sonatypeexternaldev.cloud.looker.com/");
    assertThat(cspHeaderFilter.getFrameSrc()).isEqualTo(" frame-src 'self' sonatypeexternaldev.cloud.looker.com;");
  }

  @Test
  public void testGetFrameSrc_Invalid_FeatureEnabled() {
    when(enterpriseReportingService.getBaseUrl()).thenReturn("blah");
    assertThat(cspHeaderFilter.getFrameSrc()).isEmpty();
  }

  @Test
  public void testGetFrameSrc_FeatureDisabled() {
    assertThat(cspHeaderFilter.getFrameSrc()).isEmpty();
  }
}
