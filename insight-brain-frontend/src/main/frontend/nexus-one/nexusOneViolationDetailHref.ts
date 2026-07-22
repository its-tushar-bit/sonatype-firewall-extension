/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import { NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW_STATE } from 'MainRoot/nexus-one/nexusOneViolationDetailStates';

export interface NexusOneViolationDetailHrefParams {
  readonly policyViolationId: string;
  readonly type?: string;
  readonly sidebarReference?: string;
  readonly sidebarId?: string;
  // String (not number) to match the untyped/string router query-param contract
  // (/violations/{id}?type&sidebarReference&sidebarId&page) and the sibling params, so callers that
  // read params.page back off the router compare against a string consistently.
  readonly page?: string;
}

/**
 * In-hash href for the native violation detail. The card drill-in (CLM-42259)
 * is the production caller; this helper is the single URL builder for that hand-off.
 *
 * Accepts {@code policyViolationId} (the list API's row identifier) and maps it
 * to the state's {@code id} path param, which ViolationPage resolves via
 * selectSelectedViolationId.
 */
export function nexusOneViolationDetailHref(params: NexusOneViolationDetailHrefParams): string {
  const { policyViolationId, ...rest } = params;
  return router.stateService.href(NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW_STATE, { id: policyViolationId, ...rest });
}
