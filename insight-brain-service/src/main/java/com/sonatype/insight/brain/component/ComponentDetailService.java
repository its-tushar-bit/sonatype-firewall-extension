/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.successmetrics.ComponentCountsDTO;
import com.sonatype.insight.brain.successmetrics.ComponentCountsDTO.ComponentCountDTO;
import com.sonatype.insight.brain.component.ApplicationComponentDetailsDTO.PolicyViolationSummaryDTO;
import com.sonatype.insight.brain.component.ApplicationComponentDetailsDTO.PolicyViolationSummaryDTO.ReasonDTO;
import com.sonatype.insight.brain.dashboard.DashboardUtils;
import com.sonatype.insight.brain.dashboard.StageDetailDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.organization.ApplicationAdapter;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.Ordering;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
/**
 * @since 1.11
 */
public class ComponentDetailService
{
  private static final Logger log = LoggerFactory.getLogger(ComponentDetailService.class);

  private static final int COMPONENT_COUNT_LIMIT = 5;

  private final ApplicationService appService;

  private final ApplicationAdapter appAdapter;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final StageTypeService stageTypeService;

  private final CLMLicenseManager licenseManager;

  private final DashboardUtils dashboardUtils;

  @Inject
  public ComponentDetailService(ApplicationService appService,
                                ApplicationAdapter appAdapter,
                                ApplicationComponentDAO applicationComponentDAO,
                                StageTypeService stageTypeService,
                                CLMLicenseManager licenseManager,
                                DashboardUtils dashboardUtils)
  {
    this.appService = appService;
    this.appAdapter = appAdapter;
    this.applicationComponentDAO = applicationComponentDAO;
    this.stageTypeService = stageTypeService;
    this.licenseManager = licenseManager;
    this.dashboardUtils = dashboardUtils;
  }

