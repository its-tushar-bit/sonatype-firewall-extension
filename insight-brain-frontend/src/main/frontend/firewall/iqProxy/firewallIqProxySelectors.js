/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectFirewallIqProxy = prop('firewallIqProxy');

export const selectSaving = createSelector(selectFirewallIqProxy, prop('saving'));
export const selectSaveError = createSelector(selectFirewallIqProxy, prop('saveError'));
export const selectSaveErrorId = createSelector(selectFirewallIqProxy, prop('saveErrorId'));
export const selectCreatingManager = createSelector(selectFirewallIqProxy, prop('creatingManager'));
export const selectCreateManagerError = createSelector(selectFirewallIqProxy, prop('createManagerError'));
export const selectVirtualRepositoryManagers = createSelector(selectFirewallIqProxy, prop('virtualRepositoryManagers'));
export const selectLoadingVirtualRepositoryManagers = createSelector(
  selectFirewallIqProxy,
  prop('loadingVirtualRepositoryManagers')
);
export const selectVirtualRepositoryManagersLoadError = createSelector(
  selectFirewallIqProxy,
  prop('virtualRepositoryManagersLoadError')
);
export const selectCreatingProxyRepository = createSelector(selectFirewallIqProxy, prop('creatingProxyRepository'));
export const selectCreateProxyRepositoryError = createSelector(
  selectFirewallIqProxy,
  prop('createProxyRepositoryError')
);
export const selectUpdatingProxyRepository = createSelector(selectFirewallIqProxy, prop('updatingProxyRepository'));
export const selectUpdateProxyRepositoryError = createSelector(
  selectFirewallIqProxy,
  prop('updateProxyRepositoryError')
);
