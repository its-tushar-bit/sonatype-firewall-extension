/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

public class IntegrationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private IntegrationService integrationService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetPercentageOfAppsWithCITriggeredEvaluations__Unauthenticated() {
    integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPercentageOfAppsWithCITriggeredEvaluations_Unauthorized() {
    login();
    integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
  }

  @Test
  public void testGetPercentageOfAppsWithCITriggeredEvaluations_Authorized() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);
    assertThatCode(() ->
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null))
        .doesNotThrowAnyException();
  }
}
