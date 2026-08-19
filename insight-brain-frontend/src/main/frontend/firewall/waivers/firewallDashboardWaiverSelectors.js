/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

const selectFirewallDashboardWaiverSlice = prop('firewallDashboardWaiver');

export const selectFirewallDashboardHasWaivePermission = createSelector(
  selectFirewallDashboardWaiverSlice,
  prop('hasWaivePermission')
);
export const selectFirewallWaiverToDelete = createSelector(selectFirewallDashboardWaiverSlice, prop('waiverToDelete'));
export const selectFirewallDeleteWaiverSaving = createSelector(
  selectFirewallDashboardWaiverSlice,
  prop('deleteWaiverSaving')
);
export const selectFirewallDeleteWaiverError = createSelector(
  selectFirewallDashboardWaiverSlice,
  prop('deleteWaiverError')
);
