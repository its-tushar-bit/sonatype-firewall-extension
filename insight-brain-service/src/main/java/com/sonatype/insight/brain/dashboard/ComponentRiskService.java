/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Named
public class ComponentRiskService
{
  private static final Logger log = LoggerFactory.getLogger(ComponentRiskService.class);

  private static final PolicyViolationDTOComparator POLICY_VIOLATION_DTO_COMPARATOR = new PolicyViolationDTOComparator();

  private final ApplicationService applicationService;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyViolationAdapter policyViolationAdapter;

  private final DashboardUtils dashboardUtils;

  @Inject
  public ComponentRiskService(ApplicationService applicationService,
                              ApplicationDAO applicationDAO,
                              PolicyEvaluationDAO policyEvaluationDAO,
                              PolicyViolationDAO policyViolationDAO,
                              PolicyViolationAdapter policyViolationAdapter,
                              DashboardUtils dashboardUtils)
  {
    this.applicationService = applicationService;
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.policyViolationAdapter = policyViolationAdapter;
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
    dashboardUtils.validateDashboardLicensed();

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
    Collections.sort(dtos, componentRiskComparator);
    DashboardResultsDTO<ComponentRiskDTO> result = new DashboardResultsDTO<>();
    result.numResults = dtos.size();
    result.dashboardResults = dtos.subList(0, Math.min(dtos.size(), maxResults));

    log.debug("getComponentRisks finished in {} ms", System.currentTimeMillis() - start);

    return result;
  }

  @Authorize(permission = Permission.READ)
  protected List<PolicyViolationDTO> getPolicyViolationsByApplicationId(@AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
                                                                        Set<StageType> stages,
                                                                        @Nullable Predicate<PolicyViolation> violationFilter)
  {
    if (StringUtils.isBlank(applicationId)) {
      throw new BadRequestException("Unable to get policy violations for null or empty application ID.");
    }

    Application application = applicationDAO.getByIdNotNull(applicationId);

    List<PolicyViolationDTO> policyViolationDTOs = new ArrayList<>();
    for (StageType stage : stages) {
      List<PolicyViolation> violations = getPolicyViolations(application.getId(), stage.getId());
      violations = dashboardUtils.filter(violations, violationFilter);
      policyViolationDTOs.addAll(policyViolationAdapter.createPolicyViolationDTOs(application, violations));
    }

    return policyViolationDTOs;
  }

  private List<PolicyViolation> getPolicyViolations(String applicationId, String stageId) {
    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(applicationId, stageId);

    if (policyEvaluation == null) {
      return Lists.newArrayList();
    }

    return policyViolationDAO.getByEvaluationId(policyEvaluation.getId());
  }

  /**
   * Gets the policy violations matching the specified filter criteria. Empty or null filter criteria generally mean
   * "all available" violations for that aspect.
   */
  private List<PolicyViolationDTO> getPolicyViolations(Set<String> organizationIds,
                                                       Set<String> applicationIds,
                                                       Set<String> stageIds,
                                                       Set<String> tagIds,
                                                       PolicyThreatCategoryFilter policyThreatCategoryFilter,
                                                       PolicyThreatLevelFilter policyThreatLevelFilter,
                                                       PolicyViolationStateFilter policyViolationStateFilter)
  {
    Predicate<PolicyViolation> filter = dashboardUtils.buildViolationFilter(policyThreatCategoryFilter,
        policyThreatLevelFilter, policyViolationStateFilter);

    // only of both Orgs and Apps filters are empty, return "all available" violations
    if (isEmpty(applicationIds) && isEmpty(organizationIds)) {
      return getPolicyViolations(stageIds, tagIds, filter);
    }

    Set<String> internalApplicationIds = new HashSet<>();
    if (applicationIds != null) {
      internalApplicationIds.addAll(applicationIds);
    }
    internalApplicationIds.addAll(applicationService.getApplicationIdsByOrganizationIds(organizationIds));

    if (isEmpty(internalApplicationIds)) {
      return Collections.emptyList();
    }

    return getPolicyViolationsByApplicationIds(internalApplicationIds, stageIds, tagIds, filter);
  }

