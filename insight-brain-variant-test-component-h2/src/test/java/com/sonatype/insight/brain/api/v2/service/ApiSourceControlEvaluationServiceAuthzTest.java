/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlEvaluationRequestDTO;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiSourceControlEvaluationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
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

  @Test
  public void testEvaluateSourceControl_Unauthenticated() {
    ApiSourceControlEvaluationRequestDTO apiSourceControlEvaluationRequestDTO =
        new ApiSourceControlEvaluationRequestDTO(Stage.ID_DEVELOP, "a-branch");
    assertThrows(UnauthenticatedException.class,
        () -> service.evaluateSourceControl(app.getId(), apiSourceControlEvaluationRequestDTO, "useragent"));
  }

  @Test
  public void testEvaluateSourceControl_Unauthorized() {
    login();
    ApiSourceControlEvaluationRequestDTO apiSourceControlEvaluationRequestDTO =
        new ApiSourceControlEvaluationRequestDTO(Stage.ID_DEVELOP, "a-branch");
    assertThrows(UnauthorizedException.class,
        () -> service.evaluateSourceControl(app.getId(), apiSourceControlEvaluationRequestDTO, "useragent"));
  }

  @Test
  public void testGetApplicationEvaluationStatus_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> service.getApplicationEvaluationStatus(app.getId(), "statusId"));
  }

  @Test
  public void testGetApplicationEvaluationStatus_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> service.getApplicationEvaluationStatus(app.getId(), "statusId"));
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
