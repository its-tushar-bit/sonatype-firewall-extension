/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.DashboardUtils;
import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.brain.utils.ExecutorThreadPools.ThreadPools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Facade to efficiently iterate through the latest policy evaluations and associated policy violations of several
 * owners.
 */
@Named
@Singleton
public class PolicyViolationLoader
{
  private static final Logger log = LoggerFactory.getLogger(PolicyViolationLoader.class);

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final Configuration configuration;

  private final DashboardUtils dashboardUtils;

  @Inject
  public PolicyViolationLoader(
      PolicyEvaluationDAO policyEvaluationDAO,
      PolicyViolationDAO policyViolationDAO,
      Configuration configuration,
      DashboardUtils dashboardUtils)
  {
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.configuration = configuration;
    this.dashboardUtils = dashboardUtils;
  }

  public Collection<OwnerView> getViolations(
      Collection<? extends Owner> owners,
      Collection<StageType> stageTypes,
      boolean activeViolationsOnly,
      PolicyThreatLevelFilter policyThreatLevelFilter,
      PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyViolationStateFilter policyViolationStateFilter)
  {
    return getViolations(owners, stageTypes, activeViolationsOnly, null, null,
        policyThreatLevelFilter, policyThreatCategoryFilter, policyViolationStateFilter);
  }

  public Collection<OwnerView> getViolations(
      Collection<? extends Owner> owners,
      Collection<StageType> stageTypes,
      boolean activeViolationsOnly,
      Predicate<? super PolicyViolation> violationFilter)
  {
    return getViolations(owners, stageTypes, activeViolationsOnly, violationFilter, null, null, null, null);
  }

