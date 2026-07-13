/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditService;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationRiskDTO;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class PostgresApplicationRiskService
    extends AbstractApplicationRiskService
{
  private static final Logger log = LoggerFactory.getLogger(PostgresApplicationRiskService.class);

  private final DashboardUtils dashboardUtils;

  private final ApplicationService applicationService;

  private final AuditService auditService;

  private final ApplicationDAO applicationDAO;

  @Inject
  public PostgresApplicationRiskService(
      final ApplicationService applicationService,
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO,
      final PolicyViolationLoader policyViolationLoader,
      final DashboardUtils dashboardUtils,
      final AuditService auditService)
  {
    super(applicationService, organizationDAO, policyViolationLoader, dashboardUtils, auditService);
    this.dashboardUtils = dashboardUtils;
    this.applicationService = applicationService;
    this.auditService = auditService;
    this.applicationDAO = applicationDAO;
  }

  @Override
  public DashboardResultsDTO<ApplicationRiskScoreDTO> getApplicationRisks(
      final Set<String> organizationIds,
      final Set<String> applicationIds,
      final Set<String> stageIds,
      final Set<String> tagIds,
      final PolicyThreatCategoryFilter policyThreatCategoryFilter,
      final PolicyThreatLevelFilter policyThreatLevelFilter,
      final PolicyViolationStateFilter policyViolationStateFilter,
      final String orderBy,
      final int page,
      final int pageSize)
  {
    dashboardUtils.validateDashboardLicensedAndEnabledForApplications();

    long start = System.currentTimeMillis();

    List<Application> applications = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds,
        applicationIds, tagIds);
    AuditData.get()
        .setData("selectedOrganizations", auditService.getSelectedOrganizationsById(organizationIds))
        .setData("selectedApplications",
            auditService.getSelectedApplicationsById(applicationIds, organizationIds, applications))
        .setSelectedApplicationCategories(auditService.getSelectedApplicationCategoriesById(tagIds))
        .setData("inspectedApplicationCount", applications.size());

    List<ApplicationRiskDTO> applicationRiskDTOs =
        retrieveApplicationRiskDTOs(applications, stageIds, policyThreatCategoryFilter, policyThreatLevelFilter,
            policyViolationStateFilter, orderBy, page, pageSize);

    List<ApplicationRiskScoreDTO> applicationRiskScoreDTOs = buildApplicationRiskScoreDTOs(applicationRiskDTOs);
    sortStagesInChronologicalOrder(applicationRiskScoreDTOs);

    DashboardResultsDTO<ApplicationRiskScoreDTO> dashboardResultsDTO = new DashboardResultsDTO<>();
    if (applicationRiskScoreDTOs.isEmpty()) {
      dashboardResultsDTO.dashboardResults = List.of();
    }
    else {
      dashboardResultsDTO.hasNextPage = applicationRiskScoreDTOs.size() > pageSize;
      dashboardResultsDTO.dashboardResults = dashboardResultsDTO.hasNextPage
          ? applicationRiskScoreDTOs.subList(0, applicationRiskScoreDTOs.size() - 1)
          : applicationRiskScoreDTOs;
    }

    log.debug("getApplicationRisks finished in {} ms", System.currentTimeMillis() - start);

    return dashboardResultsDTO;
  }

  private List<ApplicationRiskDTO> retrieveApplicationRiskDTOs(
      final List<Application> applications,
      final Set<String> stageIds,
      final PolicyThreatCategoryFilter policyThreatCategoryFilter,
      final PolicyThreatLevelFilter policyThreatLevelFilter,
      final PolicyViolationStateFilter policyViolationStateFilter,
      final String orderBy,
      final int page,
      final int pageSize)
  {
    Set<String> appIds = applications.stream().map(Application::getId).collect(Collectors.toSet());
    Set<String> threatCategoryFilter = policyThreatCategoryFilter != null
        ? policyThreatCategoryFilter.getPolicyThreatCategories().stream().map(Enum::name).collect(Collectors.toSet())
        : Set.of();
    int minPolicyThreatLevel = 1;
    int maxPolicyThreatLevel = 10;
    if (policyThreatLevelFilter != null) {
      minPolicyThreatLevel = policyThreatLevelFilter.getMinPolicyThreatLevel();
      maxPolicyThreatLevel = policyThreatLevelFilter.getMaxPolicyThreatLevel();
    }
    Set<String> violationStateFilter = policyViolationStateFilter != null
        ? policyViolationStateFilter.getPolicyViolationStates().stream().map(Enum::name).collect(Collectors.toSet())
        : Set.of();
    Set<String> stageTypesFilter = dashboardUtils.getStageTypes(stageIds)
        .stream()
        .map(StageType::getId)
        .collect(Collectors.toSet());

    Pair<String, String> sortColumnAndDirection = getSortColumnAndDirection(orderBy);

    return applicationDAO.getDashboardApplicationRisk(appIds, stageTypesFilter, threatCategoryFilter,
        minPolicyThreatLevel, maxPolicyThreatLevel, violationStateFilter, sortColumnAndDirection.getLeft(),
        sortColumnAndDirection.getRight(), page,
        pageSize);
  }

  private static List<ApplicationRiskScoreDTO> buildApplicationRiskScoreDTOs(
      final List<ApplicationRiskDTO> applicationRiskDTOs)
  {
    List<ApplicationRiskScoreDTO> applicationRiskScoreDTOs = new ArrayList<>();
    for (ApplicationRiskDTO applicationRiskDTO : applicationRiskDTOs) {
      ApplicationRiskScoreDTO applicationRiskScoreDTOLast =
          applicationRiskScoreDTOs.isEmpty() ? null : applicationRiskScoreDTOs.get(applicationRiskScoreDTOs.size() - 1);
      if (applicationRiskScoreDTOLast == null ||
          !applicationRiskScoreDTOLast.applicationName.equals(applicationRiskDTO.applicationName()))
      {
        RiskDTO totalApplicationRisk = new RiskDTO();
        totalApplicationRisk.totalRisk = applicationRiskDTO.totalRiskPerStageUnique();
        totalApplicationRisk.criticalRisk = applicationRiskDTO.criticalPerStageUnique();
        totalApplicationRisk.severeRisk = applicationRiskDTO.severePerStageUnique();
        totalApplicationRisk.moderateRisk = applicationRiskDTO.moderatePerStageUnique();
        totalApplicationRisk.lowRisk = applicationRiskDTO.lowPerStageUnique();

        ApplicationRiskScoreDTO applicationRiskScoreDTO = new ApplicationRiskScoreDTO(
            applicationRiskDTO.organizationName(),
            applicationRiskDTO.organizationId(),
            applicationRiskDTO.applicationName(),
            applicationRiskDTO.publicId(),
            applicationRiskDTO.applicationId());
        applicationRiskScoreDTO.totalApplicationRisk = totalApplicationRisk;

        applicationRiskScoreDTO.stageRisks = new ArrayList<>();
        StageRiskScoreDTO stageRiskScoreDTO = new StageRiskScoreDTO(applicationRiskDTO.stageTypeId());
        stageRiskScoreDTO.stageTypeName = StageTypes.getById(stageRiskScoreDTO.stageTypeId).getName();
        stageRiskScoreDTO.risk = new RiskDTO(
            applicationRiskDTO.totalRiskPerStage(),
            applicationRiskDTO.criticalPerStage(),
            applicationRiskDTO.severePerStage(),
            applicationRiskDTO.moderatePerStage(),
            applicationRiskDTO.lowPerStage());

        stageRiskScoreDTO.scanId = applicationRiskDTO.scanId();
        applicationRiskScoreDTO.stageRisks.add(stageRiskScoreDTO);
        applicationRiskScoreDTOs.add(applicationRiskScoreDTO);
      }
      else {
        applicationRiskScoreDTOLast.totalApplicationRisk.totalRisk += applicationRiskDTO.totalRiskPerStageUnique();
        applicationRiskScoreDTOLast.totalApplicationRisk.criticalRisk += applicationRiskDTO.criticalPerStageUnique();
        applicationRiskScoreDTOLast.totalApplicationRisk.severeRisk += applicationRiskDTO.severePerStageUnique();
        applicationRiskScoreDTOLast.totalApplicationRisk.moderateRisk += applicationRiskDTO.moderatePerStageUnique();
        applicationRiskScoreDTOLast.totalApplicationRisk.lowRisk += applicationRiskDTO.lowPerStageUnique();

        StageRiskScoreDTO stageRiskScoreDTO = new StageRiskScoreDTO(applicationRiskDTO.stageTypeId());
        stageRiskScoreDTO.stageTypeName = StageTypes.getById(applicationRiskDTO.stageTypeId()).getName();
        stageRiskScoreDTO.risk = new RiskDTO(applicationRiskDTO.totalRiskPerStage(),
            applicationRiskDTO.criticalPerStage(),
            applicationRiskDTO.severePerStage(),
            applicationRiskDTO.moderatePerStage(),
            applicationRiskDTO.lowPerStage());
        stageRiskScoreDTO.scanId = applicationRiskDTO.scanId();

        applicationRiskScoreDTOLast.stageRisks.add(stageRiskScoreDTO);
      }
    }

    return applicationRiskScoreDTOs;
  }

  private void sortStagesInChronologicalOrder(final List<ApplicationRiskScoreDTO> applicationRiskScoreDTOs) {
    for (ApplicationRiskScoreDTO applicationRiskScoreDTO : applicationRiskScoreDTOs) {
      Set<String> stageIds = applicationRiskScoreDTO.stageRisks.stream()
          .map(s -> s.stageTypeId)
          .collect(Collectors.toSet());
      Set<StageType> stageTypesInChronologicalOrder = dashboardUtils.getStageTypes(stageIds);

      List<StageRiskScoreDTO> stageRisksOrdered = new ArrayList<>();
      for (StageType stageType : stageTypesInChronologicalOrder) {
        for (StageRiskScoreDTO stageRiskScoreDTO : applicationRiskScoreDTO.stageRisks) {
          if (stageType.getId().equals(stageRiskScoreDTO.stageTypeId)) {
            stageRisksOrdered.add(stageRiskScoreDTO);
            break;
          }
        }
      }

      applicationRiskScoreDTO.stageRisks = stageRisksOrdered;
    }
  }

  private Pair<String, String> getSortColumnAndDirection(final String orderBy) {
    if (orderBy == null) {
      return Pair.of("total_risk_per_stage_unique", "DESC");
    }

    boolean hasDirection = orderBy.startsWith("-");
    String direction = hasDirection ? "DESC" : "ASC";

    String sortColumn = orderBy;
    if (hasDirection) {
      sortColumn = orderBy.substring(1);
    }

    sortColumn = switch (ApplicationRiskOrderByEnum.fromOrderByToken(sortColumn)) {
      case TOTAL_RISK -> "total_risk_per_stage_unique";
      case CRITICAL_RISK -> "critical_per_stage_unique";
      case SEVERE_RISK -> "severe_per_stage_unique";
      case MODERATE_RISK -> "moderate_per_stage_unique";
      case LOW_RISK -> "low_per_stage_unique";
      case NAME -> "name";
      case LAST_EVALUATION_TIME -> throw new BadRequestException(
          "orderBy lastEvaluationTime is not supported for PostgreSQL-backed application risk queries.");
    };

    return Pair.of(sortColumn, direction);
  }
}
