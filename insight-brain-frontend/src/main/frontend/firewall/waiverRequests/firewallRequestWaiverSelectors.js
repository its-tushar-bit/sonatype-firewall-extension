/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectFirewallRequestWaiverSlice = prop('firewallRequestWaiver');

export const selectFirewallRequestWaiverLoading = createSelector(selectFirewallRequestWaiverSlice, prop('loading'));

export const selectFirewallRequestWaiverLoadError = createSelector(selectFirewallRequestWaiverSlice, prop('loadError'));

export const selectFirewallRequestWaiverSubmitError = createSelector(
  selectFirewallRequestWaiverSlice,
  prop('submitError')
);

export const selectFirewallRequestWaiverSubmitMaskState = createSelector(
  selectFirewallRequestWaiverSlice,
  prop('submitMaskState')
);

export const selectFirewallRequestWaiverIsDirty = createSelector(selectFirewallRequestWaiverSlice, prop('isDirty'));

export const selectFirewallRequestWaiverComponentMatcherStrategy = createSelector(
  selectFirewallRequestWaiverSlice,
  prop('componentMatcherStrategy')
);

export const selectFirewallRequestWaiverSelectedScope = createSelector(
  selectFirewallRequestWaiverSlice,
  prop('selectedWaiverScope')
);

export const selectFirewallRequestWaiverExpiryTime = createSelector(
  selectFirewallRequestWaiverSlice,
  prop('expiryTime')
);

export const selectFirewallRequestWaiverCustomExpiryTime = createSelector(
  selectFirewallRequestWaiverSlice,
  prop('customExpiryTime')
);

export const selectFirewallRequestWaiverReasonId = createSelector(
  selectFirewallRequestWaiverSlice,
  prop('waiverReasonId')
);

export const selectFirewallRequestWaiverComments = createSelector(selectFirewallRequestWaiverSlice, prop('comments'));

export const selectFirewallRequestWaiverNoteToReviewer = createSelector(
  selectFirewallRequestWaiverSlice,
  prop('noteToReviewer')
);
