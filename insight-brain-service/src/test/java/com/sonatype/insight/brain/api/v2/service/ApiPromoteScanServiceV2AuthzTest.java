/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiPromoteScanRequestDTOV2;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiPromoteScanServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiPromoteScanServiceV2 service;

  @Test(expected = UnauthenticatedException.class)
  public void testPromoteScan_Unauthenticated() {
    service.promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan("scanId", Stage.ID_OPERATE),
        null /* userAgent */);
  }

  @Test(expected = UnauthorizedException.class)
  public void testPromoteScan_Unauthorized() {
    login();
    service.promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan("scanId", Stage.ID_OPERATE),
        null /* userAgent */);
  }

  @Test(expected = BadRequestException.class)
  public void testPromoteScan_Authorized() {
    grantEvaluateApplicationPermission(app.getId());
    service.promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan("scanId", Stage.ID_OPERATE),
        null /* userAgent */);
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
