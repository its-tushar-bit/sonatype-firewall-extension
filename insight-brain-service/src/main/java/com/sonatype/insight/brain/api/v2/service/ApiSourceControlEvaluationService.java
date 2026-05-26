/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationStatusDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlEvaluationRequestDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.git.IqForScmLicenseChecker;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtil;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service creates and publishes a <i>SourceControlEvent</i> of type SOURCE_CONTROL_EVALUATION
 *
 * @since 1.101
 */
@Named
@Singleton
public class ApiSourceControlEvaluationService
    extends AbstractApiApplicationEvaluationService
{
  private static final Logger log = LoggerFactory.getLogger(ApiSourceControlEvaluationService.class);

  private final SourceControlEventPublisher sourceControlEventPublisher;

  private final SourceControlUtils sourceControlUtils;

  private final IqForScmLicenseChecker licenseChecker;

  private final StageTypeService stageTypeService;

  private final PolicyEvaluationUtil policyEvaluationUtil;

  @Inject
  public ApiSourceControlEvaluationService(
      final SourceControlEventPublisher sourceControlEventPublisher,
      final SourceControlUtils sourceControlUtils,
      final IqForScmLicenseChecker licenseChecker,
      PolicyEvaluateService policyEvaluateService,
      ApplicationDAO applicationDAO,
      StageTypeService stageTypeService,
      PolicyEvaluationUtil policyEvaluationUtil)
  {
    super(applicationDAO, policyEvaluateService);
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.sourceControlUtils = sourceControlUtils;
    this.licenseChecker = licenseChecker;
    this.stageTypeService = stageTypeService;
    this.policyEvaluationUtil = policyEvaluationUtil;
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ApiApplicationEvaluationStatusDTOV2 evaluateSourceControl(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      ApiSourceControlEvaluationRequestDTO sourceControlEvaluationRequest,
      final String userAgent)
  {
    checkLicense();

    validateRequest(sourceControlEvaluationRequest);

    final GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);
    if (gitRepositoryInfo == null) {
      throw new BadRequestException("No SCM configuration defined for application ID " + applicationId);
    }

    Application application = applicationDAO.getByIdNotNull(applicationId);
    String statusId = UUID.randomUUID().toString().replace("-", "");
    log.debug(
        "Received request to evaluate source control for application {}, stage {} and branch {}."
            + " The status ID of the operation is {}.",
        application.getName(), sourceControlEvaluationRequest.stageId, sourceControlEvaluationRequest.branchName,
        statusId);

    policyEvaluationUtil.createPersistedPolicyEvaluationPollingResultIfNeeded(applicationId, statusId);

    String branchName;
    if (sourceControlEvaluationRequest.branchName != null) {
      branchName = sourceControlEvaluationRequest.branchName;
    }
    else {
      branchName = gitRepositoryInfo.getBaseBranch();
      log.debug("The branch name was not specified. Will use the base branch {}.", branchName);
    }

    SourceControlEvent sourceControlEvent = new SourceControlEvent() //
        .forSourceControlEvaluation()
        .setApplicationId(applicationId) //
        .setStageTypeId(sourceControlEvaluationRequest.stageId) //
        .setStatusId(statusId) //
        .setBranchName(branchName) //
        .setScanTargets(sourceControlEvaluationRequest.scanTargets) //
        .setUserAgent(userAgent) //
        .setScanTriggerType(ScanTriggerType.SOURCE_CONTROL_API);

    sourceControlEventPublisher.publishEvent(sourceControlEvent);

    ApiApplicationEvaluationStatusDTOV2 result = new ApiApplicationEvaluationStatusDTOV2();
    result.statusUrl = getStatusUrl(applicationId, statusId);
    return result;
  }

  private void validateRequest(ApiSourceControlEvaluationRequestDTO sourceControlEvaluationRequest) {
    if (sourceControlEvaluationRequest == null) {
      throw new BadRequestException("Missing parameters.");
    }

    stageTypeService.getLicensedStageTypes(StageTypeService.LIFECYCLE_CONTEXT)
        .stream()
        .filter(stageType -> stageType.getId().equals(sourceControlEvaluationRequest.stageId))
        .findFirst()
        .orElseThrow(() -> new BadRequestException("Stage " + sourceControlEvaluationRequest.stageId +
            " is invalid."));

    validateScanTargets(sourceControlEvaluationRequest);
  }

  private void validateScanTargets(ApiSourceControlEvaluationRequestDTO sourceControlEvaluationRequest) {
    if (sourceControlEvaluationRequest.scanTargets != null) {
      for (String scanTarget : sourceControlEvaluationRequest.scanTargets) {
        if (scanTarget.contains("../") || scanTarget.contains("..\\")) {
          // legit callers use normalized paths, no directory traversal into restricted areas
          throw new BadRequestException("Scan targets cannot contain ../ or ..\\");
        }
      }
    }
  }

  private void checkLicense() {
    if (!licenseChecker.isIqForScmSupported()) {
      log.debug("License does not support source control notification or automation features");
      throw new InvalidLicenseException();
    }
  }
}
