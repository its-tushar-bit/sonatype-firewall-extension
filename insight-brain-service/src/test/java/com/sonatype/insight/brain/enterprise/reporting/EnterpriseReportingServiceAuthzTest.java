/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EnterpriseReportingServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private EnterpriseReportingService enterpriseReportingService;

  @Before
  public void before() {
    enableFeature();
  }

  @After
  public void after() {
    disableFeature();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testCreateSSOEmbedUrl_UnAuthenticated() {
    enterpriseReportingService.createSSOEmbedUrl(new DashboardRequestDTO("rolling-recap"));
  }

  @Test(expected = BadRequestException.class)
  public void testCreateSSOEmbedUrl_UnAuthorized() {
    login();
    enterpriseReportingService.createSSOEmbedUrl(new DashboardRequestDTO(null));
  }

  private void enableFeature() {
    ApiConfigFeaturesService.SystemConfigurationPropertyFeature
        .INTEGRATED_ENTERPRISE_REPORTING.setEnabled(true);
  }

  private void disableFeature() {
    ApiConfigFeaturesService.SystemConfigurationPropertyFeature
        .INTEGRATED_ENTERPRISE_REPORTING.setEnabled(false);
  }
}
