/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import moment from 'moment';
import { compose, descend, filter, hasPath, prop, sort, values } from 'ramda';
import { waiverMatcherStrategy } from '../util/waiverUtils';

export const selectViolationSlice = prop('violation');
const selectTransitiveViolationsSlice = prop('transitiveViolations');

export const selectViolationLoadingError = createSelector(selectViolationSlice, prop('violationDetailsError'));
export const selectViolationIsLoading = createSelector(selectViolationSlice, prop('loading'));
export const selectViolationDetails = createSelector(selectViolationSlice, prop('violationDetails'));
export const selectViolationPolicyName = createSelector(selectViolationDetails, prop('policyName'));
export const selectViolationThreatLevel = createSelector(selectViolationDetails, prop('threatLevel'));
export const selectPolicyExists = createSelector(selectViolationDetails, hasPath(['policyOwner', 'ownerId']));

export const selectTransitiveViolationsData = createSelector(
  selectTransitiveViolationsSlice,
  (transitiveViolationsSlice) => transitiveViolationsSlice?.componentTransitivePolicyViolations?.data?.violations
);

export const selectApplicableWaivers = createSelector(selectViolationSlice, ({ activeWaivers, expiredWaivers }) => {
  return { activeWaivers, expiredWaivers };
});

export const selectApplicableAutoWaiver = createSelector(
  selectViolationSlice,
  ({ autoWaiver, loadingAutoWaiver, loadAutoWaiverError }) => ({
    autoWaiver,
    loadingAutoWaiver,
    loadAutoWaiverError,
  })
);

export const selectHasPermissionForAppWaivers = createSelector(
  selectViolationSlice,
  prop('hasPermissionForAppWaivers')
);

/**
 * Extracts the security vulnerability reference ID (e.g. CVE-2023-35116) from constraint violations.
 */
export function extractSecurityVulnerabilityRefId(constraintViolations) {
  if (!constraintViolations?.length) return null;
  for (const constraint of constraintViolations) {
    for (const reason of constraint.reasons || []) {
      if (reason.reference?.type === 'SECURITY_VULNERABILITY_REFID') {
        return reason.reference.value;
      }
    }
  }
  return null;
}

/**
 * Finds the scanId of the most recent evaluation across all stages.
 */
export function getMostRecentScanId(stageData) {
  if (!stageData) return null;
  let mostRecentTime = 0;
  let result = null;
  for (const stage of values(stageData)) {
    if (stage?.mostRecentScanId && stage.mostRecentEvaluationTime) {
      const time = new Date(stage.mostRecentEvaluationTime).getTime();
      if (time > mostRecentTime) {
        mostRecentTime = time;
        result = stage.mostRecentScanId;
      }
    }
  }
  return result;
}

export const selectViolationFilteredSimilarWaivers = createSelector(
  selectViolationSlice,
  ({ similarWaivers, similarWaiversFilterSelectedIds }) => {
    const filterActive = filter((waiver) => {
      if (!similarWaiversFilterSelectedIds.has('active') || !waiver.expiryTime) return true;
      const diff = Math.ceil(moment(waiver.expiryTime).diff(moment(), 'days', true));
      return diff > 0;
    });
    const filterExact = filter((waiver) =>
      similarWaiversFilterSelectedIds.has('exact')
        ? waiver.matcherStrategy === waiverMatcherStrategy.EXACT_COMPONENT
        : true
    );
    const filterComment = filter((waiver) =>
      similarWaiversFilterSelectedIds.has('comment') ? !!waiver.comment : true
    );

    const filteredResults = compose(
      filterActive,
      filterExact,
      filterComment,
      sort(descend(prop('createTime')))
    )(similarWaivers);
    return filteredResults;
  }
);