  public List<ApplicationComponentDetailsDTO> getApplicationDetailsByHash(String hash) {
    validateDashboardLicensed();

    long start = System.currentTimeMillis();

    List<ApplicationComponentDetailsDTO> result = new ArrayList<>();

    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

    List<StageType> stageTypes = new ArrayList<>();
    for (StageType stageType : stageTypeService.getLicensedStageTypes()) {
      if (!StageTypes.isIgnoredForDashboard(stageType.getId())) {
        stageTypes.add(stageType);
      }
    }

    // Get the list of applications the user can see
    List<Application> applications = appService.getApplications();
    for (Application application : applications) {
      if (!isComponentPartOfApplication(application, hash)) {
        // Ignore this application because it does not contain the specified component.
        continue;
      }

      ApplicationComponentDetailsDTO applicationComponentDetails = new ApplicationComponentDetailsDTO();

      Map<String, PolicyViolationSummaryDTO> policyViolationDTOsByPolicyId = new LinkedHashMap<>();
      Map<String, Map<String, StageDetailDTO>> stageDetailsByPolicyId = new LinkedHashMap<>();
      for (StageType stageType : stageTypes) {
        StageDetailDTO appStageDetailDTO = new StageDetailDTO(stageType.getId(), stageType.getName());
        applicationComponentDetails.stageDetails.add(appStageDetailDTO);

        PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(),
            stageType.getId());
        if (policyEvaluation == null) {
          continue;
        }

        List<PolicyViolation> policyViolations = policyViolationDAO.getActiveByEvaluationIdAndHash(
            policyEvaluation.getId(), hash);
        if (policyViolations.isEmpty()) {
          continue;
        }

        // only set this value if we have violations
        appStageDetailDTO.time = policyEvaluation.getTime().getTime();
        appStageDetailDTO.scanId = policyEvaluation.getScanId();

        List<PolicyViolation> firstOccurrences = policyViolationDAO
            .getFirstOccurrenceByApplicationIdAndStageTypeIdAndHash(application.getId(), stageType.getId(), hash);
        Map<String, PolicyViolation> firstOccurrencesByPolicyId = new HashMap<>();
        for (PolicyViolation firstOccurrence : firstOccurrences) {
          PolicyViolation clash = firstOccurrencesByPolicyId.put(firstOccurrence.getPolicyId(), firstOccurrence);
          if (clash != null) {
            throw new IllegalStateException("Duplicate first occurrence for violation, appId = " + application.getId()
                + ", stageId = " + stageType.getId() + ", policyId = " + firstOccurrence.getPolicyId() + ", hash = "
                + hash + ", id = " + clash.getId() + " vs " + firstOccurrence.getId());
          }
        }

        for (PolicyViolation policyViolation : policyViolations) {
          String policyId = policyViolation.getPolicyId();

          PolicyViolation firstOccurrence = firstOccurrencesByPolicyId.get(policyId);
          if (firstOccurrence == null) {
            // incomplete data migration between snapshot builds or violations for unhashed components can cause this
            firstOccurrence = policyViolation;
          }

          Map<String, StageDetailDTO> stageDetailsById = stageDetailsByPolicyId.get(policyId);
          if (stageDetailsById == null) {
            stageDetailsById = initStageDetails(stageTypes);
            stageDetailsByPolicyId.put(policyId, stageDetailsById);
          }
          StageDetailDTO policyStageDetailDTO = stageDetailsById.get(stageType.getId());
          policyStageDetailDTO.scanId = policyEvaluation.getScanId();
          policyStageDetailDTO.actionTypeId = policyViolation.getActionTypeId();
          policyStageDetailDTO.time = firstOccurrence.getTime().getTime();

          // Should always have the time/action of the first occurring violation for the stage, to indicate how long
          // violations have been around for this application.
          if (policyStageDetailDTO.time <= appStageDetailDTO.time) {
            appStageDetailDTO.time = policyStageDetailDTO.time;
            appStageDetailDTO.actionTypeId = policyStageDetailDTO.actionTypeId;
          }

          PolicyViolationSummaryDTO policyViolationSummaryDTO = policyViolationDTOsByPolicyId.get(policyId);
          if (policyViolationSummaryDTO == null) {
            policyViolationSummaryDTO = new PolicyViolationSummaryDTO();
            policyViolationSummaryDTO.policyId = policyViolation.getPolicyId();
            policyViolationSummaryDTO.stageDetails.addAll(stageDetailsById.values());
            policyViolationDTOsByPolicyId.put(policyId, policyViolationSummaryDTO);
          }
          // Use the values from the most recent policy violation
          if (policyViolationSummaryDTO.time < policyViolation.getTime().getTime()) {
            policyViolationSummaryDTO.policyName = policyViolation.getPolicyName();
            policyViolationSummaryDTO.threatLevel = policyViolation.getThreatLevel();
            policyViolationSummaryDTO.reasons = new ArrayList<>();
            policyViolationSummaryDTO.time = policyViolation.getTime().getTime();
            for (ConstraintFact constraintFact : policyViolation.getConstraintFacts()) {
              ReasonDTO reasonDTO = new ReasonDTO();
              reasonDTO.constraintName = constraintFact.getConstraintName();
              for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
                reasonDTO.reasons.add(conditionFact.getReason());
              }
              policyViolationSummaryDTO.reasons.add(reasonDTO);
            }
          }
        }
      }

