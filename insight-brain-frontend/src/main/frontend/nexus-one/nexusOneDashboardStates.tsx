/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ReactStateDeclaration } from '@uirouter/react';
import PreviewDashboardPage from 'MainRoot/nosc/dashboard/PreviewDashboardPage';
import MetricCardGrid from 'MainRoot/nosc/dashboard/metrics/MetricCardGrid';
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
    // The landing (bare `/dashboard`, empty child url) is the metric-card grid (CLM-40905). The shell
    // suppresses the tab strip for this state so the grid is a clean, tab-free landing; the classic
    // tabbed tables remain reachable as a fallback at their own child URLs (and via the card
    // click-throughs / LeftNav). Access to the entire `nexus-one` bundle is gated upstream by
    // `ensureNexusOneShellAccess` / `PREVIEW_NEXUS_ONE_UI` — this state is unconditional within
    // that bundle.
    {
      name: 'nexusOneDashboard.overview',
      url: '',
      component: MetricCardGrid,
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
