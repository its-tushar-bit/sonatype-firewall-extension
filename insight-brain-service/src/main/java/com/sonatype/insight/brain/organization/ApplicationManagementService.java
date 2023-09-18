/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.organization;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventFinder;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.REPORTS_LIST_DISABLED;

public class ApplicationManagementService
{
  private final ApplicationService applicationService;

  private final UserDirectory userDirectory;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private final SourceControlEventFinder sourceControlEventFinder;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  @Inject
  public ApplicationManagementService(
      final ApplicationService applicationService,
      final UserDirectory userDirectory,
      final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      final SourceControlEventFinder sourceControlEventFinder,
      final ScanPolicyEvaluator scanPolicyEvaluator
  )
  {
    this.applicationService = applicationService;
    this.userDirectory = userDirectory;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
    this.sourceControlEventFinder = sourceControlEventFinder;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
  }

  public ApplicationManagementSummaryDTO getApplicationManagementSummary(String applicationPublicId) {
    final Application application = applicationService.getApplicationByPublicIdNotNull(applicationPublicId);
    return getApplicationManagementSummary(application);
  }

  public List<ApplicationManagementSummaryDTO> getApplicationManagementSummaries(
      String nameFilter,
      ApplicationManagementSummaryOrder order,
      Integer page,
      Integer pageSize)
  {
    validateReportsListFeatureEnabled();

    if (page == null || pageSize == null) {
      throw new BadRequestException("Request must include required query parameters page and pageSize.");
    }

    if (nameFilter != null && nameFilter.isEmpty()) {
      nameFilter = null;
    }

    List<Application> applications = applicationService.getApplications();
    List<ApplicationManagementSummaryDTO> applicationManagementSummaryDTOs =
        ApplicationAdapter.getInstance(userDirectory).createApplicationManagementSummaries(applications, nameFilter);

    Comparator<ApplicationManagementSummaryDTO> comparator = getComparator(order);
    applicationManagementSummaryDTOs.sort(comparator);

    applicationManagementSummaryDTOs = applicationManagementSummaryDTOs.subList((page - 1) * pageSize,
        Math.min(page * pageSize, applicationManagementSummaryDTOs.size()));

    loadPolicyEvaluations(applicationManagementSummaryDTOs);
    loadPolicyEvaluationsResults(applicationManagementSummaryDTOs);
    loadPendingSourceControlPolicyEvaluations(applicationManagementSummaryDTOs);

    return applicationManagementSummaryDTOs;
  }

  private Comparator<ApplicationManagementSummaryDTO> getComparator(ApplicationManagementSummaryOrder order) {
    Comparator<ApplicationManagementSummaryDTO> comparator;
    switch (order) {
      case APP_NAME_ASC:
        comparator = Comparator.comparing(ApplicationManagementSummaryDTO::getName, String.CASE_INSENSITIVE_ORDER);
        break;
      case APP_NAME_DESC:
        comparator =
            Comparator.comparing(ApplicationManagementSummaryDTO::getName, String.CASE_INSENSITIVE_ORDER).reversed();
        break;
      case ORG_NAME_ASC:
        comparator =
            Comparator.comparing(ApplicationManagementSummaryDTO::getOrganizationName, String.CASE_INSENSITIVE_ORDER);
        break;
      case ORG_NAME_DESC:
        comparator =
            Comparator.comparing(ApplicationManagementSummaryDTO::getOrganizationName, String.CASE_INSENSITIVE_ORDER)
                .reversed();
        break;
      default:
        throw new IllegalArgumentException("Unknown ordering: " + order);
    }
    return comparator;
  }

  private ApplicationManagementSummaryDTO getApplicationManagementSummary(final Application application) {
    final ApplicationManagementSummaryDTO applicationManagement =
        ApplicationAdapter.getInstance(userDirectory).createApplicationManagementSummary(application);
    loadPolicyEvaluations(Arrays.asList(applicationManagement));

    return applicationManagement;
  }

  private void loadPendingSourceControlPolicyEvaluations(
      List<ApplicationManagementSummaryDTO> applicationManagementSummaries)
  {
    Map<String, SourceControlEvent> applicationEventMap =
        sourceControlEventFinder.getPendingOrInProgressSourceControlEvaluationEvents();
    for (ApplicationManagementSummaryDTO summaryDTO : applicationManagementSummaries) {
      summaryDTO.setHasPendingSourceControlPolicyEvaluation(applicationEventMap.containsKey(summaryDTO.getId()));
    }
  }

  private void loadPolicyEvaluationsResults(List<ApplicationManagementSummaryDTO> applicationManagementSummaries) {
    for (ApplicationManagementSummaryDTO applicationManagement : applicationManagementSummaries) {
      Map<String, PolicyEvaluationResult> policyEvaluationResults = new HashMap<>();
      for (PolicyEvaluation policyEvaluation : applicationManagement.getPolicyEvaluations().values()) {
        // Alerts are not needed by the Application Management UI and greatly bloat the JSON response
        // they are also time-consuming when we deal with thousands of applications/evaluations
        final PolicyEvaluationResult policyEvaluationResult = scanPolicyEvaluator
            .createPolicyEvaluationResult(policyEvaluation, false);

        policyEvaluationResults.put(policyEvaluation.getStageTypeId(), policyEvaluationResult);
      }
      applicationManagement.setPolicyEvaluationsResults(policyEvaluationResults);
    }
  }

  private void validateReportsListFeatureEnabled() {
    if (systemConfigurationPropertyDAO.getByName(REPORTS_LIST_DISABLED) != null) {
      throw new ConflictException("The reports list feature has been disabled.");
    }
  }

  private void loadPolicyEvaluations(List<ApplicationManagementSummaryDTO> applicationManagementSummaries) {
    Map<String, ApplicationManagementSummaryDTO> summariesByAppId = new HashMap<>();
    for (ApplicationManagementSummaryDTO summary : applicationManagementSummaries) {
      summariesByAppId.put(summary.getId(), summary);
      summary.setPolicyEvaluations(new HashMap<String, PolicyEvaluation>());
    }
    Set<String> stageTypeIds = new HashSet<>();
    for (StageType stageType : StageTypes.getAll()) {
      stageTypeIds.add(stageType.getId());
    }
    List<PolicyEvaluation> policyEvaluations = new PolicyEvaluationDAO().getLastByApplicationIds(summariesByAppId
        .keySet());
    for (PolicyEvaluation policyEvaluation : policyEvaluations) {
      if (stageTypeIds.contains(policyEvaluation.getStageTypeId())) {
        ApplicationManagementSummaryDTO summary = summariesByAppId.get(policyEvaluation.getApplicationId());
        summary.getPolicyEvaluations().put(policyEvaluation.getStageTypeId(), policyEvaluation);
      }
    }
  }
}
