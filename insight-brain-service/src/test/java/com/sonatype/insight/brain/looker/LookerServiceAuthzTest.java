/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.looker;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class LookerServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private LookerService lookerService;

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
    SSOEmbedUrlDTO ssoEmbedUrl =
        lookerService.createSSOEmbedUrl(ROOT_ORGANIZATION_ID, new LookerDashboardDTO("rolling-recap"));
    assertThat(ssoEmbedUrl).isNull();
  }

  @Test(expected = UnauthorizedException.class)
  public void testCreateSSOEmbedUrl_UnAuthorized() {
    login();
    SSOEmbedUrlDTO ssoEmbedUrl =
        lookerService.createSSOEmbedUrl(ROOT_ORGANIZATION_ID, new LookerDashboardDTO("rolling-recap"));
    assertThat(ssoEmbedUrl).isNull();
  }

  @Test(expected = BadRequestException.class)
  public void testCreateSSOEmbedUrl_Authorized() {
    grantReadPermission(ROOT_ORGANIZATION_ID);
    lookerService.createSSOEmbedUrl(ROOT_ORGANIZATION_ID, new LookerDashboardDTO(null));
  }

  private void enableFeature() {
    ApiConfigFeaturesService.SystemConfigurationPropertyFeature
        .LOOKER_INTEGRATED_ENTERPRISE_REPORTING.setEnabled(true);
  }

  private void disableFeature() {
    ApiConfigFeaturesService.SystemConfigurationPropertyFeature
        .LOOKER_INTEGRATED_ENTERPRISE_REPORTING.setEnabled(false);
  }
}
