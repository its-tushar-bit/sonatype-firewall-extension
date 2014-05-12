/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.security.AuditUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.codehaus.plexus.util.StringUtils;

@Named
public class DashboardService
{

  private static final PolicyViolationDTOComparator POLICY_VIOLATION_DTO_COMPARATOR = new PolicyViolationDTOComparator();

  private static final int DEFAULT_NEWEST_POLICY_VIOLATION_TIME_RANGE = 30;

  private static final Function<Application, String> applicationPublicIdSelector = new Function<Application, String>()
  {
    @Override
    public String apply(final Application application) {
      return application.getPublicId();
    }
  };

  private ApplicationDAO applicationDAO;

  private ApplicationService applicationService;

  private PolicyEvaluationDAO policyEvaluationDAO;

  private PolicyViolationAdapter policyViolationAdapter;

  private PolicyViolationDAO policyViolationDAO;

  private StageTypeService stageTypeService;

  private DashboardFilterDAO dashboardFilterDAO;

  @Inject
  public DashboardService(ApplicationDAO applicationDAO, ApplicationService applicationService,
      PolicyEvaluationDAO policyEvaluationDAO, PolicyViolationAdapter policyViolationAdapter,
      PolicyViolationDAO policyViolationDAO, StageTypeService stageTypeService, DashboardFilterDAO dashboardFilterDAO)
  {
    this.applicationDAO = applicationDAO;
    this.applicationService = applicationService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyViolationAdapter = policyViolationAdapter;
    this.policyViolationDAO = policyViolationDAO;
    this.stageTypeService = stageTypeService;
    this.dashboardFilterDAO = dashboardFilterDAO;
  }

  /**
   * Gets the policy violations matching the specified filter criteria. Empty or null filter criteria generally mean
   * "all available" violations for that aspect.
   */
  public List<PolicyViolationDTO> getPolicyViolations(Set<String> applicationPublicIds, Set<String> stageIds,
      Set<String> tagIds, PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter, Integer maxResults, boolean newest)
  {
    Predicate<PolicyViolation> filter = buildFilter(policyThreatCategoryFilter, policyThreatLevelFilter);

    if (applicationPublicIds == null || applicationPublicIds.isEmpty()) {
      return getPolicyViolations(stageIds, tagIds, filter, maxResults, newest);
    }

    return getPolicyViolationsByApplicationIds(applicationPublicIds, stageIds, tagIds, filter, maxResults, newest);
  }

  private Predicate<PolicyViolation> buildFilter(PolicyThreatCategoryFilter threatCategoryFilter,
      PolicyThreatLevelFilter threatLevelFilter)
  {
    if (threatCategoryFilter == null && threatLevelFilter == null) {
      return null;
    }
    else if (threatCategoryFilter != null && threatLevelFilter != null) {
      return Predicates.and(threatCategoryFilter, threatLevelFilter);
    }

    return (threatCategoryFilter != null) ? threatCategoryFilter : threatLevelFilter;
  }

  /**
   * @param applicationPublicIds A list of application public ids to get policy violations.
   * @param stageTypeIds The stages to get policy violations for, defaults to {@link BuildStageType#ID}.
   * @param tagIds The tag ids to filter the applications for which to get policy violations, defaults to all applications
   * @param violationFilter A filter for violations, defaults to accept all violations.
   * @param limit If not null returns only the top violations limited by this amount.
   * @param newest A flag indicating that only newly observed violations should be returned.
   * @return A list of {@link PolicyViolationDTO}s for the provided application public ids.
   * @throws BadRequestException Thrown if the list of application public ids is null, empty, the stage type id is
   *           unknown, or the first element is an empty string.
   * @throws com.sonatype.insight.error.exception.NotFoundException Thrown if one of the provided application ids does
   *           not match an existing application.
   * @throws org.apache.shiro.authz.UnauthenticatedException Thrown if the user has not logged in.
   * @throws org.apache.shiro.authz.UnauthorizedException Thrown if the user is not authorized to read one of the
   *           applications provided.
   */
  public List<PolicyViolationDTO> getPolicyViolationsByApplicationIds(Set<String> applicationPublicIds,
      @Nullable Set<String> stageTypeIds, @Nullable Set<String> tagIds,
      @Nullable Predicate<PolicyViolation> violationFilter, @Nullable Integer limit, boolean newest)
  {
    Set<StageType> stages = getStageTypes(stageTypeIds);

    List<PolicyViolationDTO> policyViolationDTOs = new ArrayList<>();

    // The first item being an empty string occurs when someone GETs with a query parameter that has no value (i.e.
    // ?applicationPublicIds&stageId=release).
    if (applicationPublicIds == null || applicationPublicIds.isEmpty()
        || applicationPublicIds.iterator().next().isEmpty()) {
      throw new BadRequestException("Unable to get policy violations for null or empty application public IDs.");
    }

    if (!CollectionUtils.isEmpty(tagIds)) {
      List<Application> filteredApplications = applicationDAO.getByPublicIdsThatHaveTags(applicationPublicIds, tagIds);
      applicationPublicIds = new HashSet<>(Lists.transform(filteredApplications, applicationPublicIdSelector));
    }

    for (String applicationPublicId : applicationPublicIds) {
      // getPolicyViolationsByApplicationId is handling the read authentication for each application public Id.
      policyViolationDTOs.addAll(getPolicyViolationsByApplicationId(applicationPublicId, stages, violationFilter,
          newest));
    }

    List<PolicyViolationDTO> sortedPolicyViolationDTOs = sort(policyViolationDTOs);
    return limit != null ? Lists.newArrayList(Iterables.limit(sortedPolicyViolationDTOs, limit))
        : sortedPolicyViolationDTOs;
  }

