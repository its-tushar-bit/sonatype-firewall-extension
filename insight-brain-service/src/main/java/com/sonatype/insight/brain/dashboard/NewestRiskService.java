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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.policy.TriggerReference.Type;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditUtils;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
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
import com.sonatype.insight.brain.utils.ExecutorThreadPools.ThreadPools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.utils.ExecutorThreadPools.getThreadPool;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Named
public class NewestRiskService
{
  private static final Logger log = LoggerFactory.getLogger(NewestRiskService.class);

  private final ApplicationService applicationService;

  private final OrganizationDAO organizationDAO;

  private final PolicyViolationLoader policyViolationLoader;

  private final DashboardUtils dashboardUtils;

  @Inject
  public NewestRiskService(ApplicationService applicationService,
                           OrganizationDAO organizationDAO,
                           PolicyViolationLoader policyViolationLoader,
                           DashboardUtils dashboardUtils)
  {
    this.applicationService = applicationService;
    this.organizationDAO = organizationDAO;
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
    dashboardUtils.validateDashboardLicensedAndEnabled();

    validateMaxDaysOld(maxDaysOld);

    long start = System.currentTimeMillis();

    List<Application> applications = getApplications(organizationIds, applicationIds, tagIds);

    AuditData.get() //
        .setData("selectedOrganizations", AuditUtils.getSelectedOrganizationsById(organizationIds)) //
        .setData("selectedApplications",
            AuditUtils.getSelectedApplicationsById(applicationIds, organizationIds, applications)) //
        .setSelectedApplicationCategories(AuditUtils.getSelectedApplicationCategoriesById(tagIds)) //
        .setData("inspectedApplicationCount", applications.size());

    Collection<ApplicationView> appViews = getPolicyViolations(applications, stageIds, policyThreatCategoryFilter,
        policyThreatLevelFilter, policyViolationStateFilter, maxDaysOld);

    List<NewestRiskDTO> riskDTOs = buildRiskDTOs(appViews);

    sort(riskDTOs, orderBy);

    DashboardResultsDTO<NewestRiskDTO> result = buildResultsDTO(riskDTOs, maxResults);

    AuditData.get().setData("resultRecordCount", result.numResults);

    log.debug("getNewestRisks finished in {} ms", System.currentTimeMillis() - start);

    return result;
  }

  private void validateMaxDaysOld(Integer maxDaysOld) {
    if (maxDaysOld != null && maxDaysOld < 1) {
      throw new IllegalArgumentException("Max Days Old must be a positive integer");
    }
  }

  private Collection<ApplicationView> getPolicyViolations(List<Application> applications,
                                                          Set<String> stageIds,
                                                          PolicyThreatCategoryFilter policyThreatCategoryFilter,
                                                          PolicyThreatLevelFilter policyThreatLevelFilter,
                                                          PolicyViolationStateFilter policyViolationStateFilter,
                                                          Integer maxDaysOld)
  {
    Set<StageType> stageTypes = dashboardUtils.getStageTypes(stageIds);
    Predicate<? super PolicyViolation> filter = dashboardUtils.buildViolationFilter(policyThreatCategoryFilter,
        policyThreatLevelFilter, policyViolationStateFilter);

    Date minDate = (maxDaysOld == null) ? null
        : new Date(Instant.now().minus(Duration.ofDays(maxDaysOld)).toEpochMilli());
    return policyViolationLoader.getViolations(applications, stageTypes, false, filter, minDate,
        policyThreatLevelFilter, policyThreatCategoryFilter, policyViolationStateFilter);
  }

  private List<NewestRiskDTO> buildRiskDTOs(Collection<ApplicationView> appViews) {
    List<NewestRiskDTO> riskDTOs = new ArrayList<>();

    final AtomicInteger policyEvaluationCount = new AtomicInteger(0);
    final AtomicInteger policyViolationCount = new AtomicInteger(0);

    Map<String, String> orgNames = new ConcurrentHashMap<>();
    List<CompletableFuture<List<NewestRiskDTO>>> dtoFutures = appViews.stream()
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
              NewestRiskDTO newestRiskDTO = createNewestRiskDTO(app, orgName, policyEvaluation, policyViolation);
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
        }, getThreadPool(ThreadPools.GENERAL))).collect(toList());

    dtoFutures.stream().map(CompletableFuture::join).forEach(riskDTOs::addAll);

    log.debug("getNewestRisks: Processed {} policy evaluations and {} policy violations.", policyEvaluationCount,
        policyViolationCount);

    return riskDTOs;
  }

  private DashboardResultsDTO<NewestRiskDTO> buildResultsDTO(List<NewestRiskDTO> riskDTOs, int maxResults) {
    DashboardResultsDTO<NewestRiskDTO> result = new DashboardResultsDTO<>();
    result.numResults = riskDTOs.size();
    result.dashboardResults = riskDTOs.subList(0, Math.min(riskDTOs.size(), maxResults));

    return result;
  }

  private void sort(List<NewestRiskDTO> riskDTOs, String orderBy) {
    NewestRiskDTOComparator comparator = new NewestRiskDTOComparator(orderBy);
    riskDTOs.sort(comparator);
  }

  private List<Application> getApplications(Set<String> organizationIds,
                                            Set<String> applicationIds,
                                            Set<String> tagIds)
  {
    long start = System.currentTimeMillis();

    List<Application> applications = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds,
        applicationIds, tagIds);

    log.debug("getNewestRisks: Found {} applications filtered by appIds={} and tagIds={} in {} ms.",
        applications.size(), !isEmpty(applicationIds), !isEmpty(tagIds), System.currentTimeMillis() - start);

    return applications;
  }

  private NewestRiskDTO createNewestRiskDTO(Application app,
                                            String orgName,
                                            PolicyEvaluation policyEvaluation,
                                            PolicyViolation policyViolation)
  {
    NewestRiskDTO newestRiskDTO = new NewestRiskDTO();
    newestRiskDTO.applicationPublicId = app.getPublicId();
    newestRiskDTO.applicationName = app.getName();
    newestRiskDTO.organizationName = orgName;
    newestRiskDTO.threatLevel = policyViolation.getThreatLevel();
    newestRiskDTO.firstOccurrenceTime = policyViolation.getOpenTime().getTime();
    newestRiskDTO.policyId = policyViolation.getPolicyId();
    newestRiskDTO.policyName = policyViolation.getPolicyName();
    newestRiskDTO.policyViolationId = policyViolation.getId();
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
      // return the policy violation id of the earliest violation to match how cross-stage violations work
      // See also ApiCrossStageViolationService::getCrossStageViolationById
      newestRiskDTO.policyViolationId = policyViolation.getId();
    }

    if (newestRiskDTO.referenceId == null) {
      newestRiskDTO.referenceId = findReferenceIdForPolicyViolation(policyViolation);
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
