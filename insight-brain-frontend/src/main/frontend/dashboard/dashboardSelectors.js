/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

import { getApplicationRisksExportUrl, getComponentRisksExportUrl, getNewestRisksExportUrl } from '../util/CLMLocation';
import {
  translateApplicationsSortFields,
  translateComponentsSortFields,
  translateViolationsSortFields,
} from './services/sortFieldsUtils';
import { createDashboardDataRequestPayload } from './utils/dashboard.utils.module';
import {
  APPLICATIONS_RESULTS_TYPE,
  COMPONENTS_RESULTS_TYPE,
  VIOLATIONS_RESULTS_TYPE,
} from 'MainRoot/dashboard/results/dashboardResultsTypes';

export const selectExportTitle = (state) => state.router.currentState.data.exportTitle;

export const selectExportRequestData = (state) => {
  const routeStateName = state.router.currentState.name;
  const filters = state.dashboardFilter.appliedFilter;

  const applicationsSortFields = state.dashboard.applications.sortFields;
  const componentsSortFields = state.dashboard.components.sortFields;
  const violationsSortFields = state.dashboard.violations.sortFields;

  switch (routeStateName) {
    case 'dashboard.overview.violations':
      return createDashboardDataRequestPayload(filters, null, translateViolationsSortFields(violationsSortFields));

    case 'dashboard.overview.components':
      return createDashboardDataRequestPayload(filters, null, translateComponentsSortFields(componentsSortFields));

    case 'dashboard.overview.applications':
      return createDashboardDataRequestPayload(filters, null, translateApplicationsSortFields(applicationsSortFields));

    default:
      return {};
  }
};

export const selectExportUrl = (state) => {
  const routeStateName = state.router.currentState.name;
  switch (routeStateName) {
    case 'dashboard.overview.violations':
      return getNewestRisksExportUrl();

    case 'dashboard.overview.components':
      return getComponentRisksExportUrl();

    case 'dashboard.overview.applications':
      return getApplicationRisksExportUrl();

    default:
      return '';
  }
};

const selectDashboardSlice = prop('dashboard');
export const selectViolationResults = createSelector(selectDashboardSlice, prop(VIOLATIONS_RESULTS_TYPE));
export const selectComponentResults = createSelector(selectDashboardSlice, prop(COMPONENTS_RESULTS_TYPE));
export const selectApplicationResults = createSelector(selectDashboardSlice, prop(APPLICATIONS_RESULTS_TYPE));