  /**
   * @param stageTypeIds The stages to get policy violations for, defaults to {@link BuildStageType#ID}.
   * @param tagIds The tag ids to filter the applications for which to get policy violations, defaults to all
   *          applications
   * @param violationFilter A filter for violations, defaults to accept all violations.
   * @return A list of {@link PolicyViolationDTO}s for all applications with read permissions.
   * @throws BadRequestException Thrown if the stageTypeId does not match a known {@link StageType}.
   */
  List<PolicyViolationDTO> getPolicyViolations(@Nullable final Set<String> stageTypeIds,
                                               @Nullable final Set<String> tagIds,
                                               @Nullable final Predicate<PolicyViolation> violationFilter)
  {

    List<Application> applications = applicationService.getApplications();
    final Set<StageType> stageTypes = dashboardUtils.getStageTypes(stageTypeIds);
    final Set<String> stageTypeIdsFiltered = dashboardUtils.getStageTypeIds(stageTypes);

    if (!CollectionUtils.isEmpty(tagIds)) {
      Set<String> applicationIds = dashboardUtils.getApplicationIds(applications);
      applications = applicationDAO.getByIdsAndTagIds(applicationIds, tagIds);
    }

    final ImmutableMap<String, Application> applicationIdLookupMap = Maps.uniqueIndex(applications,
        DashboardUtils.hasIdIdSelector);
    final List<PolicyEvaluation> latestEvaluations = policyEvaluationDAO.getLastByApplicationIdsAndStageIds(
        applicationIdLookupMap.keySet(), stageTypeIdsFiltered);

    final ImmutableMap<String, PolicyEvaluation> evaluationIdLookupMap = Maps.uniqueIndex(latestEvaluations,
        DashboardUtils.hasIdIdSelector);
    final List<PolicyViolation> violations = dashboardUtils.filter(policyViolationDAO.getByEvaluationIds(Sets
        .newHashSet(Lists.transform(latestEvaluations, DashboardUtils.hasIdIdSelector))), violationFilter);

    final List<PolicyViolationDTO> result = Lists.newArrayList(Lists.transform(violations,
        new Function<PolicyViolation, PolicyViolationDTO>()
        {
          @Nonnull
          @Override
          public PolicyViolationDTO apply(final PolicyViolation violation) {
            final PolicyEvaluation evaluation = evaluationIdLookupMap.get(violation.getPolicyEvaluationId());
            final Application application = applicationIdLookupMap.get(evaluation.getApplicationId());
            return policyViolationAdapter.createPolicyViolationDTO(application, violation);
          }
        }));

    return sort(result);
  }

  /**
   * @param applicationIds A list of application ids to get policy violations.
   * @param stageTypeIds The stages to get policy violations for, defaults to {@link BuildStageType#ID}.
   * @param tagIds The tag ids to filter the applications for which to get policy violations, defaults to all
   *          applications
   * @param violationFilter A filter for violations, defaults to accept all violations.
   * @return A list of {@link PolicyViolationDTO}s for the provided application ids.
   * @throws BadRequestException Thrown if the list of application ids is null, empty, the stage type id is
   *           unknown, or the first element is an empty string.
   * @throws com.sonatype.insight.error.exception.NotFoundException Thrown if one of the provided application ids does
   *           not match an existing application.
   * @throws org.apache.shiro.authz.UnauthenticatedException Thrown if the user has not logged in.
   * @throws org.apache.shiro.authz.UnauthorizedException Thrown if the user is not authorized to read one of the
   *           applications provided.
   */
  List<PolicyViolationDTO> getPolicyViolationsByApplicationIds(Set<String> applicationIds,
                                                               @Nullable Set<String> stageTypeIds,
                                                               @Nullable Set<String> tagIds,
                                                               @Nullable Predicate<PolicyViolation> violationFilter)
  {
    Set<StageType> stages = dashboardUtils.getStageTypes(stageTypeIds);

    List<PolicyViolationDTO> policyViolationDTOs = new ArrayList<>();

    // The first item being an empty string occurs when someone GETs with a query parameter that has no value (i.e.
    // ?applicationIds&stageId=release).
    if (applicationIds == null || applicationIds.isEmpty() || applicationIds.iterator().next().isEmpty()) {
      throw new BadRequestException("Unable to get policy violations for null or empty application IDs.");
    }

    if (!CollectionUtils.isEmpty(tagIds)) {
      List<Application> filteredApplications = applicationDAO.getByIdsAndTagIds(applicationIds, tagIds);
      applicationIds = dashboardUtils.getApplicationIds(filteredApplications);
    }

    for (String applicationId : applicationIds) {
      // getPolicyViolationsByApplicationId is handling the read authentication for each application Id.
      policyViolationDTOs.addAll(getPolicyViolationsByApplicationId(applicationId, stages, violationFilter));
    }

    List<PolicyViolationDTO> sortedPolicyViolationDTOs = sort(policyViolationDTOs);
    return sortedPolicyViolationDTOs;
  }

  /**
   * @return Sort by threat level (descending), policy name, application name, coordinates, and then hashes.
   */
  private List<PolicyViolationDTO> sort(List<PolicyViolationDTO> dtos) {
    Collections.sort(dtos, POLICY_VIOLATION_DTO_COMPARATOR);
    return dtos;
  }

  private static class ComponentViolationRollUp
  {
    Map<String, PolicyViolationDTO> violationsByAppAndPolicyId = new LinkedHashMap<>();
    Set<String> applicationIds = new HashSet<>();

    void add(PolicyViolationDTO violation) {
      String id = violation.applicationId + "\t" + violation.policyId;
      PolicyViolationDTO existing = violationsByAppAndPolicyId.get(id);
      if (existing == null || existing.time < violation.time) {
        // count violations for a given app+policy combo only once, using the data from the most recent evaluation
        violationsByAppAndPolicyId.put(id, violation);
      }
      applicationIds.add(violation.applicationId);
    }

    public ComponentRiskDTO toDTO() {
      ComponentRiskDTO dto = new ComponentRiskDTO();
      dto.affectedApplications = applicationIds.size();
      for (PolicyViolationDTO violation : violationsByAppAndPolicyId.values()) {
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