  public Collection<OwnerView> getViolations(
      Collection<? extends Owner> owners,
      Collection<StageType> stageTypes,
      boolean activeViolationsOnly,
      Predicate<? super PolicyViolation> violationFilter,
      Date minDate,
      PolicyThreatLevelFilter policyThreatLevelFilter,
      PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyViolationStateFilter policyViolationStateFilter)
  {
    long start = System.currentTimeMillis();

    Set<String> ownerIds = owners.stream().map(Owner::getId).collect(toSet());

    Set<String> stageTypeIds = stageTypes == null
        ? Collections.emptySet()
        : stageTypes.stream().map(StageType::getId).collect(toSet());

    Collection<PolicyEvaluation> evaluations = loadEvaluations(ownerIds, stageTypeIds, minDate);

    final int maxApplications = configuration.getMaxApplicationsToQueryOnDashboard();
    if (maxApplications > 0) {
      // find most recent evaluations and put out owner ids until limit
      ownerIds = evaluations.stream()
          .sorted(Comparator.comparing(PolicyEvaluation::getTime).reversed())
          .map(PolicyEvaluation::getOwnerId)
          .distinct()
          .limit(maxApplications)
          .collect(toSet());
    }
    else {
      ownerIds = evaluations.stream().map(PolicyEvaluation::getOwnerId).collect(toSet());
    }

    CompletableFuture<Map<String, OwnerView>> ownerViewsByOwnerIdFuture = CompletableFuture.supplyAsync(() -> {
      Collection<StageType> stageTypesToFill = stageTypes == null || stageTypes.isEmpty()
          ? StageTypes.getAll()
          : stageTypes;
      Map<String, OwnerView> ownerViewsByOwnerId = new LinkedHashMap<>();
      for (Owner owner : owners) {
        OwnerView ownerView = new OwnerView();
        ownerView.owner = owner;
        // Populate the narrower `application` field too when the owner is an Application, so
        // callers of getApplication() see it.
        if (owner instanceof Application) {
          ownerView.application = (Application) owner;
        }
        ownerView.stageViewsByStageTypeId = new LinkedHashMap<>();
        for (StageType stageType : stageTypesToFill) {
          OwnerStageView ownerStageView = new OwnerStageView();
          ownerStageView.stageType = stageType;
          ownerStageView.filteredViolations = Collections.emptyList();
          ownerView.stageViewsByStageTypeId.put(stageType.getId(), ownerStageView);
        }
        ownerViewsByOwnerId.put(owner.getId(), ownerView);
      }

      for (PolicyEvaluation evaluation : evaluations) {
        OwnerView ownerView = ownerViewsByOwnerId.get(evaluation.getOwnerId());
        OwnerStageView ownerStageView = ownerView.stageViewsByStageTypeId.get(evaluation.getStageTypeId());
        ownerStageView.lastEvaluation = evaluation;
        ownerStageView.filteredViolations = new ArrayList<>();
      }
      return ownerViewsByOwnerId;
    }, ExecutorThreadPools.getInstance().getThreadPool(ThreadPools.GENERAL));

    Integer minimumThreatLevel = null;
    Integer maximumThreatLevel = null;
    if (policyThreatLevelFilter != null) {
      minimumThreatLevel = policyThreatLevelFilter.getMinPolicyThreatLevel();
      maximumThreatLevel = policyThreatLevelFilter.getMaxPolicyThreatLevel();
    }

    log.debug("Loading violations with policy threat level between:{} - {}", minimumThreatLevel, maximumThreatLevel);

    Set<PolicyThreatCategory> policyThreatCategories = null;
    if (policyThreatCategoryFilter != null) {
      policyThreatCategories = policyThreatCategoryFilter.getPolicyThreatCategories();
      log.debug("Loading violations with policy threat level categories:{}",
          Arrays.toString(policyThreatCategories.toArray()));
    }
    else {
      log.debug("Loading violations without a filter on policy threat level categories.");
    }

    Boolean violationStateOpen = null;
    Boolean violationStateWaived = null;
    Boolean violationStateLegacyViolation = null;

    if (policyViolationStateFilter != null) {
      violationStateOpen = policyViolationStateFilter.getPolicyViolationStates().contains(PolicyViolationState.OPEN);
      violationStateWaived =
          policyViolationStateFilter.getPolicyViolationStates().contains(PolicyViolationState.WAIVED);
      violationStateLegacyViolation =
          policyViolationStateFilter.getPolicyViolationStates().contains(PolicyViolationState.LEGACY_VIOLATION);
      log.debug("Loading violations with a filter on state open: {}, waived: {} and legacy: {}",
          violationStateOpen, violationStateWaived, violationStateLegacyViolation);
    }
    else {
      log.debug("Loading violations without a filter on policy violation states.");
    }

    Collection<PolicyViolation> violations = minDate != null
        ? loadViolationsAfter(ownerIds, stageTypeIds, minDate, activeViolationsOnly, minimumThreatLevel,
            maximumThreatLevel, policyThreatCategories, violationStateOpen, violationStateWaived,
            violationStateLegacyViolation)
        : loadViolations(ownerIds, stageTypeIds, activeViolationsOnly, minimumThreatLevel, maximumThreatLevel,
            policyThreatCategories, violationStateOpen, violationStateWaived, violationStateLegacyViolation);
    if (DashboardUtils.shouldOnlyShowWaivedViolations(policyViolationStateFilter)) {
      violations = violations.stream()
          .filter(violation -> !dashboardUtils.hasExistingAutoWaiverExclusion(violation.getOwnerId(),
              violation.getAutoPolicyWaiverId(), violation.getId()))
          .toList();
    }

    Map<String, OwnerView> ownerViewsByOwnerId = ownerViewsByOwnerIdFuture.join();

    filterViolations(violations, violationFilter, ownerViewsByOwnerId);

    // Sort violations using the standard violation comparator in order to get consistent results.
    sortViolations(ownerViewsByOwnerId);

    log.debug("Created policy violation views in {} ms", System.currentTimeMillis() - start);

    return ownerViewsByOwnerId.values();
  }

  private void sortViolations(Map<String, OwnerView> ownerViewsByOwnerId) {
    List<PolicyViolation> allPolicyViolations =
        ownerViewsByOwnerId.values()
            .stream()
            .flatMap(ownerView -> ownerView.getStageViews().stream())
            .flatMap(ownerStageView -> ownerStageView.getFilteredViolations().stream())
            .toList();
    policyViolationDAO.loadConstraintFacts(allPolicyViolations);
    for (OwnerView ownerView : ownerViewsByOwnerId.values()) {
      for (OwnerStageView ownerStageView : ownerView.stageViewsByStageTypeId.values()) {
        ownerStageView.getFilteredViolations().sort(PolicyViolationComparator.COMPARATOR);
      }
    }
  }

