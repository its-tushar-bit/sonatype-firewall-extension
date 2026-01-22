/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.file.SbomFormat;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiThirdPartyScanServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiThirdPartyScanService apiThirdPartyEvaluationService;

  @Test(expected = InvalidStageException.class)
  public void testScanComponents_Authorized() {
    grantEvaluateApplicationPermission(app.getId());
    apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "invalidStage", null, null, SbomFormat.XML);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testScanComponents_Unauthenticated() {
    apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "build", "", null, SbomFormat.XML);
  }

  @Test(expected = UnauthorizedException.class)
  public void testScanComponents_Unauthorized() {
    login();
    apiThirdPartyEvaluationService.scanComponents(app.getId(), "clair", "build", "", null, SbomFormat.XML);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetScanStatus_Unauthenticated() {
    apiThirdPartyEvaluationService.getScanStatus(app.getId(), "scanRequestId");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetScanStatus_Unauthorized() {
    login();
    apiThirdPartyEvaluationService.getScanStatus(app.getId(), "scanRequestId");
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
