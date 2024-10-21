/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Collections;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PolicyEvaluationUtilTest
    extends AbstractComponentTest
{
  @Mock
  private ProductLicense mockProductLicense;

  @Mock
  private StageTypeService mockStageTypeService;

  private PolicyEvaluationUtil policyEvaluationUtil;

  @Override
  public void configure(Binder binder) {
    lenient().when(mockStageTypeService.getLicensedStageTypes()).thenReturn(StageTypes.getAll());
    binder.bind(StageTypeService.class).toInstance(mockStageTypeService);
    binder.bind(ProductLicense.class).toInstance(mockProductLicense);
    super.configure(binder);
    policyEvaluationUtil = new PolicyEvaluationUtil(mockProductLicense, mockStageTypeService);
  }

  @Test
  public void testValidateEvaluationTypeAndFeature_CLI() {
    Stage stage = new Stage(Stage.ID_BUILD);

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
    doThrow(new InvalidLicenseException("Feature not licensed")).when(mockProductLicense).validateFeature(
        LicensedFeature.CLI_INTEGRATION);

    policyEvaluationUtil.validateEvaluationTypeAndFeature(IntegrationType.CLI, stage);
  }
}
