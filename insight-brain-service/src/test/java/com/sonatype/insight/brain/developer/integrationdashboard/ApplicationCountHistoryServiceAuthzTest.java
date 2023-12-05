/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import java.util.List;
import javax.inject.Inject;

import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiUsageIncrementDto;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ApplicationCountHistoryServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final long ANY_VALID_INCREMENT_SIZE = 50_000L;

  private static final int ANY_VALID_NUMBER_OF_INCREMENTS = 3;

  @Inject
  private ApplicationCountHistoryService applicationCountHistoryService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetUsageOverTime_Unauthenticated() {
    applicationCountHistoryService.getUsageOverTime(ANY_VALID_INCREMENT_SIZE, ANY_VALID_NUMBER_OF_INCREMENTS);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetUsageOverTime_Unauthorized() {
    login();
    applicationCountHistoryService.getUsageOverTime(ANY_VALID_INCREMENT_SIZE, ANY_VALID_NUMBER_OF_INCREMENTS);
  }

  @Test
  public void testGetUsageOverTime_Authorized() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);

    final List<ApiUsageIncrementDto> results = applicationCountHistoryService.getUsageOverTime(
        ANY_VALID_INCREMENT_SIZE, ANY_VALID_NUMBER_OF_INCREMENTS);

    assertThat(results).isNotNull();
  }
}
