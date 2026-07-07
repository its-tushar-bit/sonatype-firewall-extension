/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Wire types for `POST /rest/dashboard/metrics` (CLM-40905).
 *
 * The endpoint returns one envelope per KPI. Each entry carries a `total` plus an
 * optional `breakdown` whose shape depends on the metric. Metrics not rendered by
 * this grid slice (e.g. organizations, policies) may be present but are ignored here.
 */

/** Per-severity breakdown for the Violations metric. */
export interface ViolationsBreakdown {
  readonly critical: number;
  readonly severe: number;
  readonly moderate: number;
  readonly low: number;
}

/** Existing vs requested breakdown for the Waivers metric. */
export interface WaiversBreakdown {
  readonly existing: number;
  readonly requested: number;
}

/** A single KPI entry: a headline `total` and an optional metric-specific breakdown. */
export interface MetricEntry<TBreakdown = unknown> {
  readonly total: number;
  readonly breakdown: TBreakdown | null;
  /** Provenance hint from the backend (e.g. 'index' | 'sql'); informational only. */
  readonly source?: string;
}

/** Full `POST /rest/dashboard/metrics` 200 response body. */
export interface DashboardMetricsResponse {
  readonly applications?: MetricEntry<null>;
  readonly violations?: MetricEntry<ViolationsBreakdown>;
  readonly waivers?: MetricEntry<WaiversBreakdown>;
  readonly components?: MetricEntry<unknown>;
  /** Epoch millis of the underlying data, or null when the backend can't determine it. */
  readonly lastUpdatedAt: number | null;
}

/**
 * Request scope. All fields optional; an empty object means "default RBAC-scoped view".
 * Mirrors the active dashboard filter (org / app / stage / tag selections).
 */
export interface DashboardMetricsScope {
  readonly organizationIds?: readonly string[];
  readonly applicationIds?: readonly string[];
  readonly stageIds?: readonly string[];
  readonly tagIds?: readonly string[];
}
