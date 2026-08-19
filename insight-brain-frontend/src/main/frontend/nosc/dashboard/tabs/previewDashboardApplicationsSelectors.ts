/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { selectApplicationResults } from 'MainRoot/dashboard/dashboardSelectors';
import { previewDashboardSliceError } from 'MainRoot/nosc/dashboard/tabs/previewDashboardSliceError';

/**
 * Re-export the Classic Applications-tab selector under typed names
 * scoped to the Preview surface. We do NOT duplicate the underlying
 * selector or the slice — `selectApplicationResults` is the single
 * source of truth, fetched by `loadApplicationResults` from
 * `dashboard/results/dashboardResultsActions.js`.
 *
 * The shape we type here mirrors `state.dashboard.applications` as
 * declared in `dashboardReducer.js` `initState.applications`.
 * `applicationId` is the publicId (see
 * `DashboardApplicationsTableStageRiskRow.jsx` for the ui-router
 * call site that proves this).
 */
export type PreviewDashboardStageRisk = {
  scanId: string;
  stageTypeName: string;
  risk: {
    totalRisk: number;
    criticalRisk: number;
    severeRisk: number;
    moderateRisk: number;
    lowRisk: number;
  };
};

export type PreviewDashboardApplication = {
  /** publicId — the slice field is named `applicationId` but the
   *  value IS the publicId. Renamed in our typed view via JSDoc
   *  only; the property name stays `applicationId` for slice
   *  compatibility. */
  applicationId: string;
  applicationName: string;
  totalApplicationRisk: {
    totalRisk: number;
    criticalRisk: number;
    severeRisk: number;
    moderateRisk: number;
    lowRisk: number;
  };
  stageRisks: PreviewDashboardStageRisk[];
};

export type DashboardApplicationsSlice = {
  results: PreviewDashboardApplication[] | null;
  hasNextPage: boolean;
  classyBrew: object | null;
  error: string | null;
  sortFields: string[];
  hasMultiplePages: boolean;
  page: number | null;
};

export const selectPreviewApplicationsSlice = (state: unknown): DashboardApplicationsSlice =>
  selectApplicationResults(state) as DashboardApplicationsSlice;

export const selectPreviewApplications = createSelector(
  selectPreviewApplicationsSlice,
  (slice) => slice.results ?? []
);

export const selectPreviewApplicationsLoading = createSelector(
  selectPreviewApplicationsSlice,
  (slice) => slice.results === null && slice.error === null
);

export const selectPreviewApplicationsError = createSelector(
  selectPreviewApplicationsSlice,
  (slice) => previewDashboardSliceError(slice.error, 'Failed to load applications'),
);

export const selectPreviewApplicationsSortFields = createSelector(
  selectPreviewApplicationsSlice,
  (slice) => slice.sortFields
);
