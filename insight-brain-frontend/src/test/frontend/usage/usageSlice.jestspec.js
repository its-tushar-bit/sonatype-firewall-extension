/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { actions } from 'MainRoot/usage/usageSlice';
import { axiosMockAdapter, configureStore } from 'TestRoot/SpecUtil';

describe('usageSlice.allFulfilled', () => {
  function makeFulfilledAction(payload) {
    return {
      type: actions.loadAllUsageData.fulfilled.type,
      payload,
    };
  }

  const dailyBreakdown = [{ month: '2026-05', consumed: 100, breakdown: { UI: 100 } }];
  const weeklyBreakdown = [{ month: '2026-05-04', consumed: 50, breakdown: { CLI: 50 } }];

  it('applies historyBreakdown when payload.aggregation matches current state.chartAggregation', () => {
    const state = reducer(undefined, { type: '@@INIT' });
    const stageBreakdown = [{ month: '2026-05', consumed: 30, breakdown: { build: 30 } }];
    const action = makeFulfilledAction({
      aggregation: 'daily',
      summary: { consumed: 10 },
      historyBreakdown: dailyBreakdown,
      sourceBreakdown: [],
      stageBreakdown,
      topApps: null,
      dailyHistory: null,
    });

    const next = reducer(state, action);

    expect(next.historyBreakdown).toEqual(dailyBreakdown);
    expect(next.stageBreakdown).toEqual(stageBreakdown);
    expect(next.summary).toEqual({ consumed: 10 });
    expect(next.loadingAll).toBe(false);
  });

  it('preserves historyBreakdown when payload.aggregation is stale vs current state.chartAggregation', () => {
    const intermediate = reducer(undefined, { type: '@@INIT' });
    const afterToggle = reducer(intermediate, actions.setChartAggregation('weekly'));
    const afterBreakdownLoad = reducer(afterToggle, {
      type: actions.loadHistoryBreakdown.fulfilled.type,
      payload: weeklyBreakdown,
      meta: { arg: 'weekly' },
    });
    expect(afterBreakdownLoad.chartAggregation).toBe('weekly');
    expect(afterBreakdownLoad.historyBreakdown).toEqual(weeklyBreakdown);

    const staleAllAction = makeFulfilledAction({
      aggregation: 'daily',
      summary: { consumed: 10 },
      historyBreakdown: dailyBreakdown,
      sourceBreakdown: [],
      topApps: null,
      dailyHistory: null,
    });

    const next = reducer(afterBreakdownLoad, staleAllAction);

    expect(next.historyBreakdown).toEqual(weeklyBreakdown);
    expect(next.summary).toEqual({ consumed: 10 });
    expect(next.loadingAll).toBe(false);
  });
});

describe('usageSlice.historyBreakdownFulfilled stale-aggregation guard', () => {
  const dailyBreakdown = [{ month: '2026-05', consumed: 100, breakdown: { UI: 100 } }];
  const weeklyBreakdown = [{ month: '2026-05-04', consumed: 50, breakdown: { CLI: 50 } }];

  it('applies historyBreakdown when meta.arg matches state.chartAggregation', () => {
    const initial = reducer(undefined, { type: '@@INIT' });
    const onWeekly = reducer(initial, actions.setChartAggregation('weekly'));

    const next = reducer(onWeekly, {
      type: actions.loadHistoryBreakdown.fulfilled.type,
      payload: weeklyBreakdown,
      meta: { arg: 'weekly' },
    });

    expect(next.historyBreakdown).toEqual(weeklyBreakdown);
    expect(next.loadingHistoryBreakdown).toBe(false);
  });

  it('preserves historyBreakdown when meta.arg is stale vs state.chartAggregation', () => {
    const initial = reducer(undefined, { type: '@@INIT' });
    const onWeekly = reducer(initial, actions.setChartAggregation('weekly'));
    const withWeeklyData = reducer(onWeekly, {
      type: actions.loadHistoryBreakdown.fulfilled.type,
      payload: weeklyBreakdown,
      meta: { arg: 'weekly' },
    });

    const staleAction = {
      type: actions.loadHistoryBreakdown.fulfilled.type,
      payload: dailyBreakdown,
      meta: { arg: 'daily' },
    };
    const next = reducer(withWeeklyData, staleAction);

    expect(next.historyBreakdown).toEqual(weeklyBreakdown);
    expect(next.loadingHistoryBreakdown).toBe(false);
  });

  it('does not set loadErrorHistoryBreakdown when historyBreakdownRejected meta.arg is stale', () => {
    const initial = reducer(undefined, { type: '@@INIT' });
    const onWeekly = reducer(initial, actions.setChartAggregation('weekly'));

    const staleRejected = {
      type: actions.loadHistoryBreakdown.rejected.type,
      payload: { message: 'boom' },
      meta: { arg: 'daily' },
    };
    const next = reducer(onWeekly, staleRejected);

    expect(next.loadErrorHistoryBreakdown).toBeNull();
    expect(next.loadingHistoryBreakdown).toBe(false);
  });
});

