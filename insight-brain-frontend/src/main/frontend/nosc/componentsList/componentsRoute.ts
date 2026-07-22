/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Single source of truth for the Martha V1 Components list route. Shared by route registration
 * ({@code nexus-one/routes.tsx}), the {@link ComponentsList} container, and the test harness.
 *
 * Query params: {@code source/q/page/org/ecosystem}. {@code source} is {@code local} (My Scan Data)
 * or {@code catalog} (Sonatype Catalog). Org values are friendly organization names.
 */
export const NEXUS_ONE_COMPONENTS_STATE_NAME = 'nexusOneComponents';

export const NEXUS_ONE_COMPONENTS_URL = '/components?source&q&page&org&ecosystem';

/** UI tab ↔ catalog {@code source} values. */
export type ComponentsTab = 'myScanData' | 'catalog';

export const DEFAULT_COMPONENTS_TAB: ComponentsTab = 'myScanData';

export function componentsTabToSource(tab: ComponentsTab): 'local' | 'catalog' {
  return tab === 'catalog' ? 'catalog' : 'local';
}

export function componentsSourceToTab(source: string | undefined): ComponentsTab {
  return source === 'catalog' ? 'catalog' : 'myScanData';
}