  /**
   * @param stageTypeIds The stages to get policy violations for, defaults to {@link BuildStageType#ID}.
   * @param tagIds The tag ids to filter the applications for which to get policy violations, defaults to all applications
   * @param violationFilter A filter for violations, defaults to accept all violations.
   * @param limit If not null returns only the top violations limited by this amount.
   * @param newest A flag indicating that only newly observed violations should be returned.
   * @return A list of {@link PolicyViolationDTO}s for all applications with read permissions.
   * @throws BadRequestException Thrown if the stageTypeId does not match a known {@link StageType}.
   */
  public List<PolicyViolationDTO> getPolicyViolations(@Nullable Set<String> stageTypeIds,
      @Nullable final Set<String> tagIds, @Nullable Predicate<PolicyViolation> violationFilter, @Nullable Integer limit,
      boolean newest)
  {
    Set<StageType> stages = getStageTypes(stageTypeIds);

    List<Application> applications = applicationService.getApplications();

    if (!CollectionUtils.isEmpty(tagIds)) {
      Set<String> applicationPublicIds = new HashSet<>(Lists.transform(applications, applicationPublicIdSelector));
      applications = applicationDAO.getByPublicIdsThatHaveTags(applicationPublicIds, tagIds);
    }

    List<PolicyViolationDTO> policyViolationDTOs = new ArrayList<>();
    for (Application application : applications) {
      for (StageType stage : stages) {
        List<PolicyViolation> violations = getPolicyViolations(application.getId(), stage.getId(), newest);
        violations = filter(violations, violationFilter);

        policyViolationDTOs.addAll(policyViolationAdapter.createPolicyViolationDTOs(application, violations));
      }
    }

    List<PolicyViolationDTO> sortedPolicyViolationDTOs = sort(policyViolationDTOs);
    return limit != null ? Lists.newArrayList(Iterables.limit(sortedPolicyViolationDTOs, limit))
        : sortedPolicyViolationDTOs;
  }

  List<PolicyViolation> filter(List<PolicyViolation> violations, Predicate<PolicyViolation> violationFilter) {
    if (violationFilter == null || violations == null || violations.isEmpty()) {
      return violations;
    }

    return Lists.newArrayList(Collections2.filter(violations, violationFilter));
  }

  /**
   * @return Sort by threat level (descending), policy name, application name, coordinates, and then hashes.
   */
  private List<PolicyViolationDTO> sort(List<PolicyViolationDTO> dtos) {
    Collections.sort(dtos, POLICY_VIOLATION_DTO_COMPARATOR);
    return dtos;
  }

  @Authorize(permission = Permission.READ)
  protected List<PolicyViolationDTO> getPolicyViolationsByApplicationId(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId, Set<StageType> stages,
      @Nullable Predicate<PolicyViolation> violationFilter, boolean newest)
  {
    if (StringUtils.isBlank(applicationPublicId)) {
      throw new BadRequestException("Unable to get policy violations for null or empty application public id.");
    }

    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    List<PolicyViolationDTO> policyViolationDTOs = new ArrayList<>();
    for (StageType stage : stages) {
      List<PolicyViolation> violations = getPolicyViolations(application.getId(), stage.getId(), newest);
      violations = filter(violations, violationFilter);
      policyViolationDTOs.addAll(policyViolationAdapter.createPolicyViolationDTOs(application, violations));
    }

    return policyViolationDTOs;
  }

