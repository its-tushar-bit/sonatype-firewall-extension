/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { classicReportHrefForComponent } from 'MainRoot/nosc/applications/applicationDetailUtils';

/**
 * Href for a component from Violation (and sibling) detail surfaces.
 *
 * Native Component detail is not registered in Nexus One yet, so when a scan id
 * is available this escapes to the Classic application-report component page
 * (same destination as Application ComponentsTab). Without a scan id there is no
 * Classic deep-link — returns undefined so callers can omit the link.
 */
export function componentDetailHref(
  applicationPublicId: string,
  componentHash: string,
  scanId?: string | null,
): string | undefined {
  if (!scanId) {
    return undefined;
  }
  return classicReportHrefForComponent(applicationPublicId, scanId, componentHash);
}
