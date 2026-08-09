/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { EstateComponentTab } from 'MainRoot/nosc/components/detail/estateComponentDetailHref';

export const NEXUS_ONE_ESTATE_COMPONENT_DETAIL_PARENT_STATE = 'nexusOneEstateComponentDetail';

export const ESTATE_COMPONENT_TAB_IDS: readonly EstateComponentTab[] = [
  'overview',
  'legal',
  'vulnerabilities',
  'violations',
  'applications',
  'organizations',
] as const;

const TAB_SET = new Set<string>(ESTATE_COMPONENT_TAB_IDS);

const DEFAULT_TAB: EstateComponentTab = 'overview';

/** Derive the active tab from a UI-Router child state name. */
export function tabFromEstateComponentDetailStateName(stateName: string | undefined): EstateComponentTab {
  if (!stateName?.startsWith(`${NEXUS_ONE_ESTATE_COMPONENT_DETAIL_PARENT_STATE}.`)) {
    return DEFAULT_TAB;
  }
  const suffix = stateName.slice(NEXUS_ONE_ESTATE_COMPONENT_DETAIL_PARENT_STATE.length + 1);
  return TAB_SET.has(suffix) ? (suffix as EstateComponentTab) : DEFAULT_TAB;
}

/** Target child state for a tab click. */
export function estateComponentDetailStateNameForTab(tab: EstateComponentTab): string {
  return `${NEXUS_ONE_ESTATE_COMPONENT_DETAIL_PARENT_STATE}.${tab}`;
}

/** Short label when HDS has not returned a display name. */
export function truncatedComponentHash(hash: string): string {
  if (hash.length <= 12) {
    return hash;
  }
  return `${hash.slice(0, 8)}…`;
}

/** Format where-used last-seen epoch millis for Applications / Organizations tables. */
export function formatLastSeen(epochMillis: number | undefined): string {
  if (typeof epochMillis !== 'number' || !Number.isFinite(epochMillis)) {
    return '—';
  }
  try {
    return new Date(epochMillis).toLocaleString();
  } catch {
    return '—';
  }
}
