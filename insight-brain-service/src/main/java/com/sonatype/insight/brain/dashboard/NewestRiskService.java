/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

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
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDigester;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationStageView;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationView;

import com.google.common.base.Predicate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Named
public class NewestRiskService
{
  private static final Logger log = LoggerFactory.getLogger(NewestRiskService.class);

  private final ApplicationService applicationService;

  private final PolicyViolationLoader policyViolationLoader;

  private final PolicyViolationDAO policyViolationDAO;

  private final DashboardUtils dashboardUtils;

  @Inject
  public NewestRiskService(ApplicationService applicationService,
                           PolicyViolationLoader policyViolationLoader,
                           PolicyViolationDAO policyViolationDAO,
                           DashboardUtils dashboardUtils)
  {
    this.applicationService = applicationService;
    this.policyViolationLoader = policyViolationLoader;
    this.policyViolationDAO = policyViolationDAO;
    this.dashboardUtils = dashboardUtils;
  }

  /**
   * Gets the "newest" risk matching the specified filter criteria. Empty or null filter criteria generally means
   * "all available" violations for that aspect.
   */
  public DashboardResultsDTO<NewestRiskDTO> getNewestRisks(Set<String> organizationIds,
                                                           Set<String> applicationIds,
                                                           Set<String> stageIds,
                                                           Set<String> tagIds,
                                                           PolicyThreatCategoryFilter policyThreatCategoryFilter,
                                                           PolicyThreatLevelFilter policyThreatLevelFilter,
                                                           PolicyViolationStateFilter policyViolationStateFilter,
                                                           String orderBy,
                                                           Integer maxDaysOld,
                                                           int maxResults)
  {
    dashboardUtils.validateDashboardLicensed();

    long start = System.currentTimeMillis();

    NewestRiskDTOComparator comparator = new NewestRiskDTOComparator(orderBy);
    List<Application> applications = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds,
        applicationIds, tagIds);
    log.debug("getNewestRisks: Found {} applications filtered by appIds={} and tagIds={} in {} ms.",
        applications.size(), !isEmpty(applicationIds), !isEmpty(tagIds), System.currentTimeMillis() - start);

    Set<StageType> stageTypes = dashboardUtils.getStageTypes(stageIds);
    Predicate<PolicyViolation> filter = dashboardUtils.buildViolationFilter(policyThreatCategoryFilter,
        policyThreatLevelFilter, policyViolationStateFilter);

    Collection<ApplicationView> appViews = policyViolationLoader.getViolations(applications, stageTypes, false, filter);

    List<NewestRiskDTO> riskDTOs = new ArrayList<>();

    int policyEvaluationCount = 0;
    int policyViolationCount = 0;

    for (ApplicationView appView : appViews) {
      Application app = appView.getApplication();
      List<PolicyViolation> allUniqueAppPolicyViolations = new ArrayList<>();
      Map<PolicyViolation, NewestRiskDTO> newestRiskDTOsByPolicyViolation = new HashMap<>();

      for (ApplicationStageView appStageView : appView.getStageViews()) {
        PolicyEvaluation policyEvaluation = appStageView.getLastEvaluation();
        if (policyEvaluation != null) {
          policyEvaluationCount++;
        }
        Collection<PolicyViolation> policyViolations = appStageView.getFilteredViolations();
        if (policyViolations.isEmpty()) {
          continue;
        }
        policyViolationCount += policyViolations.size();
        StageType stageType = appStageView.getStageType();

        Map<PolicyViolation, PolicyViolation> firstOccurrencePolicyViolationsByLastPolicyViolations = getFirstOccurrencePolicyViolationsForLastPolicyViolations(
            app.getId(), stageType.getId(), policyViolations);

        PolicyViolationDiff<PolicyViolation> diff = PolicyViolationDigester
            .digestPolicyViolations(allUniqueAppPolicyViolations, policyViolations);
        for (PolicyViolation policyViolation : diff.getAppeared()) {
          PolicyViolation firstOccurrencePolicyViolation = firstOccurrencePolicyViolationsByLastPolicyViolations
              .get(policyViolation);
          NewestRiskDTO newestRiskDTO = createNewestRiskDTO(app, policyEvaluation, policyViolation,
              firstOccurrencePolicyViolation.getTime().getTime());
          newestRiskDTOsByPolicyViolation.put(policyViolation, newestRiskDTO);
          riskDTOs.add(newestRiskDTO);
        }
        for (Entry<PolicyViolation, PolicyViolation> samePolicyViolationEntry : diff.getSame().entrySet()) {
          NewestRiskDTO newestRiskDTO = newestRiskDTOsByPolicyViolation.get(samePolicyViolationEntry.getKey());
          PolicyViolation policyViolation = samePolicyViolationEntry.getValue();
          PolicyViolation firstOccurrencePolicyViolation = firstOccurrencePolicyViolationsByLastPolicyViolations
              .get(policyViolation);
          addToNewestRiskDTO(newestRiskDTO, policyEvaluation, policyViolation, firstOccurrencePolicyViolation.getTime()
              .getTime());
        }

        allUniqueAppPolicyViolations.addAll(diff.getAppeared());
      }
    }

