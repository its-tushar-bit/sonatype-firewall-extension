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
  fetchConsumptionByStage,
  fetchTopConsumingApps,
  fetchDailyHistory,
} from './usageApi';
import { Messages } from 'MainRoot/util/CommonServices';

const REDUCER_NAME = 'usage';

const initialState = {
  summary: null,
  // historyBreakdown is owned by Trends; cumulativeHistoryBreakdown is owned
  // by the Overview range filter. Without separate fields, a Trends-Daily +
  // Overview-Last6Months scenario lets the monthly response overwrite the
  // daily data the Trends chart is rendering (and vice versa).
  historyBreakdown: [],
  cumulativeHistoryBreakdown: [],
  // chartAggregation is owned by the Trends tab's Daily/Weekly/Monthly <select>.
  // cumulativeChartAggregation is owned by the Overview tab's range filter
  // (This month / Last 3 / Last 6 months). Splitting the pointers keeps the
  // user's Trends selection from being clobbered when switching to Overview.
  chartAggregation: 'daily',
  cumulativeChartAggregation: 'daily',
  sourceBreakdown: [],
  stageBreakdown: [],
  topApps: null,
  dailyHistory: null,

  activeTab: 'overview',
  cumulativeFilter: 'thisMonth',
  lastRefreshedAt: null,

  loadingSummary: false,
  loadingHistoryBreakdown: false,
  loadingSourceBreakdown: false,
  loadingStageBreakdown: false,
  loadingTopApps: false,
  loadingDailyHistory: false,
  loadingAll: false,

  loadErrorSummary: null,
  loadErrorHistoryBreakdown: null,
  loadErrorSourceBreakdown: null,
  loadErrorStageBreakdown: null,
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

const loadSourceBreakdown = createAsyncThunk(`${REDUCER_NAME}/loadSourceBreakdown`, async (_, { rejectWithValue }) => {
  try {
    const response = await fetchConsumptionBySource();
    return response.data;
  } catch (error) {
    return rejectWithValue(error);
  }
});

const loadStageBreakdown = createAsyncThunk(`${REDUCER_NAME}/loadStageBreakdown`, async (_, { rejectWithValue }) => {
  try {
    const response = await fetchConsumptionByStage();
    return response.data;
  } catch (error) {
    return rejectWithValue(error);
  }
});

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
      const [summaryRes, breakdownRes, sourceRes, stageRes, topAppsRes, dailyRes] = await Promise.allSettled([
        fetchConsumptionSummary(),
        fetchConsumptionHistoryBreakdown(aggregation),
        fetchConsumptionBySource(),
        fetchConsumptionByStage(),
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
        stageBreakdown: stageRes.status === 'fulfilled' ? stageRes.value.data : [],
        topApps: topAppsRes.status === 'fulfilled' ? topAppsRes.value.data : null,
        dailyHistory: dailyRes.status === 'fulfilled' ? dailyRes.value.data : null,
        loadedAt: Date.now(),
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
// Last-wins guard. The shared loadHistoryBreakdown thunk serves two consumers:
// the Trends chart (chartAggregation) and the Overview cumulative chart
// (cumulativeChartAggregation). Route the payload (or error) to whichever
// field's pointer matches meta.arg — both, when the two contexts happen to
// agree on the same aggregation. If neither matches, the response is stale.
// Always clears the loading flag on settlement regardless of the guard — the
// in-flight request is done either way, and a stuck loading state is worse
// than stale data.
const matchesTrends = (state, arg) => arg === state.chartAggregation;
const matchesCumulative = (state, arg) => arg === state.cumulativeChartAggregation;

const historyBreakdownFulfilled = (state, { payload, meta }) => {
  const next = { ...state, loadingHistoryBreakdown: false };
  if (matchesTrends(state, meta.arg)) next.historyBreakdown = payload;
  if (matchesCumulative(state, meta.arg)) next.cumulativeHistoryBreakdown = payload;
  return next;
};
const historyBreakdownRejected = (state, { payload, meta }) => {
  // Set the error only when the failing request was for one of the active
  // selections. Stale rejections are silently dropped on the floor.
  if (!matchesTrends(state, meta.arg) && !matchesCumulative(state, meta.arg)) {
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

const stageBreakdownPending = (state) => ({
  ...state,
  loadingStageBreakdown: true,
  loadErrorStageBreakdown: null,
});
const stageBreakdownFulfilled = (state, { payload }) => ({
  ...state,
  loadingStageBreakdown: false,
  stageBreakdown: payload,
});
const stageBreakdownRejected = (state, { payload }) => ({
  ...state,
  loadingStageBreakdown: false,
  loadErrorStageBreakdown: Messages.getHttpErrorMessage(payload),
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
  // Destructure the fields that need custom routing before spreading the rest.
  // `rest` is expected to contain only slice-safe fields (summary, sourceBreakdown,
  // stageBreakdown, topApps, dailyHistory). If the API payload ever gains a top-level
  // `historyBreakdown` or `cumulativeHistoryBreakdown` field, `...rest` would silently
  // clobber the routed writes below — audit this routing if the payload schema grows.
  const { aggregation, historyBreakdown, loadedAt, ...rest } = payload;
  return {
    ...state,
    loadingAll: false,
    ...rest,
    // Stamp lastRefreshedAt on every successful full load so the "Last refreshed:" subtitle
    // shows a real relative time from first paint, not the "recently" fallback.
    lastRefreshedAt: loadedAt ?? state.lastRefreshedAt,
    // Route the bundled history payload to whichever field's pointer matches
    // the request's aggregation. On initial mount both pointers default to
    // 'daily' so both fields populate; on refresh while Trends is on Weekly
    // and Overview is on Last3Months (monthly), the response is necessarily
    // for one of them and only that field updates here — the other field's
    // dedicated loadHistoryBreakdown round-trip keeps it accurate.
    ...(aggregation === state.chartAggregation ? { historyBreakdown } : {}),
    ...(aggregation === state.cumulativeChartAggregation ? { cumulativeHistoryBreakdown: historyBreakdown } : {}),
  };
};
const allRejected = (state, { payload }) => ({
  ...state,
  loadingAll: false,
  loadErrorAll: Messages.getHttpErrorMessage(payload),
});

const refresh = createAsyncThunk(`${REDUCER_NAME}/refresh`, async (_, { dispatch, getState, rejectWithValue }) => {
  const { chartAggregation, cumulativeChartAggregation } = getState().usage;
  const result = await dispatch(loadAllUsageData(chartAggregation));
  if (result.meta.requestStatus === 'rejected') {
    return rejectWithValue(result.payload);
  }
  // The bundled load only routes the response to whichever aggregation pointer
  // matches its request arg (allFulfilled, lines 273-274). When the Overview
  // tab's filter (cumulativeChartAggregation) differs from the Trends tab's
  // (chartAggregation), fetch the Overview's view explicitly so its cumulative
  // chart doesn't go stale after the user hits ↻.
  if (cumulativeChartAggregation !== chartAggregation) {
    const fanOut = await dispatch(loadHistoryBreakdown(cumulativeChartAggregation));
    // If the fan-out 5xx's, the user would otherwise see "Last refreshed: a
    // few seconds ago" but the Overview cumulative chart silently stays stale.
    // The thunk's own rejected reducer writes to loadErrorHistoryBreakdown
    // already; promote the refresh thunk to rejected so loadErrorAll fires
    // too and the page-level retry banner appears.
    if (fanOut.meta.requestStatus === 'rejected') {
      return rejectWithValue(fanOut.payload);
    }
  }
  // No payload needed — lastRefreshedAt is stamped by allFulfilled via
  // payload.loadedAt, which covers both initial mount and manual refresh.
  return null;
});

// Switching the cumulative filter (This month / Last 3 / Last 6) also flips the
// underlying historyBreakdown aggregation: thisMonth → daily, last3/last6 → monthly.
// Writes to cumulativeChartAggregation (Overview's own field) so the Trends
// tab's chartAggregation — which the user controls via the Daily/Weekly/Monthly
// <select> — is preserved across tab switches. Returns the loadHistoryBreakdown
// promise so callers can await fetch completion.
const changeCumulativeFilter = (filter) => (dispatch) => {
  const aggregation = filter === 'thisMonth' ? 'daily' : 'monthly';
  dispatch(usageSlice.actions.setCumulativeFilter(filter));
  dispatch(usageSlice.actions.setCumulativeChartAggregation(aggregation));
  return dispatch(loadHistoryBreakdown(aggregation));
};

const setChartAggregation = (state, action) => ({
  ...state,
  chartAggregation: action.payload,
});
const setCumulativeChartAggregation = (state, action) => ({
  ...state,
  cumulativeChartAggregation: action.payload,
});

const setActiveTab = (state, action) => ({ ...state, activeTab: action.payload });
const setCumulativeFilter = (state, action) => ({ ...state, cumulativeFilter: action.payload });

const usageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setChartAggregation,
    setCumulativeChartAggregation,
    setActiveTab,
    setCumulativeFilter,
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
      .addCase(loadStageBreakdown.pending, stageBreakdownPending)
      .addCase(loadStageBreakdown.fulfilled, stageBreakdownFulfilled)
      .addCase(loadStageBreakdown.rejected, stageBreakdownRejected)
      .addCase(loadTopApps.pending, topAppsPending)
      .addCase(loadTopApps.fulfilled, topAppsFulfilled)
      .addCase(loadTopApps.rejected, topAppsRejected)
      .addCase(loadDailyHistory.pending, dailyHistoryPending)
      .addCase(loadDailyHistory.fulfilled, dailyHistoryFulfilled)
      .addCase(loadDailyHistory.rejected, dailyHistoryRejected)
      .addCase(loadAllUsageData.pending, allPending)
      .addCase(loadAllUsageData.fulfilled, allFulfilled)
      .addCase(loadAllUsageData.rejected, allRejected)
      // refresh.pending sets loadingAll synchronously so the refresh button
      // disables on click — closing the double-click race window before the
      // inner loadAllUsageData.pending action lands on the next microtask.
      .addCase(refresh.pending, (state) => ({ ...state, loadingAll: true, loadErrorAll: null }))
      // refresh.fulfilled is handled implicitly via the inner loadAllUsageData
      // lifecycle (lastRefreshedAt stamped from the loadedAt field in
      // loadAllUsageData.fulfilled so timestamps populate on initial mount
      // too, not only on manual refresh). refresh.rejected needs an explicit
      // handler for the partial-failure case where the fan-out
      // loadHistoryBreakdown rejects — the inner loadAllUsageData succeeded
      // (so allRejected didn't fire) but the trailing dispatch failed and we
      // bubbled it through rejectWithValue. Without this case, loadErrorAll
      // stays null and the refresh appears to have succeeded silently.
      .addCase(refresh.rejected, allRejected);
  },
});

export default usageSlice.reducer;

export const actions = {
  ...usageSlice.actions,
  loadSummary,
  loadHistoryBreakdown,
  loadSourceBreakdown,
  loadStageBreakdown,
  loadTopApps,
  loadDailyHistory,
  loadAllUsageData,
  refresh,
  changeCumulativeFilter,
};
