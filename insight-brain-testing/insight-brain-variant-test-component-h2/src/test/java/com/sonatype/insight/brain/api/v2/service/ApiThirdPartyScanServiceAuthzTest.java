/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.file.SbomFormat;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ComponentH2Test
public class ApiThirdPartyScanServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiThirdPartyScanService apiThirdPartyEvaluationService;

  @Test
  public void testScanComponents_Authorized() {
    grantEvaluateApplicationPermission(app.getId());
    assertThatExceptionOfType(InvalidStageException.class)
        .isThrownBy(() -> apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "invalidStage", null,
            null, SbomFormat.XML));
  }

  @Test
  public void testScanComponents_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "build", "", null,
            SbomFormat.XML));
  }

  @Test
  public void testScanComponents_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "build", "", null,
            SbomFormat.XML));
  }

  @Test
  public void testGetScanStatus_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> apiThirdPartyEvaluationService.getScanStatus(app.getId(), "scanRequestId"));
  }

  @Test
  public void testGetScanStatus_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> apiThirdPartyEvaluationService.getScanStatus(app.getId(), "scanRequestId"));
  }

  @Test
  public void testGetScanStatus_Authorized() {
    grantEvaluateApplicationPermission(app.getId());
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiThirdPartyEvaluationService.getScanStatus(app.getId(), "scanRequestId"))
        .withMessage("Policy evaluation status with id %s for public application id %s was not found.",
            "scanRequestId", app.getPublicId());
  }
}
