/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Wire types for `POST /rest/dashboard/metrics` (CLM-40905).
 *
 * The endpoint returns one envelope per KPI. Each entry carries a `total` plus an
 * optional `breakdown` whose shape depends on the metric. `components` may be absent
 * entirely (added by a parallel backend PR) — every consumer MUST treat it as optional.
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
  /** Present when backend supports expiring-within-30d; omit on older servers. */
  readonly expiring?: number;
}

/** Licensed dashboard stage count for the Applications metric (from StageTypeService). */
export interface ApplicationsBreakdown {
  readonly stages: number;
}

/** Applications-with-obligations vs components-with-obligations for the Legal metric. */
export interface LegalBreakdown {
  readonly applications: number;
  readonly components: number;
}

/**
 * Per-severity breakdown for the Vulnerabilities metric (distinct CVE / advisory IDs in scope).
 * Each CVE with a CVSS score appears in exactly one band; unscored CVEs contribute to `total`
 * only and are not included in any bucket (`sum(breakdown) <= total`). Blast radius
 * (apps/components) is out of scope for this tile — see Violations for per-instance counts.
 */
export interface VulnerabilitiesBreakdown {
  readonly critical: number;
  readonly high: number;
  readonly medium: number;
  readonly low: number;
}

/** Filter dimensions that may be reported as unsupported on a metric entry. */
export type UnsupportedMetricDimension = 'stageIds' | 'tagIds';

/** A single KPI entry: a headline `total` and an optional metric-specific breakdown. */
export interface MetricEntry<TBreakdown = unknown> {
  readonly total: number | null;
  readonly breakdown: TBreakdown | null;
  /** Provenance hint from the backend (e.g. 'index' | 'sql'); informational only. */
  readonly source?: string | null;
  readonly errorCode?: 'UNSUPPORTED_FILTER_COMBINATION' | 'METRIC_UNAVAILABLE';
  readonly unsupportedDimensions?: readonly UnsupportedMetricDimension[];
}

/** Full `POST /rest/dashboard/metrics` 200 response body. */
export interface DashboardMetricsResponse {
  readonly applications?: MetricEntry<ApplicationsBreakdown>;
  readonly violations?: MetricEntry<ViolationsBreakdown>;
  readonly waivers?: MetricEntry<WaiversBreakdown>;
  /** Optional — backend may not ship this yet. Render gracefully when absent. */
  readonly components?: MetricEntry<unknown>;
  /** Index-native cheap-tier totals (CLM-40927). All optional; treat as absent-safe. */
  readonly organizations?: MetricEntry<null>;
  readonly policies?: MetricEntry<null>;
  readonly vulnerabilities?: MetricEntry<VulnerabilitiesBreakdown>;
  readonly legal?: MetricEntry<LegalBreakdown>;
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
  /**
   * When false, backend skips heavy {@code countDistinct} tiles (components / vulnerabilities /
   * legal). The grid loads fast KPIs first, then re-requests with true to fill the rest.
   */
  readonly includeHeavyMetrics?: boolean;
}

export interface UseDashboardActiveFilterResult {
  readonly loading: boolean;
  readonly scope: DashboardMetricsScope;
  readonly needsAcknowledgement: boolean;
  readonly error: Error | null;
  readonly retry: () => void;
}
