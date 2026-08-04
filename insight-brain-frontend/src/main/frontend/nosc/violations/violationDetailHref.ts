/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * In-hash href for a violation card drill-in. Targets the native Nexus One violation detail
 * overview tab ({@code /violations/{id}/overview}).
 *
 * Built as a plain hash string rather than via {@code router.stateService.href} — mirroring the
 * deliberate approach in {@code PreviewDashboardApplicationsAppNameLink} — so the link contract is
 * verifiable in isolation and does not depend on the global router singleton being populated.
 */
export function violationDetailHref(policyViolationId: string): string {
  return `#/violations/${encodeURIComponent(policyViolationId)}/overview`;
}