    if (maxDaysOld != null) {
      riskDTOs = filter(riskDTOs, maxDaysOld.intValue());
    }

    Collections.sort(riskDTOs, comparator);
    DashboardResultsDTO<NewestRiskDTO> result = new DashboardResultsDTO<>();
    result.numResults = riskDTOs.size();
    result.dashboardResults = riskDTOs.subList(0, Math.min(riskDTOs.size(), maxResults));
    log.debug("getNewestRisks: Processed {} policy evaluations and {} policy violations.", policyEvaluationCount,
        policyViolationCount);

    log.debug("getNewestRisks finished in {} ms", System.currentTimeMillis() - start);

    return result;
  }

  private Map<PolicyViolation, PolicyViolation> getFirstOccurrencePolicyViolationsForLastPolicyViolations(String appId,
                                                                                                          String stageTypeId,
                                                                                                          Collection<PolicyViolation> lastPolicyViolations)
  {
    Map<PolicyViolation, PolicyViolation> result = new LinkedHashMap<>();

    List<PolicyViolation> firstOccurrences = policyViolationDAO.getFirstOccurrenceByApplicationIdAndStageTypeId(appId,
        stageTypeId);
    PolicyViolationDiff<PolicyViolation> diff = PolicyViolationDigester.digestPolicyViolations(lastPolicyViolations,
        firstOccurrences);
    for (Entry<PolicyViolation, PolicyViolation> samePolicyViolationEntry : diff.getSame().entrySet()) {
      result.put(samePolicyViolationEntry.getKey(), samePolicyViolationEntry.getValue());
    }
    for (PolicyViolation policyViolation : diff.getCleared()) {
      // PolicyViolation without a corresponding FirstOccurrencePolicyViolation. This can happen only if data was
      // missing during migration?
      result.put(policyViolation, policyViolation);
    }

    return result;
  }

  private NewestRiskDTO createNewestRiskDTO(Application app,
                                            PolicyEvaluation policyEvaluation,
                                            PolicyViolation policyViolation,
                                            long firstOccurrenceTime)
  {
    NewestRiskDTO newestRiskDTO = new NewestRiskDTO();
    newestRiskDTO.applicationPublicId = app.getPublicId();
    newestRiskDTO.applicationName = app.getName();
    newestRiskDTO.threatLevel = policyViolation.getThreatLevel();
    newestRiskDTO.firstOccurrenceTime = firstOccurrenceTime;
    newestRiskDTO.policyId = policyViolation.getPolicyId();
    newestRiskDTO.policyName = policyViolation.getPolicyName();
    newestRiskDTO.hash = policyViolation.getHash();
    addToNewestRiskDTO(newestRiskDTO, policyEvaluation, policyViolation, firstOccurrenceTime);

    return newestRiskDTO;
  }

  private void addToNewestRiskDTO(NewestRiskDTO newestRiskDTO,
                                  PolicyEvaluation policyEvaluation,
                                  PolicyViolation policyViolation,
                                  long firstOccurrenceTime)
  {
    long lastOccurrenceTime = policyEvaluation.getTime().getTime();
    if (newestRiskDTO.lastOccurrenceTime < lastOccurrenceTime || newestRiskDTO.stageTypeId == null) {
      newestRiskDTO.lastOccurrenceTime = lastOccurrenceTime;
      // return the latest stage/report
      newestRiskDTO.stageTypeId = policyEvaluation.getStageTypeId();
      newestRiskDTO.actionTypeId = policyViolation.getActionTypeId();
      newestRiskDTO.scanId = policyEvaluation.getScanId();
      // return the latest component name
      newestRiskDTO.displayName = ComponentDisplayNameUtil.fromPolicyViolation(policyViolation);
      newestRiskDTO.filename = policyViolation.getFilename();
      newestRiskDTO.derivedComponentName = ComponentDisplayNameUtil.deriveComponentName(newestRiskDTO);
    }

    if (newestRiskDTO.firstOccurrenceTime > firstOccurrenceTime) {
      newestRiskDTO.firstOccurrenceTime = firstOccurrenceTime;
    }
  }

  /**
   * Filters the given newestRiskDTOs to those that are newer than maxDaysOld
   */
  private static List<NewestRiskDTO> filter(List<NewestRiskDTO> newestRiskDTOs, int maxDaysOld) {
    if (maxDaysOld < 1) {
      throw new IllegalArgumentException("Max Days Old must be a positive integer");
    }

    List<NewestRiskDTO> filtered = new ArrayList<>();
    long filterFromTime = new DateTime().minusDays(maxDaysOld).getMillis();
    for (NewestRiskDTO newestRiskDTO : newestRiskDTOs) {
      if (newestRiskDTO.firstOccurrenceTime > filterFromTime) {
        filtered.add(newestRiskDTO);
      }
    }
    return filtered;
  }
}
