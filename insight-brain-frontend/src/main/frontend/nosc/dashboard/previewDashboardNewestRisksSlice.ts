/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getNewestRisksUrl } from 'MainRoot/util/CLMLocation';

/**
 * Shared Redux fetch for Overview tiles that aggregate
 * `/rest/dashboard/policy/newestRisks` (Severity Strip, Top Policy
 * Violations, Risk Over Time). One in-flight request serves all three
 * tiles instead of three independent hook-level GETs.
 */

export interface NewestRiskViolationRow {
  readonly policyId?: string;
  readonly policyName?: string;
  readonly threatLevel?: number;
  readonly firstOccurrenceTime?: number | string;
}

export type PreviewNewestRisksStatus = 'idle' | 'loading' | 'ready' | 'error';

export interface PreviewNewestRisksState {
  readonly status: PreviewNewestRisksStatus;
  readonly violations: ReadonlyArray<NewestRiskViolationRow>;
  readonly error: string | null;
}

const EMPTY_FILTER_REQUEST = {
  pageSize: 100,
  page: 0,
};

const initialState: PreviewNewestRisksState = {
  status: 'idle',
  violations: [],
  error: null,
};

export const fetchPreviewNewestRisks = createAsyncThunk(
  'previewDashboardNewestRisks/fetch',
  async (_, { signal }) => {
    const response = await axios.post<{ dashboardResults?: NewestRiskViolationRow[] }>(
      getNewestRisksUrl(),
      EMPTY_FILTER_REQUEST,
      { signal },
    );
    return response.data?.dashboardResults ?? [];
  },
);

const previewDashboardNewestRisksSlice = createSlice({
  name: 'previewDashboardNewestRisks',
  initialState,
  reducers: {
    resetPreviewNewestRisks: () => initialState,
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchPreviewNewestRisks.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(fetchPreviewNewestRisks.fulfilled, (state, action) => {
        state.status = 'ready';
        state.violations = action.payload;
      })
      .addCase(fetchPreviewNewestRisks.rejected, (state, action) => {
        if (action.meta.aborted) {
          return;
        }
        state.status = 'error';
        state.error = action.error.message ?? 'Failed to load dashboard violations';
      });
  },
});

export const { resetPreviewNewestRisks } = previewDashboardNewestRisksSlice.actions;

export default previewDashboardNewestRisksSlice.reducer;

export const selectPreviewNewestRisksState = (state: {
  previewDashboardNewestRisks: PreviewNewestRisksState;
}): PreviewNewestRisksState => state.previewDashboardNewestRisks;
