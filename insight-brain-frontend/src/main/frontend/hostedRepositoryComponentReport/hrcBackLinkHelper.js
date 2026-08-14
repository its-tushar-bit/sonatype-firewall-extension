/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Builds the "Back to HRC Report" href for both report-scope sub-pages (rawData, vulnerabilities,
// latestEvaluations) and the component-details drawer. The HRC report has no origin-based branching
// like application reports do — the hrcId + scanId in the URL is all the target state needs.
export function buildHrcReportPolicyHref(uiRouterState, hrcId, scanId) {
  return uiRouterState.href('hostedRepositoryComponentReport.policy', { hrcId, scanId });
}

export const BACK_TO_HRC_REPORT_TEXT = 'Back to HRC Report';
