/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { all, filter, find, includes, pluck, propEq, reject, uniq } from 'ramda';
import moment from 'moment';
import { EI_DEFAULT_FILTER_NAME, FILTER_STATES } from './filter/enterpriseReportingFilterSlice';

export const smallTagColors = ['blue', 'green', 'indigo', 'orange', 'pink', 'purple', 'red', 'teal', 'turquoise'];

export const combineDashboards = (dashboards) => {
  const { dashboardGroupMetadata, dashboardMetadata } = dashboards;

  const updatedDashboardGroups = dashboardGroupMetadata.map((group) => {
    const groupedDashboards = getGroupedDashboards(dashboardMetadata, group);
    return { ...group, groupedDashboards, category: groupedDashboards[0]?.category };
  });

  const combinedDashboards = dashboardMetadata.map((dash) => {
    if (includes(dash.groupId, pluck('groupId', updatedDashboardGroups))) {
      return find(propEq('groupId', dash.groupId), updatedDashboardGroups);
    } else {
      return dash;
    }
  });

  return uniq(combinedDashboards);
};

export const getGroupedDashboards = (dashboardsArr, dashboardGroup) => {
  return filter(propEq('groupId', dashboardGroup.groupId), dashboardsArr);
};

export const extractDashboardVersions = (dashboard) => pluck('sinceIQVersion', dashboard.groupedDashboards);

export const getUpgradeVersion = (dashboard) => {
  const groupedDashboardVersions = dashboard.groupedDashboards ? extractDashboardVersions(dashboard) : [];
  return Math.max(dashboard.sinceIQVersion, ...groupedDashboardVersions);
};

export const isElementDisabled = (dashboard, isDashboardDisabled) =>
  isDashboardDisabled(dashboard) ||
  (!!dashboard.groupedDashboards && all(isDashboardDisabled, dashboard.groupedDashboards));

const normalizeFilterName = (name) => (name ? name.toLowerCase().replace(/\s/g, '') : '');

export const findFilterByName = (filterName, savedFilters) =>
  find((f) => normalizeFilterName(f.name) === normalizeFilterName(filterName), savedFilters);

export const calculateIsFilterDefault = (filterName, defaultFilterId, savedFilters) => {
  const filter = findFilterByName(filterName, savedFilters);
  return filter && filter.id === defaultFilterId;
};

export const findFilterById = (filterId, savedFilters) => find(propEq('id', filterId), savedFilters);

export const calculateIsFilterDirty = (filterOptionName, appliedFilterName, filterState) => {
  const cleanedName = filterOptionName === EI_DEFAULT_FILTER_NAME ? null : filterOptionName;
  return filterState === FILTER_STATES.CHANGED && cleanedName === appliedFilterName;
};

export const RETIREMENT_DATE = moment.utc('2026-02-23T23:59:59Z');
export const RETIRING_DASHBOARD_IDS = ['rolling-recap', 'upgrade-posture'];

export const isRetiringDashboard = (dashboardId) => includes(dashboardId, RETIRING_DASHBOARD_IDS);

export const isDashboardRetired = (dashboard) =>
  !!(
    isRetiringDashboard(dashboard.dashboardId) &&
    dashboard.spotlightText &&
    /retiring/i.test(dashboard.spotlightText) &&
    moment.utc().isAfter(RETIREMENT_DATE)
  );

export const filterRetiredDashboards = (dashboards) => reject(isDashboardRetired, dashboards || []);

export const getFormattedRetirementDate = () => {
  const year = RETIREMENT_DATE.year();
  const month = String(RETIREMENT_DATE.month() + 1).padStart(2, '0');
  const day = String(RETIREMENT_DATE.date()).padStart(2, '0');
  return `${month}/${day}/${year}`;
};
