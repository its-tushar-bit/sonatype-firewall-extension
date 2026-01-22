/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiReportHistoryDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiReportServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiReportServiceV2 apiReportServiceV2;

  @Test(expected = UnauthenticatedException.class)
  public void testGetByApplicationId_Unauthenticated() {
    apiReportServiceV2.getByApplicationId(app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetByApplicationId_Unauthorized() {
    login();
    apiReportServiceV2.getByApplicationId(app.getId());
  }

  @Test
  public void testGetByApplicationId_Authorized() {
    grantReadPermission(app.getId());
    apiReportServiceV2.getByApplicationId(app.getId());
  }

  @Test
  public void testGetAll_Unauthenticated() {
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId");

    assertThat(apiReportServiceV2.getAll()).isEmpty();
  }

  @Test
  public void testGetAll_Unauthorized() {
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId");

    login();
    assertThat(apiReportServiceV2.getAll()).isEmpty();
  }

  @Test
  public void testGetAll_Authorized() {
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId");

    Application unauthorizedApp = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyEvaluation(unauthorizedApp.getId(), StageTypes.BUILD.getId(), "scanId1");

    grantReadPermission(app.getId());
    assertThat(apiReportServiceV2.getAll()).extracting(dto -> dto.applicationId).containsExactly(app.getId());
  }

  @Test
  public void testGetReportHistoryForApplication_Authorized() {
    grantReadPermission(app.getId());

    ApiReportHistoryDTO reports = apiReportServiceV2.getReportHistoryForApplication(app.getId(), null, null);

    assertThat(reports.applicationId).isEqualTo(app.getId());
    assertThat(reports.reports).hasSize(0);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetReportHistoryForApplication_Unauthenticated() {
    apiReportServiceV2.getReportHistoryForApplication(app.getId(), null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetReportHistoryForApplication_Unauthorized() {
    login();
    apiReportServiceV2.getReportHistoryForApplication(app.getId(), null, null);
  }
}
