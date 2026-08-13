/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class LegalApplicationDashboardServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private LegalApplicationDashboardService legalApplicationDashboardService;

  @Test
  public void testGetLicenseLegalApplicationDashboard_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> legalApplicationDashboardService.getLicenseLegalApplicationDashboard(null, null));
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), null));
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_Authorized() {
    grantLegalReviewerPermission(app.getId());
    assertThat(legalApplicationDashboardService.getLicenseLegalApplicationDashboard(app.getPublicId(), null)).isEmpty();
  }
}
