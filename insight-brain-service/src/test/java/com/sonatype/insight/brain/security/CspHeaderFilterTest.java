/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadGatewayException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CspHeaderFilterTest
{

  @Mock
  private Configuration configuration;

  @Mock
  private EnterpriseReportingService enterpriseReportingService;

  @Mock
  ProductLicense productLicense;

  private CspHeaderFilter cspHeaderFilter;

  @BeforeEach
  public void before() {
    cspHeaderFilter = new CspHeaderFilter(configuration, enterpriseReportingService, productLicense);
    when(productLicense.isValid()).thenReturn(true);
  }

  @Test
  public void testGetFrameSrc_Valid_FeatureEnabled() {
    when(enterpriseReportingService.getEnterpriseReportingConfigDTOBaseUrl()).thenReturn(
        "https://sonatypeexternaldev.cloud.looker.com/");
    assertThat(cspHeaderFilter.getFrameSrc()).isEqualTo("frame-src 'self' sonatypeexternaldev.cloud.looker.com; ");
  }

  @Test
  public void testGetFrameSrc_Invalid_FeatureEnabled() {
    when(enterpriseReportingService.getEnterpriseReportingConfigDTOBaseUrl()).thenReturn("blah");
    assertThat(cspHeaderFilter.getFrameSrc()).isEmpty();
  }

  @Test
  public void testGetFrameSrc_FeatureDisabled() {
    assertThat(cspHeaderFilter.getFrameSrc()).isEmpty();
  }

  @Test
  public void testGetFrameSrc_InvalidLicense() {
    when(productLicense.isValid()).thenReturn(false);
    assertThat(cspHeaderFilter.getFrameSrc()).isEmpty();
  }

  @Test
  public void testGetFrameSrc_shouldReturnEmptyStringWhenHDSIsNotReachable() {
    when(enterpriseReportingService.getEnterpriseReportingConfigDTOBaseUrl()).thenThrow(
        new BadGatewayException("Some Networking Problem"));

    assertThat(cspHeaderFilter.getFrameSrc()).isEqualTo("");
  }
}
