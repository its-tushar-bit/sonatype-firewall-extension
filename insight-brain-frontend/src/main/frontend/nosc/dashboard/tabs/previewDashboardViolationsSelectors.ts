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
 * Typed selectors for the Preview Dashboard "Violations" tab. Reads
 * the same `state.dashboard.violations` Redux slice the Classic
 * DashboardViolationsContainer reads from — no parallel slice, no
 * extra HTTP. Filter changes still flow through the existing Classic
 * filter rail's dispatch chain.
 */
/**
 * Violation row shape from /rest/dashboard/policy/newestRisks.
 * Verified against a live response on 2026-05-18.
 *
 * The component info (name, filename, displayName parts) is on the
 * violation object itself — `derivedComponentName` is the
 * pre-flattened string; `displayName` is the {parts, name} DTO.
 */
export type { DisplayNameDTO } from 'MainRoot/nosc/dashboard/tabs/previewDashboardTypes';

export interface PreviewDashboardViolation {
  policyViolationId: string;
  threatLevel: number;
  policyName: string;
  policyId?: string;
  applicationName: string;
  organizationName?: string;
  hash?: string;
  derivedComponentName?: string;
  displayName?: DisplayNameDTO;
  filename?: string;
  componentIdentifier?: PreviewDashboardComponentIdentifier;
  firstOccurrenceTime?: number;
  referenceId?: string | null;
}

interface DashboardViolationsSlice {
  results?: PreviewDashboardViolation[] | null;
  error?: unknown;
  hasNextPage?: boolean;
  hasMultiplePages?: boolean;
  page?: number;
}

interface RootStateLike {
  dashboard?: {
    violations?: DashboardViolationsSlice;
  };
}

const selectViolationsSlice = (s: RootStateLike): DashboardViolationsSlice =>
  s.dashboard?.violations ?? {};

export const selectPreviewViolations = createSelector(
  selectViolationsSlice,
  (slice): PreviewDashboardViolation[] => slice.results ?? [],
);

export const selectPreviewViolationsLoading = createSelector(
  selectViolationsSlice,
  (slice): boolean => slice.results == null && slice.error == null,
);

export const selectPreviewViolationsError = createSelector(
  selectViolationsSlice,
  (slice): string | null => previewDashboardSliceError(slice.error, 'Failed to load violations'),
);
