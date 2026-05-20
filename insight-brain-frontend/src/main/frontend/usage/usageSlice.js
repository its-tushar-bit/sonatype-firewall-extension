/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import {
  fetchConsumptionSummary,
  fetchConsumptionHistoryBreakdown,
  fetchConsumptionBySource,
  fetchTopConsumingApps,
  fetchDailyHistory,
} from './usageApi';
import { Messages } from 'MainRoot/util/CommonServices';

const REDUCER_NAME = 'usage';

const initialState = {
  summary: null,
  historyBreakdown: [],
  chartAggregation: 'daily',
  sourceBreakdown: [],
  topApps: null,
  dailyHistory: null,

  loadingSummary: false,
  loadingHistoryBreakdown: false,
  loadingSourceBreakdown: false,
  loadingTopApps: false,
  loadingDailyHistory: false,
  loadingAll: false,

  loadErrorSummary: null,
  loadErrorHistoryBreakdown: null,
  loadErrorSourceBreakdown: null,
  loadErrorTopApps: null,
  loadErrorDailyHistory: null,
  loadErrorAll: null,
};

const loadSummary = createAsyncThunk(`${REDUCER_NAME}/loadSummary`, async (_, { rejectWithValue }) => {
  try {
    const response = await fetchConsumptionSummary();
    return response.data;
  } catch (error) {
    return rejectWithValue(error);
  }
});

