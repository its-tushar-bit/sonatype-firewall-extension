/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { previewDashboardSliceError } from 'MainRoot/nosc/dashboard/tabs/previewDashboardSliceError';
import {
  DisplayNameDTO,
  PreviewDashboardComponentIdentifier,
} from 'MainRoot/nosc/dashboard/tabs/previewDashboardTypes';

/**
 * Typed selectors for the Preview Dashboard "Components" tab. Reads
 * the same `state.dashboard.components` Redux slice the Classic
 * DashboardComponentsContainer reads from — no parallel slice, no
 * extra HTTP.
 */
/**
 * Component row shape returned by /rest/dashboard/policy/componentRisks.
 *
 * Verified field names against an actual response from the dev IQ on
 * 2026-05-18 — these are the names the backend emits, not the names
 * the Classic table renders (Classic uses these verbatim too).
 *
 * `derivedComponentName` is a pre-flattened string; `displayName` is
 * the original `{parts, name}` DTO and is only needed if
 * derivedComponentName is missing.
 *
 * `score` is the total severity for this component across all its
 * violations. `scoreCritical/Severe/Moderate/Low` are the per-severity
 * counts. `affectedApplications` is the count of distinct applications
 * the component is found in.
 */
export type { DisplayNameDTO } from 'MainRoot/nosc/dashboard/tabs/previewDashboardTypes';

export interface PreviewDashboardComponent {
  hash?: string;
  score?: number;
  scoreCritical?: number;
  scoreSevere?: number;
  scoreModerate?: number;
  scoreLow?: number;
  affectedApplications?: number;
  derivedComponentName?: string;
  displayName?: DisplayNameDTO;
  filename?: string;
  componentIdentifier?: PreviewDashboardComponentIdentifier;
}

interface DashboardComponentsSlice {
  results?: PreviewDashboardComponent[] | null;
  error?: unknown;
  hasNextPage?: boolean;
  hasMultiplePages?: boolean;
  page?: number;
}

interface RootStateLike {
  dashboard?: {
    components?: DashboardComponentsSlice;
  };
}

const selectComponentsSlice = (s: RootStateLike): DashboardComponentsSlice =>
  s.dashboard?.components ?? {};

export const selectPreviewComponents = createSelector(
  selectComponentsSlice,
  (slice): PreviewDashboardComponent[] => slice.results ?? [],
);

export const selectPreviewComponentsLoading = createSelector(
  selectComponentsSlice,
  (slice): boolean => slice.results == null && slice.error == null,
);

export const selectPreviewComponentsError = createSelector(
  selectComponentsSlice,
  (slice): string | null => previewDashboardSliceError(slice.error, 'Failed to load components'),
);
