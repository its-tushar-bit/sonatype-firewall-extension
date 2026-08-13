/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.developer.integrationdashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiIntegrationsScmFeedbackStatIncrementDto;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.util.List;
import org.apache.shiro.authz.UnauthenticatedException;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class ScmStatServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  private static final long ANY_LONG_NUMBER = 1621220400000L;

  private static final int ANY_INT_NUMBER = 4;

  @Inject
  private ScmStatService scmStatService;

  @Test
  public void testGetPercentageOfAppsWithCITriggeredEvaluations__Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> scmStatService.getScmFeedbackUsageStatsOverTime(ANY_LONG_NUMBER, ANY_INT_NUMBER));
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
