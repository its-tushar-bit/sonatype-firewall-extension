/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.utils.ExecutorThreadPools.ThreadPools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.utils.ExecutorThreadPools.getThreadPool;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Facade to efficiently iterate through the latest policy evaluations and associated policy violations of several
 * applications.
 */
@Named
@Singleton
public class PolicyViolationLoader
{
  private static final Logger log = LoggerFactory.getLogger(PolicyViolationLoader.class);

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  @Inject
  public PolicyViolationLoader(PolicyEvaluationDAO policyEvaluationDAO, PolicyViolationDAO policyViolationDAO) {
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyViolationDAO = policyViolationDAO;
  }

  public Collection<ApplicationView> getViolations(Collection<Application> applications,
                                                   Collection<StageType> stageTypes,
                                                   boolean activeViolationsOnly,
                                                   Predicate<PolicyViolation> violationFilter)
  {
    return getViolations(applications, stageTypes, activeViolationsOnly, violationFilter, null);
  }

  public Collection<ApplicationView> getViolations(Collection<Application> applications,
                                                   Collection<StageType> stageTypes,
                                                   boolean activeViolationsOnly,
                                                   Predicate<PolicyViolation> violationFilter,
                                                   Date minDate)
  {
    long start = System.currentTimeMillis();

    Set<String> applicationIds = applications.stream().map(Application::getId).collect(toSet());
    Set<String> stageTypeIds = stageTypes == null ? Collections.emptySet()
        : stageTypes.stream().map(StageType::getId).collect(toSet());

    Collection<PolicyEvaluation> evaluations = loadEvaluations(applicationIds, stageTypeIds, minDate);

    applicationIds = evaluations.stream().map(e -> e.getApplicationId()).collect(toSet());

    CompletableFuture<Map<String, ApplicationView>> appViewsByAppIdFuture = CompletableFuture.supplyAsync(() -> {
      Collection<StageType> stageTypesToFill = stageTypes == null || stageTypes.isEmpty() ? StageTypes.getAll()
          : stageTypes;
      Map<String, ApplicationView> appViewsByAppId = new LinkedHashMap<>();
      for (Application application : applications) {
        ApplicationView appView = new ApplicationView();
        appView.application = application;
        appView.stageViewsByStageTypeId = new LinkedHashMap<>();
        for (StageType stageType : stageTypesToFill) {
          ApplicationStageView appStageView = new ApplicationStageView();
          appStageView.stageType = stageType;
          appStageView.filteredViolations = Collections.emptyList();
          appView.stageViewsByStageTypeId.put(stageType.getId(), appStageView);
        }
        appViewsByAppId.put(application.getId(), appView);
      }

      for (PolicyEvaluation evaluation : evaluations) {
        ApplicationView appView = appViewsByAppId.get(evaluation.getApplicationId());
        ApplicationStageView appStageView = appView.stageViewsByStageTypeId.get(evaluation.getStageTypeId());
        appStageView.lastEvaluation = evaluation;
        appStageView.filteredViolations = new ArrayList<>();
      }
      return appViewsByAppId;
    }, getThreadPool(ThreadPools.GENERAL));
    Collection<PolicyViolation> violations = minDate != null
        ? loadViolationsAfter(applicationIds, stageTypeIds, minDate, activeViolationsOnly)
        : loadViolations(applicationIds, stageTypeIds, activeViolationsOnly);

    Map<String, ApplicationView> appViewsByAppId = appViewsByAppIdFuture.join();

    filterViolations(violations, violationFilter, appViewsByAppId);

    // Sort violations using the standard violation comparator in order to get consistent results.
    sortViolations(appViewsByAppId);

    log.debug("Created policy violation views in {} ms", System.currentTimeMillis() - start);

    return appViewsByAppId.values();
  }

  private void sortViolations(Map<String, ApplicationView> appViewsByAppId) {
    for (ApplicationView appView : appViewsByAppId.values()) {
      for (ApplicationStageView appStageView : appView.stageViewsByStageTypeId.values()) {
        appStageView.getFilteredViolations().sort(PolicyViolationComparator.COMPARATOR);
      }
    }
  }

