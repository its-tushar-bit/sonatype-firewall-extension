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

/** Facet counts for the Components filter rail. */
export type ComponentsFilterFacetCounts = {
  readonly totalComponents: number;
  readonly organizations: ReadonlyArray<ComponentsFilterFacetEntry>;
  readonly ecosystems: ReadonlyArray<ComponentsFilterFacetEntry>;
  /** My Scan Data only — the Catalog source has no application or stage buckets. */
  readonly applications: ReadonlyArray<ComponentsFilterFacetEntry>;
  readonly stages: ReadonlyArray<ComponentsFilterFacetEntry>;
};

/**
 * Component card row for Martha Components.
 * Catalog rows use coordinate identity; My Scan Data hybrid rows include SQL risk enrich.
 */
export type ComponentListRow = {
  readonly id: string;
  readonly name: string;
  readonly subtitle?: string;
  readonly ecosystem?: string;
  readonly organization?: string;
  readonly source: 'local' | 'catalog';
  /**
   * Estate component hash when known (My Scan Data dashboard rows). Prefer this over
   * {@link #id} / {@code source === 'local'} for estate detail deep-links — catalog and
   * coordinate-fallback local rows leave it unset.
   */
  readonly componentHash?: string;
  /** Optional in-app detail href from the catalog API when present. */
  readonly href?: string;
  readonly scoreCritical?: number;
  readonly scoreSevere?: number;
  readonly scoreModerate?: number;
  readonly scoreLow?: number;
  readonly affectedApplications?: number;
};
