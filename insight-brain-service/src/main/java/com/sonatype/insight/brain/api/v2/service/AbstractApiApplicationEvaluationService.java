/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationResultDTOV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.DefaultPolicyEvaluateService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;

class AbstractApiApplicationEvaluationService
{
  protected final ApplicationDAO applicationDAO;

  protected final DefaultPolicyEvaluateService policyEvaluateService;

  AbstractApiApplicationEvaluationService(
      ApplicationDAO applicationDAO,
      DefaultPolicyEvaluateService policyEvaluateService)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluateService = policyEvaluateService;
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ApiApplicationEvaluationResultDTOV2 getApplicationEvaluationStatus(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      String statusId)
  {
    Application application = applicationDAO.getById(applicationId);
    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        policyEvaluateService.pollEvaluationResult(application.getPublicId(), statusId);

    ApiApplicationEvaluationResultDTOV2 result = new ApiApplicationEvaluationResultDTOV2();
    result.status = policyEvaluationPollingResult.getStatus().name();
    switch (policyEvaluationPollingResult.getStatus()) {
      case COMPLETED:
        String applicationPublicId = application.getPublicId();
        String scanId = policyEvaluationPollingResult.getScanReceipt().getScanId();
        result.reportPdfUrl = UserInterfaceLinksResource.getPdfUrl(applicationPublicId, scanId);
        result.reportHtmlUrl = UserInterfaceLinksResource.getReportUrl(applicationPublicId, scanId);
        result.embeddableReportHtmlUrl = UserInterfaceLinksResource.getEmbeddableReportUrl(applicationPublicId, scanId);
        result.reportDataUrl = ApiReportDataResourceV2.getDataUrl(applicationPublicId, scanId);
        break;
      case FAILED:
        result.reason = policyEvaluationPollingResult.getReason();
        break;
      default:
        break;
    }

    return result;
  }

  protected static String getStatusUrl(String applicationId, String statusId) {
    return UriBuilder.fromPath(PublicApiPaths.POLICY_EVALUATION_STATUS_PATH_V2).build(applicationId, statusId)
        .toString();
  }
}
