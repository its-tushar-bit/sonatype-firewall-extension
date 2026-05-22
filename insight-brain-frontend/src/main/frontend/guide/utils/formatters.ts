/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Format an EPSS (Exploit Prediction Scoring System) score as a percentage string.
 * Handles special cases:
 * - 0 returns "0%" (not "0.000%")
 * - null/undefined returns "Not available"
 * - Other values are formatted with 3 decimal places
 *
 * @param epss - The EPSS score (0-1 range) or null/undefined
 * @returns Formatted percentage string
 *
 * @example
 * formatEpssScore(0.975)    // Returns "97.500%"
 * formatEpssScore(0)       // Returns "0%"
 * formatEpssScore(null)    // Returns "Not available"
 * formatEpssScore(undefined) // Returns "Not available"
 */
export function formatEpssScore(epss: number | null | undefined): string {
  if (epss === undefined || epss === null) {
    return 'Not available';
  }
  if (epss < 0 || epss > 1) {
    return 'Not available';
  }
  if (epss === 0) {
    return '0%';
  }
  return `${(epss * 100).toFixed(3)}%`;
}
