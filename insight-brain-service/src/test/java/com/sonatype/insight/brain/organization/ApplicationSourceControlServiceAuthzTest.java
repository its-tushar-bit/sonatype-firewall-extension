/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.organization;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationSourceControlServiceAuthzTest extends AbstractServiceAuthzTest
{
  @Inject
  ApplicationSourceControlService applicationSourceControlService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabledRisk_Unauthenticated() {
    final List<?> result =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(
            getRandomValidPageSize());

    assertThat(result).isEmpty();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabledRisk_Unauthorized() {
    login();
    final List<?> result =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(
            getRandomValidPageSize());

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabledRisk_Authorized() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);
    final List<?> result =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(
            getRandomValidPageSize());

    assertThat(result).hasSize(1);
  }

  private int getRandomValidPageSize() {
    return ThreadLocalRandom.current().nextInt(1, 1000);
  }
}
