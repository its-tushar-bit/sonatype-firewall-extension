/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ReactStateDeclaration } from '@uirouter/react';
import { NexusOneApplicationReportRoute } from 'MainRoot/nexus-one/nexusOneApplicationReportRoute';

/**
 * UI-Router state for embedding Classic ReportPage inside Nexus One chrome
 * (CLM-41538). Exported as data so production routing and jest harnesses
 * register identical states.
 */
export const NEXUS_ONE_APPLICATION_REPORT_STATE = 'nexusOneApplicationReport';

const APPLICATION_REPORT_TITLE = 'Nexus One — Application Report';

export function nexusOneApplicationReportStates(): ReactStateDeclaration[] {
  return [
    {
      name: NEXUS_ONE_APPLICATION_REPORT_STATE,
      // Same contract as applicationReport.policy: publicId + scanId for the
      // stage evaluation; optional query params mirror Classic deep-links.
      url: '/applications/{publicId}/report/{scanId}?componentHash&tabId',
      component: NexusOneApplicationReportRoute,
      data: { title: APPLICATION_REPORT_TITLE },
    },
  ];
}
