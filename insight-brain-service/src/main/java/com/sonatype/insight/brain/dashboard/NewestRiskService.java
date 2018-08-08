/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.utils.ExecutorThreadPools.THREAD_POOLS;
import static com.sonatype.insight.brain.utils.ExecutorThreadPools.getThreadPool;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Named
public class NewestRiskService
{
  private static final Logger log = LoggerFactory.getLogger(NewestRiskService.class);

  private final ApplicationService applicationService;

  private final PolicyViolationLoader policyViolationLoader;

  private final DashboardUtils dashboardUtils;

  @Inject
  public NewestRiskService(ApplicationService applicationService,
                           PolicyViolationLoader policyViolationLoader,
                           DashboardUtils dashboardUtils)
  {
    this.applicationService = applicationService;
    this.policyViolationLoader = policyViolationLoader;
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

    if (maxDaysOld != null && maxDaysOld < 1) {
      throw new IllegalArgumentException("Max Days Old must be a positive integer");
    }

    long start = System.currentTimeMillis();

    NewestRiskDTOComparator comparator = new NewestRiskDTOComparator(orderBy);
    List<Application> applications = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds,
        applicationIds, tagIds);
    log.debug("getNewestRisks: Found {} applications filtered by appIds={} and tagIds={} in {} ms.",
        applications.size(), !isEmpty(applicationIds), !isEmpty(tagIds), System.currentTimeMillis() - start);

    Set<StageType> stageTypes = dashboardUtils.getStageTypes(stageIds);
    Predicate<PolicyViolation> filter = dashboardUtils.buildViolationFilter(policyThreatCategoryFilter,
        policyThreatLevelFilter, policyViolationStateFilter);

    Date minDate = (maxDaysOld == null) ? null : new Date(Instant.now().minus(Duration.ofDays(maxDaysOld)).toEpochMilli());
    Collection<ApplicationView> appViews = policyViolationLoader.getViolations(applications, stageTypes, false, filter, minDate);

    List<NewestRiskDTO> riskDTOs = new ArrayList<>();

    final AtomicInteger policyEvaluationCount = new AtomicInteger(0);
    final AtomicInteger policyViolationCount = new AtomicInteger(0);

    List<CompletableFuture<List<NewestRiskDTO>>> dtoFutures = appViews.stream()
        .map(appView -> CompletableFuture.supplyAsync(() -> {

          Application app = appView.getApplication();
          List<PolicyViolation> allUniqueAppPolicyViolations = new ArrayList<>();
          Map<PolicyViolation, NewestRiskDTO> newestRiskDTOsByPolicyViolation = new HashMap<>();

          List<NewestRiskDTO> localDTOs = new ArrayList<>();

          for (ApplicationStageView appStageView : appView.getStageViews()) {
            PolicyEvaluation policyEvaluation = appStageView.getLastEvaluation();
            if (policyEvaluation != null) {
              policyEvaluationCount.incrementAndGet();
            }
            Collection<PolicyViolation> policyViolations = appStageView.getFilteredViolations();
            if (policyViolations.isEmpty()) {
              continue;
            }
            policyViolationCount.addAndGet(policyViolations.size());

            PolicyViolationDiff<PolicyViolation> diff = PolicyViolationDigester
                .digestPolicyViolations(allUniqueAppPolicyViolations, policyViolations);
            for (PolicyViolation policyViolation : diff.getAppeared()) {
              NewestRiskDTO newestRiskDTO = createNewestRiskDTO(app, policyEvaluation, policyViolation);
              newestRiskDTOsByPolicyViolation.put(policyViolation, newestRiskDTO);
              localDTOs.add(newestRiskDTO);
            }
            for (Entry<PolicyViolation, PolicyViolation> samePolicyViolationEntry : diff.getSame().entrySet()) {
              NewestRiskDTO newestRiskDTO = newestRiskDTOsByPolicyViolation.get(samePolicyViolationEntry.getKey());
              PolicyViolation policyViolation = samePolicyViolationEntry.getValue();
              addToNewestRiskDTO(newestRiskDTO, policyEvaluation, policyViolation);
            }

            allUniqueAppPolicyViolations.addAll(diff.getAppeared());
          }
          return localDTOs;
        }, getThreadPool(THREAD_POOLS.GENERAL))).collect(toList());

    dtoFutures.stream().map(CompletableFuture::join).forEach(riskDTOs::addAll);

    Collections.sort(riskDTOs, comparator);
    DashboardResultsDTO<NewestRiskDTO> result = new DashboardResultsDTO<>();
    result.numResults = riskDTOs.size();
    result.dashboardResults = riskDTOs.subList(0, Math.min(riskDTOs.size(), maxResults));
    log.debug("getNewestRisks: Processed {} policy evaluations and {} policy violations.", policyEvaluationCount,
        policyViolationCount);

    log.debug("getNewestRisks finished in {} ms", System.currentTimeMillis() - start);

    return result;
  }

  private NewestRiskDTO createNewestRiskDTO(Application app,
                                            PolicyEvaluation policyEvaluation,
                                            PolicyViolation policyViolation)
  {
    NewestRiskDTO newestRiskDTO = new NewestRiskDTO();
    newestRiskDTO.applicationPublicId = app.getPublicId();
    newestRiskDTO.applicationName = app.getName();
    newestRiskDTO.threatLevel = policyViolation.getThreatLevel();
    newestRiskDTO.firstOccurrenceTime = policyViolation.getOpenTime().getTime();
    newestRiskDTO.policyId = policyViolation.getPolicyId();
    newestRiskDTO.policyName = policyViolation.getPolicyName();
    newestRiskDTO.hash = policyViolation.getHash();
    addToNewestRiskDTO(newestRiskDTO, policyEvaluation, policyViolation);

    return newestRiskDTO;
  }

  private void addToNewestRiskDTO(NewestRiskDTO newestRiskDTO,
                                  PolicyEvaluation policyEvaluation,
                                  PolicyViolation policyViolation)
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

    long firstOccurrenceTime = policyViolation.getOpenTime().getTime();
    if (newestRiskDTO.firstOccurrenceTime > firstOccurrenceTime) {
      newestRiskDTO.firstOccurrenceTime = firstOccurrenceTime;
    }
  }
}
