/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ReactStateDeclaration } from '@uirouter/react';
import { NexusOneViolationDetailRoute } from 'MainRoot/nexus-one/nexusOneViolationDetailRoute';

/**
 * UI-Router state for embedding the Classic violation detail page inside Nexus
 * One chrome (CLM-42256). Exported as data so production routing and jest
 * harnesses register identical states, mirroring
 * {@link nexusOneApplicationReportStates}.
 */
export const NEXUS_ONE_VIOLATION_DETAIL_STATE = 'nexusOneViolationDetail';

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
      component: NexusOneViolationDetailRoute,
      data: { title: VIOLATION_DETAIL_TITLE },
    },
  ];
}
