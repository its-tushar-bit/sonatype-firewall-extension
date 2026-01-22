/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiIntegrationsCiCdStatIncrementDto;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CIEvaluationStatServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private CIEvaluationStatService ciEvaluationStatService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetCiCdUsageStatsOverTime__Unauthenticated() {
    ciEvaluationStatService.getCiCdUsageStatsOverTime(anyNumber(), anyNumber());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetCiCdUsageStatsOverTime_Unauthorized() {
    login();
    ciEvaluationStatService.getCiCdUsageStatsOverTime(anyNumber(), anyNumber());
  }

  @Test
  public void testGetCiCdUsageStatsOverTime_Authorized() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);

    final List<ApiIntegrationsCiCdStatIncrementDto> apiIntegrationsCiCdStatIncrementDtoList =
        ciEvaluationStatService.getCiCdUsageStatsOverTime(anyNumber(), anyNumber());

    assertThat(apiIntegrationsCiCdStatIncrementDtoList).isNotNull();
  }

  private int anyNumber() {
    return ThreadLocalRandom.current().nextInt(1, 10);
  }
}
