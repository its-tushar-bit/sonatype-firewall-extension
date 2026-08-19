/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Canonical IQ lifecycle stage ids and labels for NOSC application surfaces.
 *
 * Kept in one module so list-view stage columns and the Evaluations tab cannot drift when a stage
 * is added. Unknown ids sort after these (alphabetically) and Title-Case for display so a raw
 * slug never renders.
 */
export const IQ_LIFECYCLE_STAGE_ORDER: ReadonlyArray<string> = [
  'proxy',
  'develop',
  'source',
  'build',
  'stage-release',
  'release',
  'operate',
];

const IQ_LIFECYCLE_STAGE_LABELS: Readonly<Record<string, string>> = {
  proxy: 'Proxy',
  develop: 'Develop',
  source: 'Source',
  build: 'Build',
  'stage-release': 'Stage Release',
  release: 'Release',
  operate: 'Operate',
};

/** Display name for a stage id; Title-Cases anything not in the map. */
export function iqLifecycleStageLabel(stageId: string): string {
  return (
    IQ_LIFECYCLE_STAGE_LABELS[stageId] ??
    stageId
      .split('-')
      .filter(Boolean)
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ')
  );
}

/** Lifecycle order first, then unknown ids alphabetically. */
export function compareIqLifecycleStageIds(a: string, b: string): number {
  const ia = IQ_LIFECYCLE_STAGE_ORDER.indexOf(a);
  const ib = IQ_LIFECYCLE_STAGE_ORDER.indexOf(b);
  if (ia !== -1 && ib !== -1) return ia - ib;
  if (ia !== -1) return -1;
  if (ib !== -1) return 1;
  return a.localeCompare(b);
}
