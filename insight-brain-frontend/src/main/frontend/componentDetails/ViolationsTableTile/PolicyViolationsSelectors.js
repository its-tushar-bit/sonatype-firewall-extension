/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { selectIsTransitiveViolations, selectRouterCurrentParams } from '../../reduxUiRouter/routerSelectors';
import { selectTransitiveViolationsData } from 'MainRoot/violation/violationSelectors';

export const selectComponentDetailsViolationsSlice = prop('componentDetailsPolicyViolations');

const violationsSlice = createSelector(selectComponentDetailsViolationsSlice, prop('violations'));
const violationTypeSlice = createSelector(selectComponentDetailsViolationsSlice, prop('violationType'));
export const selectComponentViolations = createSelector(
  violationsSlice,
  violationTypeSlice,
  (violations, violationType) => {
    if (violations && violationType) {
      return violations.filter((violation) => violation.policyThreatCategory === violationType);
    } else {
      return violations;
    }
  }
);

export const selectComponentWaivers = createSelector(selectComponentDetailsViolationsSlice, prop('waivers'));

export const selectSelectedViolationId = createSelector(
  selectComponentDetailsViolationsSlice,
  selectRouterCurrentParams,
  (componentDetailsPolicyViolations, routerCurrentParams) => {
    return (
      routerCurrentParams.violationId ||
      routerCurrentParams.id ||
      componentDetailsPolicyViolations.selectedPolicyViolationId
    );
  }
);

export const selectSelectedComponentPolicyViolationId = createSelector(
  selectComponentDetailsViolationsSlice,
  prop('selectedPolicyViolationId')
);

export const selectSelectedComponentPolicyViolation = createSelector(
  selectSelectedComponentPolicyViolationId,
  violationsSlice,
  selectTransitiveViolationsData,
  selectIsTransitiveViolations,
  (violationId, violations, transitiveViolations, isTransitiveViolations) =>
    (isTransitiveViolations ? transitiveViolations : violations)?.find(
      (violation) => violation.policyViolationId === violationId
    )
);

export const selectIsPolicyViolationsLoading = createSelector(selectComponentDetailsViolationsSlice, prop('loading'));

export const selectShowViolationsDetailPopover = createSelector(
  selectComponentDetailsViolationsSlice,
  prop('showViolationsDetailPopover')
);

export const selectViolationsDetailRowClicked = createSelector(
  selectComponentDetailsViolationsSlice,
  prop('violationsDetailRowClicked')
);

export const selectSelectedPolicyViolation = createSelector(
  selectComponentDetailsViolationsSlice,
  prop('selectedPolicyViolation')
);

export const selectIsViolationsDetailPopoverOpen = createSelector(
  selectShowViolationsDetailPopover,
  selectViolationsDetailRowClicked,
  (showViolationsDetailPopover, violationsDetailRowClicked) => {
    return showViolationsDetailPopover || violationsDetailRowClicked;
  }
);
