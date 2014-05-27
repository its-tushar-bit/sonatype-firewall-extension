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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dashboard.NewestRiskDTO.StageDetailDTO;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDigester;
import com.sonatype.insight.brain.security.AuditUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.codehaus.plexus.util.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class DashboardService
{
  private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

  private static final PolicyViolationDTOComparator POLICY_VIOLATION_DTO_COMPARATOR = new PolicyViolationDTOComparator();

  static final int NEWEST_RISK_TIME_RANGE_IN_DAYS = 30;

  private static final String SECRET_JOIN_STRING = "$";

  private static final Function<Application, String> applicationPublicIdSelector = new Function<Application, String>()
  {
    @Override
    public String apply(final Application application) {
      return application.getPublicId();
    }
  };

  protected static final Function<HasStringId, String> hasIdIdSelector = new Function<HasStringId, String>()
  {
    @Override
    public String apply(final HasStringId hasStringId) {
      return hasStringId.getId();
    }
  };

  private ApplicationDAO applicationDAO;

  private ApplicationComponentDAO applicationComponentDAO;

  private ApplicationService applicationService;

  private final PolicyDAO policyDAO;

  private PolicyEvaluationDAO policyEvaluationDAO;

  private PolicyViolationAdapter policyViolationAdapter;

  private PolicyViolationDAO policyViolationDAO;

  private StageTypeService stageTypeService;

  private DashboardFilterDAO dashboardFilterDAO;

  @Inject
  public DashboardService(ApplicationDAO applicationDAO, ApplicationComponentDAO applicationComponentDAO,
      ApplicationService applicationService, PolicyDAO policyDAO, PolicyEvaluationDAO policyEvaluationDAO,
      PolicyViolationAdapter policyViolationAdapter, PolicyViolationDAO policyViolationDAO,
      StageTypeService stageTypeService, DashboardFilterDAO dashboardFilterDAO)
  {
    this.applicationDAO = applicationDAO;
    this.applicationComponentDAO = applicationComponentDAO;
    this.applicationService = applicationService;
    this.policyDAO = policyDAO;
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
  private List<PolicyViolationDTO> getPolicyViolations(Set<String> applicationPublicIds, Set<String> stageIds,
      Set<String> tagIds, PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter, Integer maxResults)
  {
    Predicate<PolicyViolation> filter = buildViolationFilter(policyThreatCategoryFilter, policyThreatLevelFilter);

    if (applicationPublicIds == null || applicationPublicIds.isEmpty()) {
      return getPolicyViolations(stageIds, tagIds, filter, maxResults);
    }

    return getPolicyViolationsByApplicationIds(applicationPublicIds, stageIds, tagIds, filter, maxResults);
  }

  private Predicate<PolicyViolation> buildViolationFilter(PolicyThreatCategoryFilter threatCategoryFilter,
      PolicyThreatLevelFilter threatLevelFilter)
  {
    if (threatCategoryFilter == null && threatLevelFilter == null) {
      return null;
    }
    else if (threatCategoryFilter != null && threatLevelFilter != null) {
      return Predicates.and(threatCategoryFilter.asPolicyViolationPredicate(),
          threatLevelFilter.asPolicyViolationPredicate());
    }

    return (threatCategoryFilter != null) ? threatCategoryFilter.asPolicyViolationPredicate() : threatLevelFilter
        .asPolicyViolationPredicate();
  }

  /**
   * @param applicationPublicIds A list of application public ids to get policy violations.
   * @param stageTypeIds The stages to get policy violations for, defaults to {@link BuildStageType#ID}.
   * @param tagIds The tag ids to filter the applications for which to get policy violations, defaults to all applications
   * @param violationFilter A filter for violations, defaults to accept all violations.
   * @param limit If not null returns only the top violations limited by this amount.
   * @return A list of {@link PolicyViolationDTO}s for the provided application public ids.
   * @throws BadRequestException Thrown if the list of application public ids is null, empty, the stage type id is
   *           unknown, or the first element is an empty string.
   * @throws com.sonatype.insight.error.exception.NotFoundException Thrown if one of the provided application ids does
   *           not match an existing application.
   * @throws org.apache.shiro.authz.UnauthenticatedException Thrown if the user has not logged in.
   * @throws org.apache.shiro.authz.UnauthorizedException Thrown if the user is not authorized to read one of the
   *           applications provided.
   */
  List<PolicyViolationDTO> getPolicyViolationsByApplicationIds(Set<String> applicationPublicIds,
      @Nullable Set<String> stageTypeIds, @Nullable Set<String> tagIds,
      @Nullable Predicate<PolicyViolation> violationFilter, @Nullable Integer limit)
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
      List<Application> filteredApplications = applicationDAO.getByPublicIdsAndTagIds(applicationPublicIds, tagIds);
      applicationPublicIds = new HashSet<>(Lists.transform(filteredApplications, applicationPublicIdSelector));
    }

    for (String applicationPublicId : applicationPublicIds) {
      // getPolicyViolationsByApplicationId is handling the read authentication for each application public Id.
      policyViolationDTOs.addAll(getPolicyViolationsByApplicationId(applicationPublicId, stages, violationFilter));
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
   * @return A list of {@link PolicyViolationDTO}s for all applications with read permissions.
   * @throws BadRequestException Thrown if the stageTypeId does not match a known {@link StageType}.
   */
  List<PolicyViolationDTO> getPolicyViolations(@Nullable Set<String> stageTypeIds, @Nullable final Set<String> tagIds,
      @Nullable Predicate<PolicyViolation> violationFilter, @Nullable Integer limit)
  {
    Set<StageType> stages = getStageTypes(stageTypeIds);

    List<Application> applications = applicationService.getApplications();

    if (!CollectionUtils.isEmpty(tagIds)) {
      Set<String> applicationPublicIds = new HashSet<>(Lists.transform(applications, applicationPublicIdSelector));
      applications = applicationDAO.getByPublicIdsAndTagIds(applicationPublicIds, tagIds);
    }

    List<PolicyViolationDTO> policyViolationDTOs = new ArrayList<>();
    for (Application application : applications) {
      for (StageType stage : stages) {
        List<PolicyViolation> violations = getPolicyViolations(application.getId(), stage.getId());
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
   * @since 1.11.0
   */
  @Authorize(permission = Permission.READ)
  public List<ApplicationRiskScoreDTO> getApplicationRisks(
      @AuthzContext(value = AuthzContext.Key.APPLICATION_PUBLIC_ID, multiple = true) final Set<String> applicationPublicIds,
      final Set<String> stageIds, final Set<String> tagIds, final PolicyThreatCategoryFilter policyThreatCategoryFilter,
      final PolicyThreatLevelFilter policyThreatLevelFilter, final int maxResults)
  {
    List<Application> appsToSearch = applicationService
        .getApplicationsByPublicIdsAndTagIds(applicationPublicIds, tagIds);
    Set<StageType> stageTypes = getStageTypes(stageIds);
    Predicate<PolicyViolation> filter = buildViolationFilter(policyThreatCategoryFilter, policyThreatLevelFilter);

    List<PolicyEvaluation> evaluations = policyEvaluationDAO.getLastByApplicationIdsAndStageIds(
        Sets.newHashSet(Iterables.transform(appsToSearch, hasIdIdSelector)), getStageIds(stageTypes));

    Map<String, PolicyEvaluation> policyEvaluationsById = mapCollectionById(evaluations);
    List<PolicyViolationDTO> allPolicyViolationDTOs = createAllPolicyViolations(filter, evaluations, appsToSearch,
        policyEvaluationsById);

    Iterable<ApplicationRiskScoreDTO> applicationRisks = createApplicationRiskScores(appsToSearch, stageTypes,
        policyEvaluationsById, allPolicyViolationDTOs);

    List<ApplicationRiskScoreDTO> sortedApplicationRisks = sortAndFilterApplicationRiskScore(applicationRisks);
    return sortedApplicationRisks.subList(0, Math.min(sortedApplicationRisks.size(), maxResults));

  }

  private List<PolicyViolationDTO> createAllPolicyViolations(final Predicate<PolicyViolation> filter,
      final List<PolicyEvaluation> evaluations, final List<Application> applications,
      final Map<String, PolicyEvaluation> policyEvaluationsById)
  {
    Map<String, Application> applicationsById = mapCollectionById(applications);
    List<PolicyViolationDTO> allPolicyViolationDTOs = new ArrayList<>();
    for (PolicyViolation violation : getPolicyViolations(evaluations, filter)) {
      PolicyEvaluation sourceEvaluation = policyEvaluationsById.get(violation.getPolicyEvaluationId());
      Application sourceApplication = applicationsById.get(sourceEvaluation.getApplicationId());
      allPolicyViolationDTOs
          .addAll(policyViolationAdapter.createPolicyViolationDTOs(sourceApplication, Lists.newArrayList(violation)));
    }
    return allPolicyViolationDTOs;
  }

  private Iterable<ApplicationRiskScoreDTO> createApplicationRiskScores(final List<Application> appsToSearch,
      final Set<StageType> stagesToSearch, final Map<String, PolicyEvaluation> policyEvaluationsById,
      final List<PolicyViolationDTO> allPolicyViolationDTOs)
  {
    List<ApplicationRiskScoreDTO> applicationRiskScores = new ArrayList<>();
    for (final Application application : appsToSearch) {
      ApplicationRiskScoreDTO applicationRisk = new ApplicationRiskScoreDTO(application.getName(),
          application.getPublicId());

      Iterable<PolicyViolationDTO> violationsForApp = getViolationsForApp(allPolicyViolationDTOs, application);
      for (final StageType stage : stagesToSearch) {
        for (final PolicyViolationDTO violation : createViolationsForStage(stage.getId(), violationsForApp,
            policyEvaluationsById)) {
          PolicyEvaluation currentPolicyEvaluation = policyEvaluationsById.get(violation.policyEvaluationId);
          updateStageRisk(applicationRisk, violation, stage, currentPolicyEvaluation.getScanId());
        }
      }
      updateTotalApplicationRisks(applicationRisk, violationsForApp);
      applicationRiskScores.add(applicationRisk);
    }

    return applicationRiskScores;
  }

  private Iterable<PolicyViolationDTO> getViolationsForApp(final List<PolicyViolationDTO> allPolicyViolationDTOs,
      final Application application)
  {
    return Iterables.filter(allPolicyViolationDTOs,
        new Predicate<PolicyViolationDTO>()
        {
          @Override
          public boolean apply(@Nullable final PolicyViolationDTO violation) {
            return violation != null && application.getId().equals(violation.applicationId);
          }
        }
    );
  }

  private Iterable<PolicyViolationDTO> createViolationsForStage(final String stageId,
      final Iterable<PolicyViolationDTO> violationsForApp, final Map<String, PolicyEvaluation> policyEvaluationsById)
  {
    return Iterables
        .filter(violationsForApp, new Predicate<PolicyViolationDTO>()
        {
          @Override
          public boolean apply(@Nullable final PolicyViolationDTO violation) {

            if (violation == null) {
              return false;
            }
            final PolicyEvaluation policyEvaluation = policyEvaluationsById.get(violation.policyEvaluationId);
            return stageId.equals(policyEvaluation.getStageTypeId());
          }
        });
  }

  private <T extends HasStringId> Map<String, T> mapCollectionById(Collection<T> col) {
    Map<String, T> result = new HashMap<>();
    for (T item : col) {
      result.put(item.getId(), item);
    }
    return result;
  }

  private List<PolicyViolation> getPolicyViolations(final List<PolicyEvaluation> evaluations,
      final Predicate<PolicyViolation> violationFilter)
  {
    Set<String> evaluationIds = Sets.newHashSet(Iterables.transform(evaluations, hasIdIdSelector));
    return filter(policyViolationDAO.getByEvaluationIds(evaluationIds), violationFilter);
  }

  private void updateTotalApplicationRisks(final ApplicationRiskScoreDTO applicationRiskScore,
      final Iterable<PolicyViolationDTO> allViolations)
  {

    //squish down any dupes we have across stages
    final Map<String, PolicyViolationDTO> compHashToViolation = new HashMap<>();
    for (final PolicyViolationDTO violation1 : allViolations) {
      String vioHash = createUniqueHashForPolicy(violation1);
      PolicyViolationDTO existing = compHashToViolation.get(vioHash);
      if (existing == null) {
        //first time we see a violation, we make it
        compHashToViolation.put(vioHash, violation1);
      }
      else if (violation1.time > existing.time) {
        //we have a newer violation, update existing
        compHashToViolation.put(vioHash, violation1);
      }
    }

    //update the total risks based on the deduped risks
    for (final PolicyViolationDTO violation : compHashToViolation.values()) {
      updateRisk(applicationRiskScore.totalApplicationRisk, violation.threatLevel);
    }

  }

  private void updateStageRisk(ApplicationRiskScoreDTO applicationRiskScore, PolicyViolationDTO violation,
      StageType stage, String scanId)
  {

    StageRiskScoreDTO currentStageRiskScore = applicationRiskScore.getStageRiskScore(stage.getId());
    if (currentStageRiskScore == null) {
      currentStageRiskScore = new StageRiskScoreDTO(stage.getId());
      currentStageRiskScore.stageTypeName = stage.getName();
      currentStageRiskScore.scanId = scanId;
      applicationRiskScore.addStageRiskScore(currentStageRiskScore);
    }
    updateRisk(currentStageRiskScore.risk, violation.threatLevel);
  }

  private void updateRisk(RiskDTO risk, int threatLevel) {
    if (threatLevel >= 8) {
      risk.criticalRisk += threatLevel;
    }
    else if (threatLevel >= 4) {
      risk.severeRisk += threatLevel;
    }
    else if (threatLevel >= 2) {
      risk.moderateRisk += threatLevel;
    }
    else {
      risk.lowRisk += threatLevel;
    }
    risk.totalRisk += threatLevel;
  }


  private String createUniqueHashForPolicy(PolicyViolationDTO policyViolation) {
    return Joiner.on(SECRET_JOIN_STRING)
        .join(policyViolation.policyId, policyViolation.applicationId, policyViolation.hash);
  }

  /**
   * @param applicationRisks - Risks we want to sort.
   * @return the risks sorted in descending order by the Risk. Any guys with a Risk of 0 are removed.
   */
  private List<ApplicationRiskScoreDTO> sortAndFilterApplicationRiskScore(
      final Iterable<ApplicationRiskScoreDTO> applicationRisks)
  {
    List<ApplicationRiskScoreDTO> filteredApplicationRiskScores = Lists
        .newArrayList(Iterables.filter(applicationRisks, new Predicate<ApplicationRiskScoreDTO>()
        {

          @Override
          public boolean apply(@Nullable final ApplicationRiskScoreDTO input) {
            return input != null && input.totalApplicationRisk.totalRisk > 0;
          }
        }));
    Collections.sort(
        filteredApplicationRiskScores, new Comparator<ApplicationRiskScoreDTO>()
        {
          @Override
          public int compare(final ApplicationRiskScoreDTO o1, final ApplicationRiskScoreDTO o2) {
            int result = Integer.compare(o2.totalApplicationRisk.totalRisk, o1.totalApplicationRisk.totalRisk);
            if (result == 0) {
              result = o1.applicationId.compareTo(o2.applicationId);
            }
            return result;
          }
        }
    );
    return filteredApplicationRiskScores;
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
      @Nullable Predicate<PolicyViolation> violationFilter)
  {
    if (StringUtils.isBlank(applicationPublicId)) {
      throw new BadRequestException("Unable to get policy violations for null or empty application public id.");
    }

    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    List<PolicyViolationDTO> policyViolationDTOs = new ArrayList<>();
    for (StageType stage : stages) {
      List<PolicyViolation> violations = getPolicyViolations(application.getId(), stage.getId());
      violations = filter(violations, violationFilter);
      policyViolationDTOs.addAll(policyViolationAdapter.createPolicyViolationDTOs(application, violations));
    }

    return policyViolationDTOs;
  }

  private Set<StageType> getStageTypes(Set<String> stageTypeIds) {
    Collection<StageType> licensedStageTypes = stageTypeService.getLicensedStageTypes();

    Set<StageType> stages = new HashSet<>();

    if (stageTypeIds == null || stageTypeIds.isEmpty()) {
      for (StageType stageType : licensedStageTypes) {
        if (!StageTypes.isIgnoredForDashboard(stageType.getId())) {
          stages.add(stageType);
        }
      }
    }
    else {
      for (String stageTypeId : stageTypeIds) {
        StageType stage = StageTypes.getById(stageTypeId);
        if (stage == null || StageTypes.isIgnoredForDashboard(stage.getId())) {
          throw new BadRequestException("Invalid stage type: " + stageTypeId + ".");
        }
        else if (!licensedStageTypes.contains(stage)) {
          throw new BadRequestException("Current license does not support stage type: " + stageTypeId + ".");
        }

        stages.add(stage);
      }
    }

    return stages;
  }

  private Set<String> getStageIds(final Collection<StageType> stageTypes) {
    Set<String> stageIdsToSearch = new HashSet<>();
    for (StageType stageType : stageTypes) {
      stageIdsToSearch.add(stageType.getId());
    }
    return stageIdsToSearch;
  }

  private List<PolicyViolation> getPolicyViolations(String applicationId, String stageId) {
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
        policyThreatCategoryFilter, policyThreatLevelFilter, null);
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
    }
    else {
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

  /**
   * Calculates how many of the entities accessible to the current user are matched by the specified dashboard filter
   * settings.
   */
  public FilterSummaryDTO getFilterSummary(Set<String> applicationPublicIds, Set<String> stageIds, Set<String> tagIds,
      PolicyThreatCategoryFilter policyThreatCategoryFilter, PolicyThreatLevelFilter policyThreatLevelFilter)
  {
    long start = System.currentTimeMillis();

    FilterSummaryDTO summary = new FilterSummaryDTO();

    Collection<Application> readableApplications = applicationService.getApplications();
    summary.totalApplications = readableApplications.size();

    Collection<Application> matchedApplications = readableApplications;
    if (!CollectionUtils.isEmpty(applicationPublicIds) || !CollectionUtils.isEmpty(tagIds)) {
      Map<String, Application> appsByPublicId = Maps.newHashMapWithExpectedSize(readableApplications.size());
      for (Application app : readableApplications) {
        appsByPublicId.put(app.getPublicId(), app);
      }
      if (!CollectionUtils.isEmpty(applicationPublicIds)) {
        appsByPublicId.keySet().retainAll(applicationPublicIds);
      }
      if (!CollectionUtils.isEmpty(tagIds)) {
        matchedApplications = applicationDAO.getByPublicIdsAndTagIds(appsByPublicId.keySet(), tagIds);
      }
      else {
        matchedApplications = appsByPublicId.values();
      }
    }
    summary.matchedApplications = matchedApplications.size();

    Collection<StageType> allStageTypes = getStageTypes(null);
    summary.totalComponents = applicationComponentDAO.getUniqueCountByApplicationIdsAndStageTypeIds(
        Collections2.transform(readableApplications, hasIdIdSelector), getStageIds(allStageTypes));
    Collection<StageType> matchedStageTypes = getStageTypes(stageIds);
    summary.matchedComponents = applicationComponentDAO.getUniqueCountByApplicationIdsAndStageTypeIds(
        Collections2.transform(matchedApplications, hasIdIdSelector), getStageIds(matchedStageTypes));

    Set<String> readablePolicyOwnerIds = getPolicyOwnerIds(readableApplications);
    List<Policy> readablePolicies = policyDAO.getByOwnerIds(readablePolicyOwnerIds);
    summary.totalPolicies = readablePolicies.size();

    final Set<String> matchedPolicyOwnerIds = getPolicyOwnerIds(matchedApplications);
    Collection<Policy> matchedPolicies = Collections2.filter(readablePolicies, new Predicate<Policy>()
    {
      @Override
      public boolean apply(@Nullable Policy input) {
        return input != null && matchedPolicyOwnerIds.contains(input.getOwnerId());
      }
    });
    Predicate<Policy> policyFilter = buildPolicyFilter(policyThreatCategoryFilter, policyThreatLevelFilter);
    if (policyFilter != null) {
      matchedPolicies = Collections2.filter(matchedPolicies, policyFilter);
    }
    summary.matchedPolicies = matchedPolicies.size();

    log.debug("Calculated filter summary in {} ms", System.currentTimeMillis() - start);

    return summary;
  }

  private Set<String> getPolicyOwnerIds(Collection<Application> applications) {
    Set<String> policyOwnerIds = new HashSet<>(applications.size() * 2);
    for (Application app : applications) {
      policyOwnerIds.add(app.getId());
      policyOwnerIds.add(app.getOrganizationId());
    }
    return policyOwnerIds;
  }

  private Predicate<Policy> buildPolicyFilter(PolicyThreatCategoryFilter threatCategoryFilter,
      PolicyThreatLevelFilter threatLevelFilter)
  {
    if (threatCategoryFilter == null && threatLevelFilter == null) {
      return null;
    }
    else if (threatCategoryFilter != null && threatLevelFilter != null) {
      return Predicates.and(threatCategoryFilter.asPolicyPredicate(), threatLevelFilter.asPolicyPredicate());
    }

    return (threatCategoryFilter != null) ? threatCategoryFilter.asPolicyPredicate() : threatLevelFilter
        .asPolicyPredicate();
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
          dto.gavs.add(new GavDTO(violation.groupId, violation.artifactId, violation.version));
        }
        if (violation.pathnames != null) {
          dto.pathnames.addAll(violation.pathnames);
        }
      }
      return dto;
    }
  }

  /**
   * Gets the "newest" risk matching the specified filter criteria. Empty or null filter criteria generally means
   * "all available" violations for that aspect.
   */
  public List<NewestRiskDTO> getNewestRisks(Set<String> applicationPublicIds, Set<String> stageIds, Set<String> tagIds,
      PolicyThreatCategoryFilter policyThreatCategoryFilter, PolicyThreatLevelFilter policyThreatLevelFilter,
      int maxResults)
  {
    long start = System.currentTimeMillis();

    List<Application> applications = applicationService.getApplicationsByPublicIdsAndTagIds(applicationPublicIds,
        tagIds);
    Set<StageType> stageTypes = getStageTypes(stageIds);
    Predicate<PolicyViolation> filter = buildViolationFilter(policyThreatCategoryFilter, policyThreatLevelFilter);

    List<NewestRiskDTO> result = new ArrayList<>();

    for (Application app : applications) {
      List<PolicyViolation> allUniqueAppPolicyViolations = new ArrayList<>();
      Map<PolicyViolation, NewestRiskDTO> newestRiskDTOsByPolicyViolation = new HashMap<>();
      Map<String, PolicyEvaluation> policyEvaluationCache = new HashMap<>();

      for (StageType stageType : stageTypes) {
        List<PolicyViolation> policyViolations = policyViolationDAO.getNewestByApplicationIdAndStageTypeId(app.getId(),
            stageType.getId());
        policyViolations = filter(policyViolations, filter);
        PolicyViolationDiff diff = PolicyViolationDigester.digestPolicyViolations(policyViolations,
            allUniqueAppPolicyViolations);
        for (PolicyViolation policyViolation : diff.getAppeared()) {
          NewestRiskDTO newestRiskDTO = createNewestRiskDTO(app, stageType, policyViolation,
              getScanId(policyViolation, policyEvaluationCache));
          newestRiskDTOsByPolicyViolation.put(policyViolation, newestRiskDTO);
          result.add(newestRiskDTO);
        }
        for (Entry<PolicyViolation, PolicyViolation> samePolicyViolationEntry : diff.getSame().entrySet()) {
          NewestRiskDTO newestRiskDTO = newestRiskDTOsByPolicyViolation.get(samePolicyViolationEntry.getKey());
          PolicyViolation policyViolation = samePolicyViolationEntry.getValue();
          addToNewestRiskDTO(newestRiskDTO, stageType, policyViolation,
              getScanId(policyViolation, policyEvaluationCache));
        }

        allUniqueAppPolicyViolations.addAll(diff.getAppeared());
      }
      for (NewestRiskDTO newestRiskDTO : newestRiskDTOsByPolicyViolation.values()) {
        padStageDetails(newestRiskDTO);
      }
    }

    result = filter(result);
    Collections.sort(result, NewestRiskDTOComparator.INSTANCE);
    result.subList(Math.min(result.size(), maxResults), result.size()).clear();

    log.debug("getNewestRisks finished in {}", System.currentTimeMillis() - start);

    return result;
  }

  /**
   * Filters the given newestRiskDTOs to those that are newer than NEWEST_RISK_TIME_RANGE_IN_DAYS
   */
  private static List<NewestRiskDTO> filter(List<NewestRiskDTO> newestRiskDTOs) {
    List<NewestRiskDTO> filtered = new ArrayList<>();
    long filterFromTime = new DateTime().minusDays(NEWEST_RISK_TIME_RANGE_IN_DAYS).getMillis();
    for (NewestRiskDTO newestRiskDTO : newestRiskDTOs) {
      if (newestRiskDTO.time > filterFromTime) {
        filtered.add(newestRiskDTO);
      }
    }
    return filtered;
  }

  private void padStageDetails(final NewestRiskDTO newestRiskDTO) {
    List<String> seenStages = new ArrayList<>();
    for (StageDetailDTO stageDetail : newestRiskDTO.stageDetails) {
      seenStages.add(stageDetail.stageTypeId);
    }
    for (StageType stageType : StageTypes.getAll()) {
      if(!StageTypes.isIgnoredForDashboard(stageType.getId())){
        if(!seenStages.contains(stageType.getId())){
          StageDetailDTO emptyStageDetails = new StageDetailDTO();
          emptyStageDetails.stageTypeId = stageType.getId();
          newestRiskDTO.stageDetails.add(emptyStageDetails);
        }
      }
    }
  }

  private NewestRiskDTO createNewestRiskDTO(Application app, StageType stageType, PolicyViolation policyViolation,
      String scanId)
  {
    NewestRiskDTO newestRiskDTO = new NewestRiskDTO();
    newestRiskDTO.applicationPublicId = app.getPublicId();
    newestRiskDTO.applicationName = app.getName();
    newestRiskDTO.threatLevel = policyViolation.getThreatLevel();
    newestRiskDTO.time = policyViolation.getTime().getTime();
    newestRiskDTO.policyId = policyViolation.getPolicyId();
    newestRiskDTO.policyName = policyViolation.getPolicyName();
    newestRiskDTO.hash = policyViolation.getHash();
    if (policyViolation.getGroupId() != null) {
      newestRiskDTO.gav = new GavDTO(policyViolation.getGroupId(), policyViolation.getArtifactId(),
          policyViolation.getVersion());
    }
    newestRiskDTO.pathnames = policyViolation.getPathnames();

    StageDetailDTO stageDetailDTO = new StageDetailDTO();
    stageDetailDTO.stageTypeId = stageType.getId();
    stageDetailDTO.actionTypeId = policyViolation.getActionTypeId();
    stageDetailDTO.time = policyViolation.getTime().getTime();
    stageDetailDTO.scanId = scanId;
    newestRiskDTO.stageDetails.add(stageDetailDTO);

    return newestRiskDTO;
  }

  private void addToNewestRiskDTO(NewestRiskDTO newestRiskDTO, StageType stageType, PolicyViolation policyViolation,
      String scanId)
  {
    if (newestRiskDTO.time < policyViolation.getTime().getTime()) {
      newestRiskDTO.time = policyViolation.getTime().getTime();
      if (policyViolation.getGroupId() != null) {
        newestRiskDTO.gav = new GavDTO(policyViolation.getGroupId(), policyViolation.getArtifactId(),
            policyViolation.getVersion());
      }
      newestRiskDTO.pathnames = policyViolation.getPathnames();
    }

    StageDetailDTO stageDetailDTO = new StageDetailDTO();
    stageDetailDTO.stageTypeId = stageType.getId();
    stageDetailDTO.actionTypeId = policyViolation.getActionTypeId();
    stageDetailDTO.time = policyViolation.getTime().getTime();
    stageDetailDTO.scanId = scanId;
    newestRiskDTO.stageDetails.add(stageDetailDTO);
  }

  private String getScanId(PolicyViolation policyViolation, Map<String, PolicyEvaluation> policyEvaluationCache) {
    PolicyEvaluation policyEvaluation = policyEvaluationCache.get(policyViolation.getPolicyEvaluationId());
    if (policyEvaluation == null) {
      policyEvaluation = policyEvaluationDAO.getById(policyViolation.getPolicyEvaluationId());
      policyEvaluationCache.put(policyEvaluation.getId(), policyEvaluation);
    }
    return policyEvaluation.getScanId();
  }
}
