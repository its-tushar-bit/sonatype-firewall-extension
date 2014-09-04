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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationAdapter;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.organization.ContactDTO;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDigester;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
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

  static final int POLICY_SUMMARY_WEEKS = 12;

  private static final long ONE_WEEK_IN_MILLISECS = 7L * 24 * 3600 * 1000;

  private static final String SECRET_JOIN_STRING = "$";

  protected static final Function<HasStringId, String> hasIdIdSelector = new Function<HasStringId, String>()
  {
    @Override
    public String apply(final HasStringId hasStringId) {
      return hasStringId.getId();
    }
  };

  private final ApplicationDAO applicationDAO;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final ApplicationService applicationService;

  private final PolicyDAO policyDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyViolationAdapter policyViolationAdapter;

  private final PolicyViolationDAO policyViolationDAO;

  private final StageTypeService stageTypeService;

  private final DashboardFilterDAO dashboardFilterDAO;

  private final CurrentUser currentUser;
  
  private final ApplicationAdapter applicationAdapter;

  private final CLMLicenseManager licenseManager;

  @Inject
  public DashboardService(ApplicationDAO applicationDAO, ApplicationComponentDAO applicationComponentDAO,
      ApplicationService applicationService, PolicyDAO policyDAO, PolicyEvaluationDAO policyEvaluationDAO,
      PolicyViolationAdapter policyViolationAdapter, PolicyViolationDAO policyViolationDAO,
      StageTypeService stageTypeService, DashboardFilterDAO dashboardFilterDAO, CurrentUser currentUser,
      ApplicationAdapter applicationAdapter, CLMLicenseManager licenseManager)
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
    this.currentUser = currentUser;
    this.applicationAdapter = applicationAdapter;
    this.licenseManager = licenseManager;
  }

  /**
   * Gets the policy violations matching the specified filter criteria. Empty or null filter criteria generally mean
   * "all available" violations for that aspect.
   */
  private List<PolicyViolationDTO> getPolicyViolations(Set<String> applicationIds, Set<String> stageIds,
      Set<String> tagIds, PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter)
  {
    Predicate<PolicyViolation> filter = buildViolationFilter(policyThreatCategoryFilter, policyThreatLevelFilter);

    if (applicationIds == null || applicationIds.isEmpty()) {
      return getPolicyViolations(stageIds, tagIds, filter);
    }

    return getPolicyViolationsByApplicationIds(applicationIds, stageIds, tagIds, filter);
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
   * @param applicationIds A list of application ids to get policy violations.
   * @param stageTypeIds The stages to get policy violations for, defaults to {@link BuildStageType#ID}.
   * @param tagIds The tag ids to filter the applications for which to get policy violations, defaults to all applications
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
      @Nullable Set<String> stageTypeIds, @Nullable Set<String> tagIds,
      @Nullable Predicate<PolicyViolation> violationFilter)
  {
    Set<StageType> stages = getStageTypes(stageTypeIds);

    List<PolicyViolationDTO> policyViolationDTOs = new ArrayList<>();

    // The first item being an empty string occurs when someone GETs with a query parameter that has no value (i.e.
    // ?applicationIds&stageId=release).
    if (applicationIds == null || applicationIds.isEmpty() || applicationIds.iterator().next().isEmpty()) {
      throw new BadRequestException("Unable to get policy violations for null or empty application IDs.");
    }

    if (!CollectionUtils.isEmpty(tagIds)) {
      List<Application> filteredApplications = applicationDAO.getByIdsAndTagIds(applicationIds, tagIds);
      applicationIds = new HashSet<>(Lists.transform(filteredApplications, hasIdIdSelector));
    }

    for (String applicationId : applicationIds) {
      // getPolicyViolationsByApplicationId is handling the read authentication for each application Id.
      policyViolationDTOs.addAll(getPolicyViolationsByApplicationId(applicationId, stages, violationFilter));
    }

    List<PolicyViolationDTO> sortedPolicyViolationDTOs = sort(policyViolationDTOs);
    return sortedPolicyViolationDTOs;
  }

  /**
   * @param stageTypeIds The stages to get policy violations for, defaults to {@link BuildStageType#ID}.
   * @param tagIds The tag ids to filter the applications for which to get policy violations, defaults to all applications
   * @param violationFilter A filter for violations, defaults to accept all violations.
   * @return A list of {@link PolicyViolationDTO}s for all applications with read permissions.
   * @throws BadRequestException Thrown if the stageTypeId does not match a known {@link StageType}.
   */
  List<PolicyViolationDTO> getPolicyViolations(@Nullable final Set<String> stageTypeIds,
      @Nullable final Set<String> tagIds, @Nullable final Predicate<PolicyViolation> violationFilter)
  {

    List<Application> applications = applicationService.getApplications();
    final Set<StageType> stageTypes = getStageTypes(stageTypeIds);
    final Set<String> stageTypeIdsFiltered = getStageIds(stageTypes);

    if (!CollectionUtils.isEmpty(tagIds)) {
      Set<String> applicationIds = new HashSet<>(Lists.transform(applications, hasIdIdSelector));
      applications = applicationDAO.getByIdsAndTagIds(applicationIds, tagIds);
    }

    final ImmutableMap<String, Application> applicationIdLookupMap = Maps.uniqueIndex(applications, hasIdIdSelector);
    final List<PolicyEvaluation> latestEvaluations = policyEvaluationDAO
        .getLastByApplicationIdsAndStageIds(applicationIdLookupMap.keySet(), stageTypeIdsFiltered);

    final ImmutableMap<String, PolicyEvaluation> evaluationIdLookupMap = Maps.uniqueIndex(latestEvaluations,
        hasIdIdSelector);
    final List<PolicyViolation> violations = filter(policyViolationDAO.getActiveByEvaluationIds(
        Sets.newHashSet(Lists.transform(latestEvaluations, hasIdIdSelector))), violationFilter);


    final List<PolicyViolationDTO> result = Lists.newArrayList(Lists
        .transform(violations, new Function<PolicyViolation, PolicyViolationDTO>()
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
      @AuthzContext(value = AuthzContext.Key.APPLICATION_ID, multiple = true) final Set<String> applicationIds,
      final Set<String> stageIds, final Set<String> tagIds, final PolicyThreatCategoryFilter policyThreatCategoryFilter,
      final PolicyThreatLevelFilter policyThreatLevelFilter, final int maxResults)
  {
    validateDashboardLicensed();

    long start = System.currentTimeMillis();

    List<Application> appsToSearch = applicationService.getApplicationsByIdsAndTagIds(applicationIds, tagIds);
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

    List<ApplicationRiskScoreDTO> result = sortedApplicationRisks.subList(0,
        Math.min(sortedApplicationRisks.size(), maxResults));

    log.debug("getApplicationRisks finished in {}", System.currentTimeMillis() - start);

    return result;
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
    ContactDTO[] contactsForApplications = findContactsForApplications(appsToSearch);
    for (int i = 0; i < appsToSearch.size(); i++) {
      Application application = appsToSearch.get(i);
      ContactDTO contactDTO = (i < contactsForApplications.length) ? contactsForApplications[i] : null;
      ApplicationRiskScoreDTO applicationRisk = new ApplicationRiskScoreDTO(application.getName(),
          application.getPublicId(), contactDTO);

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

  private ContactDTO[] findContactsForApplications(final List<Application> applications) {
    List<String> contactNames = Lists.newArrayList(Iterables.filter(
        Iterables.transform(applications, new Function<Application, String>()
        {
          @Nullable
          @Override
          public String apply(@Nullable final Application application) {
            return (application == null || application.getContactInternalName() == null) ? null : application
                .getContactInternalName();
          }
        }), Predicates.notNull()));

    //still kind of hazy what happens down in LDAP land if we get nulls for some of the elements,
    //we may have to deal with that
    return applicationAdapter.getContacts(contactNames);
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
    return filter(policyViolationDAO.getActiveByEvaluationIds(evaluationIds), violationFilter);
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
    return Joiner.on(SECRET_JOIN_STRING).useForNull("")
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
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId, Set<StageType> stages,
      @Nullable Predicate<PolicyViolation> violationFilter)
  {
    if (StringUtils.isBlank(applicationId)) {
      throw new BadRequestException("Unable to get policy violations for null or empty application ID.");
    }

    Application application = applicationDAO.getByIdNotNull(applicationId);

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

    if (stageTypeIds == null) {
      stageTypeIds = Collections.emptySet();
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
      }
    }

    Set<StageType> stages = new LinkedHashSet<>();

    for (StageType stageType : licensedStageTypes) {
      if (!StageTypes.isIgnoredForDashboard(stageType.getId())
          && (stageTypeIds.isEmpty() || stageTypeIds.contains(stageType.getId()))) {
        stages.add(stageType);
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

    return policyViolationDAO.getActiveByEvaluationId(policyEvaluation.getId());
  }

  /**
   * Gets the risk per component by rolling up the policy violations matching the specified filter criteria. Empty or
   * null filter criteria generally mean "all available" violations for that aspect. The results are sorted by
   * descending component risk scores.
   */
  public List<ComponentRiskDTO> getComponentRisks(Set<String> applicationIds, Set<String> stageIds,
      Set<String> tagIds, PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter, int maxResults)
  {
    validateDashboardLicensed();

    long start = System.currentTimeMillis();

    List<PolicyViolationDTO> violations = getPolicyViolations(applicationIds, stageIds, tagIds,
        policyThreatCategoryFilter, policyThreatLevelFilter);
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

    log.debug("getComponentRisks finished in {}", System.currentTimeMillis() - start);

    return dtos;
  }

  /**
   * @since 1.11.0
   */
  public DashboardFilterDTO getDashboardFilterForCurrentUser() throws IOException {
    validateDashboardLicensed();

    String username = currentUser.getUsername();
    DashboardFilter dashboardFilter = dashboardFilterDAO.getByUsername(username);
    if (dashboardFilter == null) {
      return createDefaultDashboardFilterForCurrentUser();
    }
    DashboardFilterDTO dto = JsonUtils.parse(dashboardFilter.getFilter(), DashboardFilterDTO.class);

    //prune out any unauthorized applications
    pruneUnauthorizedApplicationIds(dto);
    return dto;
  }

  protected void pruneUnauthorizedApplicationIds(DashboardFilterDTO dto) {
    List<Application> apps = getApplicationsByIds(dto.applicationFilters);
    dto.applicationFilters.clear();
    for (Application app : apps) {
      dto.applicationFilters.add(app.getId());
    }
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  protected List<Application> getApplicationsByIds(final List<String> applicationIds) {
    return applicationDAO.getByIds(new LinkedHashSet<String>(applicationIds));
  }

  private DashboardFilterDTO createDefaultDashboardFilterForCurrentUser(){
    DashboardFilterDTO dashboardFilterDTO = new DashboardFilterDTO();
    dashboardFilterDTO.applicationFilters = new ArrayList<>();
    //Threat levels of 0 or 1 are intended to be informational only, and therefore are
    //not pertinent to assessing the "real" risk of a given Application or component
    dashboardFilterDTO.minPolicyThreatLevel = 2;
    dashboardFilterDTO.maxPolicyThreatLevel = 10;
    dashboardFilterDTO.stageTypeFilters = new ArrayList<>();
    dashboardFilterDTO.policyThreatCategoryFilters = new ArrayList<>();
    dashboardFilterDTO.tagFilters = new ArrayList<>();
    return createOrUpdateDashboardFilterForCurrentUser(dashboardFilterDTO);
  }

  /**
   * @since 1.11.0
   */
  public DashboardFilterDTO createOrUpdateDashboardFilterForCurrentUser(DashboardFilterDTO dashboardFilterDTO) {
    validateDashboardLicensed();

    String username = currentUser.getUsername();
    DashboardFilter dashboardFilter = new DashboardFilter();
    dashboardFilter.setUsername(username);
    dashboardFilter.setFilter(JsonUtils.format(dashboardFilterDTO));

    DashboardFilter existingDashboardFilter = dashboardFilterDAO.getByUsername(username);
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
    validateDashboardLicensed();

    String username = currentUser.getUsername();
    DashboardFilter dashboardFilter = dashboardFilterDAO.getByUsername(username);
    if (dashboardFilter != null) {
      dashboardFilterDAO.delete(dashboardFilter);
    }
  }

  /**
   * Calculates how many of the entities accessible to the current user are matched by the specified dashboard filter
   * settings.
   */
  public FilterSummaryDTO getFilterSummary(Set<String> applicationIds, Set<String> stageIds, Set<String> tagIds,
      PolicyThreatCategoryFilter policyThreatCategoryFilter, PolicyThreatLevelFilter policyThreatLevelFilter)
  {
    validateDashboardLicensed();

    long start = System.currentTimeMillis();

    FilterSummaryDTO summary = new FilterSummaryDTO();

    Collection<Application> readableApplications = applicationService.getApplications();
    summary.totalApplications = readableApplications.size();

    Collection<Application> matchedApplications = readableApplications;
    if (!CollectionUtils.isEmpty(applicationIds) || !CollectionUtils.isEmpty(tagIds)) {
      Map<String, Application> appsById = Maps.newHashMapWithExpectedSize(readableApplications.size());
      for (Application app : readableApplications) {
        appsById.put(app.getId(), app);
      }
      if (!CollectionUtils.isEmpty(applicationIds)) {
        appsById.keySet().retainAll(applicationIds);
      }
      if (!CollectionUtils.isEmpty(tagIds)) {
        matchedApplications = applicationDAO.getByIdsAndTagIds(appsById.keySet(), tagIds);
      }
      else {
        matchedApplications = appsById.values();
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
        GavDTO gav = GavDTO.from(violation.groupId, violation.artifactId, violation.version);
        if (gav != null) {
          dto.gavs.add(gav);
        }
        if (violation.pathnames != null) {
          dto.pathnames.addAll(violation.pathnames);
        }
      }
      return dto;
    }
  }

  private Map<PolicyViolation, PolicyViolation> getFirstOccurrencePolicyViolationsForLastPolicyViolations(
      String appId, String stageTypeId, List<PolicyViolation> lastPolicyViolations)
  {
    Map<PolicyViolation, PolicyViolation> result = new LinkedHashMap<>();

    List<PolicyViolation> firstOccurrences = policyViolationDAO.getFirstOccurrenceByApplicationIdAndStageTypeId(appId,
        stageTypeId);
    PolicyViolationDiff diff = PolicyViolationDigester.digestPolicyViolations(lastPolicyViolations, firstOccurrences);
    for (Entry<PolicyViolation, PolicyViolation> samePolicyViolationEntry : diff.getSame().entrySet()) {
      result.put(samePolicyViolationEntry.getKey(), samePolicyViolationEntry.getValue());
    }
    for (PolicyViolation policyViolation : diff.getCleared()) {
      PolicyViolation firstOccurrence = policyViolationDAO.getFirstOccurrence(appId, stageTypeId, policyViolation);
      result.put(policyViolation, firstOccurrence);
    }

    return result;
  }

  /**
   * Gets the "newest" risk matching the specified filter criteria. Empty or null filter criteria generally means
   * "all available" violations for that aspect.
   */
  public List<NewestRiskDTO> getNewestRisks(Set<String> applicationIds, Set<String> stageIds, Set<String> tagIds,
      PolicyThreatCategoryFilter policyThreatCategoryFilter, PolicyThreatLevelFilter policyThreatLevelFilter,
      int maxResults)
  {
    validateDashboardLicensed();

    long start = System.currentTimeMillis();

    List<Application> applications = applicationService.getApplicationsByIdsAndTagIds(applicationIds, tagIds);
    Set<StageType> stageTypes = getStageTypes(stageIds);
    Predicate<PolicyViolation> filter = buildViolationFilter(policyThreatCategoryFilter, policyThreatLevelFilter);

    List<NewestRiskDTO> result = new ArrayList<>();

    for (Application app : applications) {
      List<PolicyViolation> allUniqueAppPolicyViolations = new ArrayList<>();
      Map<PolicyViolation, NewestRiskDTO> newestRiskDTOsByPolicyViolation = new HashMap<>();
      Map<String, PolicyEvaluation> policyEvaluationCache = new HashMap<>();

      for (StageType stageType : stageTypes) {
        PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(),
            stageType.getId());
        if (policyEvaluation == null) {
          continue;
        }

        List<PolicyViolation> policyViolations = policyViolationDAO.getActiveByEvaluationId(policyEvaluation.getId());
        policyViolations = filter(policyViolations, filter);
        if (policyViolations.isEmpty()) {
          continue;
        }

        Map<PolicyViolation, PolicyViolation> firstOccurrencePolicyViolationsByLastPolicyViolations = getFirstOccurrencePolicyViolationsForLastPolicyViolations(
            app.getId(), stageType.getId(), policyViolations);

        PolicyViolationDiff diff = PolicyViolationDigester.digestPolicyViolations(allUniqueAppPolicyViolations,
            policyViolations);
        for (PolicyViolation policyViolation : diff.getAppeared()) {
          PolicyViolation firstOccurrencePolicyViolation = firstOccurrencePolicyViolationsByLastPolicyViolations
              .get(policyViolation);
          NewestRiskDTO newestRiskDTO = createNewestRiskDTO(app, stageType, policyViolation,
              firstOccurrencePolicyViolation.getTime().getTime(), getScanId(policyViolation, policyEvaluationCache));
          newestRiskDTOsByPolicyViolation.put(policyViolation, newestRiskDTO);
          result.add(newestRiskDTO);
        }
        for (Entry<PolicyViolation, PolicyViolation> samePolicyViolationEntry : diff.getSame().entrySet()) {
          NewestRiskDTO newestRiskDTO = newestRiskDTOsByPolicyViolation.get(samePolicyViolationEntry.getKey());
          PolicyViolation policyViolation = samePolicyViolationEntry.getValue();
          PolicyViolation firstOccurrencePolicyViolation = firstOccurrencePolicyViolationsByLastPolicyViolations
              .get(policyViolation);
          addToNewestRiskDTO(newestRiskDTO, stageType, policyViolation, firstOccurrencePolicyViolation.getTime()
              .getTime(), getScanId(policyViolation, policyEvaluationCache));
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

  /**
   * Add an 'empty' record for each missing stage we need to show in the UI.
   */
  private void padStageDetails(final NewestRiskDTO newestRiskDTO) {
    Set<String> seenStages = new HashSet<>();
    for (StageDetailDTO stageDetail : newestRiskDTO.stageDetails) {
      seenStages.add(stageDetail.stageTypeId);
    }
    for (StageType stageType : getStageTypes(null)) {
      if (!seenStages.contains(stageType.getId())) {
        StageDetailDTO emptyStageDetails = new StageDetailDTO();
        emptyStageDetails.stageTypeId = stageType.getId();
        newestRiskDTO.stageDetails.add(emptyStageDetails);
      }
    }
  }

  private NewestRiskDTO createNewestRiskDTO(Application app, StageType stageType, PolicyViolation policyViolation,
      long time, String scanId)
  {
    NewestRiskDTO newestRiskDTO = new NewestRiskDTO();
    newestRiskDTO.applicationPublicId = app.getPublicId();
    newestRiskDTO.applicationName = app.getName();
    newestRiskDTO.threatLevel = policyViolation.getThreatLevel();
    newestRiskDTO.time = time;
    newestRiskDTO.policyId = policyViolation.getPolicyId();
    newestRiskDTO.policyName = policyViolation.getPolicyName();
    newestRiskDTO.hash = policyViolation.getHash();
    newestRiskDTO.gav = GavDTO.from(policyViolation);
    newestRiskDTO.pathnames = policyViolation.getPathnames();

    StageDetailDTO stageDetailDTO = new StageDetailDTO();
    stageDetailDTO.stageTypeId = stageType.getId();
    stageDetailDTO.actionTypeId = policyViolation.getActionTypeId();
    stageDetailDTO.time = time;
    stageDetailDTO.scanId = scanId;
    newestRiskDTO.stageDetails.add(stageDetailDTO);

    return newestRiskDTO;
  }

  private void addToNewestRiskDTO(NewestRiskDTO newestRiskDTO, StageType stageType, PolicyViolation policyViolation,
      long time, String scanId)
  {
    if (newestRiskDTO.time < policyViolation.getTime().getTime()) {
      newestRiskDTO.gav = GavDTO.from(policyViolation);
      newestRiskDTO.pathnames = policyViolation.getPathnames();
    }

    if (newestRiskDTO.time < time) {
      newestRiskDTO.time = time;
    }

    StageDetailDTO stageDetailDTO = new StageDetailDTO();
    stageDetailDTO.stageTypeId = stageType.getId();
    stageDetailDTO.actionTypeId = policyViolation.getActionTypeId();
    stageDetailDTO.time = time;
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

  /**
   * Calculates how the non-proprietary components within the matched applications and stages are distributed across the
   * various match states.
   */
  public ComponentSummaryDTO getComponentSummary(Set<String> applicationIds, Set<String> stageIds,
      Set<String> tagIds)
  {
    validateDashboardLicensed();

    long start = System.currentTimeMillis();

    ComponentSummaryDTO summary = new ComponentSummaryDTO();

    Collection<String> appIds = Collections2.transform(
        applicationService.getApplicationsByIdsAndTagIds(applicationIds, tagIds), hasIdIdSelector);
    Collection<String> stageTypeIds = getStageIds(getStageTypes(stageIds));
    List<ApplicationComponent> components = applicationComponentDAO.getNonProprietaryByApplicationIdsAndStageTypeIds(
        appIds, stageTypeIds);
    Map<String, ApplicationComponent> componentsByHash = new HashMap<>();
    for (ApplicationComponent component : components) {
      String hash = component.getHash();
      ApplicationComponent other = componentsByHash.get(hash);
      if (other == null || other.getTime().getTime() < component.getTime().getTime()) {
        componentsByHash.put(hash, component);
      }
    }

    summary.total = componentsByHash.size();
    for (ApplicationComponent component : componentsByHash.values()) {
      String matchState = component.getMatchStateId();
      if (MatchState.EXACT.getId().equals(matchState)) {
        summary.exact++;
      }
      else if (MatchState.SIMILAR.getId().equals(matchState)) {
        summary.similar++;
      }
      else if (MatchState.UNKNOWN.getId().equals(matchState)) {
        summary.unknown++;
      }
      else {
        throw new IllegalStateException("unknown match state: " + matchState);
      }
    }

    log.debug("Calculated component summary in {} ms", System.currentTimeMillis() - start);

    return summary;
  }

  private static class PolicyViolationHistory
  {
    private static class Data
    {
      private Set<String> stageTypeIds = new LinkedHashSet<>();

      private Date firstOccurrenceTime;
    }

    private Map<PolicyViolation, Data> dataByViolation = new LinkedHashMap<>();

    void addViolationWithStageType(PolicyViolation policyViolation, String stageTypeId) {
      Data data = new Data();
      data.stageTypeIds.add(stageTypeId);
      data.firstOccurrenceTime = policyViolation.getTime();
      dataByViolation.put(policyViolation, data);
    }

    void addStageTypeToViolation(PolicyViolation policyViolation, String stageTypeId) {
      dataByViolation.get(policyViolation).stageTypeIds.add(stageTypeId);
    }

    /**
     * Returns true if this operation clears all stage types for the given policy violation, which effectively means
     * that the policy violation was fixed for all stages.
     */
    boolean removeStageTypeFromViolation(PolicyViolation policyViolation, String stageTypeId) {
      Data data = dataByViolation.get(policyViolation);
      data.stageTypeIds.remove(stageTypeId);
      if (data.stageTypeIds.isEmpty()) {
        dataByViolation.remove(policyViolation);
        return true;
      }
      return false;
    }

    void replacePolicyViolation(PolicyViolation oldViolation, PolicyViolation newViolation) {
      Data data = dataByViolation.remove(oldViolation);
      dataByViolation.put(newViolation, data);
    }

    Collection<PolicyViolation> getPolicyViolations() {
      return Collections.unmodifiableCollection(dataByViolation.keySet());
    }

    Date getPolicyViolationFirstOccurrenceTime(PolicyViolation policyViolation) {
      return dataByViolation.get(policyViolation).firstOccurrenceTime;
    }
  }

  private void addWeekViolation(int weekIndex, List<Integer> weeklyDeltas) {
    addWeekViolation(weekIndex, weeklyDeltas, 1);
  }

  private void addWeekViolation(int weekIndex, List<Integer> weeklyDeltas, int delta) {
    if (weekIndex >= 0) {
      weeklyDeltas.set(weekIndex, weeklyDeltas.get(weekIndex) + delta);
    }
  }

  private int getPolicySummaryWeekFromTime(long now, long time) {
    return POLICY_SUMMARY_WEEKS - (int) ((now - time) / ONE_WEEK_IN_MILLISECS) - 1;
  }

  /**
   * Gets the policy summary matching the specified filter criteria. Empty or null filter criteria generally means
   * "all available" violations for that aspect.
   */
  public PolicySummaryDTO getPolicySummary(Set<String> applicationIds, Set<String> stageIds, Set<String> tagIds,
      PolicyThreatCategoryFilter policyThreatCategoryFilter, PolicyThreatLevelFilter policyThreatLevelFilter)
  {
    validateDashboardLicensed();

    long start = System.currentTimeMillis();

    List<Application> applications = applicationService.getApplicationsByIdsAndTagIds(applicationIds, tagIds);
    Set<StageType> stageTypes = getStageTypes(stageIds);
    Set<String> stageTypeIds = getStageIds(stageTypes);
    Predicate<PolicyViolation> filter = buildViolationFilter(policyThreatCategoryFilter, policyThreatLevelFilter);
    Long now = System.currentTimeMillis();

    PolicySummaryDTO result = new PolicySummaryDTO();
    result.timestamp = now;
    for (int iWeek = 0; iWeek < POLICY_SUMMARY_WEEKS; iWeek++) {
      result.weeklyDeltaNew.add(0);
      result.weeklyDeltaWaived.add(0);
      result.weeklyDeltaFixed.add(0);
    }

    DescriptiveStatistics ageWaivedStatistics = new DescriptiveStatistics();
    DescriptiveStatistics ageFixedStatistics = new DescriptiveStatistics();
    DescriptiveStatistics ageUnresolvedStatistics = new DescriptiveStatistics();
    for (Application app : applications) {
      PolicyViolationHistory policyViolationHistory = new PolicyViolationHistory();

      List<PolicyEvaluation> policyEvaluations = policyEvaluationDAO.getByApplicationIdAndStageIds(app.getId(),
          stageTypeIds);
      for (PolicyEvaluation policyEvaluation : policyEvaluations) {
        if (policyEvaluation.getTime().getTime() > now) {
          // This policy evaluation is after we started calculating the policy summary. In order to be consistent,
          // ignore it.
          continue;
        }

        int weekIndex = getPolicySummaryWeekFromTime(now, policyEvaluation.getTime().getTime());

        List<PolicyViolation> policyViolations = policyViolationDAO.getByEvaluationId(policyEvaluation.getId());
        policyViolations = filter(policyViolations, filter);

        PolicyViolationDiff diff = PolicyViolationDigester.digestPolicyViolations(
            policyViolationHistory.getPolicyViolations(), policyViolations);
        for (PolicyViolation policyViolation : diff.getAppeared()) {
          policyViolationHistory.addViolationWithStageType(policyViolation, policyEvaluation.getStageTypeId());
          result.totalNew++;
          addWeekViolation(weekIndex, result.weeklyDeltaNew);
          if (policyViolation.isWaived()) {
            result.totalWaived++;
            addWeekViolation(weekIndex, result.weeklyDeltaWaived);
            // The policy violation was waived when it occurred the first time, so the age for "waived" is zero.
            ageWaivedStatistics.addValue(0);
          }
        }
        for (Entry<PolicyViolation, PolicyViolation> samePolicyViolationEntry : diff.getSame().entrySet()) {
          final PolicyViolation newViolation = samePolicyViolationEntry.getValue();
          final PolicyViolation oldViolation = samePolicyViolationEntry.getKey();

          policyViolationHistory.addStageTypeToViolation(oldViolation, policyEvaluation.getStageTypeId());

          if (newViolation.isWaived() && !oldViolation.isWaived()) {
            result.totalWaived++;
            addWeekViolation(weekIndex, result.weeklyDeltaWaived);
            policyViolationHistory.replacePolicyViolation(oldViolation, newViolation);
            long policyViolationAgeWhenWaived = newViolation.getTime().getTime()
                - policyViolationHistory.getPolicyViolationFirstOccurrenceTime(newViolation).getTime();
            ageWaivedStatistics.addValue(policyViolationAgeWhenWaived);
          }
          else if (!newViolation.isWaived() && oldViolation.isWaived()) {
            result.totalWaived--;
            addWeekViolation(weekIndex, result.weeklyDeltaWaived, -1);
            policyViolationHistory.replacePolicyViolation(oldViolation, newViolation);
          }
        }
        for (PolicyViolation policyViolation : diff.getCleared()) {
          Date policyViolationFirstOccurrenceTime = policyViolationHistory
              .getPolicyViolationFirstOccurrenceTime(policyViolation);
          if (policyViolationHistory.removeStageTypeFromViolation(policyViolation,
              policyEvaluation.getStageTypeId())) {
            result.totalFixed++;
            addWeekViolation(weekIndex, result.weeklyDeltaFixed);
            if (policyViolation.isWaived()) {
              result.totalWaived--;
              addWeekViolation(weekIndex, result.weeklyDeltaWaived, -1);
            }
            long policyViolationAgeWhenFixed = policyEvaluation.getTime().getTime()
                - policyViolationFirstOccurrenceTime.getTime();
            ageFixedStatistics.addValue(policyViolationAgeWhenFixed);
          }
        }
      }

      // Calculate age statistics for unresolved policy violations
      for (PolicyViolation policyViolation : policyViolationHistory.getPolicyViolations()) {
        if (policyViolation.isWaived()) {
          continue;
        }

        Date policyViolationFirstOccurrenceTime = policyViolationHistory
            .getPolicyViolationFirstOccurrenceTime(policyViolation);
        long policyViolationAge = now - policyViolationFirstOccurrenceTime.getTime();
        ageUnresolvedStatistics.addValue(policyViolationAge);
      }
    }

    result.currentUnresolved = result.totalNew - result.totalWaived - result.totalFixed;
    for (int iWeek = 0; iWeek < POLICY_SUMMARY_WEEKS; iWeek++) {
      result.weeklyDeltaUnresolved.add(
          result.weeklyDeltaNew.get(iWeek) - result.weeklyDeltaWaived.get(iWeek) - result.weeklyDeltaFixed.get(iWeek));
    }

    if (ageWaivedStatistics.getN() > 0) {
      result.ageAverageWaived = (long) ageWaivedStatistics.getMean();
      result.agePercentile90Waived = (long) ageWaivedStatistics.getPercentile(90);
    }
    if (ageFixedStatistics.getN() > 0) {
      result.ageAverageFixed = (long) ageFixedStatistics.getMean();
      result.agePercentile90Fixed = (long) ageFixedStatistics.getPercentile(90);
    }
    if (ageUnresolvedStatistics.getN() > 0) {
      result.ageAverageUnresolved = (long) ageUnresolvedStatistics.getMean();
      result.agePercentile90Unresolved = (long) ageUnresolvedStatistics.getPercentile(90);
    }

    log.debug("getPolicySummary finished in {}", System.currentTimeMillis() - start);

    return result;
  }

  private void validateDashboardLicensed() {
    if (!licenseManager.hasDashboard()) {
      throw new InvalidLicenseException("Invalid license for the Dashboard feature");
    }
  }
}
