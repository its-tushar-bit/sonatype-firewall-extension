/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditService;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationStageView;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationView;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class H2ComponentRiskService
    extends AbstractComponentRiskService
{
  private static final Logger log = LoggerFactory.getLogger(H2ComponentRiskService.class);

  private static final PolicyViolationDTOComparator POLICY_VIOLATION_DTO_COMPARATOR =
      new PolicyViolationDTOComparator();

  private final PolicyViolationLoader policyViolationLoader;

  private final PolicyViolationDAO policyViolationDAO;

  @Inject
  public H2ComponentRiskService(
      final ApplicationService applicationService,
      final PolicyViolationLoader policyViolationLoader,
      final DashboardUtils dashboardUtils,
      final AuditService auditService,
      final PolicyViolationDAO policyViolationDAO)
  {
    super(applicationService, dashboardUtils, auditService);
    this.policyViolationLoader = policyViolationLoader;
    this.policyViolationDAO = policyViolationDAO;
  }

  @Override
  public DashboardResultsDTO<ComponentRiskDTO> load(
      List<Application> applications,
      Set<String> stageIds,
      PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter,
      PolicyViolationStateFilter policyViolationStateFilter,
      String orderBy,
      int page,
      int pageSize)
  {
    DashboardResultsDTO<ComponentRiskDTO> result = new DashboardResultsDTO<>();

    ComponentRiskDTOComparator componentRiskComparator = new ComponentRiskDTOComparator(orderBy);
    List<PolicyViolationDTO> violations = getPolicyViolations(applications, stageIds,
        policyThreatCategoryFilter, policyThreatLevelFilter, policyViolationStateFilter);
    Map<String, ComponentViolationRollUp> componentsByHash = new LinkedHashMap<>();
    for (PolicyViolationDTO violation : violations) {
      ComponentViolationRollUp component = componentsByHash.get(violation.hash);
      if (component == null) {
        component = new ComponentViolationRollUp();
        componentsByHash.put(violation.hash, component);
      }
      component.add(violation);
    }

    List<ComponentRiskDTO> dtos = new ArrayList<>(componentsByHash.size());
    for (ComponentViolationRollUp component : componentsByHash.values()) {
      dtos.add(component.toDTO());
    }
    dtos.sort(componentRiskComparator);

    if (dtos.isEmpty()) {
      result.dashboardResults = new ArrayList<>();
    }
    else {
      List<List<ComponentRiskDTO>> pages = Lists.partition(dtos, pageSize);
      result.dashboardResults = page >= pages.size() ? new ArrayList<>() : pages.get(page);
      result.hasNextPage = pages.size() > (page + 1);
    }

    return result;
  }

  @VisibleForTesting
  List<PolicyViolationDTO> getPolicyViolations(
      Set<String> organizationIds,
      Set<String> applicationIds,
      Set<String> stageIds,
      Set<String> tagIds,
      PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter,
      PolicyViolationStateFilter policyViolationStateFilter)
  {
    List<Application> applications = getApplications(organizationIds, applicationIds, tagIds);

    return getPolicyViolations(applications, stageIds, policyThreatCategoryFilter, policyThreatLevelFilter,
        policyViolationStateFilter);
  }

  /**
   * Gets the policy violations matching the specified filter criteria. Empty or null filter criteria generally mean
   * "all available" violations for that aspect.
   */
  private List<PolicyViolationDTO> getPolicyViolations(
      List<Application> applications,
      Set<String> stageIds,
      PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter,
      PolicyViolationStateFilter policyViolationStateFilter)
  {
    log.debug("Loaded {} applications", applications.size());
    Set<StageType> stageTypes = dashboardUtils.getStageTypes(stageIds);
    Collection<ApplicationView> appViews = policyViolationLoader.getViolations(applications, stageTypes, false,
        policyThreatLevelFilter, policyThreatCategoryFilter, policyViolationStateFilter);

    List<PolicyViolationDTO> policyViolationDTOs = new ArrayList<>();

    for (ApplicationView appView : appViews) {
      Application application = appView.getApplication();
      for (ApplicationStageView appStageView : appView.getStageViews()) {
        PolicyEvaluation evaluation = appStageView.getLastEvaluation();
        List<PolicyViolation> policyViolations = appStageView.getFilteredViolations();
        policyViolationDAO.loadConstraintFacts(policyViolations);
        for (PolicyViolation violation : policyViolations) {
          policyViolationDTOs.add(PolicyViolationAdapter.createPolicyViolationDTO(application, evaluation, violation));
        }
      }
    }

    sort(policyViolationDTOs);

    return policyViolationDTOs;
  }

  /**
   * @return Sort by threat level (descending), policy name, application name, coordinates, and then hashes.
   */
  private List<PolicyViolationDTO> sort(List<PolicyViolationDTO> dtos) {
    dtos.sort(POLICY_VIOLATION_DTO_COMPARATOR);
    return dtos;
  }

  private static class ComponentViolationRollUp
  {
    Map<String, PolicyViolationDTO> violationsByAppPolicyAndConstraint = new LinkedHashMap<>();

    Set<String> applicationIds = new HashSet<>();

    void add(PolicyViolationDTO violation) {
      String id = violation.computeUniqueAppPolicyConstraintId();
      PolicyViolationDTO existing = violationsByAppPolicyAndConstraint.get(id);
      if (existing == null || existing.time < violation.time) {
        // count violations for a given app+policy+constraint combo only once,
        // using the data from the most recent evaluation
        violationsByAppPolicyAndConstraint.put(id, violation);
      }
      applicationIds.add(violation.applicationId);
    }

    public ComponentRiskDTO toDTO() {
      ComponentRiskDTO dto = new ComponentRiskDTO();
      dto.affectedApplications = applicationIds.size();
      for (PolicyViolationDTO violation : violationsByAppPolicyAndConstraint.values()) {
        dto.hash = violation.hash;
        dto.score += violation.threatLevel;
        if (violation.threatLevel >= 8) {
          dto.scoreCritical += violation.threatLevel;
        }
        else if (violation.threatLevel >= 4) {
          dto.scoreSevere += violation.threatLevel;
        }
        else if (violation.threatLevel >= 2) {
          dto.scoreModerate += violation.threatLevel;
        }
        else {
          dto.scoreLow += violation.threatLevel;
        }
        if (dto.displayName == null) {
          dto.displayName = ComponentDisplayNameUtil.fromIdentifier(violation.componentIdentifier);
        }
        if (!StringUtils.isBlank(violation.filename)) {
          dto.filename = violation.filename;
        }
        dto.derivedComponentName = ComponentDisplayNameUtil.deriveComponentName(dto);
      }
      return dto;
    }
  }
}
