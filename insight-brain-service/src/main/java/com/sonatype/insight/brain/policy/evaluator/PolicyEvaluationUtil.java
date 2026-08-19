/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSubStatus;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.license.model.LicensedFeature;

@Named
@Singleton
public class PolicyEvaluationUtil
{
  private final ProductLicense productLicense;

  private final StageTypeService stageTypeService;

  private final PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  @Inject
  public PolicyEvaluationUtil(
      final ProductLicense productLicense,
      final StageTypeService stageTypeService,
      final PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO)
  {
    this.productLicense = productLicense;
    this.stageTypeService = stageTypeService;
    this.persistedPolicyEvaluationPollingResultDAO = persistedPolicyEvaluationPollingResultDAO;
  }

  public void validateEvaluationTypeAndFeature(IntegrationType integrationType, Stage stage) {
    final boolean hasContainerImagesEvaluationFeature =
        (SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.isEnabled() &&
            productLicense.hasFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION));

    if (integrationType.equals(IntegrationType.CLI)) {
      if (!hasContainerImagesEvaluationFeature) {
        productLicense.validateFeature(LicensedFeature.CLI_INTEGRATION);

        if (stage.getStageTypeId().equals(Stage.ID_PROXY)) {
          throw new InvalidLicenseException(
              "Application evaluation using the proxy stage is not supported by your license.");
        }
      }
    }
    else if (integrationType.equals(IntegrationType.CI)) {
      productLicense.validateFeature(LicensedFeature.CI_INTEGRATION);
    }
    else if (integrationType.equals(IntegrationType.RM)) {
      productLicense.validateFeature(LicensedFeature.RM_STAGING_INTEGRATION);
    }

    if (!(hasContainerImagesEvaluationFeature && stage.getStageTypeId().equals(Stage.ID_PROXY))) {
      if (!Stage.isValidStageTypeId(stage.getStageTypeId())) {
        throw new InvalidStageException(stage.getStageTypeId());
      }
    }

    if (!stageTypeService.getLicensedStageTypes().contains(StageTypes.getById(stage.getStageTypeId()))) {
      throw new InvalidLicenseException("Stage '" + stage.getStageTypeId() + "' is not supported by your license.");
    }
  }

  public PersistedPolicyEvaluationPollingResult createPersistedPolicyEvaluationPollingResultIfNeeded(
      final String appId,
      final String statusId)
  {
    return createPersistedPolicyEvaluationPollingResultIfNeeded(appId, statusId, false, false);
  }

  public PersistedPolicyEvaluationPollingResult createPersistedPolicyEvaluationPollingResultIfNeeded(
      final String appId,
      final String statusId,
      final boolean disablePollingIntervalForTesting)
  {
    return createPersistedPolicyEvaluationPollingResultIfNeeded(appId, statusId, disablePollingIntervalForTesting,
        false);
  }

  public PersistedPolicyEvaluationPollingResult createPersistedPolicyEvaluationPollingResultWithSubStatusIfNeeded(
      final String appId,
      final String statusId,
      final boolean disablePollingIntervalForTesting)
  {
    return createPersistedPolicyEvaluationPollingResultIfNeeded(appId, statusId, disablePollingIntervalForTesting,
        true);
  }

  private PersistedPolicyEvaluationPollingResult createPersistedPolicyEvaluationPollingResultIfNeeded(
      final String appId,
      final String statusId,
      final boolean disablePollingIntervalForTesting,
      final boolean setNewProcessSubStatus)
  {
    PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult =
        persistedPolicyEvaluationPollingResultDAO.getByApplicationIdAndStatusId(appId, statusId);
    if (persistedPolicyEvaluationPollingResult != null) {
      return persistedPolicyEvaluationPollingResult;
    }

    final PolicyEvaluationPollingResult initialResult = new PolicyEvaluationPollingResult();
    initialResult.setStatus(PolicyEvaluationStatus.PENDING);
    initialResult.setNextPollingIntervalInSeconds(
        EvaluationTask.getNextPollingInterval(disablePollingIntervalForTesting));
    if (setNewProcessSubStatus) {
      initialResult.setSubStatus(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_PENDING);
    }

    persistedPolicyEvaluationPollingResult =
        new PersistedPolicyEvaluationPollingResult(appId, statusId, initialResult);
    persistedPolicyEvaluationPollingResultDAO.insert(persistedPolicyEvaluationPollingResult);

    return persistedPolicyEvaluationPollingResult;
  }
}
