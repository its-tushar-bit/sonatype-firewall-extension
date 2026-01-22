/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiIntegrationsScmFeedbackStatIncrementDto;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ScmStatServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final long ANY_LONG_NUMBER = 1621220400000L;

  private static final int ANY_INT_NUMBER = 4;

  @Inject
  private ScmStatService scmStatService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetPercentageOfAppsWithCITriggeredEvaluations__Unauthenticated() {
    scmStatService.getScmFeedbackUsageStatsOverTime(ANY_LONG_NUMBER, ANY_INT_NUMBER);
  }

  @Test
  public void testGetPercentageOfAppsWithCITriggeredEvaluations__authorized() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);

    final List<ApiIntegrationsScmFeedbackStatIncrementDto> results =
        scmStatService.getScmFeedbackUsageStatsOverTime(ANY_LONG_NUMBER, ANY_INT_NUMBER);

    // actual logic tested in ScmStatServiceTest
    assertThat(results).isNotNull();
  }
}
