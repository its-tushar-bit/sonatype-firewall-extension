/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.developer.integrationdashboard.api.IntegrationStatusDTO;
import com.sonatype.insight.brain.developer.integrationdashboard.api.IntegrationStatusFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.insight.brain.organization.ApplicationSourceControlService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.lang3.StringUtils;

@Named
public class IntegrationService
{
  private final ApplicationSourceControlService applicationSourceControlService;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final SourceControlDefaultBranchCommitHistoryDAO sourceControlDefaultBranchCommitHistoryDAO;

  private final TelemetrySender telemetrySender;

  @Inject
  public IntegrationService(
      final ApplicationSourceControlService applicationSourceControlService,
      final ApplicationDAO applicationDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final SourceControlDefaultBranchCommitHistoryDAO sourceControlDefaultBranchCommitHistoryDAO,
      final TelemetrySender telemetrySender)
  {
    this.applicationSourceControlService = applicationSourceControlService;
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.sourceControlDefaultBranchCommitHistoryDAO = sourceControlDefaultBranchCommitHistoryDAO;
    this.telemetrySender = telemetrySender;
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

    sendAppIntegrationFilterTelemetry(
        StringUtils.isNotEmpty(optionalFilterApplicationNamesBy),
        Boolean.TRUE.equals(optionalFilterAppsByScmIntegration),
        Boolean.TRUE.equals(optionalFilterAppsByCiCdIntegration),
        optionalOrderBy
    );

    final boolean paginateEarly = optionalFilterAppsByScmIntegration == null;
    final int skipCount = (filter.getPage() - 1) * filter.getPageSize();

    final List<Application> applications = getApplicationsWithReadPermission();

    final List<Application> filteredApps =
        StringUtils.isNotEmpty(filter.getOptionalFilterApplicationNamesBy()) ? applications.stream()
            .filter(
                application -> matchesFilter(application.getName(),
                    filter.getOptionalFilterApplicationNamesBy()))
            .toList() : applications;

    final List<IntegrationStatusDTO> minimalAppSummaries = filteredApps.stream()
        // Set application
        .map(application -> new IntegrationStatusDTO()
            .setApplicationName(application.getName())
            .setApplicationId(application.getId())
            .setApplicationPublicId(application.getPublicId())
            .setOrganizationId(application.getOrganizationId()))
        // Set last policy evaluation time (build stage), priorities report status and scan ID
        .map(statusDTO -> {
          final PolicyEvaluation latestBuildStageEvaluation =
              policyEvaluationDAO.getLastByApplicationIdAndStageIdNoMonitoringNoReeval(statusDTO.getApplicationId(),
                  Stage.ID_BUILD);
          if (Objects.isNull(latestBuildStageEvaluation)) {
            return statusDTO.setHasPrioritiesReport(false);
          }
          return statusDTO.setLastEvaluationTimestamp(latestBuildStageEvaluation.getTime().getTime())
              .setHasPrioritiesReport(true)
              .setLastScanId(latestBuildStageEvaluation.getScanId());
        })
        // Set last commit time
        .map(statusDTO -> {
          final SourceControlDefaultBranchCommitHistory commitHistory =
              sourceControlDefaultBranchCommitHistoryDAO.getLatestCommitForApplicationId(statusDTO.getApplicationId());
          return statusDTO.setLastCommitTimestamp(
              commitHistory != null ? commitHistory.getCommitTime().getTime() : 0L);
        })
        // Set CI/CD Integration status
        .map(statusDTO -> {
          // 84 days is meant to approximate 3 months and is a value used for several related metrics in
          // Sonatype Developer
          final long lookBackWindowMs = 7257600000L; // 84 Days * 24 hours * 60 Minutes * 60 Seconds * 1000 Ms
          final Date sinceUtcDate = new Date(System.currentTimeMillis() - lookBackWindowMs);

          return statusDTO.setCiIntegrationEnabled(
              policyEvaluationDAO.hasCIIntegrationEvaluation(statusDTO.getApplicationId(), sinceUtcDate));
        })
        .filter(statusDTO ->
            // Optionally filter on CI/CD Integration status
            optionalFilterAppsByCiCdIntegration == null ||
                statusDTO.isCiIntegrationEnabled() == optionalFilterAppsByCiCdIntegration)
        .toList();

    final List<IntegrationStatusDTO> possiblyPaginatedSummaries = paginateEarly ?
        minimalAppSummaries.stream()
            .sorted(new IntegrationStatusDTOComparator(filter.getOptionalOrderBy()))
            .skip(skipCount)
            .limit(filter.getPageSize())
            .toList() : minimalAppSummaries;

    final List<IntegrationStatusDTO> enrichedSummaries = possiblyPaginatedSummaries.stream()
        // Set Automated Source Control Feedback status
        .map(statusDTO -> statusDTO.setAutomatedSourceControlFeedbackEnabled(
            applicationSourceControlService.isAutomatedSourceControlFeedbackEnabledForApp(
                statusDTO.getApplicationId())))
        // Optionally filter on SCM Integration status
        .filter(statusDTO ->
            optionalFilterAppsByScmIntegration == null ||
                statusDTO.isAutomatedSourceControlFeedbackEnabled() == optionalFilterAppsByScmIntegration)
        .collect(Collectors.toList());

    final List<IntegrationStatusDTO> completeSummaries = paginateEarly ? enrichedSummaries :
        enrichedSummaries.stream()
            .sorted(new IntegrationStatusDTOComparator(filter.getOptionalOrderBy()))
            .skip(skipCount)
            .limit(filter.getPageSize())
            .collect(Collectors.toList());

    final int totalSize = paginateEarly ? minimalAppSummaries.size() : enrichedSummaries.size();
    return new ApiPageResult<>(totalSize, filter.getPage(), filter.getPageSize(), completeSummaries);
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.ID) String ownerId)
  {
    // The @Authorize annotation provides the implementation for this function
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  List<Application> getApplicationsWithReadPermission() {
    return applicationDAO.getAll();
  }

  private static boolean matchesFilter(final String applicationName, final String filter) {
    return applicationName.toLowerCase(Locale.ROOT)
        .matches(String.format(".*%s.*", Pattern.quote(filter.toLowerCase(Locale.ROOT))));
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

  private void sendAppIntegrationFilterTelemetry(
      final boolean includesAppNameSearch,
      final boolean includesScmIntegrationFilter,
      final boolean includesCiCdIntegrationFilter,
      final String orderBy
  )
  {
    final Map<String, Object> attributes = new HashMap<>();

    attributes.put("includes_app_name_search", includesAppNameSearch);
    attributes.put("includes_scm_integration_filter", includesScmIntegrationFilter);
    attributes.put("includes_ci_cd_integration_filter", includesCiCdIntegrationFilter);
    attributes.put("order_by", orderBy);

    final TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.DEVELOPER_INTEGRATIONS_DASHBOARD);
    telemetryData.setAttributes(attributes);
    telemetrySender.send(telemetryData);
  }
}
