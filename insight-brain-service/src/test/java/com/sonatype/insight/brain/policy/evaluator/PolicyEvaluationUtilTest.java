/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSubStatus;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.LicensedFeature;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PolicyEvaluationUtilTest
    extends AbstractComponentTest
{
  @Mock
  private ProductLicense mockProductLicense;

  @Mock
  private StageTypeService mockStageTypeService;

  @Mock
  private PersistedPolicyEvaluationPollingResultDAO mockPersistedPolicyEvaluationPollingResultDAO;

  private PolicyEvaluationUtil policyEvaluationUtil;

  @Before
  public void setUp() {
    policyEvaluationUtil =
        new PolicyEvaluationUtil(mockProductLicense, mockStageTypeService,
            mockPersistedPolicyEvaluationPollingResultDAO);
  }

  @Test
  public void testValidateEvaluationTypeAndFeature_CLI() {
    Stage stage = new Stage(Stage.ID_BUILD);
    when(mockStageTypeService.getLicensedStageTypes()).thenReturn(Set.of(StageTypes.BUILD));

    policyEvaluationUtil.validateEvaluationTypeAndFeature(IntegrationType.CLI, stage);

    verify(mockStageTypeService).getLicensedStageTypes();
  }

  @Test(expected = InvalidLicenseException.class)
  public void testValidateEvaluationTypeAndFeature_CLI_Proxy() {
    Stage stage = new Stage(Stage.ID_PROXY);

    policyEvaluationUtil.validateEvaluationTypeAndFeature(IntegrationType.CLI, stage);

    verify(mockStageTypeService).getLicensedStageTypes();
  }

  @Test(expected = InvalidLicenseException.class)
  public void testValidateEvaluationTypeAndFeature_CLI_ProxyWithCorrectFeatureOnly() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    Stage stage = new Stage(Stage.ID_PROXY);

    policyEvaluationUtil.validateEvaluationTypeAndFeature(IntegrationType.CLI, stage);

    verify(mockStageTypeService).getLicensedStageTypes();
  }

  @Test(expected = InvalidLicenseException.class)
  public void testValidateEvaluationTypeAndFeature_CLI_ProxyWithCorrectLicenseOnly() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
    lenient().when(mockProductLicense.hasFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION)).thenReturn(true);

    Stage stage = new Stage(Stage.ID_PROXY);

    policyEvaluationUtil.validateEvaluationTypeAndFeature(IntegrationType.CLI, stage);

    verify(mockStageTypeService).getLicensedStageTypes();
  }

  @Test
  public void testValidateEvaluationTypeAndFeature_CLI_ProxyWithCorrectFeatureAndLicense() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    lenient().when(mockProductLicense.hasFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION)).thenReturn(true);
    when(mockStageTypeService.getLicensedStageTypes()).thenReturn(Set.of(StageTypes.BUILD, StageTypes.PROXY));

    Stage stage = new Stage(Stage.ID_PROXY);

    policyEvaluationUtil.validateEvaluationTypeAndFeature(IntegrationType.CLI, stage);

    verify(mockStageTypeService).getLicensedStageTypes();
  }

  @Test
  public void testValidateEvaluationTypeAndFeature_CLI_ProxyWithCorrectFeatureAndLicense_WithoutCiIntegration() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    lenient().when(mockProductLicense.hasFeature(LicensedFeature.CI_INTEGRATION)).thenReturn(false);
    lenient().when(mockProductLicense.hasFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION)).thenReturn(true);
    when(mockStageTypeService.getLicensedStageTypes()).thenReturn(Set.of(StageTypes.BUILD, StageTypes.PROXY));

    Stage stage = new Stage(Stage.ID_PROXY);

    policyEvaluationUtil.validateEvaluationTypeAndFeature(IntegrationType.CLI, stage);

    verify(mockStageTypeService).getLicensedStageTypes();
  }

  @Test(expected = InvalidLicenseException.class)
  public void testValidateEvaluationTypeAndFeature_CLI_ProxyWithoutContainerImagesEval_WithCiIntegration() {
    lenient().when(mockProductLicense.hasFeature(LicensedFeature.CI_INTEGRATION)).thenReturn(true);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);

    Stage stage = new Stage(Stage.ID_PROXY);

    policyEvaluationUtil.validateEvaluationTypeAndFeature(IntegrationType.CLI, stage);

    verify(mockStageTypeService).getLicensedStageTypes();
  }

  @Test
  public void testValidateEvaluationTypeAndFeature_CI() {
    Stage stage = new Stage(Stage.ID_BUILD);
    when(mockStageTypeService.getLicensedStageTypes()).thenReturn(Collections.singleton(StageTypes.BUILD));

    policyEvaluationUtil.validateEvaluationTypeAndFeature(IntegrationType.CI, stage);

    verify(mockStageTypeService).getLicensedStageTypes();
  }

  @Test
  public void testValidateEvaluationTypeAndFeature_RM() {
    Stage stage = new Stage(Stage.ID_BUILD);
    when(mockStageTypeService.getLicensedStageTypes()).thenReturn(Collections.singleton(StageTypes.BUILD));

    policyEvaluationUtil.validateEvaluationTypeAndFeature(IntegrationType.RM, stage);

    verify(mockStageTypeService).getLicensedStageTypes();
  }

  @Test(expected = InvalidStageException.class)
  public void testValidateEvaluationTypeAndFeature_InvalidStage() {
    Stage stage = new Stage("invalidStage");

    policyEvaluationUtil.validateEvaluationTypeAndFeature(IntegrationType.CLI, stage);
  }

  @Test(expected = InvalidLicenseException.class)
  public void testValidateEvaluationTypeAndFeature_StageNotLicensed() {
    Stage stage = new Stage(Stage.ID_BUILD);
    when(mockStageTypeService.getLicensedStageTypes()).thenReturn(Collections.emptySet());

    policyEvaluationUtil.validateEvaluationTypeAndFeature(IntegrationType.CLI, stage);
  }

  @Test(expected = InvalidLicenseException.class)
  public void testValidateEvaluationTypeAndFeature_FeatureNotLicensed() {
    Stage stage = new Stage(Stage.ID_BUILD);
    doThrow(new InvalidLicenseException("Feature not licensed")).when(mockProductLicense)
        .validateFeature(
            LicensedFeature.CLI_INTEGRATION);

    policyEvaluationUtil.validateEvaluationTypeAndFeature(IntegrationType.CLI, stage);
  }

  @Test
  public void testCreatePersistedPolicyEvaluationPollingResultWithSubStatusIfNeeded_FeatureFlagEnabled() {
    final String appId = "testAppId";

    final PersistedPolicyEvaluationPollingResult result =
        policyEvaluationUtil.createPersistedPolicyEvaluationPollingResultWithSubStatusIfNeeded(appId, "testScanId",
            true);
    assertThat(result.getStatusId()).isNotNull();
    assertThat(result.getApplicationId()).isEqualTo(appId);
    assertThat(result.getPolicyEvaluationPollingResult().getStatus())
        .isEqualTo(PolicyEvaluationStatus.PENDING);
    assertThat(result.getPolicyEvaluationPollingResult()).isNotNull();
    assertThat(result.getPolicyEvaluationPollingResult().getSubStatus())
        .isEqualTo(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_PENDING);
  }

  @Test
  public void testCreatePersistedPolicyEvaluationPollingResultIfNeeded() {
    final String appId = "testAppId";

    final PersistedPolicyEvaluationPollingResult result =
        policyEvaluationUtil.createPersistedPolicyEvaluationPollingResultIfNeeded(appId, "testScanId", true);
    assertThat(result.getStatusId()).isNotNull();
    assertThat(result.getApplicationId()).isEqualTo(appId);
    assertThat(result.getPolicyEvaluationPollingResult().getStatus())
        .isEqualTo(PolicyEvaluationStatus.PENDING);
    assertThat(result.getPolicyEvaluationPollingResult()).isNotNull();
    assertThat(result.getPolicyEvaluationPollingResult().getSubStatus())
        .isNull();
  }

  @Test
  public void testCreatePersistedPolicyEvaluationPollingResultIfNeeded_WithPreexistingPollingResultRecord() {
    final Application app = tempEntity.newApplicationWithParent();
    final String appId = app.getId();
    final PersistedPolicyEvaluationPollingResult pollingResult =
        createPersistedPolicyEvaluationPollingResult(appId, PolicyEvaluationStatus.COMPLETED,
            PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_COMPLETE);
    doReturn(pollingResult).when(mockPersistedPolicyEvaluationPollingResultDAO)
        .getByApplicationIdAndStatusId(anyString(), anyString());

    final PersistedPolicyEvaluationPollingResult result =
        policyEvaluationUtil.createPersistedPolicyEvaluationPollingResultIfNeeded(appId, pollingResult.getStatusId(),
            true);
    assertThat(result.getStatusId()).isEqualTo(pollingResult.getStatusId());
    assertThat(result.getApplicationId()).isEqualTo(pollingResult.getApplicationId());
    assertThat(result.getPolicyEvaluationPollingResult().getStatus())
        .isEqualTo(pollingResult.getPolicyEvaluationPollingResult().getStatus());
    assertThat(result.getPolicyEvaluationPollingResult().getSubStatus())
        .isEqualTo(pollingResult.getPolicyEvaluationPollingResult().getSubStatus());
  }

  private PersistedPolicyEvaluationPollingResult createPersistedPolicyEvaluationPollingResult(
      String applicationId,
      PolicyEvaluationStatus status,
      PolicyEvaluationSubStatus subStatus)
  {
    final PolicyEvaluationPollingResult policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
    policyEvaluationPollingResult.setStatus(status);
    policyEvaluationPollingResult.setSubStatus(subStatus);
    return new PersistedPolicyEvaluationPollingResult(applicationId, UUID.randomUUID().toString(),
        policyEvaluationPollingResult);
  }
}
