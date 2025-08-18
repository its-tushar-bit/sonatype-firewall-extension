/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { all, filter, find, includes, pluck, propEq, uniq } from 'ramda';

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
