/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationResultDTOV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationPollingResultDTO;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;

class AbstractApiApplicationEvaluationService
{
  protected final ApplicationDAO applicationDAO;

  protected final PolicyEvaluateService policyEvaluateService;

  AbstractApiApplicationEvaluationService(
      ApplicationDAO applicationDAO,
      PolicyEvaluateService policyEvaluateService)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluateService = policyEvaluateService;
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ApiApplicationEvaluationResultDTOV2 getApplicationEvaluationStatus(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      String statusId)
  {
    Application application = applicationDAO.getByIdNotNull(applicationId);
    PolicyEvaluationPollingResultDTO dto =
        policyEvaluateService.pollEvaluationResult(application, statusId);

    ApiApplicationEvaluationResultDTOV2 result = new ApiApplicationEvaluationResultDTOV2();
    result.status = dto.status.name();
    switch (dto.status) {
      case COMPLETED:
        String applicationPublicId = application.getPublicId();
        String scanId = dto.scanReceipt.getScanId();
        result.reportPdfUrl = UserInterfaceLinksHelper.getPdfUrl(applicationPublicId, scanId);
        result.reportHtmlUrl = UserInterfaceLinksHelper.getReportUrl(applicationPublicId, scanId);
        result.embeddableReportHtmlUrl = UserInterfaceLinksHelper.getEmbeddableReportUrl(applicationPublicId, scanId);
        result.reportDataUrl = ApiReportDataResourceV2.getDataUrl(applicationPublicId, scanId);
        break;
      case FAILED:
        result.reason = dto.reason;
        break;
      default:
        break;
    }

    return result;
  }

  protected static String getStatusUrl(String applicationId, String statusId) {
    return UriBuilder.fromPath(PublicApiPaths.POLICY_EVALUATION_STATUS_PATH_V2)
        .build(applicationId, statusId)
        .toString();
  }
}
