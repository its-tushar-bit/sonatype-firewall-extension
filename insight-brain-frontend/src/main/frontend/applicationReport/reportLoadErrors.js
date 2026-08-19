/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * True when a report load error is a retention purge (permanent), not a transient failure.
 * Matches ReportService's NotFoundException message for purged reports.
 */
export function isPurgedReportLoadError(loadError) {
  return typeof loadError === 'string' && /purged to the trash|data retention policies/i.test(loadError);
}
