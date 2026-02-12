/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { filter, prop, propEq, unless } from 'ramda';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { combineDashboards } from './utils';

export const selectEnterpriseReportingLandingPage = prop('enterpriseReportingLandingPage');
export const selectDashboardsData = createSelector(selectEnterpriseReportingLandingPage, prop('dashboardsData'));

export const selectCombinedDashboards = createSelector(selectDashboardsData, (dashboards) =>
  unless(isNilOrEmpty, combineDashboards)(dashboards)
);

export const selectEnterpriseDashboards = createSelector(selectCombinedDashboards, (dashboards) => {
  if (isNilOrEmpty(dashboards)) {
    return [];
  }
  return filter(propEq('category', 'enterprise'), dashboards);
});

export const selectDataInsightsDashboards = createSelector(selectCombinedDashboards, (dashboards) => {
  if (isNilOrEmpty(dashboards)) {
    return [];
  }
  return filter(propEq('category', 'dataInsight'), dashboards);
});

export const selectPartnerDashboards = createSelector(selectCombinedDashboards, (dashboards) => {
  if (isNilOrEmpty(dashboards)) {
    return [];
  }
  return filter(propEq('category', 'partner'), dashboards);
});
