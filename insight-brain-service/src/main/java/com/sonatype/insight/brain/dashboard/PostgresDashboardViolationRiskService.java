/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.policy.TriggerReference.Type;
import com.sonatype.insight.brain.audit.AuditService;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.policy.InternalDashboardViolationRiskDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationConstraintFactsDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolationConstraintFacts;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.json.store.JsonUtils;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class PostgresDashboardViolationRiskService
    extends AbstractDashboardViolationRiskService
{
  private static final Logger log = LoggerFactory.getLogger(PostgresDashboardViolationRiskService.class);

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO;

  private final DashboardUtils dashboardUtils;

  @Inject
  public PostgresDashboardViolationRiskService(
      ApplicationService applicationService,
      PolicyViolationDAO policyViolationDAO,
      PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO,
      DashboardUtils dashboardUtils,
      AuditService auditService)
  {
    super(applicationService, dashboardUtils, auditService);
    this.policyViolationDAO = policyViolationDAO;
    this.policyViolationConstraintFactsDAO = policyViolationConstraintFactsDAO;
    this.dashboardUtils = dashboardUtils;
  }

  @Override
  protected DashboardResultsDTO<DashboardViolationRiskDTO> load(
      List<Application> applications,
      Set<StageType> stageTypes,
      PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter,
      PolicyViolationStateFilter policyViolationStateFilter,
      String orderBy,
      Date minDate,
      int page,
      int pageSize)
  {
    // PolicyThreatCategoryFilter, PolicyThreatLevelFilter and PolicyViolationStateFilter validate their content,
    // so we don't have to worry about SQL injection attacks via these parameters.

    // This also validates the orderBy values, so we don't have to worry about SQL injection.
    List<DashboardViolationRiskOrderBy> orderBys = DashboardViolationRiskOrderBy.getOrderBys(orderBy);

    DashboardResultsDTO<DashboardViolationRiskDTO> result = new DashboardResultsDTO<>();

    Set<String> applicationIds = applications.stream().map(Application::getId).collect(Collectors.toSet());
    Set<String> stageTypeIds = stageTypes.stream().map(StageType::getId).collect(Collectors.toSet());

    Integer minPolicyThreatLevel = null;
    Integer maxPolicyThreatLevel = null;
    if (policyThreatLevelFilter != null) {
      if (policyThreatLevelFilter.getMinPolicyThreatLevel() > 0) {
        minPolicyThreatLevel = policyThreatLevelFilter.getMinPolicyThreatLevel();
      }
      if (policyThreatLevelFilter.getMaxPolicyThreatLevel() < 10) {
        maxPolicyThreatLevel = policyThreatLevelFilter.getMaxPolicyThreatLevel();
      }
    }
    Set<String> policyThreatCategoryNames = null;
    if (policyThreatCategoryFilter != null && policyThreatCategoryFilter.getPolicyThreatCategories() != null
        && policyThreatCategoryFilter.getPolicyThreatCategories().size() != PolicyThreatCategory.values().length)
    {
      policyThreatCategoryNames = policyThreatCategoryFilter.getPolicyThreatCategories()
          .stream()
          .map(PolicyThreatCategory::name)
          .collect(Collectors.toSet());
    }

    Boolean violationStateOpen = null;
    Boolean violationStateWaived = null;
    Boolean violationStateLegacyViolation = null;
    if (policyViolationStateFilter != null) {
      violationStateOpen = policyViolationStateFilter.getPolicyViolationStates().contains(PolicyViolationState.OPEN);
      violationStateWaived =
          policyViolationStateFilter.getPolicyViolationStates().contains(PolicyViolationState.WAIVED);
      violationStateLegacyViolation =
          policyViolationStateFilter.getPolicyViolationStates().contains(PolicyViolationState.LEGACY_VIOLATION);
      log.debug("Loading violations with a filter on state open: {}, waived: {} and legacy: {}", violationStateOpen,
          violationStateWaived, violationStateLegacyViolation);
    }

    List<InternalDashboardViolationRiskDTO> rows = policyViolationDAO.getDashboardViolationRisk(applicationIds,
        stageTypeIds, minPolicyThreatLevel, maxPolicyThreatLevel, minDate, policyThreatCategoryNames,
        violationStateOpen, violationStateWaived, violationStateLegacyViolation, getSqlOrderBy(orderBys), page,
        pageSize);
    if (DashboardUtils.shouldOnlyShowWaivedViolations(policyViolationStateFilter)) {
      Set<String> excludedViolationIds = dashboardUtils.getAutoWaiverExcludedViolationIds(rows);
      rows = rows.stream()
          .filter(dto -> !excludedViolationIds.contains(dto.policyViolationId))
          .toList();
    }

    if (rows.isEmpty()) {
      result.dashboardResults = Collections.emptyList();
      return result;
    }

    Map<String, PolicyViolationConstraintFacts> constraintFactsById = getConstraintFactsById(rows);

    if (rows.size() > pageSize) {
      result.hasNextPage = true;
    }
    result.dashboardResults =
        rows.stream()
            .limit(pageSize)
            .map(row -> toDashboardViolationRiskDTO(row, constraintFactsById.get(row.constraintFactsId)))
            .toList();

    return result;
  }

  private Map<String, PolicyViolationConstraintFacts> getConstraintFactsById(
      List<InternalDashboardViolationRiskDTO> rows)
  {
    Set<String> constraintFactsIds = rows.stream().map(row -> row.constraintFactsId).collect(Collectors.toSet());
    return policyViolationConstraintFactsDAO.getByIds(constraintFactsIds)
        .stream()
        .collect(Collectors.toMap(PolicyViolationConstraintFacts::getId, Function.identity()));
  }

  private DashboardViolationRiskDTO toDashboardViolationRiskDTO(
      InternalDashboardViolationRiskDTO internalDTO,
      PolicyViolationConstraintFacts constraintFacts)
  {
    DashboardViolationRiskDTO result = new DashboardViolationRiskDTO();
    result.applicationName = internalDTO.applicationName;
    result.organizationName = internalDTO.organizationName;
    result.threatLevel = internalDTO.threatLevel;
    result.policyName = internalDTO.policyName;
    result.policyId = internalDTO.policyId;
    result.policyViolationId = internalDTO.policyViolationId;
    result.hash = internalDTO.hash;
    result.filename = internalDTO.filename;
    ComponentIdentifier componentIdentifierFromJson = ComponentIdentifierAdapter
        .formatAndJsonToComponentIdentifier(internalDTO.componentIdFormat, internalDTO.componentIdCoordinatesJson);
    result.displayName = ComponentDisplayNameUtil.fromIdentifier(componentIdentifierFromJson);
    result.derivedComponentName = ComponentDisplayNameUtil.deriveComponentName(result);
    result.firstOccurrenceTime = internalDTO.firstOccurrenceTime;
    result.referenceId = findSecurityVulnerabilityReferenceId(constraintFacts);
    return result;
  }

  private String findSecurityVulnerabilityReferenceId(PolicyViolationConstraintFacts constraintFacts) {
    // If the constraint facts migration is not yet complete, it is possible that the constraint facts are null.
    // Since this is a temporary state, we can just short-circuit and return null.
    if (constraintFacts == null) {
      return null;
    }

    try {
      for (ConstraintFact constraintFact : JsonUtils.parse(constraintFacts.getConstraintFactsJson(),
          ConstraintFact[].class))
      {
        for (ConditionFact fact : constraintFact.getConditionFacts()) {
          TriggerReference reference = fact.getReference();
          if (reference != null && Type.SECURITY_VULNERABILITY_REFID.equals(reference.getType())) {
            // All security vulnerability references must point to the same CVE/reference
            return reference.getValue();
          }
        }
      }
    }
    catch (IOException e) {
      String message =
          String.format("Failed to parse constraint facts for constraint facts ID %s with JSON %s. Error was: %s",
              constraintFacts.getId(), constraintFacts.getConstraintFactsJson(), e.getMessage());
      throw new UncheckedIOException(message, e);
    }
    return null;
  }

  private static List<String> getSqlOrderBy(List<DashboardViolationRiskOrderBy> orderBys) {
    List<String> result = new ArrayList<>();
    for (DashboardViolationRiskOrderBy orderBy : orderBys) {
      switch (orderBy.getDashboardViolationRiskOrderByEnumRiskOrderByEnum()) {
        case AGE:
          result.add("open_time " + getSqlOrderByAscDesc(orderBy));
          break;
        case APPLICATION_NAME:
          result.add("application_name " + getSqlOrderByAscDesc(orderBy));
          break;
        case COMPONENT_NAME:
          // Not supported anymore
          break;
        case POLICY_NAME:
          result.add("policy_name " + getSqlOrderByAscDesc(orderBy));
          break;
        case THREAT_LEVEL:
          result.add("threat_level " + getSqlOrderByAscDesc(orderBy));
          break;
        default:
          throw new IllegalArgumentException(
              "unsupported order by " + orderBy.getDashboardViolationRiskOrderByEnumRiskOrderByEnum());
      }
    }

    return result;
  }

  private static String getSqlOrderByAscDesc(DashboardViolationRiskOrderBy orderBy) {
    return orderBy.isOrderByAsc() ? "ASC" : "DESC";
  }
}
