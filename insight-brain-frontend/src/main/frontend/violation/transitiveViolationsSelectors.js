/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { selectPreviousRouteName, selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

export const selectTransitiveViolations = prop('transitiveViolations');

export const selectDeleteWaiver = prop('deleteWaiver');

export const selectAvailableScopes = createSelector(selectTransitiveViolations, prop('availableScopes'));

export const selectReportMetadata = createSelector(selectTransitiveViolations, prop('reportMetadata'));

export const selectComponentTransitivePolicyViolations = createSelector(
  selectTransitiveViolations,
  prop('componentTransitivePolicyViolations')
);

export const selectIsRequestWaiveTransitiveViolationsOpen = createSelector(
  selectTransitiveViolations,
  prop('isRequestWaiveTransitiveViolationsOpen')
);

export const selectTransitiveViolationWaivers = createSelector(
  selectTransitiveViolations,
  prop('transitiveViolationWaivers')
);

export const selectIsWaiveTransitiveViolationsOpen = createSelector(
  selectTransitiveViolations,
  prop('isWaiveTransitiveViolationsOpen')
);

export const selectIsViewTransitiveViolationWaiversOpen = createSelector(
  selectTransitiveViolations,
  prop('isViewTransitiveViolationWaiversOpen')
);

export const selectTransitiveOwnerType = createSelector(selectRouterCurrentParams, prop('ownerType'));

export const selectTransitiveOwnerId = createSelector(selectRouterCurrentParams, prop('ownerId'));

export const selectTransitiveHash = createSelector(selectRouterCurrentParams, prop('hash'));

export const selectTransitiveScanId = createSelector(selectRouterCurrentParams, prop('scanId'));

export const selectShouldGoBackToComponentDetails = createSelector(
  selectPreviousRouteName,
  (prevState) => 'applicationReport.policy' !== prevState
);
