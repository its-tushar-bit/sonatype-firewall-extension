/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { prop, path } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

export const selectViolationSlice = prop('violation');
export const selectRequestWaiverSlice = prop('requestWaiver');
export const selectWaiverSlice = prop('waivers');
export const selectAddWaiverSlice = prop('addWaiver');

export const selectLoadingViolation = createSelector(selectViolationSlice, prop('loading'));
export const selectViolationDetailsError = createSelector(selectViolationSlice, prop('violationDetailsError'));
export const selectViolationDetails = createSelector(selectViolationSlice, prop('violationDetails'));

export const selectAddWaiverDataLoading = createSelector(selectAddWaiverSlice, prop('loading'));
export const selectAddWaiverDataError = createSelector(selectAddWaiverSlice, prop('loadError'));
export const selectAddWaiverData = createSelector(selectAddWaiverSlice, (addWaiver) => ({ ...addWaiver }));
export const selectWaiverReasonsState = createSelector(selectWaiverSlice, prop('waiverReasons'));
export const selectWaiverReasons = createSelector(selectWaiverSlice, path(['waiverReasons', 'data']));

export const selectWaiverSelectedScopeLoading = createSelector(selectRequestWaiverSlice, prop('loading'));
export const selectWaiverSelectedScopeError = createSelector(selectRequestWaiverSlice, prop('loadError'));
export const selectSubmitError = createSelector(selectRequestWaiverSlice, prop('submitError'));
export const selectSubmitMaskState = createSelector(selectRequestWaiverSlice, prop('submitMaskState'));
export const selectSelectedWaiverScope = createSelector(selectRequestWaiverSlice, prop('selectedWaiverScope'));
export const selectComponentMatcherStrategy = createSelector(
  selectRequestWaiverSlice,
  prop('componentMatcherStrategy')
);
export const selectExpiryTime = createSelector(selectRequestWaiverSlice, prop('expiryTime'));
export const selectCustomExpiryTime = createSelector(selectRequestWaiverSlice, prop('customExpiryTime'));
export const selectWaiverReasonId = createSelector(selectRequestWaiverSlice, prop('waiverReasonId'));
export const selectComments = createSelector(selectRequestWaiverSlice, prop('comments'));
export const selectNoteToReviewer = createSelector(selectRequestWaiverSlice, prop('noteToReviewer'));
export const selectRejectionReason = createSelector(selectRequestWaiverSlice, prop('rejectionReason'));
