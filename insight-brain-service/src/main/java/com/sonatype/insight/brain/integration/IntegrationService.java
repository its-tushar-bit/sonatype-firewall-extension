/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.integration;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.IntegrationStatusDTO;
import com.sonatype.insight.brain.dashboard.ApplicationRiskScoreDTO;
import com.sonatype.insight.brain.dashboard.ApplicationRiskService;
import com.sonatype.insight.brain.dataaccess.IntegrationStatusFilter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.insight.brain.organization.ApplicationSourceControlService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

@Named
public class IntegrationService
{
  private final ApplicationRiskService applicationRiskService;

  private final ApplicationSourceControlService applicationSourceControlService;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final SourceControlDefaultBranchCommitHistoryDAO sourceControlDefaultBranchCommitHistoryDAO;

  @Inject
  public IntegrationService(
      final ApplicationRiskService applicationRiskService,
      final ApplicationSourceControlService applicationSourceControlService,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final SourceControlDefaultBranchCommitHistoryDAO sourceControlDefaultBranchCommitHistoryDAO)
  {
    this.applicationRiskService = applicationRiskService;
    this.applicationSourceControlService = applicationSourceControlService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.sourceControlDefaultBranchCommitHistoryDAO = sourceControlDefaultBranchCommitHistoryDAO;
  }

  public ApiPageResult<IntegrationStatusDTO> getIntegrationStatuses(
      final int page,
      final int pageSize,
      final String optionalOrderBy,
      final String optionalFilterApplicationNamesBy,
      final Boolean optionalFilterAppsByScmIntegration,
      final Boolean optionalFilterAppsByCiCdIntegration

  )
  {
    final IntegrationStatusFilter filter =
        getIntegrationStatusFilter(page, pageSize, optionalOrderBy, optionalFilterApplicationNamesBy);

    if (filter.getPage() <= 0 || filter.getPageSize() <= 0) {
      throw new BadRequestException("Page and Page size must be greater than 0");
    }

    checkReadPermission(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);

    final int skipCount = (filter.getPage() - 1) * filter.getPageSize();
    final List<ApplicationRiskScoreDTO> apps = applicationRiskService.getRiskForAllApps();
    final List<ApplicationRiskScoreDTO> filteredApps =
        StringUtils.isNotEmpty(filter.getOptionalFilterApplicationNamesBy()) ? apps.stream()
            .filter(
                riskScoreDTO -> matchesFilter(riskScoreDTO.applicationName,
                    filter.getOptionalFilterApplicationNamesBy()))
            .collect(Collectors.toList()) : apps;
    final int totalSize = filteredApps.size();

    final List<IntegrationStatusDTO> summaries = filteredApps.stream()
        // Set application and total risk
        .map(riskScoreDTO -> new IntegrationStatusDTO().setApplicationName(riskScoreDTO.applicationName)
            .setApplicationId(riskScoreDTO.id).setApplicationPublicId(riskScoreDTO.applicationId)
            .setTotalRiskScore(riskScoreDTO.totalApplicationRisk.totalRisk)
            .setOrganizationId(riskScoreDTO.organizationId))
        // Set CI/CD Integration status
        .map(statusDTO -> statusDTO.setCiIntegrationEnabled(
            policyEvaluationDAO.hasCIIntegrationEvaluation(statusDTO.getApplicationId())))
        // Set Automated Source Control Feedback status
        .map(statusDTO -> statusDTO.setAutomatedSourceControlFeedbackEnabled(
            !applicationSourceControlService.isAutomatedSourceControlFeedbackDisabledForApp(
                statusDTO.getApplicationId())))
        // Set last commit time
        .map(statusDTO -> {
          final SourceControlDefaultBranchCommitHistory commitHistory =
              sourceControlDefaultBranchCommitHistoryDAO.getLatestCommitForApplicationId(statusDTO.getApplicationId());
          return statusDTO.setLastCommitTimestamp(
              commitHistory != null ? commitHistory.getCommitTime().getTime() : 0L);
        })
        // Set last policy evaluation time
        .map(statusDTO -> {
          final List<PolicyEvaluation> policyEvaluations = policyEvaluationDAO.getLastByApplicationIds(
              Collections.singleton(statusDTO.getApplicationId()));
          return statusDTO.setLastEvaluationTimestamp(getLatestEvaluation(policyEvaluations));
        })
        .filter(statusDTO ->
                optionalFilterAppsByScmIntegration == null ||
                statusDTO.isAutomatedSourceControlFeedbackEnabled() == optionalFilterAppsByScmIntegration)
        .filter(statusDTO ->
                optionalFilterAppsByCiCdIntegration == null ||
                statusDTO.isCiIntegrationEnabled() == optionalFilterAppsByCiCdIntegration)
        .sorted(new IntegrationStatusDTOComparator(filter.getOptionalOrderBy()))
        .skip(skipCount)
        .limit(filter.getPageSize())
        .collect(Collectors.toList());

    return new ApiPageResult<>(totalSize, filter.getPage(), filter.getPageSize(), summaries);
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.ID) String ownerId)
  {
    // The @Authorize annotation provides the implementation for this function
  }

  private static boolean matchesFilter(final String applicationName, final String filter) {
    return applicationName.toLowerCase(Locale.ROOT).matches(String.format(".*%s.*", filter.toLowerCase(Locale.ROOT)));
  }

  private long getLatestEvaluation(final List<PolicyEvaluation> evaluations) {
    if (!evaluations.isEmpty()) {
      return evaluations.stream()
          .max(Comparator.comparing(eval -> eval.getTime().getTime()))
          .map(policyEvaluation -> policyEvaluation.getTime().getTime())
          .orElse(0L);
    }
    return 0L;
  }

  private static IntegrationStatusFilter getIntegrationStatusFilter(
      final int page,
      final int pageSize,
      final String optionalOrderBy,
      final String optionalFilterApplicationNamesBy)
  {
    final IntegrationStatusFilter filter = new IntegrationStatusFilter(page, pageSize);
    if (StringUtils.isNotEmpty(optionalOrderBy)) {
      filter.setOptionalOrderBy(optionalOrderBy);
    }
    if (StringUtils.isNotEmpty(optionalFilterApplicationNamesBy)) {
      filter.setOptionalFilterApplicationNamesBy(optionalFilterApplicationNamesBy);
    }

    return filter;
  }
}
