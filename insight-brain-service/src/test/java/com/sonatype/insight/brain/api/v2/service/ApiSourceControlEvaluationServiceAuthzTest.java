/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlEvaluationRequestDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiSourceControlEvaluationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiSourceControlEvaluationService service;

  @Test
  public void testEvaluateSourceControl_Authorized() {
    grantEvaluateApplicationPermission(app.getId());

    ApiSourceControlEvaluationRequestDTO apiSourceControlEvaluationRequestDTO =
        new ApiSourceControlEvaluationRequestDTO(Stage.ID_DEVELOP, "a-branch");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.evaluateSourceControl(app.getId(), apiSourceControlEvaluationRequestDTO, "useragent"))
        .withMessage("No SCM configuration defined for application ID " + app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateSourceControl_Unauthenticated() {
    ApiSourceControlEvaluationRequestDTO apiSourceControlEvaluationRequestDTO =
        new ApiSourceControlEvaluationRequestDTO(Stage.ID_DEVELOP, "a-branch");
    service.evaluateSourceControl(app.getId(), apiSourceControlEvaluationRequestDTO, "useragent");
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateSourceControl_Unauthorized() {
    login();
    ApiSourceControlEvaluationRequestDTO apiSourceControlEvaluationRequestDTO =
        new ApiSourceControlEvaluationRequestDTO(Stage.ID_DEVELOP, "a-branch");
    service.evaluateSourceControl(app.getId(), apiSourceControlEvaluationRequestDTO, "useragent");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicationEvaluationStatus_Unauthenticated() {
    service.getApplicationEvaluationStatus(app.getId(), "statusId");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicationEvaluationStatus_Unauthorized() {
    login();
    service.getApplicationEvaluationStatus(app.getId(), "statusId");
  }

  @Test
  public void testGetApplicationEvaluationStatus_Authorized() {
    grantEvaluateApplicationPermission(app.getId());
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getApplicationEvaluationStatus(app.getId(), "statusId"))
        .withMessage("Policy evaluation status with id %s for public application id %s was not found.", "statusId",
            app.getPublicId());
  }
}
