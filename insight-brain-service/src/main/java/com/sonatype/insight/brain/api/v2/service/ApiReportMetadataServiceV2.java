/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static com.sonatype.nexus.git.utils.repository.RepositoryUrlFinderUtils.maskCredentialsFromUrl;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;

import com.sonatype.clm.dto.model.ci.config.ApiReportMetadataDto;
import com.sonatype.clm.dto.model.ci.config.ApiReportMetadataResponseDto;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
public class ApiReportMetadataServiceV2
{
  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApplicationDAO applicationDAO;

  @Inject
  public ApiReportMetadataServiceV2(
      PolicyEvaluationDAO policyEvaluationDAO,
      ApplicationDAO applicationDAO)
  {
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.applicationDAO = applicationDAO;
  }

  @Authorize(permission = Permission.READ)
  public ApiReportMetadataResponseDto getMetadata(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      String scanId)
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    PolicyEvaluation evaluation = policyEvaluationDAO.getByScanIdAndApplicationId(scanId, application.getId());
    if (evaluation == null) {
      throw new NotFoundException("Policy evaluation not found for scan: " + scanId);
    }

    return buildResponse(evaluation, application);
  }

  private ApiReportMetadataResponseDto buildResponse(PolicyEvaluation evaluation, Application application) {
    ApiReportMetadataResponseDto response = new ApiReportMetadataResponseDto();

    // Build data object
    ApiReportMetadataDto data = new ApiReportMetadataDto();
    data.setScanId(evaluation.getScanId());
    data.setApplicationId(evaluation.getOwnerId());
    data.setApplicationPublicId(application.getPublicId());
    data.setStage(evaluation.getStageTypeId());
    data.setScanDate(evaluation.getTime());
    data.setCommitHash(evaluation.getCommitHash());
    data.setBranchName(evaluation.getBranchName());
    data.setScmRepositoryUrl(maskUrlCredentials(evaluation.getScmRepositoryUrl()));
    response.setData(data);

    // Build source map
    Map<String, String> source = new HashMap<>();
    if (evaluation.getCommitHashSource() != null) {
      source.put("commitHash", evaluation.getCommitHashSource().name());
    }
    if (evaluation.getBranchNameSource() != null) {
      source.put("branchName", evaluation.getBranchNameSource().name());
    }
    if (evaluation.getScmRepositoryUrlSource() != null) {
      source.put("scmRepositoryUrl", evaluation.getScmRepositoryUrlSource().name());
    }
    response.setSource(source);

    return response;
  }

  /**
   * Mask credentials in URL. Returns null for blank URLs because the DTO field is optional
   * and null avoids serializing an empty string in the JSON response.
   */
  private String maskUrlCredentials(String url) {
    if (StringUtils.isBlank(url)) {
      return null;
    }
    return maskCredentialsFromUrl(url);
  }
}
