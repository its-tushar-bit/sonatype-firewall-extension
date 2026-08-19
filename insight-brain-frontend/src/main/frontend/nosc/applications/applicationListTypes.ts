/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/** Risk counts — mirrors backend {@code RiskDTO}. Reused for app totals and per-stage rows. */
export type ApplicationRiskCounts = {
  readonly totalRisk: number;
  readonly criticalRisk: number;
  readonly severeRisk: number;
  readonly moderateRisk: number;
  readonly lowRisk: number;
};

/**
 * Per-stage evaluation row — mirrors backend {@code StageRiskScoreDTO}.
 * Distinct from {@link ApplicationRiskCounts}: this adds stage identity, scanId,
 * and evaluationDate around the nested risk counts.
 */
export type ApplicationStageRisk = {
  readonly stageTypeId: string;
  readonly stageTypeName: string;
  readonly scanId: string;
  /** ISO-8601 evaluation timestamp when provided by the list API (CLM-42228). */
  readonly evaluationDate?: string;
  readonly risk: ApplicationRiskCounts;
};

/**
 * Evaluation card payload — mirrors backend {@code ApplicationRiskScoreDTO}.
 * {@code applicationId} is the publicId used in Preview routes.
 */
export type ApplicationRiskScore = {
  readonly organizationName: string;
  readonly organizationId: string;
  readonly applicationName: string;
  readonly applicationId: string;
  readonly totalApplicationRisk: ApplicationRiskCounts;
  readonly stageRisks: ReadonlyArray<ApplicationStageRisk>;
  /** Latest evaluation timestamp from the list API when provided. */
  readonly lastEvaluationDate?: string;
};

export type ApplicationsFacetEntry = {
  readonly id: string;
  readonly label: string;
  readonly count: number;
};

/**
 * Policy types offered in the rail, in Martha's display order. Ids are the indexed
 * {@code policyViolationThreatCategory} terms.
 */
export const APPLICATIONS_POLICY_TYPES: ReadonlyArray<{ readonly id: string; readonly label: string }> = [
  { id: 'security', label: 'Security' },
  { id: 'license', label: 'License' },
  { id: 'quality', label: 'Quality' },
  { id: 'other', label: 'Other' },
];

/** Violation states offered in the rail. */
export const APPLICATIONS_VIOLATION_STATES: ReadonlyArray<{ readonly id: string; readonly label: string }> = [
  { id: 'OPEN', label: 'Open' },
  { id: 'WAIVED', label: 'Waived' },
  { id: 'LEGACY_VIOLATION', label: 'Legacy' },
];

/**
 * Builds facet rows for a fixed option domain. Unlike discovered org/app/stage ids these options are
 * always rendered, so an option the server omitted (no matching applications) reports a zero count
 * rather than disappearing from the rail.
 */
export function fixedDomainFacetEntries(
  domain: ReadonlyArray<{ readonly id: string; readonly label: string }>,
  counts?: Readonly<Record<string, number>> | null,
): ReadonlyArray<ApplicationsFacetEntry> {
  return domain.map(({ id, label }) => ({ id, label, count: counts?.[id] ?? 0 }));
}

/** Renders a fixed domain with no counts yet, so the rail keeps its options while facets load. */
export function zeroCountFacetEntries(
  domain: ReadonlyArray<{ readonly id: string; readonly label: string }>,
): ReadonlyArray<ApplicationsFacetEntry> {
  return fixedDomainFacetEntries(domain);
}

/** Facet counts for the filter rail (CLM-42225 wires live facets from S6). */
export type ApplicationsFilterFacetCounts = {
  readonly totalApplications: number;
  readonly stages: ReadonlyArray<ApplicationsFacetEntry>;
  readonly organizations: ReadonlyArray<ApplicationsFacetEntry>;
  readonly applications: ReadonlyArray<ApplicationsFacetEntry>;
  /**
   * Policy type and violation state are fixed domains rather than discovered ids, so every option is
   * always present and a zero count means "no matching applications under the current filters"
   * (CLM-43211).
   */
  readonly policyTypes: ReadonlyArray<ApplicationsFacetEntry>;
  readonly violationStates: ReadonlyArray<ApplicationsFacetEntry>;
};
