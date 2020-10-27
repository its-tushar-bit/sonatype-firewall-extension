/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.legal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * Provides legal information for application components.
 *
 * @since 1.101
 */
@Named
public class LicenseLegalService
{
  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApiReportDataServiceV2 apiReportDataServiceV2;

  @Inject
  public LicenseLegalService(
      ApplicationDAO applicationDAO,
      PolicyEvaluationDAO policyEvaluationDAO,
      ApiReportDataServiceV2 apiReportDataServiceV2)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.apiReportDataServiceV2 = apiReportDataServiceV2;
  }

  @Authorize(permission = Permission.READ)
  public Optional<ApiReportRawDataDTOV2> getLatestRawReportForApplication(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId)
  {
    return Optional.ofNullable(applicationDAO.getByPublicId(applicationPublicId)).flatMap(
        application -> getLastRawReportsByAppPublicId(Collections.singletonList(application))
            .get(application.getPublicId()));
  }

  private Map<String, Optional<ApiReportRawDataDTOV2>> getLastRawReportsByAppPublicId(List<Application> applications) {
    List<PolicyEvaluation> lastPolicyEvaluationsForAllStages = policyEvaluationDAO
        .getLastByApplicationIds(applications.stream().map(Application::getId).collect(Collectors.toSet()));
    return applications.stream().collect(Collectors.toMap(Application::getPublicId, application ->
        lastPolicyEvaluationsForAllStages.stream()
            .filter(policyEvaluation -> policyEvaluation.getApplicationId().equals(application.getId()))
            .max(Comparator.comparing(PolicyEvaluation::getTime))
            .map(policyEvaluation -> getLastRawReportForApplication(application.getPublicId(), policyEvaluation))));
  }

  private ApiReportRawDataDTOV2 getLastRawReportForApplication(
      String applicationPublicId,
      PolicyEvaluation lastPolicyEvaluation)
  {
    try {
      return apiReportDataServiceV2.getDataNoAuth(applicationPublicId, lastPolicyEvaluation.getScanId());
    }
    catch (IOException e) {
      throw new UncheckedIOException(e.getMessage(), e);
    }
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  public List<Application> getApplications() {
    return applicationDAO.getAll();
  }

  @Authorize(permission = Permission.READ)
  public Set<ApplicationReportRawDataDTO> getReportsForOrg(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId)
  {
    List<Application> applications = applicationDAO.getByOrganizationId(organizationId);
    if (applications.isEmpty()) {
      throw new NotFoundException("Cannot find applications for organization with id " + organizationId + ".");
    }
    return getLastRawReportsByAppPublicId(applications).entrySet().stream()
        .filter(e -> e.getValue().isPresent())
        .map(e -> new ApplicationReportRawDataDTO(e.getKey(), e.getValue().get())).collect(Collectors.toSet());
  }
}
