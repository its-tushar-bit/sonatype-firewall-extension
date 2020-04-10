/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiCrossStageViolationDTOV2;
import com.sonatype.insight.brain.api.v2.service.PolicyViolationAdapter;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationComparable;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationComparator;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.86.0
 */
@Named
public class ApiCrossStageViolationService
{
  private final PolicyViolationDAO policyViolationDAO;

  private final ApplicationService applicationService;

  private final OrganizationDAO organizationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyDAO policyDAO;

  private final OwnerDAO ownerDAO;

  private final PolicyViolationAdapter policyViolationAdapter;

  private final Comparator<PolicyViolationComparable> policyViolationComparator = new PolicyViolationComparator();

  private static final Comparator<Date> DATE_COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());

  @Inject
  public ApiCrossStageViolationService(
      PolicyViolationDAO policyViolationDAO,
      ApplicationService applicationService,
      OrganizationDAO organizationDAO,
      PolicyEvaluationDAO policyEvaluationDAO,
      PolicyDAO policyDAO,
      OwnerDAO ownerDAO,
      PolicyViolationAdapter policyViolationAdapter)
  {
    this.policyViolationDAO = policyViolationDAO;
    this.applicationService = applicationService;
    this.organizationDAO = organizationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyDAO = policyDAO;
    this.ownerDAO = ownerDAO;
    this.policyViolationAdapter = policyViolationAdapter;
  }

  public ApiCrossStageViolationDTOV2 getCrossStageViolationById(String violationId) {
    PolicyViolation baseViolation = policyViolationDAO.getById(violationId);

    if (baseViolation == null) {
      throwNotFound(violationId);
    }

    Application app = applicationService.getApplicationByIdForRead(baseViolation.getApplicationId());
    Organization org = organizationDAO.getById(app.getOrganizationId());
    Policy policy = policyDAO.getById(baseViolation.getPolicyId());
    Owner policyOwner = ownerDAO.getById(policy.getOwnerId());
    List<PolicyViolation> allViolationsForApp = policyViolationDAO.getByApplicationId(app.getId());

    ensureNoEquivalentOpenPriorTo(baseViolation, allViolationsForApp);

    Collection<PolicyViolation> violationsToMerge = getViolationsToMerge(baseViolation, allViolationsForApp);
    Collection<PolicyEvaluation> evaluationsForViolationsToMerge = violationsToMerge.stream()
        .map(this::getLatestEvaluationForViolation)
        .collect(Collectors.toList());

    return createDto(violationId, app, org, policyOwner, violationsToMerge, evaluationsForViolationsToMerge);
  }

  private Collection<PolicyViolation> getViolationsToMerge(
      PolicyViolation baseViolation,
      List<PolicyViolation> allViolationsForApp)
  {
    Date overallFixTime = baseViolation.getFixOrWaiveTime();
    Collection<PolicyViolation> retval = new ArrayList<>();

    // Ordered list of violations opened after or simultaneously with baseViolation.
    // Note that this list will include the baseViolation again, so no need to add that to the output separately
    List<PolicyViolation> allLaterViolations =
        getOrderedListOfViolationsAfter(baseViolation.getOpenTime(), allViolationsForApp);

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

  private List<PolicyViolation> getOrderedListOfViolationsAfter(Date date, Collection<PolicyViolation> violations) {
    return violations.stream()
        .filter(v -> DATE_COMPARATOR.compare(v.getOpenTime(), date) >= 0)
        .sorted(Comparator.comparing(PolicyViolation::getOpenTime))
        .collect(Collectors.toList());
  }

  private PolicyEvaluation getLatestEvaluationForViolation(PolicyViolation violation) {
    PolicyEvaluation latestEvaluationForViolation = policyEvaluationDAO.getLastInTimeRangeByApplicationAndStage(
        violation.getApplicationId(), violation.getStageTypeId(), violation.getOpenTime(),
        violation.getFixOrWaiveTime());

    return latestEvaluationForViolation;
  }

  private ApiCrossStageViolationDTOV2 createDto(
      String violationId,
      Application app,
      Organization org,
      Owner policyOwner,
      Collection<PolicyViolation> policyViolations,
      Collection<PolicyEvaluation> policyEvaluations)
  {
    ApiCrossStageViolationDTOV2 dto = new ApiCrossStageViolationDTOV2();

    PolicyViolation firstViolation = policyViolations.iterator().next();

    Map<String, PolicyViolation> violationsByStageTypeId = policyViolations.stream()
        .collect(Collectors.toMap(PolicyViolation::getStageTypeId, v -> v));

    dto.policyViolationId = violationId;
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
    dto.policyOwner.ownerId = policyOwner.getId();
    dto.policyOwner.ownerName = policyOwner.getName();
    dto.policyOwner.ownerType = policyOwner.getType().toString();

    // even though the Organization model is willing to return its internal id as a public id, we don't want
    // to give external consumers the impression that Organizations have a public id
    if (policyOwner instanceof Application) {
      dto.policyOwner.ownerPublicId = policyOwner.getPublicId();
    }

    dto.openTime = policyViolations.stream()
        .map(PolicyViolation::getOpenTime)
        .min(DATE_COMPARATOR)
        .get()
        .getTime();

    Date maxFixTime = policyViolations.stream()
        .map(PolicyViolation::getFixOrWaiveTime)
        // can't use max here because it doesn't like nulls
        .reduce(new Date(0), (d1, d2) -> DATE_COMPARATOR.compare(d1, d2) > 0 ? d1 : d2);

    dto.fixTime = maxFixTime == null ? null : maxFixTime.getTime();

    dto.stageData = policyEvaluations.stream()
        .collect(Collectors.toMap(
            PolicyEvaluation::getStageTypeId,
            eval -> createStageData(eval, violationsByStageTypeId.get(eval.getStageTypeId()))
        ));

    dto.constraintViolations = policyViolationAdapter.convert(firstViolation);

    return dto;
  }

  private ApiCrossStageViolationDTOV2.StageData createStageData(
      PolicyEvaluation policyEvaluation,
      PolicyViolation policyViolation)
  {
    ApiCrossStageViolationDTOV2.StageData stageData = new ApiCrossStageViolationDTOV2.StageData();

    stageData.mostRecentEvaluationTime = policyEvaluation.getTime().getTime();
    stageData.mostRecentScanId = policyEvaluation.getScanId();
    stageData.actionTypeId = policyViolation.getActionTypeId();

    return stageData;
  }

  /**
   * Throws an exception if there is a violation equivalent to baseViolation that is open at the time that baseViolation
   * is opened.  This is done to keep the API clear: CrossStageViolation ids are defined as the ids of the _first_
   * violation that they aggregate.  The id of a non-first violation in the aggregation should be treated the same as
   * any other invalid id
   */
  private void ensureNoEquivalentOpenPriorTo(
      PolicyViolation baseViolation,
      Collection<PolicyViolation> allViolationsForApp)
  {
    Date openTime = baseViolation.getOpenTime();

    Stream<PolicyViolation> violationsClosedAfter = allViolationsForApp.stream()
        .filter(v -> {
          Date fixOrWaiveTime = v.getFixOrWaiveTime();
          return fixOrWaiveTime == null || DATE_COMPARATOR.compare(fixOrWaiveTime, openTime) >= 0;
        });

    Stream<PolicyViolation> violationsClosedAfterAndOpenedBefore = violationsClosedAfter
        .filter(v -> DATE_COMPARATOR.compare(v.getOpenTime(), openTime) < 0);

    Stream<PolicyViolation> equivalentEarlierViolations = violationsClosedAfterAndOpenedBefore
        .filter(v -> policyViolationComparator.compare(v, baseViolation) == 0);

    // if the resulting stream is non-empty, throw an exception
    if (equivalentEarlierViolations.findAny().isPresent()) {
      throwNotFound(baseViolation.getId());
    }
  }

  private void throwNotFound(String violationId) {
    throw new NotFoundException("Policy Violation " + violationId + " not found");
  }
}
