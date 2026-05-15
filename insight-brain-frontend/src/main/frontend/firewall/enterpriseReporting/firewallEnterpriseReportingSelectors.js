/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { filter, prop, propEq, sortBy } from 'ramda';

// Base selector - gets the slice from root state
export const selectFirewallEnterpriseReporting = prop('firewallEnterpriseReporting');

// Landing page selectors - all dashboards
export const selectAllDashboards = createSelector(selectFirewallEnterpriseReporting, prop('dashboards'));

// Filtered and sorted selector - firewall category dashboards ordered by priorityOrder ascending
// Dashboards without priorityOrder are sorted last (Infinity fallback)
export const selectDashboards = createSelector(selectAllDashboards, (dashboards) => {
  if (!dashboards?.length) return [];
  return sortBy((d) => d.priorityOrder ?? Infinity, filter(propEq('category', 'firewall'), dashboards));
});

export const selectLoading = createSelector(selectFirewallEnterpriseReporting, prop('loading'));

export const selectLoadError = createSelector(selectFirewallEnterpriseReporting, prop('loadError'));

export const selectIqVersion = createSelector(selectFirewallEnterpriseReporting, prop('iqVersion'));

// Dashboard detail page selectors
export const selectBaseUrl = createSelector(selectFirewallEnterpriseReporting, prop('baseUrl'));

export const selectSelectedDashboard = createSelector(selectFirewallEnterpriseReporting, prop('selectedDashboard'));

export const selectSelectedDashboardName = createSelector(
  selectFirewallEnterpriseReporting,
  prop('selectedDashboardName')
);
