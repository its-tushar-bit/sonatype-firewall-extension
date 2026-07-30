/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiCrossStageViolationDTOV2;
import com.sonatype.insight.brain.api.v2.service.PolicyViolationAdapter;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO.StageEvaluationWindow;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationComparable;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.repository.hosted.ApplicationForHostedRepositoryComponentService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationComparator;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.86.0
 */
@Named
public class ApiCrossStageViolationService
{
  private final PolicyViolationDAO policyViolationDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final RepositoryDAO repositoryDAO;

  private final ApplicationDAO applicationDAO;

  private final ApplicationService applicationService;

  private final OrganizationDAO organizationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyDAO policyDAO;

  private final OwnerDAO ownerDAO;

  private final Comparator<PolicyViolationComparable> policyViolationComparator = new PolicyViolationComparator();

  private static final Comparator<Date> DATE_COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());

  @Inject
  public ApiCrossStageViolationService(
      PolicyViolationDAO policyViolationDAO,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      RepositoryDAO repositoryDAO,
      ApplicationDAO applicationDAO,
      ApplicationService applicationService,
      OrganizationDAO organizationDAO,
      PolicyEvaluationDAO policyEvaluationDAO,
      PolicyDAO policyDAO,
      OwnerDAO ownerDAO)
  {
    this.policyViolationDAO = policyViolationDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.repositoryDAO = repositoryDAO;
    this.applicationDAO = applicationDAO;
    this.applicationService = applicationService;
    this.organizationDAO = organizationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyDAO = policyDAO;
    this.ownerDAO = ownerDAO;
  }

  /**
   * Throws an exception if there is a violation equivalent to baseViolation that is open at the time that baseViolation
   * is opened. This is done to keep the API clear: CrossStageViolation ids are defined as the ids of the _first_
   * violation that they aggregate. The id of a non-first violation in the aggregation should be treated the same as
   * any other invalid id
   */
  public ApiCrossStageViolationDTOV2 getCrossStageViolationById(String violationId) {
    return getCrossStageViolationByConstituentId(violationId, false);
  }

  /**
   * Returns the CrossStageViolation that _contains_ the given constituentId.
   */
  public ApiCrossStageViolationDTOV2 getCrossStageViolationByConstituentId(String constituentId) {
    return getCrossStageViolationByConstituentId(constituentId, true);
  }

  private ApiCrossStageViolationDTOV2 getCrossStageViolationByConstituentId(
      String constituentId,
      boolean allowEarlierViolations)
  {
    PolicyViolation constituentViolation = policyViolationDAO.getById(constituentId);

    if (constituentViolation == null) {
      RepositoryPolicyViolation repoViolation =
          repositoryPolicyViolationDAO.getByIdWithConstraintFacts(constituentId);
      if (repoViolation != null) {
        return createDtoFromRepositoryViolation(repoViolation);
      }
      throwNotFound(constituentId);
    }

    ReachabilityStatus reachabilityStatus = constituentViolation.getReachabilityStatus();
    Application app = applicationService.getApplicationByIdForRead(constituentViolation.getOwnerId());
    Organization org = organizationDAO.getById(app.getOrganizationId());
    Policy policy = policyDAO.getById(constituentViolation.getPolicyId());
    Owner policyOwner = policy == null ? null : ownerDAO.getById(policy.getOwnerId());

    String ownerId = constituentViolation.getOwnerId();
    String policyId = constituentViolation.getPolicyId();
    String hash = constituentViolation.getHash();

    List<PolicyViolation> allApplicableViolations = policyViolationDAO
        .getByOwnerIdAndPolicyIdAndHash(ownerId, policyId, hash)
        .stream()
        .sorted(Comparator.comparing(PolicyViolation::getOpenTime))
        .collect(Collectors.toList());

    // constituentViolation is a distinct instance from its same-id entry in allApplicableViolations, so it needs its
    // own facts loaded for the comparator.
    List<PolicyViolation> violationsNeedingConstraintFacts = new ArrayList<>(allApplicableViolations);
    violationsNeedingConstraintFacts.add(constituentViolation);
    policyViolationDAO.loadConstraintFacts(violationsNeedingConstraintFacts);

    Collection<PolicyViolation> violationsToMerge = getViolationsToMerge(constituentViolation, allApplicableViolations,
        allowEarlierViolations);
    Collection<PolicyEvaluation> evaluationsForViolationsToMerge = getEvaluationsForViolations(violationsToMerge);

    return createDto(app, org, policyOwner, violationsToMerge, evaluationsForViolationsToMerge, reachabilityStatus);
  }

  private Collection<PolicyEvaluation> getEvaluationsForViolations(Collection<PolicyViolation> violations) {
    if (violations.isEmpty()) {
      return Collections.emptyList();
    }

    // all violations share an ownerId: they originate from getByOwnerIdAndPolicyIdAndHash
    String ownerId = violations.iterator().next().getOwnerId();
    List<StageEvaluationWindow> windows = violations.stream()
        .map(violation -> new StageEvaluationWindow(violation.getStageTypeId(), violation.getOpenTime(),
            getEvaluationMaxDate(violation)))
        .collect(Collectors.toList());

    return policyEvaluationDAO.getLatestEvaluationPerWindow(ownerId, windows);
  }

  private Collection<PolicyViolation> getViolationsToMerge(
      PolicyViolation baseViolation,
      List<PolicyViolation> allViolationsForApp,
      boolean allowEarlierViolations)
  {
    Date baseOpenTime = baseViolation.getOpenTime();

    // separate violations into those before and those after baseViolation
    Map<Boolean, List<PolicyViolation>> groupedViolations = allViolationsForApp.stream()
        .collect(Collectors.groupingBy(viol -> DATE_COMPARATOR.compare(viol.getOpenTime(), baseOpenTime) >= 0));

    List<PolicyViolation> allEarlierViolations = groupedViolations.getOrDefault(false, Collections.emptyList());
    List<PolicyViolation> allLaterViolations = groupedViolations.getOrDefault(true, Collections.emptyList());

    List<PolicyViolation> earlierEquivalentViolations =
        getEquivalentEarlierViolations(baseViolation, allEarlierViolations);

    if (!allowEarlierViolations && !earlierEquivalentViolations.isEmpty()) {
      throwNotFound(baseViolation.getId());
    }

    List<PolicyViolation> laterEquivalentViolations =
        getEquivalentLaterViolations(baseViolation, allLaterViolations);

    List<PolicyViolation> retval = earlierEquivalentViolations;
    retval.addAll(laterEquivalentViolations);
    return retval;
  }

  private List<PolicyViolation> getEquivalentLaterViolations(
      PolicyViolation baseViolation,
      List<PolicyViolation> allLaterViolations)
  {
    Date overallFixTime = baseViolation.getFixOrWaiveTime();
    List<PolicyViolation> retval = new ArrayList<>();

    for (PolicyViolation laterViolation : allLaterViolations) {
      if (overallFixTime != null && DATE_COMPARATOR.compare(overallFixTime, laterViolation.getOpenTime()) < 0) {
        // we've seen all relevant violations; stop iterating
        break;
      }
      else if (policyViolationComparator.compare(baseViolation, laterViolation) == 0) {
        // found an equivalent violation with overlapping time span; add it to the lists
        retval.add(laterViolation);

        Date laterViolationFixTime = laterViolation.getFixOrWaiveTime();
        if (DATE_COMPARATOR.compare(laterViolationFixTime, overallFixTime) > 0) {
          overallFixTime = laterViolationFixTime;
        }
      }
      // else unrelated violation; ignore
    }

    return retval;
  }

  private List<PolicyViolation> getEquivalentEarlierViolations(
      PolicyViolation baseViolation,
      List<PolicyViolation> allEarlierViolations)
  {
    Date openTime = baseViolation.getOpenTime();
    List<PolicyViolation> retval = new LinkedList<>();

    for (int i = allEarlierViolations.size() - 1; i >= 0; i--) {
      PolicyViolation violation = allEarlierViolations.get(i);
      Date violationFixOrWaiveTime = violation.getFixOrWaiveTime();

      if (DATE_COMPARATOR.compare(violationFixOrWaiveTime, openTime) > 0 &&
          policyViolationComparator.compare(baseViolation, violation) == 0)
      {
        // prepend in order to preserve order
        retval.add(0, violation);

        if (DATE_COMPARATOR.compare(openTime, violation.getOpenTime()) > 0) {
          openTime = violation.getOpenTime();
        }
      }
    }

    return retval;
  }

  private Date getEvaluationMaxDate(PolicyViolation violation) {
    // for auto-waived violations, we don't care about the max date, we just want the latest evaluation
    return violation.isAutoWaived() ? null : violation.getFixOrWaiveTime();
  }

  private ApiCrossStageViolationDTOV2 createDto(
      Application app,
      Organization org,
      Owner policyOwner,
      Collection<PolicyViolation> policyViolations,
      Collection<PolicyEvaluation> policyEvaluations,
      final ReachabilityStatus reachabilityStatus)
  {
    ApiCrossStageViolationDTOV2 dto = new ApiCrossStageViolationDTOV2();

    PolicyViolation firstViolation = policyViolations.iterator().next();

    Map<String, PolicyViolation> violationsByStageTypeId = policyViolations.stream()
        .collect(Collectors.toMap(PolicyViolation::getStageTypeId, v -> v, (first, later) -> later));

    dto.policyViolationId = firstViolation.getId();
    dto.reachabilityStatus = reachabilityStatus;
    dto.applicationPublicId = app.getPublicId();
    dto.applicationName = app.getName();
    dto.organizationName = org.getName();
    dto.threatLevel = firstViolation.getThreatLevel();
    dto.policyId = firstViolation.getPolicyId();
    dto.policyName = firstViolation.getPolicyName();
    dto.hash = firstViolation.getHash();
    dto.policyThreatCategory = firstViolation.getThreatCategory().getName();
    dto.displayName = ComponentDisplayNameUtil.fromPolicyViolation(firstViolation);
    dto.filename = firstViolation.getFilename();
    dto.componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(firstViolation.getComponentIdentifier());

    dto.policyOwner = new ApiCrossStageViolationDTOV2.PolicyOwner();
    if (policyOwner != null) {
      dto.policyOwner.ownerId = policyOwner.getId();
      dto.policyOwner.ownerName = policyOwner.getName();
      dto.policyOwner.ownerType = policyOwner.getType().toString();

      // even though the Organization model is willing to return its internal id as a public id, we don't want
      // to give external consumers the impression that Organizations have a public id
      if (policyOwner instanceof Application) {
        dto.policyOwner.ownerPublicId = policyOwner.getPublicId();
      }
    }

    dto.openTime = policyViolations.stream()
        .map(PolicyViolation::getOpenTime)
        .min(DATE_COMPARATOR)
        .get();

    dto.fixTime = policyViolations.stream()
        .map(PolicyViolation::getFixOrWaiveTime)
        // can't use max here because it doesn't like nulls
        .reduce(new Date(0), (d1, d2) -> DATE_COMPARATOR.compare(d1, d2) > 0 ? d1 : d2);

    dto.stageData = policyEvaluations.stream()
        .collect(Collectors.toMap(
            PolicyEvaluation::getStageTypeId,
            eval -> createStageData(eval, violationsByStageTypeId.get(eval.getStageTypeId())),
            (first, later) -> later));
    dto.constraintViolations = PolicyViolationAdapter.convert(firstViolation);

    return dto;
  }

  private ApiCrossStageViolationDTOV2.StageData createStageData(
      PolicyEvaluation policyEvaluation,
      PolicyViolation policyViolation)
  {
    ApiCrossStageViolationDTOV2.StageData stageData = new ApiCrossStageViolationDTOV2.StageData();

    stageData.mostRecentEvaluationTime = policyEvaluation.getTime();
    stageData.mostRecentScanId = policyEvaluation.getScanId();
    stageData.actionTypeId = policyViolation.getActionTypeId();

    return stageData;
  }

  private ApiCrossStageViolationDTOV2 createDtoFromRepositoryViolation(
      final RepositoryPolicyViolation violation)
  {
    Repository repository = repositoryDAO.getById(violation.getRepositoryId());
    String repoPublicId = repository != null ? repository.getPublicId() : null;
    // For archive-of-archives fan-out (CLM-40943) the evaluator persists inner-jar violations
    // against synthetic inner pathnames like `outer.zip!/inner.jar`, but only ONE synthetic
    // application is created — for the OUTER pathname. Resolve inner-pathname violations to
    // the outer's synthetic app by stripping the "!/..." suffix before generating the
    // publicId; outer-pathname violations and pre-fan-out single-component scans go through
    // unchanged because they don't contain "!/" .
    String resolvedPathname = stripInnerArchiveSuffix(violation.getPathname());
    String appPublicId = ApplicationForHostedRepositoryComponentService
        .generatePublicId(repoPublicId, resolvedPathname);
    Application app = applicationDAO.getByPublicId(appPublicId);
    if (app == null) {
      throwNotFound(violation.getId());
    }
    Organization org = organizationDAO.getById(app.getOrganizationId());
    Policy policy = policyDAO.getById(violation.getPolicyId());
    Owner policyOwner = policy == null ? null : ownerDAO.getById(policy.getOwnerId());

    ApiCrossStageViolationDTOV2 dto = new ApiCrossStageViolationDTOV2();
    dto.policyViolationId = violation.getId();
    dto.applicationPublicId = app.getPublicId();
    dto.applicationName = app.getName();
    dto.organizationName = org != null ? org.getName() : null;
    dto.threatLevel = violation.getThreatLevel();
    dto.policyId = violation.getPolicyId();
    dto.policyName = violation.getPolicyName();
    dto.hash = violation.getHash();
    dto.policyThreatCategory = violation.getThreatCategory() != null
        ? violation.getThreatCategory().getName()
        : null;
    dto.displayName = ComponentDisplayNameUtil.fromIdentifier(violation.getComponentIdentifier());
    dto.componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(violation.getComponentIdentifier());
    dto.constraintViolations = PolicyViolationAdapter.convert(violation);
    dto.stageData = Collections.emptyMap();
    dto.openTime = violation.getOpenTime();
    dto.policyOwner = new ApiCrossStageViolationDTOV2.PolicyOwner();
    if (policyOwner != null) {
      dto.policyOwner.ownerId = policyOwner.getId();
      dto.policyOwner.ownerName = policyOwner.getName();
      dto.policyOwner.ownerType = policyOwner.getType().toString();
      if (policyOwner instanceof Application) {
        dto.policyOwner.ownerPublicId = policyOwner.getPublicId();
      }
    }
    return dto;
  }

  private void throwNotFound(String violationId) {
    throw new NotFoundException("Policy Violation " + violationId + " not found");
  }

  /**
   * Strips the inner-archive {@code "!/..."} suffix from a pathname so that an
   * archive-of-archives inner-jar pathname (e.g. {@code outer.zip!/log4j-core-2.14.1.jar})
   * resolves to the outer artifact's synthetic application (e.g. {@code outer.zip}). Returns
   * the input unchanged if there is no {@code "!/"} marker.
   */
  private static String stripInnerArchiveSuffix(final String pathname) {
    if (pathname == null) {
      return null;
    }
    int sep = pathname.indexOf("!/");
    return sep < 0 ? pathname : pathname.substring(0, sep);
  }
}
