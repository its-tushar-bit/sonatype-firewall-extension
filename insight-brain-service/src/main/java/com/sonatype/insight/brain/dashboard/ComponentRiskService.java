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

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditUtils;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationStageView;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationView;

import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ComponentRiskService
{
  private static final Logger log = LoggerFactory.getLogger(ComponentRiskService.class);

  private static final PolicyViolationDTOComparator POLICY_VIOLATION_DTO_COMPARATOR =
      new PolicyViolationDTOComparator();

  private final ApplicationService applicationService;

  private final PolicyViolationLoader policyViolationLoader;

  private final DashboardUtils dashboardUtils;

  @Inject
  public ComponentRiskService(ApplicationService applicationService,
                              PolicyViolationLoader policyViolationLoader,
                              DashboardUtils dashboardUtils)
  {
    this.applicationService = applicationService;
    this.policyViolationLoader = policyViolationLoader;
    this.dashboardUtils = dashboardUtils;
  }

  /**
   * Gets the risk per component by rolling up the policy violations matching the specified filter criteria. Empty or
   * null filter criteria generally mean "all available" violations for that aspect. The results are sorted by
   * descending component risk scores.
   */
  public DashboardResultsDTO<ComponentRiskDTO> getComponentRisks(Set<String> organizationIds,
                                                                 Set<String> applicationIds,
                                                                 Set<String> stageIds,
                                                                 Set<String> tagIds,
                                                                 PolicyThreatCategoryFilter policyThreatCategoryFilter,
                                                                 PolicyThreatLevelFilter policyThreatLevelFilter,
                                                                 PolicyViolationStateFilter policyViolationStateFilter,
                                                                 String orderBy,
                                                                 int maxResults)
  {
    dashboardUtils.validateDashboardLicensedAndEnabled();

    long start = System.currentTimeMillis();

    ComponentRiskDTOComparator componentRiskComparator = new ComponentRiskDTOComparator(orderBy);
    List<PolicyViolationDTO> violations = getPolicyViolations(organizationIds, applicationIds, stageIds, tagIds,
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
    DashboardResultsDTO<ComponentRiskDTO> result = new DashboardResultsDTO<>();
    result.numResults = dtos.size();
    result.dashboardResults = dtos.subList(0, Math.min(dtos.size(), maxResults));

    AuditData.get().setData("resultRecordCount", result.numResults);

    log.debug("getComponentRisks finished in {} ms", System.currentTimeMillis() - start);

    return result;
  }

  /**
   * Gets the policy violations matching the specified filter criteria. Empty or null filter criteria generally mean
   * "all available" violations for that aspect.
   */
  List<PolicyViolationDTO> getPolicyViolations(Set<String> organizationIds,
                                               Set<String> applicationIds,
                                               Set<String> stageIds,
                                               Set<String> tagIds,
                                               PolicyThreatCategoryFilter policyThreatCategoryFilter,
                                               PolicyThreatLevelFilter policyThreatLevelFilter,
                                               PolicyViolationStateFilter policyViolationStateFilter)
  {
    List<Application> applications = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds,
        applicationIds, tagIds);

    AuditData.get() //
        .setData("selectedOrganizations", AuditUtils.getSelectedOrganizationsById(organizationIds)) //
        .setData("selectedApplications",
            AuditUtils.getSelectedApplicationsById(applicationIds, organizationIds, applications)) //
        .setSelectedApplicationCategories(AuditUtils.getSelectedApplicationCategoriesById(tagIds)) //
        .setData("inspectedApplicationCount", applications.size());

    log.debug("Loaded {} applications", applications.size());
    Set<StageType> stageTypes = dashboardUtils.getStageTypes(stageIds);
    Collection<ApplicationView> appViews = policyViolationLoader.getViolations(applications, stageTypes, false,
        policyThreatLevelFilter, policyThreatCategoryFilter, policyViolationStateFilter);

    List<PolicyViolationDTO> policyViolationDTOs = new ArrayList<>();

    for (ApplicationView appView : appViews) {
      Application application = appView.getApplication();
      for (ApplicationStageView appStageView : appView.getStageViews()) {
        PolicyEvaluation evaluation = appStageView.getLastEvaluation();
        for (PolicyViolation violation : appStageView.getFilteredViolations()) {
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
