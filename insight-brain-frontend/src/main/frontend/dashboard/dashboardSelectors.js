/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

import {
  getApplicationRisksExportUrl,
  getComponentRisksExportUrl,
  getNewestRisksExportUrl,
  getWaiversExportUrl,
  getWaiversAndAutoWaiversExportUrl,
} from '../util/CLMLocation';
import {
  translateApplicationsSortFields,
  translateComponentsSortFields,
  translateViolationsSortFields,
  translateWaiversSortFields,
} from './services/sortFieldsUtils';
import { createDashboardDataRequestPayload } from './utils/dashboardUtils';
import {
  APPLICATIONS_RESULTS_TYPE,
  COMPONENTS_RESULTS_TYPE,
  VIOLATIONS_RESULTS_TYPE,
  WAIVERS_RESULTS_TYPE,
} from 'MainRoot/dashboard/results/dashboardResultsTypes';

export const selectExportTitle = (state) => state.router.currentState.data.exportTitle;

export const selectExportRequestData = (state) => {
  const routeStateName = state.router.currentState.name;
  const filters = state.dashboardFilter.appliedFilter;

  const applicationsSortFields = state.dashboard.applications.sortFields;
  const componentsSortFields = state.dashboard.components.sortFields;
  const violationsSortFields = state.dashboard.violations.sortFields;
  const waiversSortFields = state.dashboard.waivers.sortFields;

  switch (routeStateName) {
    case 'dashboard.overview.violations':
      return createDashboardDataRequestPayload(filters, null, translateViolationsSortFields(violationsSortFields));

    case 'dashboard.overview.components':
      return createDashboardDataRequestPayload(filters, null, translateComponentsSortFields(componentsSortFields));

    case 'dashboard.overview.applications':
      return createDashboardDataRequestPayload(filters, null, translateApplicationsSortFields(applicationsSortFields));

    case 'dashboard.overview.waivers':
      return createDashboardDataRequestPayload(filters, null, translateWaiversSortFields(waiversSortFields));

    default:
      return {};
  }
};

export const selectExportUrl = (state, isAutoWaiversEnabled) => {
  const routeStateName = state.router.currentState.name;
  switch (routeStateName) {
    case 'dashboard.overview.violations':
      return getNewestRisksExportUrl();

    case 'dashboard.overview.components':
      return getComponentRisksExportUrl();

    case 'dashboard.overview.applications':
      return getApplicationRisksExportUrl();

    case 'dashboard.overview.waivers':
      if (isAutoWaiversEnabled === true) {
        return getWaiversAndAutoWaiversExportUrl();
      } else {
        return getWaiversExportUrl();
      }

    default:
      return '';
  }
};

const selectDashboardSlice = prop('dashboard');
export const selectViolationResults = createSelector(selectDashboardSlice, prop(VIOLATIONS_RESULTS_TYPE));
export const selectComponentResults = createSelector(selectDashboardSlice, prop(COMPONENTS_RESULTS_TYPE));
export const selectApplicationResults = createSelector(selectDashboardSlice, prop(APPLICATIONS_RESULTS_TYPE));
export const selectWaiversResults = createSelector(selectDashboardSlice, prop(WAIVERS_RESULTS_TYPE));

export const selectDashboardFilter = prop('dashboardFilter');
