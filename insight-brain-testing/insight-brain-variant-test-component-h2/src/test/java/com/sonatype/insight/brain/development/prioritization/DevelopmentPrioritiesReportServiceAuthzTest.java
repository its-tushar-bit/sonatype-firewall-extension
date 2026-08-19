/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.development.prioritization;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ComponentH2Test
public class DevelopmentPrioritiesReportServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private DevelopmentPrioritiesReportService developmentPrioritiesReportService;

  @Test
  public void getDependencyInformation_Anon() {
    assertThatThrownBy(
        () -> developmentPrioritiesReportService.getDependencyInformation(app.getPublicId(), "irrelevant"))
            .isInstanceOf(UnauthenticatedException.class);
  }

  @Test
  public void getDependencyInformation_Unauthorized() {
    login();
    assertThatThrownBy(
        () -> developmentPrioritiesReportService.getDependencyInformation(app.getPublicId(), "irrelevant"))
            .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void getDependencyInformation_Authorized() {
    grantReadPermission(app.getId());
    assertThatThrownBy(
        () -> developmentPrioritiesReportService.getDependencyInformation(app.getPublicId(), "irrelevant"))
            .isInstanceOf(NotFoundException.class);
  }
}
