/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.ArrayList;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.callflowanalysis.ApiCallFlowAnalysisConfigDTO;
import com.sonatype.clm.dto.model.callflowanalysis.CallFlowAlgorithm;
import com.sonatype.insight.brain.dataaccess.configuration.CallFlowAnalysisConfigDAO;
import com.sonatype.insight.brain.model.configuration.CallFlowAnalysisConfig;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

@ComponentH2Test
public class ApiCallFlowAnalysisConfigServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  CallFlowAnalysisConfigDAO callFlowDao;

  @Inject
  private ApiCallFlowAnalysisConfigService apiCallFlowAnalysisService;

  @BeforeEach
  public void before() {
    callFlowDao = lookup(CallFlowAnalysisConfigDAO.class);
  }

  @Test
  public void testGetCallFlowAnalysisConfig_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> apiCallFlowAnalysisService.getCallFlowAnalysisConfig(org.getType(), org.getId()));
  }

  @Test
  public void testGetCallFlowAnalysisConfig_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> apiCallFlowAnalysisService.getCallFlowAnalysisConfig(org.getType(), org.getId()));
  }

  @Test
  public void testGetCallFlowAnalysisConfig_Authorized() {
    grantWritePermission(org.getId());
    ApiCallFlowAnalysisConfigDTO persisted = apiCallFlowAnalysisService.upsertCallFlowAnalysisConfig(
        org.getType(), org.getId(), buildCallFlowAnalysisConfig(org.getId()));
    grantEvaluateApplicationPermission(org.getId());
    ApiCallFlowAnalysisConfigDTO result =
        apiCallFlowAnalysisService.getCallFlowAnalysisConfig(org.getType(), org.getId());
    assertCallFlowAnalysis(result, persisted);
  }

  @Test
  public void testGetCallFlowAnalysisConfigByPublicId_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(
            () -> apiCallFlowAnalysisService.getCallFlowAnalysisConfigByPublicId(org.getType(), org.getPublicId()));
  }

  @Test
  public void testGetCallFlowAnalysisConfigByPublicId_Authorized() {
    grantWritePermission(org.getId());
    ApiCallFlowAnalysisConfigDTO persisted = apiCallFlowAnalysisService.upsertCallFlowAnalysisConfig(
        org.getType(), org.getId(), buildCallFlowAnalysisConfig(org.getId()));
    grantEvaluateApplicationPermission(org.getId());
    ApiCallFlowAnalysisConfigDTO result =
        apiCallFlowAnalysisService.getCallFlowAnalysisConfigByPublicId(org.getType(), org.getPublicId());
    assertCallFlowAnalysis(result, persisted);
  }

  @Test
  public void testGetCallFlowAnalysisConfigByPublicId_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> apiCallFlowAnalysisService.getCallFlowAnalysisConfig(org.getType(), org.getPublicId()));
  }

  @Test
  public void testUpsertCallFlowAnalysisConfig_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> apiCallFlowAnalysisService.upsertCallFlowAnalysisConfig(
            org.getType(), org.getId(), buildCallFlowAnalysisConfig(org.getId())));
  }

  @Test
  public void testUpsertCallFlowAnalysisConfig_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> apiCallFlowAnalysisService.upsertCallFlowAnalysisConfig(
            org.getType(), org.getId(), buildCallFlowAnalysisConfig(org.getId())));
  }

  @Test
  public void testUpsertCallFlowAnalysisConfig_Authorized() {
    grantWritePermission(org.getId());
    ApiCallFlowAnalysisConfigDTO result = apiCallFlowAnalysisService.upsertCallFlowAnalysisConfig(
        org.getType(), org.getId(), buildCallFlowAnalysisConfig(org.getId()));
    assertThat(result).isNotNull();
    assertThat(result.id).isNotNull();
    assertThat(result.algorithm).isNull();
  }

  @Test
  public void testDeleteCallFlowAnalysisConfig_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> apiCallFlowAnalysisService.deleteCallFlowAnalysisConfig(org.getType(), org.getId()));
  }

  @Test
  public void testDeleteCallFlowAnalysisConfig_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> apiCallFlowAnalysisService.deleteCallFlowAnalysisConfig(org.getType(), org.getId()));
  }

  @Test
  public void testDeleteCallFlowAnalysisConfig_Authorized() {
    grantWritePermission(org.getId());
    insertElementToSearch();
    apiCallFlowAnalysisService.deleteCallFlowAnalysisConfig(org.getType(), org.getId());
    grantEvaluateApplicationPermission(org.getId());
    // Verify: Assert that retrieving the config now throws NotFoundException
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      apiCallFlowAnalysisService
          .getCallFlowAnalysisConfig(org.getType(), org.getId());
    })
        .withMessage("Call Flow Analysis Config not found for ownerId "
            + org.getId());
  }

  @Test
  public void testDeleteCallFlowAnalysisConfig_Authorized_NotFound() {
    grantWritePermission(org.getId());
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiCallFlowAnalysisService.deleteCallFlowAnalysisConfig(org.getType(), org.getId()));
  }

  private ApiCallFlowAnalysisConfigDTO buildCallFlowAnalysisConfig(String ownerId) {
    ApiCallFlowAnalysisConfigDTO apiCallFlowAnalysisConfigDTO = new ApiCallFlowAnalysisConfigDTO();
    apiCallFlowAnalysisConfigDTO.enabled = true;
    apiCallFlowAnalysisConfigDTO.ownerId = ownerId;
    apiCallFlowAnalysisConfigDTO.threadCount = 1;
    apiCallFlowAnalysisConfigDTO.namespaces = new ArrayList<>();
    return apiCallFlowAnalysisConfigDTO;
  }

  private void insertElementToSearch() {
    callFlowDao.insert(new CallFlowAnalysisConfig(
        true, new ArrayList<>(), CallFlowAlgorithm.CLASS_HIERARCHY_ANALYSIS, 2, org.getId()));
  }

  private void assertCallFlowAnalysis(ApiCallFlowAnalysisConfigDTO expected, ApiCallFlowAnalysisConfigDTO actual) {
    assertThat(expected.ownerId).isEqualTo(actual.ownerId);
    assertThat(expected.id).isEqualTo(actual.id);
    assertThat(expected.threadCount).isEqualTo(actual.threadCount);
    assertThat(expected.namespaces).isEqualTo(actual.namespaces);
  }
}
