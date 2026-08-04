/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ReactStateDeclaration } from '@uirouter/react';
import ApplicationDetail from 'MainRoot/nosc/applications/ApplicationDetail';
import {
  ApplicationDetailComponentsRoute,
  ApplicationDetailEvaluationsRoute,
  ApplicationDetailOverviewRoute,
  ApplicationDetailViolationsRoute,
  ApplicationDetailWaiversRoute,
} from 'MainRoot/nosc/applications/applicationDetailTabRoutes';
import {
  NEXUS_ONE_APPLICATION_DETAIL_PARENT_STATE,
  tabFromSlug,
  TAB_TO_URL,
} from 'MainRoot/nosc/applications/applicationDetailUtils';
import type { TabId } from 'MainRoot/nosc/applications/applicationDetailTypes';

/**
 * UI-Router state declarations for the Nexus One Application Detail page
 * (CLM-40901). Mirrors {@link nexusOneDashboardStates}: an abstract parent
 * shell renders the header + tab strip + nested `<UIView />`; each tab is a
 * child state whose component renders into that view.
 */
const APPLICATION_DETAIL_TITLE = 'Nexus One — Application';

export { NEXUS_ONE_APPLICATION_DETAIL_PARENT_STATE };

/** Child state name derived from the parent constant so the two never drift
 *  (e.g. `nexusOneApplicationsDetail.overview`). */
const childState = (suffix: string): string => `${NEXUS_ONE_APPLICATION_DETAIL_PARENT_STATE}.${suffix}`;

export function nexusOneApplicationDetailStates(): ReactStateDeclaration[] {
  return [
    {
      name: NEXUS_ONE_APPLICATION_DETAIL_PARENT_STATE,
      url: '/applications/{publicId}',
      abstract: true,
      component: ApplicationDetail,
      data: { title: APPLICATION_DETAIL_TITLE },
    },
    {
      name: childState('overview'),
      url: '',
      component: ApplicationDetailOverviewRoute,
      data: { title: APPLICATION_DETAIL_TITLE },
    },
    {
      name: childState('violations'),
      url: '/violations',
      component: ApplicationDetailViolationsRoute,
      data: { title: APPLICATION_DETAIL_TITLE },
    },
    {
      name: childState('components'),
      url: '/components',
      component: ApplicationDetailComponentsRoute,
      data: { title: APPLICATION_DETAIL_TITLE },
    },
    {
      name: childState('evaluations'),
      url: '/evaluations',
      component: ApplicationDetailEvaluationsRoute,
      data: { title: APPLICATION_DETAIL_TITLE },
    },
    {
      name: childState('waivers'),
      url: '/waivers',
      component: ApplicationDetailWaiversRoute,
      data: { title: APPLICATION_DETAIL_TITLE },
    },
    // Legacy flat tab state from CLM-39709 — redirect bookmarks to child states.
    // Removed V1 Coming Soon tabs (sboms, team-members) map to Overview via tabFromSlug.
    {
      name: 'nexusOneApplicationsDetailTab',
      url: '/applications/{publicId}/{tab}',
      redirectTo: (trans) => {
        const tabSlug = trans.params().tab as string | undefined;
        const tabId: TabId = tabFromSlug(tabSlug);
        const suffix = TAB_TO_URL[tabId];
        return {
          state: childState(suffix),
          params: { publicId: trans.params().publicId },
        };
      },
    },
  ];
}
