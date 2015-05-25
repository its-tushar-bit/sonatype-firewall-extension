/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.organization.ApplicationAdapter;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.organization.ContactDTO;
import com.sonatype.insight.model.HasStringId;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApplicationRiskService
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationRiskService.class);

  private static final String SECRET_JOIN_STRING = "$";

  private final ApplicationService applicationService;

  private final ApplicationAdapter applicationAdapter;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyViolationAdapter policyViolationAdapter;

  private final DashboardUtils dashboardUtils;

  @Inject
  public ApplicationRiskService(ApplicationService applicationService, ApplicationAdapter applicationAdapter,
      PolicyEvaluationDAO policyEvaluationDAO, PolicyViolationDAO policyViolationDAO,
      PolicyViolationAdapter policyViolationAdapter, DashboardUtils dashboardUtils)
  {
    this.applicationService = applicationService;
    this.applicationAdapter = applicationAdapter;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.policyViolationAdapter = policyViolationAdapter;
    this.dashboardUtils = dashboardUtils;
  }

  /**
   * @since 1.11.0
   */
  public List<ApplicationRiskScoreDTO> getApplicationRisks(final Set<String> applicationIds,
      final Set<String> stageIds, final Set<String> tagIds,
      final PolicyThreatCategoryFilter policyThreatCategoryFilter,
      final PolicyThreatLevelFilter policyThreatLevelFilter, final int maxResults)
  {
    dashboardUtils.validateDashboardLicensed();

    long start = System.currentTimeMillis();

    List<Application> appsToSearch = applicationService.getApplicationsByIdsAndTagIds(applicationIds, tagIds);
    Set<StageType> stageTypes = dashboardUtils.getStageTypes(stageIds);
    Predicate<PolicyViolation> filter = dashboardUtils.buildViolationFilter(policyThreatCategoryFilter,
        policyThreatLevelFilter);

    List<PolicyEvaluation> evaluations = policyEvaluationDAO.getLastByApplicationIdsAndStageIds(
        dashboardUtils.getApplicationIds(appsToSearch), dashboardUtils.getStageIds(stageTypes));

    Map<String, PolicyEvaluation> policyEvaluationsById = mapCollectionById(evaluations);
    List<PolicyViolationDTO> allPolicyViolationDTOs = createAllPolicyViolations(filter, evaluations, appsToSearch,
        policyEvaluationsById);

    Iterable<ApplicationRiskScoreDTO> applicationRisks = createApplicationRiskScores(appsToSearch, stageTypes,
        policyEvaluationsById, allPolicyViolationDTOs);

    List<ApplicationRiskScoreDTO> sortedApplicationRisks = sortAndFilterApplicationRiskScore(applicationRisks);

    List<ApplicationRiskScoreDTO> result = sortedApplicationRisks.subList(0,
        Math.min(sortedApplicationRisks.size(), maxResults));

    log.debug("getApplicationRisks finished in {} ms", System.currentTimeMillis() - start);

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
      allPolicyViolationDTOs.addAll(policyViolationAdapter.createPolicyViolationDTOs(sourceApplication,
          Lists.newArrayList(violation)));
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

    // still kind of hazy what happens down in LDAP land if we get nulls for some of the elements,
    // we may have to deal with that
    return applicationAdapter.getContacts(contactNames);
  }

  private Iterable<PolicyViolationDTO> getViolationsForApp(final List<PolicyViolationDTO> allPolicyViolationDTOs,
      final Application application)
  {
    return Iterables.filter(allPolicyViolationDTOs, new Predicate<PolicyViolationDTO>()
    {
      @Override
      public boolean apply(@Nullable final PolicyViolationDTO violation) {
        return violation != null && application.getId().equals(violation.applicationId);
      }
    });
  }

  private Iterable<PolicyViolationDTO> createViolationsForStage(final String stageId,
      final Iterable<PolicyViolationDTO> violationsForApp, final Map<String, PolicyEvaluation> policyEvaluationsById)
  {
    return Iterables.filter(violationsForApp, new Predicate<PolicyViolationDTO>()
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
    Set<String> evaluationIds = Sets.newHashSet(Iterables.transform(evaluations, DashboardUtils.hasIdIdSelector));
    return dashboardUtils.filter(policyViolationDAO.getActiveByEvaluationIds(evaluationIds), violationFilter);
  }

  private void updateTotalApplicationRisks(final ApplicationRiskScoreDTO applicationRiskScore,
      final Iterable<PolicyViolationDTO> allViolations)
  {
    // squish down any dupes we have across stages
    final Map<String, PolicyViolationDTO> compHashToViolation = new HashMap<>();
    for (final PolicyViolationDTO violation1 : allViolations) {
      String vioHash = createUniqueHashForPolicy(violation1);
      PolicyViolationDTO existing = compHashToViolation.get(vioHash);
      if (existing == null) {
        // first time we see a violation, we make it
        compHashToViolation.put(vioHash, violation1);
      }
      else if (violation1.time > existing.time) {
        // we have a newer violation, update existing
        compHashToViolation.put(vioHash, violation1);
      }
    }

    // update the total risks based on the deduped risks
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
    List<ApplicationRiskScoreDTO> filteredApplicationRiskScores = Lists.newArrayList(Iterables.filter(applicationRisks,
        new Predicate<ApplicationRiskScoreDTO>()
        {

          @Override
          public boolean apply(@Nullable final ApplicationRiskScoreDTO input) {
            return input != null && input.totalApplicationRisk.totalRisk > 0;
          }
        }));
    Collections.sort(filteredApplicationRiskScores, new Comparator<ApplicationRiskScoreDTO>()
    {
      @Override
      public int compare(final ApplicationRiskScoreDTO o1, final ApplicationRiskScoreDTO o2) {
        int result = Integer.compare(o2.totalApplicationRisk.totalRisk, o1.totalApplicationRisk.totalRisk);
        if (result == 0) {
          result = o1.applicationId.compareTo(o2.applicationId);
        }
        return result;
      }
    });
    return filteredApplicationRiskScores;
  }
}
