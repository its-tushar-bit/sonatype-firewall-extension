/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.ArrayList;
import java.util.Collections;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.callflowanalysis.ApiCallFlowAnalysisConfigDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.CallFlowAnalysisConfig;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

@Category(SlowTest.class)
public class ApiCallFlowAnalysisConfigServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiCallFlowAnalysisConfigService apiCallFlowAnalysisService;

  @Inject
  private Application application;

  @Before
  public void before() {
    application = tempEntity.newApplicationWithParent("ApiCallFlowAnalysis");
  }

  @Test
  public void testUpsertCallFlowAnalysisConfig_Create_Success() {
    ApiCallFlowAnalysisConfigDTO callFlowAnalysisConfig = buildCallFlowAnalysisConfig(application.getId());
    ApiCallFlowAnalysisConfigDTO result = apiCallFlowAnalysisService.upsertCallFlowAnalysisConfig(
        application.getType(), application.getId(), callFlowAnalysisConfig);
    assertThat(result).isNotNull();
    assertThat(result.id).isNotNull();
    assertThat(result.algorithm).isNull();

    ApiCallFlowAnalysisConfigDTO persisted = apiCallFlowAnalysisService.getCallFlowAnalysisConfig(
        application.getType(), application.getId());

    assertCallFlowAnalysis(result, persisted);
  }

  @Test
  public void testUpsertCallFlowAnalysisConfig_BadRequest() {

    final ApiCallFlowAnalysisConfigDTO callFlowAnalysisConfigOwnerMatch =
        buildCallFlowAnalysisConfigBadRequestOwnerNotMatch();

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      apiCallFlowAnalysisService.upsertCallFlowAnalysisConfig(
          application.getType(), application.getId(), callFlowAnalysisConfigOwnerMatch);
    }).withMessage("ownerId does not match");

    final ApiCallFlowAnalysisConfigDTO callFlowAnalysisConfigOwnerNull =
        buildCallFlowAnalysisConfigBadRequestOwnerNull();

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      apiCallFlowAnalysisService.upsertCallFlowAnalysisConfig(
          application.getType(), application.getId(), callFlowAnalysisConfigOwnerNull);
    }).withMessage("ownerId cannot be null");
  }

  @Test
  public void testUpsertCallFlowAnalysisConfig_Update() {
    ApiCallFlowAnalysisConfigDTO persisted = buildApiCallFlowConfigDTO(
        tempEntity.newCallFlowAnalysisConfig(application.getId(), 1));

    ApiCallFlowAnalysisConfigDTO updatedConfig = buildCallFlowAnalysisConfig(application.getId());
    updatedConfig.enabled = false;
    ApiCallFlowAnalysisConfigDTO result = apiCallFlowAnalysisService.upsertCallFlowAnalysisConfig(
        application.getType(), application.getId(), updatedConfig);

    assertThat(result).isNotNull();
    assertThat(result.enabled).isFalse();
    assertCallFlowAnalysis(result, persisted);
  }

  @Test
  public void testDeleteCallFlowAnalysisConfig() {
    tempEntity.newCallFlowAnalysisConfig(application.getId(), 1);
    apiCallFlowAnalysisService.deleteCallFlowAnalysisConfig(application.getType(), application.getId());

    // Verify: Assert that retrieving the config now throws NotFoundException
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      apiCallFlowAnalysisService
          .getCallFlowAnalysisConfig(application.getType(), application.getId());
    }).withMessage("Call Flow Analysis Config not found for ownerId " + application.getId());
  }

  @Test
  public void testDeleteCallFlowAnalysisConfig_NotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      apiCallFlowAnalysisService.deleteCallFlowAnalysisConfig(application.getType(), application.getId());
    }).withMessage("Call Flow Analysis Config not found for ownerId " + application.getId());
  }

  @Test
  public void testGetCallFlowAnalysisConfig_Success() {
    CallFlowAnalysisConfig existing = tempEntity.newCallFlowAnalysisConfig(application.getId(), 1);
    ApiCallFlowAnalysisConfigDTO result = apiCallFlowAnalysisService.getCallFlowAnalysisConfig(
        application.getType(), application.getId());

    assertThat(result).isNotNull();
    assertCallFlowAnalysis(buildApiCallFlowConfigDTO(existing), result);
  }

  @Test
  public void testGetCallFlowAnalysisConfig_NotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      apiCallFlowAnalysisService.getCallFlowAnalysisConfig(application.getType(), application.getId());
    }).withMessage("Call Flow Analysis Config not found for ownerId " + application.getId());
  }

  @Test
  public void testGetCallFlowAnalysisConfig_Hierarchy() {
    ApiCallFlowAnalysisConfigDTO existing = buildApiCallFlowConfigDTO(
        tempEntity.newCallFlowAnalysisConfig(application.getOrganizationId(), 1));
    ApiCallFlowAnalysisConfigDTO result = apiCallFlowAnalysisService.getCallFlowAnalysisConfig(
        application.getType(), application.getParentOwnerId());

    assertThat(result).isNotNull();
    assertCallFlowAnalysis(existing, result);
  }

  @Test
  public void testGetCallFlowAnalysisConfigByPublicId_Success() {
    CallFlowAnalysisConfig existing = tempEntity.newCallFlowAnalysisConfig(application.getId(), 1);
    ApiCallFlowAnalysisConfigDTO result = apiCallFlowAnalysisService.getCallFlowAnalysisConfigByPublicId(
        application.getType(), application.getPublicId());

    assertThat(result).isNotNull();
    assertCallFlowAnalysis(buildApiCallFlowConfigDTO(existing), result);
  }

  @Test
  public void testGetCallFlowAnalysisConfigByPublicId_NotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      apiCallFlowAnalysisService.getCallFlowAnalysisConfigByPublicId(application.getType(), application.getPublicId());
    }).withMessage("Call Flow Analysis Config not found for ownerId " + application.getId());
  }

  private ApiCallFlowAnalysisConfigDTO buildCallFlowAnalysisConfig(String ownerId) {
    ApiCallFlowAnalysisConfigDTO apiCallFlowAnalysisConfigDTO = new ApiCallFlowAnalysisConfigDTO();
    apiCallFlowAnalysisConfigDTO.enabled = true;
    apiCallFlowAnalysisConfigDTO.ownerId = ownerId;
    apiCallFlowAnalysisConfigDTO.threadCount = 1;
    apiCallFlowAnalysisConfigDTO.namespaces = Collections.singletonList("com.sonatype");
    return apiCallFlowAnalysisConfigDTO;
  }

  private ApiCallFlowAnalysisConfigDTO buildCallFlowAnalysisConfigBadRequestOwnerNull() {
    ApiCallFlowAnalysisConfigDTO apiCallFlowAnalysisConfigDTO = new ApiCallFlowAnalysisConfigDTO();
    apiCallFlowAnalysisConfigDTO.threadCount = 1;
    apiCallFlowAnalysisConfigDTO.enabled = true;
    apiCallFlowAnalysisConfigDTO.namespaces = new ArrayList<>();
    return apiCallFlowAnalysisConfigDTO;
  }

  private ApiCallFlowAnalysisConfigDTO buildCallFlowAnalysisConfigBadRequestOwnerNotMatch() {
    ApiCallFlowAnalysisConfigDTO apiCallFlowAnalysisConfigDTO = new ApiCallFlowAnalysisConfigDTO();
    apiCallFlowAnalysisConfigDTO.threadCount = 1;
    apiCallFlowAnalysisConfigDTO.enabled = true;
    apiCallFlowAnalysisConfigDTO.ownerId = application.getPublicId();
    apiCallFlowAnalysisConfigDTO.namespaces = new ArrayList<>();
    return apiCallFlowAnalysisConfigDTO;
  }

  private void assertCallFlowAnalysis(ApiCallFlowAnalysisConfigDTO expected, ApiCallFlowAnalysisConfigDTO actual) {
    assertThat(expected.ownerId).isEqualTo(actual.ownerId);
    assertThat(expected.id).isEqualTo(actual.id);
    assertThat(expected.threadCount).isEqualTo(actual.threadCount);
    assertThat(expected.namespaces).isEqualTo(actual.namespaces);
  }

  private ApiCallFlowAnalysisConfigDTO buildApiCallFlowConfigDTO(CallFlowAnalysisConfig callFlowAnalysisConfig) {
    ApiCallFlowAnalysisConfigDTO apiCallFlowAnalysisConfigDTO = new ApiCallFlowAnalysisConfigDTO();
    apiCallFlowAnalysisConfigDTO.id = callFlowAnalysisConfig.getId();
    apiCallFlowAnalysisConfigDTO.algorithm = callFlowAnalysisConfig.getAlgorithm();
    apiCallFlowAnalysisConfigDTO.enabled = callFlowAnalysisConfig.isEnabled();
    apiCallFlowAnalysisConfigDTO.namespaces = callFlowAnalysisConfig.getNamespaces();
    apiCallFlowAnalysisConfigDTO.threadCount = callFlowAnalysisConfig.getThreadCount();
    apiCallFlowAnalysisConfigDTO.ownerId = callFlowAnalysisConfig.getOwnerId();
    return apiCallFlowAnalysisConfigDTO;
  }
}
