/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectRenewWaiverSlice = prop('firewallRenewWaiver');

export const selectRenewWaiverLoading = createSelector(selectRenewWaiverSlice, prop('loading'));
export const selectRenewWaiverError = createSelector(selectRenewWaiverSlice, prop('loadError'));
export const selectRenewWaiverWaiver = createSelector(selectRenewWaiverSlice, prop('waiver'));
export const selectRenewWaiverNewExpiryTime = createSelector(selectRenewWaiverSlice, prop('newExpiryTime'));
export const selectRenewWaiverCustomExpiryTime = createSelector(selectRenewWaiverSlice, prop('customExpiryTime'));
export const selectRenewWaiverComment = createSelector(selectRenewWaiverSlice, prop('comment'));
export const selectRenewWaiverReasonId = createSelector(selectRenewWaiverSlice, prop('reasonId'));
export const selectRenewWaiverSubmitMaskState = createSelector(selectRenewWaiverSlice, prop('submitMaskState'));
export const selectRenewWaiverSubmitError = createSelector(selectRenewWaiverSlice, prop('submitError'));
export const selectRenewWaiverIsDirty = createSelector(selectRenewWaiverSlice, prop('isDirty'));
export const selectRenewWaiverReasons = createSelector(selectRenewWaiverSlice, prop('waiverReasons'));
export const selectRenewWaiverReasonsLoading = createSelector(selectRenewWaiverSlice, prop('waiverReasonsLoading'));
export const selectRenewWaiverReasonsError = createSelector(selectRenewWaiverSlice, prop('waiverReasonsError'));
export const selectRenewWaiverReturnStateName = createSelector(selectRenewWaiverSlice, prop('returnStateName'));
export const selectRenewWaiverReturnParams = createSelector(selectRenewWaiverSlice, prop('returnParams'));
