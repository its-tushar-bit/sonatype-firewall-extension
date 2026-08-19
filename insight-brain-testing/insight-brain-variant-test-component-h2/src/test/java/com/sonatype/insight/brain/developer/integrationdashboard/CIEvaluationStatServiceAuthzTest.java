/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.developer.integrationdashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiIntegrationsCiCdStatIncrementDto;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class CIEvaluationStatServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private CIEvaluationStatService ciEvaluationStatService;

  @Test
  public void testGetCiCdUsageStatsOverTime__Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> ciEvaluationStatService.getCiCdUsageStatsOverTime(anyNumber(), anyNumber()));
  }

  @Test
  public void testGetCiCdUsageStatsOverTime_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> ciEvaluationStatService.getCiCdUsageStatsOverTime(anyNumber(), anyNumber()));
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