      applicationComponentDetails.application = appAdapter.convert(application, false);
      applicationComponentDetails.policyViolations.addAll(policyViolationDTOsByPolicyId.values());
      result.add(applicationComponentDetails);
    }

    log.debug("Loaded component details from {} out of {} applications in {} ms", result.size(), applications.size(),
        System.currentTimeMillis() - start);

    return result;
  }

  public ComponentCountsDTO getComponentCounts(Set<String> organizationIds, Set<String> applicationIds) {
    Collection<Application> applications = appService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds, applicationIds, null);

    Set<String> applicationIdsToQuery = new HashSet<>();
    for (Application app : applications) {
      applicationIdsToQuery.add(app.getId());
    }

    // beginning of last month a year ago
    Date sinceDate = new LocalDate().withDayOfMonth(1).minusMonths(13).toDate();

    List<StageType> stageTypes = new ArrayList<>();
    for (StageType stageType : stageTypeService.getLicensedStageTypes()) {
      if (!StageTypes.isIgnoredForPolicyViolationAggregation(stageType.getId())) {
        stageTypes.add(stageType);
      }
    }
    Set<String> stageTypeIds = dashboardUtils.getStageTypeIds(stageTypes);

    Map<ComponentIdentifier, Integer> componentApplicationCounts = getComponentApplicationCounts(applicationIdsToQuery,
        stageTypeIds, sinceDate);

    Map<ComponentIdentifier, Integer> componentViolationCounts =
        getComponentViolationCounts(applicationIdsToQuery, stageTypeIds, sinceDate);

    // An Ordering that sorts by count, ascending, and then by ComponentIdentifier string, descending
    Ordering<Entry<ComponentIdentifier, Integer>> ordering = new ComponentCountOrdering();

    List<Entry<ComponentIdentifier, Integer>> topComponentApplicationCounts =
        ordering.greatestOf(componentApplicationCounts.entrySet().iterator(), COMPONENT_COUNT_LIMIT);

    List<Entry<ComponentIdentifier, Integer>> topComponentViolationCounts =
        ordering.greatestOf(componentViolationCounts.entrySet().iterator(), COMPONENT_COUNT_LIMIT);

    ComponentCountsDTO retval = new ComponentCountsDTO();
    retval.componentsInTheMostApplications = toComponentCountDTOs(topComponentApplicationCounts);
    retval.componentsWithTheMostViolations = toComponentCountDTOs(topComponentViolationCounts);
    retval.componentsPerApplication = getAverageComponentCountPerApplication(applications.size(),
        componentApplicationCounts);

    return retval;
  }

  /**
   * @return a map from Component Id to count of applications that contain that component in their most recent
   * evaluation. Only evaluations more recent than the passed-in date are considered.
   */
  private Map<ComponentIdentifier, Integer> getComponentApplicationCounts(Set<String> applicationIds,
                                                                          Set<String> stageTypeIds,
                                                                          Date date)
  {
    List<ApplicationComponent> applicationComponents =
        applicationComponentDAO.getByApplicationIdsAndStageTypeIdsSince(applicationIds, stageTypeIds, date);

    Map<ComponentIdentifier, Set<String>> componentApplicationMap = new HashMap<>();

    // build a map from component id -> set of application ids
    for (ApplicationComponent applicationComponent : applicationComponents) {
      ComponentIdentifier componentId = applicationComponent.getComponentIdentifier();

      if (componentId != null) {
        String applicationId = applicationComponent.getApplicationId();

        Set<String> appIdSet = componentApplicationMap.get(componentId);

        if (appIdSet == null) {
          appIdSet = new HashSet<>();
          componentApplicationMap.put(componentId, appIdSet);
        }
        appIdSet.add(applicationId);
      }
    }

    Map<ComponentIdentifier, Integer> retval = new HashMap<>();
    for (Map.Entry<ComponentIdentifier, Set<String>> entry : componentApplicationMap.entrySet()) {
      retval.put(entry.getKey(), entry.getValue().size());
    }

    return retval;
  }

  private List<ComponentCountDTO> toComponentCountDTOs(List<Map.Entry<ComponentIdentifier, Integer>> mapEntries) {
    List<ComponentCountDTO> retval = new ArrayList<>(mapEntries.size());

    for (Map.Entry<ComponentIdentifier, Integer> entry : mapEntries) {
      ComponentCountDTO dto = new ComponentCountDTO();
      dto.componentDisplayName = ComponentDisplayNameUtil.fromIdentifier(entry.getKey()).toString();
      dto.count = entry.getValue();

      retval.add(dto);
    }

    return retval;
  }

  /**
   * @return a map from Component Id to total violation count in the specified applications.  Only applications
   * with an evaluation more recent than the specified date are included.
   */
  private Map<ComponentIdentifier, Integer> getComponentViolationCounts(Set<String> applicationIds,
                                                                        Set<String> stageTypeIds,
                                                                        Date date)
  {
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();

    Map<ComponentIdentifier, Integer> retval = new HashMap<>();

    for (PolicyEvaluation evaluation : policyEvaluationDAO
        .getLastByApplicationIdsAndStageIds(applicationIds, stageTypeIds)) {
      if (evaluation == null || evaluation.getTime().compareTo(date) < 0) {
        continue;
      }

      Collection<PolicyViolation> violations = policyViolationDAO.getActiveByEvaluationId(evaluation.getId());

      for (PolicyViolation violation : violations) {
        ComponentIdentifier componentId = violation.getComponentIdentifier();
        if (componentId == null) {
          continue;
        }

        Integer componentViolationCount = retval.get(componentId);

        if (componentViolationCount == null) {
          retval.put(componentId, 1);
        }
        else {
          retval.put(componentId, componentViolationCount + 1);
        }
      }
    }

    return retval;
  }

  private int getAverageComponentCountPerApplication(int applicationCount,
                                                     Map<ComponentIdentifier, Integer> componentApplicationCounts)
  {
    int totalComponentApplicationCounts = 0;
    for (Map.Entry<ComponentIdentifier, Integer> entry : componentApplicationCounts.entrySet()) {
      totalComponentApplicationCounts += entry.getValue();
    }

    if (applicationCount == 0) {
      return 0;
    }

    return totalComponentApplicationCounts / applicationCount;
  }

  private Map<String, StageDetailDTO> initStageDetails(Collection<StageType> stageTypes) {
    Map<String, StageDetailDTO> stageDetailsById = new LinkedHashMap<>();
    for (StageType stageType : stageTypes) {
      StageDetailDTO stageDetailDTO = new StageDetailDTO(stageType.getId(), stageType.getName());
      stageDetailsById.put(stageDetailDTO.stageTypeId, stageDetailDTO);
    }
    return stageDetailsById;
  }

  private boolean isComponentPartOfApplication(Application application, String hash) {
    List<ApplicationComponent> appComponents = applicationComponentDAO.getByApplicationIdAndHash(application.getId(),
        hash);
    for (ApplicationComponent appComponent : appComponents) {
      if (!StageTypes.isIgnoredForDashboard(appComponent.getStageTypeId())) {
        return true;
      }
    }
    return false;
  }

  public ComponentDisplayName getComponentNameByHash(String hash) {
    validateDashboardLicensed();

    ApplicationComponent applicationComponent = applicationComponentDAO.getLastByHash(hash);
    if (applicationComponent == null) {
      throw new BadRequestException("Unknown component with hash " + hash + ".");
    }
    ComponentDisplayName componentNameDTO = null;
    if (applicationComponent.getComponentIdentifier() != null) {
      componentNameDTO = ComponentDisplayNameUtil.fromIdentifier(applicationComponent.getComponentIdentifier());
    }

    if (componentNameDTO == null && !applicationComponent.getPathnames().isEmpty()) {
      componentNameDTO = new ComponentDisplayName().add("Pathname", applicationComponent.getPathnames().get(0));
    }

    return componentNameDTO;
  }

  private void validateDashboardLicensed() {
    if (!licenseManager.hasDashboard()) {
      throw new InvalidLicenseException();
    }
  }

  private static class ComponentCountOrdering
      extends Ordering<Entry<ComponentIdentifier, Integer>>
  {
    @Override
    public int compare(Entry<ComponentIdentifier, Integer> a, Entry<ComponentIdentifier, Integer> b) {
      int countDiff = a.getValue() - b.getValue();

      if (countDiff != 0) {
        return countDiff;
      }
      else {
        String aComponentIdentifierName =
            ComponentDisplayNameUtil.fromIdentifier(a.getKey()).toString();
        String bComponentIdentifierName =
            ComponentDisplayNameUtil.fromIdentifier(b.getKey()).toString();

        return bComponentIdentifierName.compareToIgnoreCase(aComponentIdentifierName);
      }
    }
  }
}
