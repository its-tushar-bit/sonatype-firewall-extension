/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.UUID;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationStatusDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiManifestEvaluationRequestDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.DefaultPolicyEvaluateService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service creates and publishes a <i>SourceControlEvent</i> of type MANIFEST_SCAN_EVENT
 *
 * @since 1.101
 */
public class ApiManifestEvaluationService
    extends AbstractApiApplicationEvaluationService
{
  private static final Logger log = LoggerFactory.getLogger(ApiManifestEvaluationService.class);

  private final SourceControlEventPublisher sourceControlEventPublisher;

  private final SourceControlUtils sourceControlUtils;

  @Inject
  public ApiManifestEvaluationService(
      final SourceControlEventPublisher sourceControlEventPublisher,
      final SourceControlUtils sourceControlUtils,
      DefaultPolicyEvaluateService policyEvaluateService,
      ApplicationDAO applicationDAO)
  {
    super(applicationDAO, policyEvaluateService);
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.sourceControlUtils = sourceControlUtils;
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ApiApplicationEvaluationStatusDTOV2 doManifestEvaluation(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      ApiManifestEvaluationRequestDTO manifestEvaluationRequest,
      final String userAgent)
  {
    validateRequest(manifestEvaluationRequest);

    final GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);
    if (gitRepositoryInfo == null) {
      throw new BadRequestException("No SCM configuration defined for application ID " + applicationId);
    }

    Application application = applicationDAO.getByIdNotNull(applicationId);
    String statusId = UUID.randomUUID().toString().replace("-", "");
    log.debug(
        "Received request to do manifest evaluation for application {}, stage {} and branch {}."
            + " The status ID of the operation is {}.",
        application.getName(), manifestEvaluationRequest.stageId, manifestEvaluationRequest.branchName, statusId);

    policyEvaluateService.createPersistedPolicyEvaluationPollingResultIfNeeded(application, statusId);

    String branchName;
    if (manifestEvaluationRequest.branchName != null) {
      branchName = manifestEvaluationRequest.branchName;
    }
    else {
      branchName = gitRepositoryInfo.getBaseBranch();
      log.debug("The branch name was not specified. Will use the base branch {}.", branchName);
    }

    SourceControlEvent sourceControlEvent = new SourceControlEvent()
        .setApplicationId(applicationId)
        .setEventType(SourceControlEvent.MANIFEST_EVALUATION_EVENT)
        .setStageTypeId(manifestEvaluationRequest.stageId)
        .setStatusId(statusId)
        .setBranchName(branchName)
        .setUserAgent(userAgent);

    sourceControlEventPublisher.publishEvent(sourceControlEvent);

    ApiApplicationEvaluationStatusDTOV2 result = new ApiApplicationEvaluationStatusDTOV2();
    result.statusUrl = getStatusUrl(applicationId, statusId);
    return result;
  }

  private void validateRequest(ApiManifestEvaluationRequestDTO manifestEvaluationRequest) {
    if (manifestEvaluationRequest == null) {
      throw new BadRequestException("Missing parameters.");
    }

    if (!Stage.isValidStageTypeId(manifestEvaluationRequest.stageId)) {
      throw new BadRequestException("Stage " + manifestEvaluationRequest.stageId + " is invalid.");
    }
  }
}
