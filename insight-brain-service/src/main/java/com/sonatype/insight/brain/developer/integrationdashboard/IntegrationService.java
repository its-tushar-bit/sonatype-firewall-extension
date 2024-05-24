/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.dashboard.StageRiskScoreDTO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.developer.integrationdashboard.api.IntegrationStatusDTO;
import com.sonatype.insight.brain.dashboard.ApplicationRiskScoreDTO;
import com.sonatype.insight.brain.dashboard.ApplicationRiskService;
import com.sonatype.insight.brain.developer.integrationdashboard.api.IntegrationStatusFilter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastScanDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.sast.SastScan;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
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

  private final SastScanDAO sastScanDAO;

  private final SourceControlDAO sourceControlDAO;

  private final OwnerDAO ownerDAO;

  @Inject
  public IntegrationService(
      final ApplicationRiskService applicationRiskService,
      final ApplicationSourceControlService applicationSourceControlService,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final SourceControlDefaultBranchCommitHistoryDAO sourceControlDefaultBranchCommitHistoryDAO,
      final SastScanDAO sastScanDAO,
      final SourceControlDAO sourceControlDAO,
      final OwnerDAO ownerDAO)
  {
    this.applicationRiskService = applicationRiskService;
    this.applicationSourceControlService = applicationSourceControlService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.sourceControlDefaultBranchCommitHistoryDAO = sourceControlDefaultBranchCommitHistoryDAO;
    this.sastScanDAO = sastScanDAO;
    this.sourceControlDAO = sourceControlDAO;
    this.ownerDAO = ownerDAO;
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

    final boolean paginateEarly = optionalFilterAppsByScmIntegration == null;
    final int skipCount = (filter.getPage() - 1) * filter.getPageSize();
    final List<ApplicationRiskScoreDTO> apps = applicationRiskService.getRiskForApplicationsWithReadPermissions();
    final List<ApplicationRiskScoreDTO> filteredApps =
        StringUtils.isNotEmpty(filter.getOptionalFilterApplicationNamesBy()) ? apps.stream()
            .filter(
                riskScoreDTO -> matchesFilter(riskScoreDTO.applicationName,
                    filter.getOptionalFilterApplicationNamesBy()))
            .collect(Collectors.toList()) : apps;

    final List<IntegrationStatusDTO> minimalSummaries = filteredApps.stream()
        // Set application and total risk (build stage)
        .map(riskScoreDTO -> new IntegrationStatusDTO().setApplicationName(riskScoreDTO.applicationName)
            .setApplicationId(riskScoreDTO.id).setApplicationPublicId(riskScoreDTO.applicationId)
            .setTotalRiskScore(getBuildStageTotalRisk(riskScoreDTO))
            .setOrganizationId(riskScoreDTO.organizationId))
        // Set last commit time
        .map(statusDTO -> {
          final SourceControlDefaultBranchCommitHistory commitHistory =
              sourceControlDefaultBranchCommitHistoryDAO.getLatestCommitForApplicationId(statusDTO.getApplicationId());
          return statusDTO.setLastCommitTimestamp(
              commitHistory != null ? commitHistory.getCommitTime().getTime() : 0L);
        })
        // Set last policy evaluation time (build stage)
        .map(statusDTO -> {
          final PolicyEvaluation latestBuildStageEvaluation =
              policyEvaluationDAO.getLastByApplicationIdAndStageIdNoMonitoringNoReeval(statusDTO.getApplicationId(),
                  Stage.ID_BUILD);
          final long latestBuildStageEvaluationTimestamp =
              Objects.nonNull(latestBuildStageEvaluation) ? latestBuildStageEvaluation.getTime().getTime() : 0L;
          // Update the risk score if there is no latest build stage evaluation so that we can differentiate
          // between apps with 0 risk and apps with no risk data
          if (Objects.isNull(latestBuildStageEvaluation)) {
            statusDTO.setTotalRiskScore(-1);
          }
          return statusDTO.setLastEvaluationTimestamp(latestBuildStageEvaluationTimestamp);
        })
        // Set CI/CD Integration status
        .map(statusDTO -> statusDTO.setCiIntegrationEnabled(
            policyEvaluationDAO.hasCIIntegrationEvaluation(statusDTO.getApplicationId())))
        // Optionally filter on CI/CD Integration status
        .filter(statusDTO ->
            optionalFilterAppsByCiCdIntegration == null ||
                statusDTO.isCiIntegrationEnabled() == optionalFilterAppsByCiCdIntegration)
        .collect(Collectors.toList());

    final List<IntegrationStatusDTO> possiblyPaginatedSummaries = paginateEarly ?
        minimalSummaries.stream()
        .sorted(new IntegrationStatusDTOComparator(filter.getOptionalOrderBy()))
        .skip(skipCount)
        .limit(filter.getPageSize())
        .collect(Collectors.toList()) : minimalSummaries;

    final List<IntegrationStatusDTO> enrichedSummaries = possiblyPaginatedSummaries.stream()
        // Set Automated Source Control Feedback status
        .map(statusDTO -> statusDTO.setAutomatedSourceControlFeedbackEnabled(
            !applicationSourceControlService.isAutomatedSourceControlFeedbackDisabledForApp(
                statusDTO.getApplicationId())))
        // Optionally filter on SCM Integration status
        .filter(statusDTO ->
            optionalFilterAppsByScmIntegration == null ||
                statusDTO.isAutomatedSourceControlFeedbackEnabled() == optionalFilterAppsByScmIntegration)
        // Enrich after filtering to save some round trips to the sast_scan table
        .map(this::addSastScanData)
        .collect(Collectors.toList());

    final List<IntegrationStatusDTO> completeSummaries = paginateEarly ? enrichedSummaries :
        enrichedSummaries.stream()
        .sorted(new IntegrationStatusDTOComparator(filter.getOptionalOrderBy()))
        .skip(skipCount)
        .limit(filter.getPageSize())
        .collect(Collectors.toList());

    final int totalSize = paginateEarly ? minimalSummaries.size() : enrichedSummaries.size();
    return new ApiPageResult<>(totalSize, filter.getPage(), filter.getPageSize(), completeSummaries);
  }

  private IntegrationStatusDTO addSastScanData(final IntegrationStatusDTO integrationStatusDTO) {
    Optional<SastScan> lastSastScan = getLatestSastScan(integrationStatusDTO.getApplicationId());
    return new IntegrationStatusDTO(
        integrationStatusDTO.getApplicationName(),
        integrationStatusDTO.getApplicationId(),
        integrationStatusDTO.getApplicationPublicId(),
        integrationStatusDTO.isCiIntegrationEnabled(),
        integrationStatusDTO.isAutomatedSourceControlFeedbackEnabled(),
        integrationStatusDTO.getLastCommitTimestamp(),
        integrationStatusDTO.getLastEvaluationTimestamp(),
        integrationStatusDTO.getOrganizationId(),
        integrationStatusDTO.getTotalRiskScore(),
        lastSastScan.isPresent(),
        lastSastScan.map(SastScan::getId).orElse(null),
        getCreatedAt(lastSastScan)
    );
  }

  private Long getCreatedAt(Optional<SastScan> sastScan) {
    // Handling the case that SastScan contains a null value of `createdAt` field
    return sastScan.flatMap(scan -> Optional.ofNullable(scan.getCreatedAt()))
        .map(Date::getTime)
        .orElse(null);
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

  private int getBuildStageTotalRisk(final ApplicationRiskScoreDTO riskScoreDTO) {
    final StageRiskScoreDTO stageRiskScoreDTO = riskScoreDTO.getStageRiskScore(BuildStageType.ID);
    return Objects.nonNull(stageRiskScoreDTO) ? stageRiskScoreDTO.risk.totalRisk : 0;
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

  private Optional<SastScan> getLatestSastScan(final String applicationId) {
    final Optional<SastScan> latestSastScanByBaseBranch =
        sastScanDAO.getByApplicationIdAndBranchName(applicationId, getBaseBranch(applicationId))
          .stream()
          .max(Comparator.comparing(SastScan::getCreatedAt));

    if (latestSastScanByBaseBranch.isPresent()) {
      return latestSastScanByBaseBranch;
    }

    return sastScanDAO.getByApplicationId(applicationId)
        .stream()
        .max(Comparator.comparing(SastScan::getCreatedAt));
  }

  private String getBaseBranch(final String ownerId) {
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      if (OwnerType.APPLICATION.equals(owner.getType()) || OwnerType.ORGANIZATION.equals(owner.getType())) {
        final SourceControl sourceControl = sourceControlDAO.getByOwnerId(owner.getId());
        final String baseBranch = sourceControl != null ? sourceControl.getBaseBranch() : null;
        if (StringUtils.isNotEmpty(baseBranch)) {
          return baseBranch;
        }
      }
    }
    return null;
  }
}