describe('usageSlice.loadAllUsageData thunk', () => {
  let axiosMock;
  let store;

  const summaryResponse = { consumed: 100, limit: 1000 };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    store = configureStore({ reducer: { usage: reducer } });
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('absorbs secondary endpoint failures with empty fallbacks; only summary failure rejects', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, summaryResponse);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(500);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-source/).reply(500);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-stage/).reply(500);
    axiosMock.onGet(/\/api\/v2\/consumption\/top-apps/).reply(500);
    axiosMock.onGet(/\/api\/v2\/consumption\/daily-history/).reply(500);

    const result = await store.dispatch(actions.loadAllUsageData('daily'));

    expect(result.type).toBe(actions.loadAllUsageData.fulfilled.type);
    expect(result.payload.summary).toEqual(summaryResponse);
    expect(result.payload.historyBreakdown).toEqual([]);
    expect(result.payload.sourceBreakdown).toEqual([]);
    expect(result.payload.stageBreakdown).toEqual([]);
    expect(result.payload.topApps).toBeNull();
    expect(result.payload.dailyHistory).toBeNull();
  });

  it('rejects via rejectWithValue when summary fails, regardless of secondary outcomes', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(500);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-source/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-stage/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/top-apps/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/daily-history/).reply(200, null);

    const result = await store.dispatch(actions.loadAllUsageData('daily'));

    expect(result.type).toBe(actions.loadAllUsageData.rejected.type);
  });

  it('populates stageBreakdown from /history/by-stage on success', async () => {
    const stageBreakdown = [{ month: '2026-05', consumed: 60, breakdown: { build: 60 } }];
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, summaryResponse);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-source/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-stage/).reply(200, stageBreakdown);
    axiosMock.onGet(/\/api\/v2\/consumption\/top-apps/).reply(200, null);
    axiosMock.onGet(/\/api\/v2\/consumption\/daily-history/).reply(200, null);

    const result = await store.dispatch(actions.loadAllUsageData('daily'));

    expect(result.type).toBe(actions.loadAllUsageData.fulfilled.type);
    expect(result.payload.stageBreakdown).toEqual(stageBreakdown);
  });
});

describe('usageSlice.loadStageBreakdown', () => {
  it('sets loadingStageBreakdown=true on pending', () => {
    const action = { type: actions.loadStageBreakdown.pending.type };
    const state = reducer(undefined, action);
    expect(state.loadingStageBreakdown).toBe(true);
    expect(state.loadErrorStageBreakdown).toBeNull();
  });

  it('stores stageBreakdown on fulfilled', () => {
    const payload = [{ month: '2026-04-01', consumed: 100, breakdown: { build: 100 } }];
    const action = { type: actions.loadStageBreakdown.fulfilled.type, payload };
    const state = reducer(undefined, action);
    expect(state.loadingStageBreakdown).toBe(false);
    expect(state.stageBreakdown).toEqual(payload);
  });

  it('stores error on rejected', () => {
    const errorPayload = { response: { data: { message: 'boom' } } };
    const action = { type: actions.loadStageBreakdown.rejected.type, payload: errorPayload };
    const state = reducer(undefined, action);
    expect(state.loadingStageBreakdown).toBe(false);
    expect(state.loadErrorStageBreakdown).toBeTruthy();
  });
});
