/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ReactStateDeclaration } from '@uirouter/react';
import PreviewDashboardPage from 'MainRoot/nosc/dashboard/PreviewDashboardPage';
import DashboardOverviewContent from 'MainRoot/nosc/dashboard/DashboardOverviewContent';
import PreviewViolationsTab from 'MainRoot/nosc/dashboard/tabs/PreviewViolationsTab';
import PreviewComponentsTab from 'MainRoot/nosc/dashboard/tabs/PreviewComponentsTab';
import PreviewApplicationsTab from 'MainRoot/nosc/dashboard/tabs/PreviewApplicationsTab';
import PreviewWaiversTab from 'MainRoot/nosc/dashboard/tabs/PreviewWaiversTab';

/**
 * UI-Router state declarations for the Nexus One Dashboard (CLM-39992 / CLM-39641 review follow-up).
 *
 * An abstract parent shell (`nexusOneDashboard`) renders the tab strip + a nested `<UIView>`; each tab
 * is a child state whose component renders into that view. Exported as data (rather than registered
 * inline) so both production routing ({@code nexus-one/routes.tsx}) and the jest router harness can
 * register the exact same states against their respective router instances — keeping the test wiring
 * honest with production.
 */
const DASHBOARD_TITLE = 'Nexus One — Dashboard';

export const NEXUS_ONE_DASHBOARD_PARENT_STATE = 'nexusOneDashboard';

export function nexusOneDashboardStates(): ReactStateDeclaration[] {
  return [
    {
      name: 'nexusOneDashboard',
      url: '/dashboard',
      abstract: true,
      component: PreviewDashboardPage,
      data: { title: DASHBOARD_TITLE },
    },
    // Overview keeps the bare `/dashboard` URL (empty child url) so the default landing URL stays clean.
    {
      name: 'nexusOneDashboard.overview',
      url: '',
      component: DashboardOverviewContent,
      data: { title: DASHBOARD_TITLE },
    },
    {
      name: 'nexusOneDashboard.violations',
      url: '/violations',
      component: PreviewViolationsTab,
      data: { title: DASHBOARD_TITLE },
    },
    {
      name: 'nexusOneDashboard.components',
      url: '/components',
      component: PreviewComponentsTab,
      data: { title: DASHBOARD_TITLE },
    },
    {
      name: 'nexusOneDashboard.applications',
      url: '/applications',
      component: PreviewApplicationsTab,
      data: { title: DASHBOARD_TITLE },
    },
    {
      name: 'nexusOneDashboard.waivers',
      url: '/waivers',
      component: PreviewWaiversTab,
      data: { title: DASHBOARD_TITLE },
    },
  ];
}
