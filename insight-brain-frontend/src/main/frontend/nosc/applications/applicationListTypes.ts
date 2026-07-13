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

/** Facet counts for the filter rail (CLM-42225 wires live facets from S6). */
export type ApplicationsFilterFacetCounts = {
  readonly totalApplications: number;
  readonly threatLevels: ReadonlyArray<{ readonly id: string; readonly label: string; readonly count: number }>;
  readonly stages: ReadonlyArray<{ readonly id: string; readonly label: string; readonly count: number }>;
  readonly organizations: ReadonlyArray<{ readonly id: string; readonly label: string; readonly count: number }>;
  readonly applications: ReadonlyArray<{ readonly id: string; readonly label: string; readonly count: number }>;
};
