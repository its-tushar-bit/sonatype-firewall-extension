/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                                                   Predicate<PolicyViolation> violationFilter)
  {
    long start = System.currentTimeMillis();

    Map<String, ApplicationView> appViewsByAppId = new LinkedHashMap<>();
    for (Application application : applications) {
      ApplicationView appView = new ApplicationView();
      appView.application = application;
      appView.stageViewsByStageTypeId = new LinkedHashMap<>();
      for (StageType stageType : stageTypes) {
        ApplicationStageView appStageView = new ApplicationStageView();
        appStageView.stageType = stageType;
        appStageView.filteredViolations = Collections.emptyList();
        appView.stageViewsByStageTypeId.put(stageType.getId(), appStageView);
      }
      appViewsByAppId.put(application.getId(), appView);
    }

    Collection<PolicyEvaluation> evaluations = loadEvaluations(applications, stageTypes);

    Map<String, ApplicationStageView> appStageViewsByEvaluationId = new HashMap<>();
    for (PolicyEvaluation evaluation : evaluations) {
      ApplicationView appView = appViewsByAppId.get(evaluation.getApplicationId());
      ApplicationStageView appStageView = appView.stageViewsByStageTypeId.get(evaluation.getStageTypeId());
      appStageView.lastEvaluation = evaluation;
      appStageView.filteredViolations = new ArrayList<>();
      appStageViewsByEvaluationId.put(evaluation.getId(), appStageView);
    }

    Collection<PolicyViolation> violations = loadViolations(appStageViewsByEvaluationId.keySet());
    filterViolations(violations, violationFilter, appStageViewsByEvaluationId);

    log.debug("Created policy violation views in {} ms", System.currentTimeMillis() - start);

    return appViewsByAppId.values();
  }

  private Collection<PolicyEvaluation> loadEvaluations(Collection<Application> applications,
                                                       Collection<StageType> stageTypes)
  {
    long start = System.currentTimeMillis();
    Collection<PolicyEvaluation> evaluations = policyEvaluationDAO.getLastByApplicationIdsAndStageIds(
        applications.stream().map(Application::getId).collect(toSet()),
        stageTypes.stream().map(StageType::getId).collect(toSet()));
    log.debug("Loaded {} policy evaluations for {} applications across {} stages in {} ms", evaluations.size(),
        applications.size(), stageTypes.size(), System.currentTimeMillis() - start);
    return evaluations;
  }

  private Collection<PolicyViolation> loadViolations(Set<String> evaluationIds) {
    long start = System.currentTimeMillis();
    Collection<PolicyViolation> violations = policyViolationDAO.getByEvaluationIds(evaluationIds);
    log.debug("Loaded {} policy violations in {} ms", violations.size(), System.currentTimeMillis() - start);
    return violations;
  }

  private void filterViolations(Collection<PolicyViolation> violations,
                                Predicate<PolicyViolation> violationFilter,
                                Map<String, ApplicationStageView> appStageViewsByEvaluationId)
  {
    long start = System.currentTimeMillis();
    int filtered = 0;
    for (PolicyViolation violation : violations) {
      if (violationFilter == null || violationFilter.test(violation)) {
        ApplicationStageView appStageView = appStageViewsByEvaluationId.get(violation.getPolicyEvaluationId());
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

    Collection<PolicyViolation> filteredViolations;

    public StageType getStageType() {
      return stageType;
    }

    public PolicyEvaluation getLastEvaluation() {
      return lastEvaluation;
    }

    public Collection<PolicyViolation> getFilteredViolations() {
      return filteredViolations;
    }
  }
}
