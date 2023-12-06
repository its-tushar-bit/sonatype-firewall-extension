/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import javax.inject.Inject;

import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiChartVisibilityDto;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StatsChartVisibilityServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private StatsChartVisibilityService statsChartVisibilityService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetChartVisibilityForUser_Unauthenticated() {
    statsChartVisibilityService.getChartVisibilityForUser();
  }

  @Test
  public void testGetChartVisibilityForUser_ShouldReturnCorrectChartVisiblityWhenUserDoesNotHaveRootReadAccess() {
    login();
    final ApiChartVisibilityDto result = statsChartVisibilityService.getChartVisibilityForUser();

    assertThat(result).isEqualTo(new ApiChartVisibilityDto(false));
  }

  @Test
  public void testGetChartVisibilityForUser_ShouldReturnCorrectChartVisiblityWhenUserHasRootReadAccess() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);
    final ApiChartVisibilityDto result = statsChartVisibilityService.getChartVisibilityForUser();

    assertThat(result).isEqualTo(new ApiChartVisibilityDto(true));
  }
}
