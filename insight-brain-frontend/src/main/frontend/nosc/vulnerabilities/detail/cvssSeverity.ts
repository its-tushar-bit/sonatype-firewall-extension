/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export type CvssSeverityLabel = 'Critical' | 'High' | 'Medium' | 'Low' | 'None';
export type CvssBadgeColor = 'red' | 'orange' | 'yellow' | 'indigo' | 'gray';

export interface CvssSeverity {
  readonly label: CvssSeverityLabel;
  readonly color: CvssBadgeColor;
}

/**
 * Maps a numeric CVSS score to the standard qualitative severity band and a
 * matching Radix badge color. Color is never the only signal — callers should
 * also render {@link CvssSeverity.label}.
 *
 * Bands (CVSS v3 qualitative): Critical ≥ 9.0, High ≥ 7.0, Medium ≥ 4.0,
 * Low > 0, None = 0.
 */
export function cvssSeverityForScore(score: number): CvssSeverity {
  if (score >= 9) return { label: 'Critical', color: 'red' };
  if (score >= 7) return { label: 'High', color: 'orange' };
  if (score >= 4) return { label: 'Medium', color: 'yellow' };
  if (score > 0) return { label: 'Low', color: 'indigo' };
  return { label: 'None', color: 'gray' };
}