  private Collection<PolicyEvaluation> loadEvaluations(
      Set<String> ownerIds,
      Set<String> stageTypeIds,
      Date minDate)
  {
    long start = System.currentTimeMillis();
    Collection<PolicyEvaluation> evaluations;
    if (stageTypeIds.isEmpty()) {
      evaluations = policyEvaluationDAO.getLastByOwnerIds(ownerIds);
    }
    else {
      evaluations = policyEvaluationDAO.getLastByOwnerIdsAndStageIds(ownerIds, stageTypeIds);
    }
    log.debug("Loaded {} policy evaluations for {} owners across {} stages in {} ms", evaluations.size(),
        ownerIds.size(), stageTypeIds.isEmpty() ? "all" : stageTypeIds.size(),
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

  private Collection<PolicyViolation> loadViolations(
      Set<String> ownerIds,
      Set<String> stageTypeIds,
      boolean activeViolationsOnly,
      Integer minThreatLevel,
      Integer maxThreatLevel,
      Collection<PolicyThreatCategory> policyThreatCategories,
      Boolean violationStateOpen,
      Boolean violationStateWaived,
      Boolean violationStateLegacyViolation)
  {
    long start = System.currentTimeMillis();
    Collection<PolicyViolation> violations;
    if (stageTypeIds.isEmpty()) {
      if (activeViolationsOnly) {
        violations = policyViolationDAO.getActiveByOwnerIds(ownerIds);
      }
      else {
        violations = policyViolationDAO.getUnfixedByOwnerIds(ownerIds);
      }
    }
    else {
      if (activeViolationsOnly) {
        violations =
            policyViolationDAO.getActiveByOwnerIdsAndStageIds(ownerIds, stageTypeIds, minThreatLevel,
                maxThreatLevel, policyThreatCategories);
      }
      else {
        violations = policyViolationDAO.getUnfixedBy(ownerIds, stageTypeIds, minThreatLevel, maxThreatLevel,
            policyThreatCategories, violationStateOpen, violationStateWaived, violationStateLegacyViolation);
      }
    }
    log.debug("Loaded {} policy violations in {} ms", violations.size(), System.currentTimeMillis() - start);
    return violations;
  }

  private Collection<PolicyViolation> loadViolationsAfter(
      Set<String> ownerIds,
      Set<String> stageTypeIds,
      Date minDate,
      boolean activeViolationsOnly,
      Integer minThreatLevel,
      Integer maxThreatLevel,
      Collection<PolicyThreatCategory> policyThreatCategories,
      Boolean violationStateOpen,
      Boolean violationStateWaived,
      Boolean violationStateLegacyViolation)
  {
    long start = System.currentTimeMillis();
    Collection<PolicyViolation> violations;
    if (stageTypeIds.isEmpty()) {
      if (activeViolationsOnly) {
        violations =
            policyViolationDAO.getActiveByOwnerIdsOpenedAfterDate(ownerIds, minDate, minThreatLevel,
                maxThreatLevel, policyThreatCategories);
      }
      else {
        violations =
            policyViolationDAO.getUnfixedByOwnerIdsOpenedAfterDate(ownerIds, minDate, minThreatLevel,
                maxThreatLevel, policyThreatCategories);
      }
    }
    else {
      if (activeViolationsOnly) {
        violations = policyViolationDAO.getActiveByOwnerIdsAndStageIdsOpenedAfterDate(ownerIds,
            stageTypeIds, minDate, minThreatLevel, maxThreatLevel, policyThreatCategories);
      }
      else {
        violations =
            policyViolationDAO.getUnfixedBy(ownerIds, stageTypeIds, minDate, minThreatLevel,
                maxThreatLevel, policyThreatCategories, violationStateOpen, violationStateWaived,
                violationStateLegacyViolation);
      }
    }
    log.debug("Loaded {} policy violations after date in {} ms", violations.size(), System.currentTimeMillis() - start);
    return violations;
  }

  private void filterViolations(
      Collection<PolicyViolation> violations,
      Predicate<? super PolicyViolation> violationFilter,
      Map<String, OwnerView> ownerViewsByOwnerId)
  {
    long start = System.currentTimeMillis();
    int filtered = 0;
    for (PolicyViolation violation : violations) {
      if (violationFilter == null || violationFilter.test(violation)) {
        OwnerView ownerView = ownerViewsByOwnerId.get(violation.getOwnerId());
        OwnerStageView ownerStageView = ownerView.stageViewsByStageTypeId.get(violation.getStageTypeId());
        ownerStageView.filteredViolations.add(violation);
        filtered++;
      }
    }
    log.debug("Filtered {} policy violations out of {} in {} ms", filtered, violations.size(),
        System.currentTimeMillis() - start);
  }

  public static class OwnerView
  {
    Owner owner;

    // Populated only when `owner` is an Application. Callers that read getApplication() must
    // null-guard for non-Application owners.
    Application application;

    Map<String, OwnerStageView> stageViewsByStageTypeId;

    @Nonnull
    public Owner getOwner() {
      return owner;
    }

    public Application getApplication() {
      return application;
    }

    public Collection<OwnerStageView> getStageViews() {
      return stageViewsByStageTypeId.values();
    }
  }

  public static class OwnerStageView
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