const loadHistoryBreakdown = createAsyncThunk(
  `${REDUCER_NAME}/loadHistoryBreakdown`,
  async (aggregation = 'daily', { rejectWithValue }) => {
    try {
      const response = await fetchConsumptionHistoryBreakdown(aggregation);
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const loadSourceBreakdown = createAsyncThunk(
  `${REDUCER_NAME}/loadSourceBreakdown`,
  async (_, { rejectWithValue }) => {
    try {
      const response = await fetchConsumptionBySource();
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const loadTopApps = createAsyncThunk(`${REDUCER_NAME}/loadTopApps`, async (_, { rejectWithValue }) => {
  try {
    const response = await fetchTopConsumingApps();
    return response.data;
  } catch (error) {
    return rejectWithValue(error);
  }
});

const loadDailyHistory = createAsyncThunk(`${REDUCER_NAME}/loadDailyHistory`, async (_, { rejectWithValue }) => {
  try {
    const response = await fetchDailyHistory();
    return response.data;
  } catch (error) {
    return rejectWithValue(error);
  }
});

const loadAllUsageData = createAsyncThunk(
  `${REDUCER_NAME}/loadAllUsageData`,
  async (aggregation = 'daily', { rejectWithValue }) => {
    try {
      const [summaryRes, breakdownRes, sourceRes, topAppsRes, dailyRes] =
        await Promise.allSettled([
          fetchConsumptionSummary(),
          fetchConsumptionHistoryBreakdown(aggregation),
          fetchConsumptionBySource(),
          fetchTopConsumingApps(),
          fetchDailyHistory(),
        ]);

      if (summaryRes.status === 'rejected') {
        return rejectWithValue(summaryRes.reason);
      }

      return {
        aggregation,
        summary: summaryRes.value.data,
        historyBreakdown: breakdownRes.status === 'fulfilled' ? breakdownRes.value.data : [],
        sourceBreakdown: sourceRes.status === 'fulfilled' ? sourceRes.value.data : [],
        topApps: topAppsRes.status === 'fulfilled' ? topAppsRes.value.data : null,
        dailyHistory: dailyRes.status === 'fulfilled' ? dailyRes.value.data : null,
      };
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const summaryPending = (state) => ({ ...state, loadingSummary: true, loadErrorSummary: null });
const summaryFulfilled = (state, { payload }) => ({
  ...state,
  loadingSummary: false,
  summary: payload,
});
const summaryRejected = (state, { payload }) => ({
  ...state,
  loadingSummary: false,
  loadErrorSummary: Messages.getHttpErrorMessage(payload),
});

const historyBreakdownPending = (state) => ({
  ...state,
  loadingHistoryBreakdown: true,
  loadErrorHistoryBreakdown: null,
});
// Last-wins guard: only apply the payload (or error) when the settled request's
// aggregation matches the currently-selected one. Prevents a stale slow response
// from overwriting fresh state after rapid dropdown changes. Always clears the
// loading flag on settlement regardless of the guard — the in-flight request is
// done either way, and a stuck loading state is worse than stale data.
const historyBreakdownFulfilled = (state, { payload, meta }) => {
  if (meta.arg !== state.chartAggregation) {
    return { ...state, loadingHistoryBreakdown: false };
  }
  return {
    ...state,
    loadingHistoryBreakdown: false,
    historyBreakdown: payload,
  };
};
const historyBreakdownRejected = (state, { payload, meta }) => {
  if (meta.arg !== state.chartAggregation) {
    return { ...state, loadingHistoryBreakdown: false };
  }
  return {
    ...state,
    loadingHistoryBreakdown: false,
    loadErrorHistoryBreakdown: Messages.getHttpErrorMessage(payload),
  };
};

const sourceBreakdownPending = (state) => ({
  ...state,
  loadingSourceBreakdown: true,
  loadErrorSourceBreakdown: null,
});
const sourceBreakdownFulfilled = (state, { payload }) => ({
  ...state,
  loadingSourceBreakdown: false,
  sourceBreakdown: payload,
});
const sourceBreakdownRejected = (state, { payload }) => ({
  ...state,
  loadingSourceBreakdown: false,
  loadErrorSourceBreakdown: Messages.getHttpErrorMessage(payload),
});

const topAppsPending = (state) => ({ ...state, loadingTopApps: true, loadErrorTopApps: null });
const topAppsFulfilled = (state, { payload }) => ({
  ...state,
  loadingTopApps: false,
  topApps: payload,
});
const topAppsRejected = (state, { payload }) => ({
  ...state,
  loadingTopApps: false,
  loadErrorTopApps: Messages.getHttpErrorMessage(payload),
});

const dailyHistoryPending = (state) => ({
  ...state,
  loadingDailyHistory: true,
  loadErrorDailyHistory: null,
});
const dailyHistoryFulfilled = (state, { payload }) => ({
  ...state,
  loadingDailyHistory: false,
  dailyHistory: payload,
});
const dailyHistoryRejected = (state, { payload }) => ({
  ...state,
  loadingDailyHistory: false,
  loadErrorDailyHistory: Messages.getHttpErrorMessage(payload),
});

const allPending = (state) => ({ ...state, loadingAll: true, loadErrorAll: null });
const allFulfilled = (state, { payload }) => {
  const { aggregation, historyBreakdown, ...rest } = payload;
  return {
    ...state,
    loadingAll: false,
    ...rest,
    ...(aggregation === state.chartAggregation ? { historyBreakdown } : {}),
  };
};
const allRejected = (state, { payload }) => ({
  ...state,
  loadingAll: false,
  loadErrorAll: Messages.getHttpErrorMessage(payload),
});

const setChartAggregation = (state, action) => ({
  ...state,
  chartAggregation: action.payload,
});

const usageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setChartAggregation,
  },
  extraReducers: (builder) => {
    builder
      .addCase(loadSummary.pending, summaryPending)
      .addCase(loadSummary.fulfilled, summaryFulfilled)
      .addCase(loadSummary.rejected, summaryRejected)
      .addCase(loadHistoryBreakdown.pending, historyBreakdownPending)
      .addCase(loadHistoryBreakdown.fulfilled, historyBreakdownFulfilled)
      .addCase(loadHistoryBreakdown.rejected, historyBreakdownRejected)
      .addCase(loadSourceBreakdown.pending, sourceBreakdownPending)
      .addCase(loadSourceBreakdown.fulfilled, sourceBreakdownFulfilled)
      .addCase(loadSourceBreakdown.rejected, sourceBreakdownRejected)
      .addCase(loadTopApps.pending, topAppsPending)
      .addCase(loadTopApps.fulfilled, topAppsFulfilled)
      .addCase(loadTopApps.rejected, topAppsRejected)
      .addCase(loadDailyHistory.pending, dailyHistoryPending)
      .addCase(loadDailyHistory.fulfilled, dailyHistoryFulfilled)
      .addCase(loadDailyHistory.rejected, dailyHistoryRejected)
      .addCase(loadAllUsageData.pending, allPending)
      .addCase(loadAllUsageData.fulfilled, allFulfilled)
      .addCase(loadAllUsageData.rejected, allRejected);
  },
});

export default usageSlice.reducer;

export const actions = {
  ...usageSlice.actions,
  loadSummary,
  loadHistoryBreakdown,
  loadSourceBreakdown,
  loadTopApps,
  loadDailyHistory,
  loadAllUsageData,
};
