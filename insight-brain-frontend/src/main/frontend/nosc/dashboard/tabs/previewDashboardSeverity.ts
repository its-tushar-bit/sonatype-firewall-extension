/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { threatColorFor } from 'MainRoot/nosc/applications/applicationDetailUtils';

/**
 * Single badge-color union for every Preview dashboard severity/threat badge. Sourced from the canonical
 * {@link threatColorFor} helper (shared with the application-detail and waivers views) plus the `green`
 * zero-state used by the score grids, so one threat level renders the same hue across all Preview surfaces.
 */
export type BadgeColor = ReturnType<typeof threatColorFor> | 'green';

export type ComponentScoreKind = 'total' | 'crit' | 'sev' | 'mod' | 'low';

/**
 * Representative policy-threat level for each per-severity score column, taken from the lower bound of
 * the canonical {@link THREAT_GROUPS} ranges (Critical 8-10, Severe 4-7, Moderate 2-3, Low 1). Routing
 * these through {@link threatColorFor} keeps the column hues in lock-step with the canonical
 * `classifyThreat` palette instead of hand-copying colors that silently drift (e.g. Low is `indigo`,
 * not `gray`).
 */
const SCORE_KIND_THREAT_LEVEL: Record<Exclude<ComponentScoreKind, 'total'>, number> = {
  crit: 8,
  sev: 4,
  mod: 2,
  low: 1,
};

/** Maps component score columns to Radix badge colors for the Preview Dashboard Components tab. */
export function severityColor(value: number | undefined, kind: ComponentScoreKind): BadgeColor {
  const v = value ?? 0;
  if (v === 0) return 'green';
  // For `total` the magnitude is the score itself; per-severity columns map to the canonical
  // threat level for their category so the hue comes from the single source of truth.
  const level = kind === 'total' ? v : SCORE_KIND_THREAT_LEVEL[kind];
  return threatColorFor(level);
}

/**
 * Maps violation threat levels to Radix badge colors for the Preview Dashboard Violations tab. Delegates to the
 * canonical {@link threatColorFor} so a threat level renders identically on the Preview tabs, the
 * application-detail view, and the waivers view.
 */
export function threatColor(level: number): BadgeColor {
  return threatColorFor(level);
}