  private Collection<PolicyEvaluation> loadEvaluations(Set<String> applicationIds,
                                                       Set<String> stageTypeIds,
                                                       Date minDate)
  {
    long start = System.currentTimeMillis();
    Collection<PolicyEvaluation> evaluations;
    if (stageTypeIds.isEmpty()) {
      evaluations = policyEvaluationDAO.getLastByApplicationIds(applicationIds);
    }
    else {
      evaluations = policyEvaluationDAO.getLastByApplicationIdsAndStageIds(applicationIds, stageTypeIds);
    }
    log.debug("Loaded {} policy evaluations for {} applications across {} stages in {} ms", evaluations.size(),
        applicationIds.size(), stageTypeIds.isEmpty() ? "all" : stageTypeIds.size(),
        System.currentTimeMillis() - start);

    if (minDate != null) {
      start = System.currentTimeMillis();
      int unfiltered = evaluations.size();

      evaluations = evaluations.stream().filter(e -> !e.getTime().before(minDate)).collect(toList());
      log.debug("Filtered {} policy evaluations out of {} in {} ms", evaluations.size(), unfiltered,
          System.currentTimeMillis() - start);
    }

    return evaluations;
  }

  private Collection<PolicyViolation> loadViolations(Set<String> applicationIds,
                                                     Set<String> stageTypeIds,
                                                     boolean activeViolationsOnly)
  {
    long start = System.currentTimeMillis();
    Collection<PolicyViolation> violations;
    if (stageTypeIds.isEmpty()) {
      if (activeViolationsOnly) {
        violations = policyViolationDAO.getActiveByApplicationIds(applicationIds);
      }
      else {
        violations = policyViolationDAO.getUnfixedByApplicationIds(applicationIds);
      }
    }
    else {
      if (activeViolationsOnly) {
        violations = policyViolationDAO.getActiveByApplicationIdsAndStageIds(applicationIds, stageTypeIds);
      }
      else {
        violations = policyViolationDAO.getUnfixedByApplicationIdsAndStageIds(applicationIds, stageTypeIds);
      }
    }
    log.debug("Loaded {} policy violations in {} ms", violations.size(), System.currentTimeMillis() - start);
    return violations;
  }

  private Collection<PolicyViolation> loadViolationsAfter(Set<String> applicationIds,
                                                          Set<String> stageTypeIds,
                                                          Date minDate,
                                                          boolean activeViolationsOnly)
  {
    long start = System.currentTimeMillis();
    Collection<PolicyViolation> violations;
    if (stageTypeIds.isEmpty()) {
      if (activeViolationsOnly) {
        violations = policyViolationDAO.getActiveByApplicationIdsOpenedAfterDate(applicationIds, minDate);
      }
      else {
        violations = policyViolationDAO.getUnfixedByApplicationIdsOpenedAfterDate(applicationIds, minDate);
      }
    }
    else {
      if (activeViolationsOnly) {
        violations = policyViolationDAO.getActiveByApplicationIdsAndStageIdsOpenedAfterDate(applicationIds,
            stageTypeIds, minDate);
      }
      else {
        violations = policyViolationDAO.getUnfixedByApplicationIdsAndStageIdsOpenedAfterDate(applicationIds,
            stageTypeIds, minDate);
      }
    }
    log.debug("Loaded {} policy violations after date in {} ms", violations.size(), System.currentTimeMillis() - start);
    return violations;
  }

  private void filterViolations(Collection<PolicyViolation> violations,
                                Predicate<PolicyViolation> violationFilter,
                                Map<String, ApplicationView> appViewsByAppId)
  {
    long start = System.currentTimeMillis();
    int filtered = 0;
    for (PolicyViolation violation : violations) {
      if (violationFilter == null || violationFilter.test(violation)) {
        ApplicationView appView = appViewsByAppId.get(violation.getApplicationId());
        ApplicationStageView appStageView = appView.stageViewsByStageTypeId.get(violation.getStageTypeId());
        appStageView.filteredViolations.add(violation);
        filtered++;
      }
    }
    log.debug("Filtered {} policy violations out of {} in {} ms", filtered, violations.size(),
        System.currentTimeMillis() - start);
  }

  public static class ApplicationView
  {
    Application application;

    Map<String, ApplicationStageView> stageViewsByStageTypeId;

    public Application getApplication() {
      return application;
    }

    public Collection<ApplicationStageView> getStageViews() {
      return stageViewsByStageTypeId.values();
    }
  }

  public static class ApplicationStageView
  {
    StageType stageType;

    PolicyEvaluation lastEvaluation;

    List<PolicyViolation> filteredViolations;

    public StageType getStageType() {
      return stageType;
    }

    public PolicyEvaluation getLastEvaluation() {
      return lastEvaluation;
    }

    public List<PolicyViolation> getFilteredViolations() {
      return filteredViolations;
    }
  }
}
