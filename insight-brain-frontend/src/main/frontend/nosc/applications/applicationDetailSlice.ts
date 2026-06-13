/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import {
  getApplicationReportsUrl,
  getApplicationReportRawUrl,
  getReportPolicyThreatsUrl,
} from 'MainRoot/util/CLMLocation';
import {
  ApiApplicationReport,
  PolicyThreatsResponse,
  RawReportResponse,
} from './applicationDetailTypes';

/**
 * Redux fetch backing the native Nexus One Application Detail page
 * (CLM-39709). Replaces the three in-component `useEffect` + `axios.get`
 * blocks (reports, policythreats, raw report) with `createAsyncThunk`s so
 * the page reads its data through the project's standard RTK pattern (see
 * `previewDashboardNewestRisksSlice` for the reference shape).
 *
 * The three fetches have a dependency chain that the `useApplicationDetailData`
 * hook orchestrates: `reports` needs the application's internal id, while
 * `policyThreats` and `rawReport` need the publicId + the scanId parsed from
 * the latest report. The slice itself stays dumb — it only models the
 * status/data/error of each fetch and exposes a `reset` so the hook can clear
 * stale data when the user navigates to a different application.
 */

export type ApplicationDetailFetchStatus = 'idle' | 'loading' | 'ready' | 'error';

export interface ApplicationDetailSubState<T> {
  readonly status: ApplicationDetailFetchStatus;
  readonly data: T | null;
  readonly error: string | null;
}

export interface ApplicationDetailState {
  readonly reports: ApplicationDetailSubState<ReadonlyArray<ApiApplicationReport>>;
  readonly policyThreats: ApplicationDetailSubState<PolicyThreatsResponse>;
  readonly rawReport: ApplicationDetailSubState<RawReportResponse>;
}

const emptySubState = <T>(): ApplicationDetailSubState<T> => ({
  status: 'idle',
  data: null,
  error: null,
});

const initialState: ApplicationDetailState = {
  reports: emptySubState<ReadonlyArray<ApiApplicationReport>>(),
  policyThreats: emptySubState<PolicyThreatsResponse>(),
  rawReport: emptySubState<RawReportResponse>(),
};

export const fetchApplicationReports = createAsyncThunk(
  'applicationDetail/fetchReports',
  async ({ applicationInternalId }: { applicationInternalId: string }, { signal }) => {
    const { data } = await axios.get<ReadonlyArray<ApiApplicationReport>>(
      getApplicationReportsUrl(applicationInternalId),
      { signal },
    );
    return Array.isArray(data) ? data : [];
  },
);

export const fetchApplicationPolicyThreats = createAsyncThunk(
  'applicationDetail/fetchPolicyThreats',
  async ({ publicId, scanId }: { publicId: string; scanId: string }, { signal }) => {
    const { data } = await axios.get<PolicyThreatsResponse>(
      getReportPolicyThreatsUrl(publicId, scanId),
      { signal },
    );
    return data;
  },
);

export const fetchApplicationRawReport = createAsyncThunk(
  'applicationDetail/fetchRawReport',
  async ({ publicId, scanId }: { publicId: string; scanId: string }, { signal }) => {
    const { data } = await axios.get<RawReportResponse>(
      getApplicationReportRawUrl(publicId, scanId),
      { signal },
    );
    return data;
  },
);

const applicationDetailSlice = createSlice({
  name: 'applicationDetail',
  initialState,
  reducers: {
    /** Clear all three fetches back to their initial state — used when the
     *  user navigates to a different application so stale data never flashes. */
    reset: () => initialState,
    /** Clear just the policythreats fetch (no scanId → nothing to show). */
    resetPolicyThreats: (state) => {
      state.policyThreats = emptySubState<PolicyThreatsResponse>();
    },
    /** Clear just the raw-report fetch (no scanId → nothing to show). */
    resetRawReport: (state) => {
      state.rawReport = emptySubState<RawReportResponse>();
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchApplicationReports.pending, (state) => {
        state.reports.status = 'loading';
        state.reports.error = null;
      })
      .addCase(fetchApplicationReports.fulfilled, (state, action) => {
        state.reports.status = 'ready';
        state.reports.data = action.payload;
      })
      .addCase(fetchApplicationReports.rejected, (state, action) => {
        if (action.meta.aborted) return;
        state.reports.status = 'error';
        state.reports.data = null;
        state.reports.error = action.error.message ?? 'Failed to load reports';
      })
      .addCase(fetchApplicationPolicyThreats.pending, (state) => {
        state.policyThreats.status = 'loading';
        state.policyThreats.error = null;
      })
      .addCase(fetchApplicationPolicyThreats.fulfilled, (state, action) => {
        state.policyThreats.status = 'ready';
        state.policyThreats.data = action.payload;
      })
      .addCase(fetchApplicationPolicyThreats.rejected, (state, action) => {
        if (action.meta.aborted) return;
        state.policyThreats.status = 'error';
        state.policyThreats.data = null;
        state.policyThreats.error = action.error.message ?? 'Failed to load policy data';
      })
      .addCase(fetchApplicationRawReport.pending, (state) => {
        state.rawReport.status = 'loading';
        state.rawReport.error = null;
      })
      .addCase(fetchApplicationRawReport.fulfilled, (state, action) => {
        state.rawReport.status = 'ready';
        state.rawReport.data = action.payload;
      })
      .addCase(fetchApplicationRawReport.rejected, (state, action) => {
        if (action.meta.aborted) return;
        state.rawReport.status = 'error';
        state.rawReport.data = null;
        state.rawReport.error = action.error.message ?? 'Failed to load raw report';
      });
  },
});

export const { reset, resetPolicyThreats, resetRawReport } = applicationDetailSlice.actions;

export default applicationDetailSlice.reducer;

interface ApplicationDetailRootState {
  applicationDetail: ApplicationDetailState;
}

export const selectApplicationReportsState = (
  state: ApplicationDetailRootState,
): ApplicationDetailSubState<ReadonlyArray<ApiApplicationReport>> => state.applicationDetail.reports;

export const selectApplicationPolicyThreatsState = (
  state: ApplicationDetailRootState,
): ApplicationDetailSubState<PolicyThreatsResponse> => state.applicationDetail.policyThreats;

export const selectApplicationRawReportState = (
  state: ApplicationDetailRootState,
): ApplicationDetailSubState<RawReportResponse> => state.applicationDetail.rawReport;