  private Set<StageType> getStageTypes(Set<String> stageTypeIds) {

    Collection<StageType> licensedStageTypes = stageTypeService.getLicensedStageTypes();

    if (stageTypeIds == null || stageTypeIds.isEmpty()) {
      return new HashSet<StageType>(licensedStageTypes);
    }

    Set<StageType> stages = new HashSet<>();
    for (String stageTypeId : stageTypeIds) {
      StageType stage = StageTypes.getById(stageTypeId);
      if (stage == null) {
        throw new BadRequestException("Unknown stage type: " + stageTypeId + ".");
      }
      else if (!licensedStageTypes.contains(stage)) {
        throw new BadRequestException("Current license does not support stage type: " + stageTypeId + ".");
      }

      stages.add(stage);
    }

    return stages;
  }

  private List<PolicyViolation> getPolicyViolations(String applicationId, String stageId, boolean newest) {
    if (newest) {
      return policyViolationDAO.getNewestByApplicationIdAndStageTypeIdAndLastNDays(applicationId, stageId,
          DEFAULT_NEWEST_POLICY_VIOLATION_TIME_RANGE);
    }

    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(applicationId, stageId);

    if (policyEvaluation == null) {
      return Lists.newArrayList();
    }

    return policyViolationDAO.getByEvaluationId(policyEvaluation.getId());
  }

  /**
   * Gets the risk per component by rolling up the policy violations matching the specified filter criteria. Empty or
   * null filter criteria generally mean "all available" violations for that aspect. The results are sorted by
   * descending component risk scores.
   */
  public List<ComponentRiskDTO> getComponentRisks(Set<String> applicationPublicIds, Set<String> stageIds,
      Set<String> tagIds, PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter, int maxResults)
  {
    List<PolicyViolationDTO> violations = getPolicyViolations(applicationPublicIds, stageIds, tagIds,
        policyThreatCategoryFilter, policyThreatLevelFilter, null, false);
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
    Collections.sort(dtos, ComponentRiskDTOComparator.INSTANCE);
    dtos.subList(Math.min(dtos.size(), maxResults), dtos.size()).clear();
    return dtos;
  }

  /**
   * @since 1.11.0
   */
  public DashboardFilterDTO getDashboardFilterForCurrentUser() throws IOException {
    String username = AuditUtils.findUser();
    DashboardFilter dashboardFilter = dashboardFilterDAO.getByUsername(username);
    if (dashboardFilter == null) {
      return null;
    }
    return JsonUtils.parse(dashboardFilter.getFilter(), DashboardFilterDTO.class);
  }

  /**
   * @since 1.11.0
   */
  public DashboardFilterDTO createOrUpdateDashboardFilterForCurrentUser(DashboardFilterDTO dashboardFilterDTO) {
    DashboardFilter dashboardFilter = new DashboardFilter();
    dashboardFilter.setUsername(AuditUtils.findUser());
    dashboardFilter.setFilter(JsonUtils.format(dashboardFilterDTO));

    DashboardFilter existingDashboardFilter = dashboardFilterDAO.getByUsername(AuditUtils.findUser());
    if (existingDashboardFilter == null) {
      dashboardFilterDAO.insert(dashboardFilter);
    } else {
      dashboardFilter.setId(existingDashboardFilter.getId());
      dashboardFilterDAO.update(dashboardFilter);
    }

    return dashboardFilterDTO;
  }

  /**
   * @since 1.11.0
   */
  public void deleteDashboardFilterForCurrentUser() {
    String username = AuditUtils.findUser();
    DashboardFilter dashboardFilter = dashboardFilterDAO.getByUsername(username);
    if (dashboardFilter != null) {
      dashboardFilterDAO.delete(dashboardFilter);
    }
  }

  private static class ComponentViolationRollUp
  {
    Map<String, PolicyViolationDTO> violationsByAppAndPolicyId = new LinkedHashMap<>();

    void add(PolicyViolationDTO violation) {
      String id = violation.applicationId + "\t" + violation.policyId;
      PolicyViolationDTO existing = violationsByAppAndPolicyId.get(id);
      if (existing == null || existing.time < violation.time) {
        // count violations for a given app+policy combo only once, using the data from the most recent evaluation
        violationsByAppAndPolicyId.put(id, violation);
      }
    }

    public ComponentRiskDTO toDTO() {
      ComponentRiskDTO dto = new ComponentRiskDTO();
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
        if (StringUtils.isNotEmpty(violation.groupId)) {
          dto.gavs.add(new ComponentRiskDTO.GavDTO(violation.groupId, violation.artifactId, violation.version));
        }
        if (violation.pathnames != null) {
          dto.pathnames.addAll(violation.pathnames);
        }
      }
      return dto;
    }
  }
}
