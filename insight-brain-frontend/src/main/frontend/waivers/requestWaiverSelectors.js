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

export const selectLoadingViolation = createSelector(selectViolationSlice, prop('loading'));
export const selectViolationDetailsError = createSelector(selectViolationSlice, prop('violationDetailsError'));
export const selectViolationDetails = createSelector(selectViolationSlice, prop('violationDetails'));

export const selectSubmitLoading = createSelector(selectRequestWaiverSlice, prop('loading'));
export const selectSubmitError = createSelector(selectRequestWaiverSlice, prop('submitError'));
export const selectComments = createSelector(selectRequestWaiverSlice, prop('comments'));
export const selectSubmitMaskState = createSelector(selectRequestWaiverSlice, prop('submitMaskState'));
export const selectWaiverReasons = createSelector(selectWaiverSlice, path(['waiverReasons', 'data']));
export const selectWaiverReasonId = createSelector(selectRequestWaiverSlice, prop('waiverReasonId'));

export const selectWaiverRequestWebhookState = createSelector(selectRequestWaiverSlice, prop('webhooks'));
