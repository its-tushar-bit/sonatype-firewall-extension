/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { filter, prop, propEq } from 'ramda';

// Base selector - gets the slice from root state
export const selectFirewallEnterpriseReporting = prop('firewallEnterpriseReporting');

// Landing page selectors - all dashboards
export const selectAllDashboards = createSelector(selectFirewallEnterpriseReporting, prop('dashboards'));

// Filtered selector - only firewall category dashboards
export const selectDashboards = createSelector(selectAllDashboards, filter(propEq('category', 'firewall')));

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
