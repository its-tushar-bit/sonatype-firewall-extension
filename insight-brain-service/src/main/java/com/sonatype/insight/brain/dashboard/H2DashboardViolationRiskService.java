/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.policy.TriggerReference.Type;
import com.sonatype.insight.brain.audit.AuditService;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
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
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.brain.utils.ExecutorThreadPools.ThreadPools;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

@Named
public class H2DashboardViolationRiskService
    extends AbstractDashboardViolationRiskService
{
  private static final Logger log = LoggerFactory.getLogger(H2DashboardViolationRiskService.class);

  private final OrganizationDAO organizationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyViolationLoader policyViolationLoader;

  @Inject
  public H2DashboardViolationRiskService(
      ApplicationService applicationService,
      OrganizationDAO organizationDAO,
      PolicyViolationDAO policyViolationDAO,
      PolicyViolationLoader policyViolationLoader,
      DashboardUtils dashboardUtils,
      final AuditService auditService)
  {
    super(applicationService, dashboardUtils, auditService);
    this.organizationDAO = organizationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.policyViolationLoader = policyViolationLoader;
  }

  @Override
  protected DashboardResultsDTO<DashboardViolationRiskDTO> load(
      List<Application> applications,
      Set<StageType> stageTypes,
      PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter,
      PolicyViolationStateFilter policyViolationStateFilter,
      String orderBy,
      Date minDate,
      int page,
      int pageSize)
  {
    Collection<ApplicationView> appViews = getPolicyViolations(applications, stageTypes, policyThreatCategoryFilter,
        policyThreatLevelFilter, policyViolationStateFilter, minDate);

    List<DashboardViolationRiskDTO> riskDTOs = buildRiskDTOs(appViews);

    sort(riskDTOs, orderBy);

    return buildResultsDTO(riskDTOs, page, pageSize);
  }

  private Collection<ApplicationView> getPolicyViolations(
      List<Application> applications,
      Collection<StageType> stageTypes,
      PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter,
      PolicyViolationStateFilter policyViolationStateFilter,
      Date minDate)
  {
    return policyViolationLoader.getViolations(applications, stageTypes, false, null, minDate,
        policyThreatLevelFilter, policyThreatCategoryFilter, policyViolationStateFilter);
  }

  private List<DashboardViolationRiskDTO> buildRiskDTOs(Collection<ApplicationView> appViews) {
    List<DashboardViolationRiskDTO> riskDTOs = new ArrayList<>();

    final AtomicInteger policyEvaluationCount = new AtomicInteger(0);
    final AtomicInteger policyViolationCount = new AtomicInteger(0);

    Map<String, String> orgNames = new ConcurrentHashMap<>();
    List<CompletableFuture<List<DashboardViolationRiskDTO>>> dtoFutures = appViews.stream()
        .map(appView -> CompletableFuture.supplyAsync(() -> {
          Application app = appView.getApplication();

          // We must limit ourselves only to the organization name to preserve access controls. We cannot use
          // an approach with auth checks as it's possible to not have rights on the parent organization, but
          // we still want to display the organization name. The organization name is already viewable in the
          // sidebar using the same approach in situations where the user does not have any access rights, so
          // this should be consistent with existing behavior and information visibility (see SidebarService).
          // Also store the org names once fetched to avoid multiple fetches incurring a performance penalty.
          String orgName = orgNames.computeIfAbsent(appView.getApplication().getOrganizationId(),
              orgId -> organizationDAO.getByIdNotNull(orgId).getName());

          List<PolicyViolation> allUniqueAppPolicyViolations = new ArrayList<>();
          Map<PolicyViolation, DashboardViolationRiskDTO> violationRiskDTOsByPolicyViolation = new HashMap<>();

          List<DashboardViolationRiskDTO> localDTOs = new ArrayList<>();

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

            policyViolationDAO.loadConstraintFacts(policyViolations);
            PolicyViolationDiff<PolicyViolation> diff = PolicyViolationDigester
                .digestPolicyViolations(allUniqueAppPolicyViolations, policyViolations);
            for (PolicyViolation policyViolation : diff.getAppeared()) {
              DashboardViolationRiskDTO violationRiskDTO =
                  createViolationRiskDTO(app, orgName, policyViolation);
              violationRiskDTOsByPolicyViolation.put(policyViolation, violationRiskDTO);
              localDTOs.add(violationRiskDTO);
            }
            for (Entry<PolicyViolation, PolicyViolation> samePolicyViolationEntry : diff.getSame().entrySet()) {
              DashboardViolationRiskDTO violationRiskDTO =
                  violationRiskDTOsByPolicyViolation.get(samePolicyViolationEntry.getKey());
              PolicyViolation policyViolation = samePolicyViolationEntry.getValue();
              addToViolationRiskDTO(violationRiskDTO, policyViolation);
            }

            allUniqueAppPolicyViolations.addAll(diff.getAppeared());
          }
          return localDTOs;
        }, ExecutorThreadPools.getInstance().getThreadPool(ThreadPools.GENERAL)))
        .collect(toList());

    dtoFutures.stream().map(CompletableFuture::join).forEach(riskDTOs::addAll);

    log.debug("buildRiskDTOs: Processed {} policy evaluations and {} policy violations.", policyEvaluationCount,
        policyViolationCount);

    return riskDTOs;
  }

  private DashboardResultsDTO<DashboardViolationRiskDTO> buildResultsDTO(
      List<DashboardViolationRiskDTO> riskDTOs,
      int page,
      int pageSize)
  {
    DashboardResultsDTO<DashboardViolationRiskDTO> result = new DashboardResultsDTO<>();
    if (riskDTOs.isEmpty()) {
      result.dashboardResults = List.of();
    }
    else {
      List<List<DashboardViolationRiskDTO>> pages = Lists.partition(riskDTOs, pageSize);
      result.dashboardResults = page >= pages.size() ? new ArrayList<>() : pages.get(page);
      result.hasNextPage = pages.size() > (page + 1);
    }
    return result;
  }

  private void sort(List<DashboardViolationRiskDTO> riskDTOs, String orderBy) {
    DashboardViolationRiskDTOComparator comparator = new DashboardViolationRiskDTOComparator(orderBy);
    riskDTOs.sort(comparator);
  }

  private DashboardViolationRiskDTO createViolationRiskDTO(
      Application app,
      String orgName,
      PolicyViolation policyViolation)
  {
    DashboardViolationRiskDTO violationRiskDTO = new DashboardViolationRiskDTO();
    violationRiskDTO.applicationName = app.getName();
    violationRiskDTO.organizationName = orgName;
    violationRiskDTO.threatLevel = policyViolation.getThreatLevel();
    violationRiskDTO.firstOccurrenceTime = policyViolation.getOpenTime().getTime();
    violationRiskDTO.policyName = policyViolation.getPolicyName();
    violationRiskDTO.policyViolationId = policyViolation.getId();
    violationRiskDTO.hash = policyViolation.getHash();
    violationRiskDTO.displayName = ComponentDisplayNameUtil.fromPolicyViolation(policyViolation);
    violationRiskDTO.filename = policyViolation.getFilename();
    violationRiskDTO.derivedComponentName = ComponentDisplayNameUtil.deriveComponentName(violationRiskDTO);
    violationRiskDTO.referenceId = findReferenceIdForPolicyViolation(policyViolation);

    return violationRiskDTO;
  }

  private void addToViolationRiskDTO(
      DashboardViolationRiskDTO dashboardViolationRiskDTO,
      PolicyViolation policyViolation)
  {
    long firstOccurrenceTime = policyViolation.getOpenTime().getTime();
    if (dashboardViolationRiskDTO.firstOccurrenceTime > firstOccurrenceTime) {
      dashboardViolationRiskDTO.firstOccurrenceTime = firstOccurrenceTime;
      // return the policy violation id of the earliest violation to match how cross-stage violations work
      // See also ApiCrossStageViolationService::getCrossStageViolationById
      dashboardViolationRiskDTO.policyViolationId = policyViolation.getId();
    }

    if (dashboardViolationRiskDTO.referenceId == null) {
      dashboardViolationRiskDTO.referenceId = findReferenceIdForPolicyViolation(policyViolation);
    }
  }

  private String findReferenceIdForPolicyViolation(final PolicyViolation policyViolation) {
    for (ConstraintFact constraintFact : policyViolation.getConstraintFacts()) {
      for (ConditionFact fact : constraintFact.getConditionFacts()) {
        TriggerReference reference = fact.getReference();
        if (reference != null && Type.SECURITY_VULNERABILITY_REFID.equals(reference.getType())) {
          // All security vulnerability references must point to the same CVE/reference
          return reference.getValue();
        }
      }
    }
    return null;
  }
}
