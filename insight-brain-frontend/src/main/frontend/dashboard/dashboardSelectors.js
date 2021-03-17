/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  getComponentRisksExportUrl,
  getNewestRisksExportUrl,
  getApplicationRisksExportUrl
} from '../util/CLMLocation';
import {
  translateViolationsSortFields,
  translateComponentsSortFields,
  translateApplicationsSortFields
} from './services/sortFieldsUtils';
import { createDashboardDataRequestPayload } from './utils/dashboard.utils.module';

export const selectExportTitle = (state) => state.router.currentState.data.exportTitle;

export const selectExportRequestData = (state) => {
  const routeStateName = state.router.currentState.name;
  const filters = state.dashboardFilter.appliedFilter;

  const applicationsSortFields = state.dashboard.applications.sortFields;
  const componentsSortFields = state.dashboard.components.sortFields;
  const violationsSortFields = state.dashboard.violations.sortFields;

  switch (routeStateName) {
    case 'dashboard.overview.violations':
      return createDashboardDataRequestPayload(filters, null,
          translateViolationsSortFields(violationsSortFields));

    case 'dashboard.overview.components':
      return createDashboardDataRequestPayload(filters, null,
          translateComponentsSortFields(componentsSortFields));

    case 'dashboard.overview.applications':
      return createDashboardDataRequestPayload(filters, null,
          translateApplicationsSortFields(applicationsSortFields));

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
