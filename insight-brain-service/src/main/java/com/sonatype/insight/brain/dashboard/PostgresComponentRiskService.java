/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.audit.AuditService;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponentRisk;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

@Named
@Singleton
public class PostgresComponentRiskService
    extends AbstractComponentRiskService
{
  private final ApplicationComponentDAO applicationComponentDAO;

  @Inject
  public PostgresComponentRiskService(
      final DashboardUtils dashboardUtils,
      final ApplicationService applicationService,
      final ApplicationComponentDAO applicationComponentDAO,
      final AuditService auditService)
  {
    super(applicationService, dashboardUtils, auditService);
    this.applicationComponentDAO = applicationComponentDAO;
  }

  @Override
  public DashboardResultsDTO<ComponentRiskDTO> load(
      final List<Application> applications,
      final Set<String> stageIds,
      final PolicyThreatCategoryFilter policyThreatCategoryFilter,
      final PolicyThreatLevelFilter policyThreatLevelFilter,
      final PolicyViolationStateFilter policyViolationStateFilter,
      final String orderBy,
      final int page,
      final int pageSize)
  {
    DashboardResultsDTO<ComponentRiskDTO> result = new DashboardResultsDTO<>();

    Set<String> appIds = applications.stream().map(Application::getId).collect(Collectors.toSet());

    Set<String> threatCategoryFilter = policyThreatCategoryFilter != null
        ? policyThreatCategoryFilter.getPolicyThreatCategories().stream().map(Enum::name).collect(Collectors.toSet())
        : Collections.emptySet();
    Entry<Integer, Integer> threatLevelFilter = policyThreatLevelFilter != null
        ? Map.entry(policyThreatLevelFilter.getMinPolicyThreatLevel(),
            policyThreatLevelFilter.getMaxPolicyThreatLevel())
        : Map.entry(0, 10);
    Set<String> violationStateFilter = policyViolationStateFilter != null
        ? policyViolationStateFilter.getPolicyViolationStates().stream().map(Enum::name).collect(Collectors.toSet())
        : Collections.emptySet();
    Set<String> stageTypesFilter = dashboardUtils.getStageTypes(stageIds)
        .stream()
        .map(StageType::getId)
        .collect(Collectors.toSet());

    List<ComponentRiskDTO> dtos = applicationComponentDAO
        .getComponentsRiskFiltered(appIds, stageTypesFilter, threatCategoryFilter,
            threatLevelFilter, violationStateFilter, getSortColumnAndDirection(orderBy), page, pageSize)
        .stream()
        .map(this::toDTO)
        .toList();

    if (dtos.isEmpty()) {
      result.dashboardResults = List.of();
    }
    else {
      result.hasNextPage = dtos.size() > pageSize;
      result.dashboardResults = result.hasNextPage ? dtos.subList(0, dtos.size() - 1) : dtos;
    }

    return result;
  }

  private String getSortColumnAndDirection(String orderBy) {
    if (orderBy == null) {
      return "score DESC";
    }

    String direction = "ASC";

    if (orderBy.startsWith("-")) {
      direction = "DESC";
      orderBy = orderBy.substring(1);
    }

    return switch (ComponentRiskOrderByEnum.valueOf(orderBy)) {
      case TOTAL_RISK -> "score";
      case CRITICAL_RISK -> "scoreCritical";
      case SEVERE_RISK -> "scoreSevere";
      case MODERATE_RISK -> "scoreModerate";
      case LOW_RISK -> "scoreLow";
      case NUMBER_OF_AFFECTED_APPS -> "affectedApplications";
      default -> throw new BadRequestException("Invalid orderBy value: " + orderBy);
    } + " " + direction;
  }

  private ComponentRiskDTO toDTO(ApplicationComponentRisk applicationComponentRiskDTO) {
    ComponentRiskDTO dto = new ComponentRiskDTO();

    dto.hash = applicationComponentRiskDTO.hash();
    dto.filename = applicationComponentRiskDTO.filename();
    dto.affectedApplications = applicationComponentRiskDTO.affectedApplications();
    dto.score = applicationComponentRiskDTO.score();
    dto.scoreCritical = applicationComponentRiskDTO.scoreCritical();
    dto.scoreSevere = applicationComponentRiskDTO.scoreSevere();
    dto.scoreModerate = applicationComponentRiskDTO.scoreModerate();
    dto.scoreLow = applicationComponentRiskDTO.scoreLow();

    if (applicationComponentRiskDTO.componentIdCoordinatesJson() != null) {
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter
          .formatAndJsonToComponentIdentifier(applicationComponentRiskDTO.componentIdFormat(),
              applicationComponentRiskDTO.componentIdCoordinatesJson());
      dto.displayName = ComponentDisplayNameUtil.fromIdentifier(componentIdentifier);
    }

    dto.derivedComponentName = ComponentDisplayNameUtil.deriveComponentName(dto);

    return dto;
  }
}
