/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.integration;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class CIEvaluationStatServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final long CUT_OFF_DATE_MILLIS = 1621220400000L;

  @Inject
  private CIEvaluationStatService ciEvaluationStatService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetPercentageOfAppsWithCITriggeredEvaluations__Unauthenticated() {
    ciEvaluationStatService.getDataForAppsWithoutCITriggeredEvaluations(CUT_OFF_DATE_MILLIS);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPercentageOfAppsWithCITriggeredEvaluations_Unauthorized() {
    login();
    ciEvaluationStatService.getDataForAppsWithoutCITriggeredEvaluations(CUT_OFF_DATE_MILLIS);
  }

  @Test
  public void testGetPercentageOfAppsWithCITriggeredEvaluations_Authorized() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);
    ciEvaluationStatService.getDataForAppsWithoutCITriggeredEvaluations(CUT_OFF_DATE_MILLIS);
  }
}
