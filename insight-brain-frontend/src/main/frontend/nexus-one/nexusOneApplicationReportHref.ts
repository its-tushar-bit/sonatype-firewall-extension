/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import { NEXUS_ONE_APPLICATION_REPORT_STATE } from 'MainRoot/nexus-one/nexusOneApplicationReportStates';

export interface NexusOneApplicationReportHrefParams {
  readonly publicId: string;
  readonly scanId: string;
  readonly componentHash?: string;
  readonly tabId?: string;
}

/**
 * In-hash href for the embedded Classic policy report (CLM-41538 / CLM-42224).
 * Production callers land in CLM-42224 when Martha stage tiles wire evaluation
 * navigation; this helper is the single URL builder for that hand-off.
 */
export function nexusOneApplicationReportHref(params: NexusOneApplicationReportHrefParams): string {
  return router.stateService.href(NEXUS_ONE_APPLICATION_REPORT_STATE, params);
}
