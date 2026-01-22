/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class LegalApplicationDashboardServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private LegalApplicationDashboardService legalApplicationDashboardService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetLicenseLegalApplicationDashboard_Unauthenticated() {
    legalApplicationDashboardService.getLicenseLegalApplicationDashboard(null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLicenseLegalApplicationDashboard_Unauthorized() {
    login();
    legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), null);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_Authorized() {
    grantLegalReviewerPermission(app.getId());
    assertThat(legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), null)).isEmpty();
  }
}
