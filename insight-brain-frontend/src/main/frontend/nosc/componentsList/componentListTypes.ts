/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/** One facet option in the Components filter rail. */
export type ComponentsFilterFacetEntry = {
  readonly id: string;
  readonly label: string;
  readonly count: number;
};

/** Facet counts for the Components filter rail (from {@code POST /rest/search/catalog}). */
export type ComponentsFilterFacetCounts = {
  readonly totalComponents: number;
  readonly organizations: ReadonlyArray<ComponentsFilterFacetEntry>;
  readonly ecosystems: ReadonlyArray<ComponentsFilterFacetEntry>;
};

/**
 * Catalog row mapped for the Martha Components card grid.
 * {@code id} is the catalog row id (component name / coordinate); risk scores are not on this API.
 */
export type ComponentListRow = {
  readonly id: string;
  readonly name: string;
  readonly subtitle?: string;
  readonly ecosystem?: string;
  readonly organization?: string;
  readonly source: 'local' | 'catalog';
  /** Optional in-app detail href from the catalog API when present. */
  readonly href?: string;
};
