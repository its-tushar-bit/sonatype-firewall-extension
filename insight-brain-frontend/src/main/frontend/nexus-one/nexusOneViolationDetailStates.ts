/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ReactStateDeclaration } from '@uirouter/react';
import ViolationDetail from 'MainRoot/nosc/violations/detail/ViolationDetail';
import {
  ViolationDetailOverviewRoute,
  ViolationDetailVulnerabilityRoute,
  ViolationDetailWaiversRoute,
} from 'MainRoot/nosc/violations/detail/violationDetailTabRoutes';

/**
 * UI-Router state declarations for the native Nexus One Violation Detail page.
 * Exported as data so production routing and jest harnesses register identical
 * states, mirroring {@link nexusOneApplicationDetailStates}.
 */
export const NEXUS_ONE_VIOLATION_DETAIL_STATE = 'nexusOneViolationDetail';
/** Empty-url child that redirects bare `#/violations/{id}` → `/overview`. */
export const NEXUS_ONE_VIOLATION_DETAIL_DEFAULT_STATE = `${NEXUS_ONE_VIOLATION_DETAIL_STATE}.default`;
export const NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW_STATE = `${NEXUS_ONE_VIOLATION_DETAIL_STATE}.overview`;
export const NEXUS_ONE_VIOLATION_DETAIL_VULNERABILITY_STATE = `${NEXUS_ONE_VIOLATION_DETAIL_STATE}.vulnerability`;
export const NEXUS_ONE_VIOLATION_DETAIL_WAIVERS_STATE = `${NEXUS_ONE_VIOLATION_DETAIL_STATE}.waivers`;

const VIOLATION_DETAIL_TITLE = 'Nexus One — Violation Detail';

export function nexusOneViolationDetailStates(): ReactStateDeclaration[] {
  return [
    {
      name: NEXUS_ONE_VIOLATION_DETAIL_STATE,
      // Path param is {id} (not policyViolationId) because ViolationPage reads
      // the violation id from router params via selectSelectedViolationId
      // (violationId || id). Query params mirror the Classic sidebarView.violation
      // deep-link contract (/violation/{id}?type&sidebarReference&sidebarId&page).
      url: '/violations/{id}?type&sidebarReference&sidebarId&page',
      abstract: true,
      component: ViolationDetail,
      data: { title: VIOLATION_DETAIL_TITLE },
    },
    {
      name: NEXUS_ONE_VIOLATION_DETAIL_DEFAULT_STATE,
      url: '',
      redirectTo: (trans) => ({
        state: NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW_STATE,
        params: {
          id: trans.params().id,
          type: trans.params().type,
          sidebarReference: trans.params().sidebarReference,
          sidebarId: trans.params().sidebarId,
          page: trans.params().page,
        },
      }),
    },
    {
      name: NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW_STATE,
      url: '/overview',
      component: ViolationDetailOverviewRoute,
      data: { title: VIOLATION_DETAIL_TITLE },
    },
    {
      name: NEXUS_ONE_VIOLATION_DETAIL_VULNERABILITY_STATE,
      url: '/vulnerability',
      component: ViolationDetailVulnerabilityRoute,
      data: { title: VIOLATION_DETAIL_TITLE },
    },
    {
      name: NEXUS_ONE_VIOLATION_DETAIL_WAIVERS_STATE,
      url: '/waivers',
      component: ViolationDetailWaiversRoute,
      data: { title: VIOLATION_DETAIL_TITLE },
    },
  ];
}
