/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.development.prioritization;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DevelopmentPrioritiesServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private DevelopmentPrioritiesService developmentPrioritiesService;

  @Test
  public void getPrioritizedFindings_Unauthorized() {
    login();
    assertThatThrownBy(
            () -> developmentPrioritiesService.getPrioritizedFindings(app.getPublicId(), "irrelevant",
                    0, 10, null, false, false))
            .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void getPrioritizedFindings_Authorized() {
    grantReadPermission(app.getId());
    assertThatThrownBy(
            () -> developmentPrioritiesService.getPrioritizedFindings(app.getPublicId(),
        "irrelevant", 0, 10, null, false, false))
            .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void getAllPrioritizedFindings_Unauthorized() {
    login();
    assertThatThrownBy(
            () -> developmentPrioritiesService.getAllPrioritizedFindings(app.getPublicId(), "irrelevant",null, null))
            .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void getAllPrioritizedFindings_Authorized() {
    grantReadPermission(app.getId());
    assertThatThrownBy(
            () -> developmentPrioritiesService.getAllPrioritizedFindings(app.getPublicId(), "irrelevant", null, null))
            .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void testGetComponentVersions_Unauthenticated() throws Exception {
    assertThatThrownBy(
        () -> developmentPrioritiesService.getAllPrioritizedFindings(app.getPublicId(), "irrelevant", null, null))
        .isInstanceOf(UnauthenticatedException.class);
  }
}
