/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { actions } from 'MainRoot/usage/usageSlice';
import { selectPeriodIsActive } from 'MainRoot/usage/usageSelectors';
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

  it('preserves historyBreakdown when meta.arg matches neither chartAggregation nor cumulativeChartAggregation', () => {
    // Move both pointers off the default 'daily' so a stale 'daily' response
    // is unambiguously stale (the stale-guard accepts arg matching either field).
    const initial = reducer(undefined, { type: '@@INIT' });
    const onWeekly = reducer(initial, actions.setChartAggregation('weekly'));
    const onMonthlyCumulative = reducer(onWeekly, actions.setCumulativeChartAggregation('monthly'));
    const withWeeklyData = reducer(onMonthlyCumulative, {
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

  it('routes response to cumulativeHistoryBreakdown when meta.arg matches cumulativeChartAggregation only (Overview filter path)', () => {
    // The Overview range filter writes cumulativeChartAggregation = 'monthly'
    // when the user picks Last 3/6 months. The shared loadHistoryBreakdown
    // fulfilled handler must populate the cumulative-owned field — and must
    // NOT clobber the Trends-owned historyBreakdown (which holds e.g. weekly
    // data the Trends chart is rendering).
    const initial = reducer(undefined, { type: '@@INIT' });
    const onWeeklyTrends = reducer(initial, actions.setChartAggregation('weekly'));
    const onMonthlyOverview = reducer(onWeeklyTrends, actions.setCumulativeChartAggregation('monthly'));
    // Pre-populate the Trends-owned field so we can assert it survives.
    const weeklyData = [{ month: '2026-W22', consumed: 200, breakdown: {} }];
    const withWeeklyTrendsData = {
      ...onMonthlyOverview,
      historyBreakdown: weeklyData,
    };

    const monthlyPayload = [{ month: '2026-04', consumed: 500, breakdown: {} }];
    const next = reducer(withWeeklyTrendsData, {
      type: actions.loadHistoryBreakdown.fulfilled.type,
      payload: monthlyPayload,
      meta: { arg: 'monthly' },
    });

    expect(next.cumulativeHistoryBreakdown).toEqual(monthlyPayload);
    expect(next.historyBreakdown).toEqual(weeklyData); // Trends-owned data preserved
    expect(next.chartAggregation).toBe('weekly');
    expect(next.cumulativeChartAggregation).toBe('monthly');
  });

  it('routes response to historyBreakdown when meta.arg matches chartAggregation only (Trends path)', () => {
    const initial = reducer(undefined, { type: '@@INIT' });
    const onDailyTrends = reducer(initial, actions.setChartAggregation('daily'));
    const onMonthlyOverview = reducer(onDailyTrends, actions.setCumulativeChartAggregation('monthly'));
    const monthlyData = [{ month: '2026-04', consumed: 500, breakdown: {} }];
    const withMonthlyOverviewData = {
      ...onMonthlyOverview,
      cumulativeHistoryBreakdown: monthlyData,
    };

    const dailyPayload = [{ month: '2026-06-01', consumed: 50, breakdown: {} }];
    const next = reducer(withMonthlyOverviewData, {
      type: actions.loadHistoryBreakdown.fulfilled.type,
      payload: dailyPayload,
      meta: { arg: 'daily' },
    });

    expect(next.historyBreakdown).toEqual(dailyPayload);
    expect(next.cumulativeHistoryBreakdown).toEqual(monthlyData); // Overview-owned data preserved
  });

  it('writes BOTH fields when meta.arg matches both pointers (initial mount, Trends and Overview agree)', () => {
    // Default state has chartAggregation = cumulativeChartAggregation = 'daily'.
    const initial = reducer(undefined, { type: '@@INIT' });
    const dailyPayload = [{ month: '2026-06-01', consumed: 50, breakdown: {} }];
    const next = reducer(initial, {
      type: actions.loadHistoryBreakdown.fulfilled.type,
      payload: dailyPayload,
      meta: { arg: 'daily' },
    });
    expect(next.historyBreakdown).toEqual(dailyPayload);
    expect(next.cumulativeHistoryBreakdown).toEqual(dailyPayload);
  });

  it('does not set loadErrorHistoryBreakdown when historyBreakdownRejected meta.arg matches neither pointer', () => {
    const initial = reducer(undefined, { type: '@@INIT' });
    const onWeekly = reducer(initial, actions.setChartAggregation('weekly'));
    const onMonthlyCumulative = reducer(onWeekly, actions.setCumulativeChartAggregation('monthly'));

    const staleRejected = {
      type: actions.loadHistoryBreakdown.rejected.type,
      payload: { message: 'boom' },
      meta: { arg: 'daily' },
    };
    const next = reducer(onMonthlyCumulative, staleRejected);

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

  it('stamps lastRefreshedAt on initial loadAllUsageData success (no separate refresh click needed)', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, summaryResponse);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-source/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-stage/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/top-apps/).reply(200, null);
    axiosMock.onGet(/\/api\/v2\/consumption\/daily-history/).reply(200, null);

    expect(store.getState().usage.lastRefreshedAt).toBeNull();
    await store.dispatch(actions.loadAllUsageData('daily'));

    const stamped = store.getState().usage.lastRefreshedAt;
    expect(typeof stamped).toBe('number');
    expect(stamped).toBeGreaterThan(0);
  });

  it('surfaces per-endpoint rejections to dedicated loadError fields (partial-failure visibility)', async () => {
    // Regression guard: when /daily-history 400s but the other endpoints succeed
    // (e.g. a custom range > 92 days hits the daily-history cap), the daily-history
    // tile would historically render blank with no error indicator. The
    // loadErrorDailyHistory field carries the message so the UI can surface it.
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, summaryResponse);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-source/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-stage/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/top-apps/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/daily-history/).reply(400, 'Date range exceeds maximum of 92 days');

    await store.dispatch(actions.loadAllUsageData('daily'));

    const state = store.getState().usage;
    // Summary success → no top-level error
    expect(state.loadErrorAll).toBeNull();
    // The 5 non-summary endpoints each route their rejection to a dedicated field
    expect(state.loadErrorDailyHistory).toBeTruthy();
    // Fulfilled endpoints keep their error fields null
    expect(state.loadErrorSourceBreakdown).toBeNull();
    expect(state.loadErrorStageBreakdown).toBeNull();
    expect(state.loadErrorTopApps).toBeNull();
    expect(state.loadErrorHistoryBreakdown).toBeNull();
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

describe('usageSlice new state fields and reducers', () => {
  it('initial state has activeTab="overview", cumulativeFilter="thisMonth", lastRefreshedAt=null', () => {
    const s = reducer(undefined, { type: '@@INIT' });
    expect(s.activeTab).toBe('overview');
    expect(s.cumulativeFilter).toBe('thisMonth');
    expect(s.lastRefreshedAt).toBeNull();
  });

  it('setActiveTab updates activeTab', () => {
    const s = reducer(
      { activeTab: 'overview', cumulativeFilter: 'thisMonth', lastRefreshedAt: null },
      actions.setActiveTab('trends')
    );
    expect(s.activeTab).toBe('trends');
  });

  it('setCumulativeFilter updates cumulativeFilter', () => {
    const s = reducer(
      { activeTab: 'overview', cumulativeFilter: 'thisMonth', lastRefreshedAt: null },
      actions.setCumulativeFilter('last3Months')
    );
    expect(s.cumulativeFilter).toBe('last3Months');
  });
});

describe('usageSlice.changeCumulativeFilter thunk', () => {
  let axiosMock;
  let store;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    store = configureStore({ reducer: { usage: reducer } });
  });

  afterEach(() => {
    axiosMock.reset();
  });

  // The aggregation goes in the URL query string (not axios config.params), so parse it from the URL.
  const aggregationFromUrl = (url) => {
    const m = /[?&]aggregation=([^&]+)/.exec(url || '');
    return m ? decodeURIComponent(m[1]) : null;
  };

  it('thisMonth: sets filter, sets cumulativeChartAggregation=daily (NOT chartAggregation), fires daily breakdown', async () => {
    const dailyData = [{ month: '2026-06-01', consumed: 50, breakdown: {} }];
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, dailyData);

    // Pre-set chartAggregation to a Trends-only value to confirm Overview's
    // filter does NOT clobber it.
    store.dispatch(actions.setChartAggregation('weekly'));

    await store.dispatch(actions.changeCumulativeFilter('thisMonth'));

    const state = store.getState().usage;
    expect(state.cumulativeFilter).toBe('thisMonth');
    expect(state.cumulativeChartAggregation).toBe('daily');
    expect(state.chartAggregation).toBe('weekly'); // Trends pointer preserved
    const lastReq = axiosMock.history.get[axiosMock.history.get.length - 1];
    expect(aggregationFromUrl(lastReq.url)).toBe('daily');
    expect(state.cumulativeHistoryBreakdown).toEqual(dailyData);
  });

  it('last3Months: sets filter, sets cumulativeChartAggregation=monthly, fires monthly breakdown, leaves chartAggregation alone', async () => {
    const monthlyData = [{ month: '2026-04-01', consumed: 100, breakdown: {} }];
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, monthlyData);

    store.dispatch(actions.setChartAggregation('weekly'));

    await store.dispatch(actions.changeCumulativeFilter('last3Months'));

    const state = store.getState().usage;
    expect(state.cumulativeFilter).toBe('last3Months');
    expect(state.cumulativeChartAggregation).toBe('monthly');
    expect(state.chartAggregation).toBe('weekly'); // Trends pointer preserved
    const lastReq = axiosMock.history.get[axiosMock.history.get.length - 1];
    expect(aggregationFromUrl(lastReq.url)).toBe('monthly');
    expect(state.cumulativeHistoryBreakdown).toEqual(monthlyData);
  });

  it('last6Months: sets filter and cumulativeChartAggregation=monthly, leaves chartAggregation alone', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, []);

    store.dispatch(actions.setChartAggregation('weekly'));

    await store.dispatch(actions.changeCumulativeFilter('last6Months'));

    const state = store.getState().usage;
    expect(state.cumulativeFilter).toBe('last6Months');
    expect(state.cumulativeChartAggregation).toBe('monthly');
    expect(state.chartAggregation).toBe('weekly');
    const lastReq = axiosMock.history.get[axiosMock.history.get.length - 1];
    expect(aggregationFromUrl(lastReq.url)).toBe('monthly');
  });

  it('does NOT forward the period range — loadHistoryBreakdown always fetches billing-window data', async () => {
    // Regression guard: loadHistoryBreakdown must never read periodRange.
    // Period filter is scoped to Categories tile only (loadSummaryForPeriod).
    // Carrying the range here would contaminate cumulativeHistoryBreakdown with
    // period-filtered data when the user switches the Overview cumulative filter
    // while a custom period is active.
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, []);
    store.dispatch(actions.setPeriodRange({ startDate: '2026-04-01', endDate: '2026-06-30' }));

    await store.dispatch(actions.changeCumulativeFilter('last3Months'));

    const lastReq = axiosMock.history.get[axiosMock.history.get.length - 1];
    expect(lastReq.url).not.toMatch(/startDate/);
    expect(lastReq.url).not.toMatch(/endDate/);
  });
});

describe('usageSlice.refresh thunk', () => {
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

  it('stamps lastRefreshedAt with a positive number on success', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, summaryResponse);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-source/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-stage/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/top-apps/).reply(200, null);
    axiosMock.onGet(/\/api\/v2\/consumption\/daily-history/).reply(200, null);

    const result = await store.dispatch(actions.refresh());

    expect(result.type).toBe(actions.refresh.fulfilled.type);
    const state = store.getState().usage;
    expect(typeof state.lastRefreshedAt).toBe('number');
    expect(state.lastRefreshedAt).toBeGreaterThan(0);
  });

  it('keeps lastRefreshedAt null and sets loadErrorAll when summary fails', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(500);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-source/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-stage/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/top-apps/).reply(200, null);
    axiosMock.onGet(/\/api\/v2\/consumption\/daily-history/).reply(200, null);

    await store.dispatch(actions.refresh());

    const state = store.getState().usage;
    expect(state.lastRefreshedAt).toBeNull();
    expect(state.loadErrorAll).toBeTruthy();
  });

  it('fans out a second loadHistoryBreakdown when Trends and Overview aggregations differ (regression: Overview was stale on refresh)', async () => {
    // Trends is on Weekly, Overview is on Last6Months (monthly). The bundled load
    // ships one aggregation; the fulfilled handler only writes to the matching
    // pointer's data field. Refresh must follow up with a dedicated call for the
    // other pointer or the Overview cumulative chart silently stays stale.
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, summaryResponse);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-source/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-stage/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/top-apps/).reply(200, null);
    axiosMock.onGet(/\/api\/v2\/consumption\/daily-history/).reply(200, null);

    store.dispatch(actions.setChartAggregation('weekly'));
    store.dispatch(actions.setCumulativeChartAggregation('monthly'));

    await store.dispatch(actions.refresh());

    // Aggregation lives in the URL query string. Both aggregations must have
    // been requested — one inside loadAllUsageData, one in the trailing fan-out call.
    const breakdownAggregations = axiosMock.history.get
      .filter((req) => req.url.includes('/api/v2/consumption/history/breakdown'))
      .map((req) => {
        const match = req.url.match(/aggregation=(\w+)/);
        return match ? match[1] : null;
      });
    expect(breakdownAggregations).toEqual(expect.arrayContaining(['weekly', 'monthly']));
  });

  it('does NOT fire a second loadHistoryBreakdown when Trends and Overview share an aggregation', async () => {
    // Both aggregations match (the default state on a fresh mount: both 'daily').
    // The bundled load already covers them — the trailing fan-out would be a
    // wasted round-trip.
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, summaryResponse);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-source/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-stage/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/top-apps/).reply(200, null);
    axiosMock.onGet(/\/api\/v2\/consumption\/daily-history/).reply(200, null);

    // Default state: chartAggregation === cumulativeChartAggregation === 'daily'.
    await store.dispatch(actions.refresh());

    const breakdownCalls = axiosMock.history.get.filter((req) =>
      req.url.includes('/api/v2/consumption/history/breakdown')
    );
    expect(breakdownCalls).toHaveLength(1);
  });

  it('rejects refresh when the fan-out loadHistoryBreakdown fails (does not silently succeed)', async () => {
    // Regression guard: previously the fan-out dispatch was awaited but its
    // rejection was silently dropped. The user would see "Last refreshed: a
    // few seconds ago" while the Overview cumulative chart stayed stale.
    // Now the second dispatch's status promotes to the refresh thunk's
    // rejection so loadErrorAll fires and the retry banner appears.
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, summaryResponse);
    axiosMock.onGet(/\/api\/v2\/consumption\/by-source/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/by-stage/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/top-apps/).reply(200, null);
    axiosMock.onGet(/\/api\/v2\/consumption\/daily-history/).reply(200, null);
    let breakdownCallIndex = 0;
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(() => {
      breakdownCallIndex += 1;
      // First call (from loadAllUsageData) succeeds; second (the fan-out) 5xx.
      return breakdownCallIndex === 1 ? [200, []] : [500, 'breakdown blew up'];
    });

    store.dispatch(actions.setChartAggregation('weekly'));
    store.dispatch(actions.setCumulativeChartAggregation('monthly'));

    const result = await store.dispatch(actions.refresh());

    expect(result.type).toBe(actions.refresh.rejected.type);
    const state = store.getState().usage;
    expect(state.loadErrorAll).toBeTruthy();
  });
});

describe('usageSlice.period state and reducers', () => {
  it('initial state has periodPreset=currentBillingPeriod and periodRange={null,null}', () => {
    const s = reducer(undefined, { type: '@@INIT' });
    expect(s.periodPreset).toBe('currentBillingPeriod');
    expect(s.periodRange).toEqual({ startDate: null, endDate: null });
  });

  it('setPeriodPreset("last30Days") updates both preset and derived range', () => {
    const next = reducer(undefined, actions.setPeriodPreset('last30Days'));
    expect(next.periodPreset).toBe('last30Days');
    expect(next.periodRange.startDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(next.periodRange.endDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('setPeriodPreset("currentBillingPeriod") resets range to {null,null}', () => {
    const seeded = reducer(undefined, actions.setPeriodPreset('last30Days'));
    const next = reducer(seeded, actions.setPeriodPreset('currentBillingPeriod'));
    expect(next.periodRange).toEqual({ startDate: null, endDate: null });
  });

  it('setPeriodPreset("custom") is a no-op (custom requires setPeriodRange)', () => {
    const seeded = reducer(undefined, actions.setPeriodPreset('last30Days'));
    const next = reducer(seeded, actions.setPeriodPreset('custom'));
    // State should be unchanged from seeded
    expect(next.periodPreset).toBe('last30Days');
  });

  it('setPeriodRange flips preset to "custom" and stores the dates', () => {
    const next = reducer(undefined, actions.setPeriodRange({ startDate: '2026-06-01', endDate: '2026-06-30' }));
    expect(next.periodPreset).toBe('custom');
    expect(next.periodRange).toEqual({ startDate: '2026-06-01', endDate: '2026-06-30' });
  });

  it('selectPeriodIsActive is true when periodPreset !== currentBillingPeriod', () => {
    expect(selectPeriodIsActive({ usage: { periodPreset: 'currentBillingPeriod' } })).toBe(false);
    expect(selectPeriodIsActive({ usage: { periodPreset: 'last30Days' } })).toBe(true);
    expect(selectPeriodIsActive({ usage: { periodPreset: 'custom' } })).toBe(true);
  });
});

describe('usageSlice.loadAllUsageData with period range', () => {
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

  it('never passes startDate/endDate to any endpoint, even when periodRange is active (regression: period filter scoped to Categories only)', async () => {
    // Regression guard: loadAllUsageData is used for initial mount and the ↻ refresh.
    // It must always fetch billing-window (unfiltered) data regardless of periodRange.
    // The period filter is scoped exclusively to the Categories tile via loadSummaryForPeriod.
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, summaryResponse);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-source/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-stage/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/top-apps/).reply(200, null);
    axiosMock.onGet(/\/api\/v2\/consumption\/daily-history/).reply(200, null);

    store.dispatch(actions.setPeriodRange({ startDate: '2026-06-01', endDate: '2026-06-30' }));
    await store.dispatch(actions.loadAllUsageData('daily'));

    const allUrls = axiosMock.history.get.map((req) => req.url);
    expect(allUrls).toHaveLength(6);
    allUrls.forEach((url) => {
      expect(url).not.toContain('startDate');
      expect(url).not.toContain('endDate');
    });
  });

  it('omits startDate/endDate params when periodPreset=currentBillingPeriod (default)', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, summaryResponse);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-source/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-stage/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/top-apps/).reply(200, null);
    axiosMock.onGet(/\/api\/v2\/consumption\/daily-history/).reply(200, null);

    // Default state: periodPreset = 'currentBillingPeriod', periodRange = { null, null }
    await store.dispatch(actions.loadAllUsageData('daily'));

    const allUrls = axiosMock.history.get.map((req) => req.url);
    allUrls.forEach((url) => {
      expect(url).not.toContain('startDate');
      expect(url).not.toContain('endDate');
    });
  });
});

describe('usageSlice.loadSummaryForPeriod thunk', () => {
  let axiosMock;
  let store;

  const summaryResponse = { consumed: 100, limit: 1000 };
  const filteredSummaryResponse = {
    consumed: 40,
    limit: 1000,
    activityBreakdown: { APIs: 10, 'App Scan + Re-evaluate': 30 },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    store = configureStore({ reducer: { usage: reducer } });
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('fires exactly one /summary request and writes to summaryForPeriod — not summary', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, filteredSummaryResponse);
    store.dispatch(actions.setPeriodRange({ startDate: '2026-06-01', endDate: '2026-06-30' }));

    const result = await store.dispatch(actions.loadSummaryForPeriod());

    expect(result.type).toBe(actions.loadSummaryForPeriod.fulfilled.type);
    const state = store.getState().usage;
    expect(state.summaryForPeriod).toEqual(filteredSummaryResponse);
    // summary (billing-window value) must remain null — loadSummaryForPeriod must
    // NOT write to the summary field used by My Usage tile.
    expect(state.summary).toBeNull();
    // Exactly 1 HTTP request — only /summary, none of the 5 other endpoints.
    expect(axiosMock.history.get).toHaveLength(1);
    expect(axiosMock.history.get[0].url).toMatch(/\/api\/v2\/consumption\/summary/);
  });

  it('passes startDate/endDate params to /summary when periodRange is active', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, filteredSummaryResponse);
    store.dispatch(actions.setPeriodRange({ startDate: '2026-06-01', endDate: '2026-06-30' }));

    await store.dispatch(actions.loadSummaryForPeriod());

    expect(axiosMock.history.get[0].url).toMatch(/startDate=2026-06-01/);
    expect(axiosMock.history.get[0].url).toMatch(/endDate=2026-06-30/);
  });

  it('omits startDate/endDate when periodPreset=currentBillingPeriod', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, summaryResponse);
    // Default state: periodPreset = 'currentBillingPeriod', periodRange = { null, null }

    await store.dispatch(actions.loadSummaryForPeriod());

    expect(axiosMock.history.get[0].url).not.toMatch(/startDate/);
    expect(axiosMock.history.get[0].url).not.toMatch(/endDate/);
  });

  it('sets loadingSummaryForPeriod=true on pending, false on fulfilled', () => {
    const pendingAction = { type: actions.loadSummaryForPeriod.pending.type };
    const pendingState = reducer(undefined, pendingAction);
    expect(pendingState.loadingSummaryForPeriod).toBe(true);
    expect(pendingState.loadErrorSummaryForPeriod).toBeNull();

    const fulfilledAction = { type: actions.loadSummaryForPeriod.fulfilled.type, payload: filteredSummaryResponse };
    const fulfilledState = reducer(pendingState, fulfilledAction);
    expect(fulfilledState.loadingSummaryForPeriod).toBe(false);
    expect(fulfilledState.summaryForPeriod).toEqual(filteredSummaryResponse);
  });

  it('sets loadErrorSummaryForPeriod on rejected and does NOT affect loadErrorAll', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(500);
    store.dispatch(actions.setPeriodRange({ startDate: '2026-06-01', endDate: '2026-06-30' }));

    await store.dispatch(actions.loadSummaryForPeriod());

    const state = store.getState().usage;
    expect(state.loadErrorSummaryForPeriod).toBeTruthy();
    // loadErrorAll must stay null — the period-filter error is scoped to the
    // Categories tile, not the entire page.
    expect(state.loadErrorAll).toBeNull();
  });

  it('does NOT fire any of the 5 other endpoints when dispatched', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, filteredSummaryResponse);
    store.dispatch(actions.setPeriodRange({ startDate: '2026-06-01', endDate: '2026-06-30' }));

    await store.dispatch(actions.loadSummaryForPeriod());

    const urls = axiosMock.history.get.map((req) => req.url);
    expect(urls.every((u) => u.includes('/api/v2/consumption/summary'))).toBe(true);
    expect(urls.some((u) => u.includes('/history/breakdown'))).toBe(false);
    expect(urls.some((u) => u.includes('/history/by-source'))).toBe(false);
    expect(urls.some((u) => u.includes('/history/by-stage'))).toBe(false);
    expect(urls.some((u) => u.includes('/top-apps'))).toBe(false);
    expect(urls.some((u) => u.includes('/daily-history'))).toBe(false);
  });
});

describe('usageSlice — period range isolation regression (non-period thunks must never carry range)', () => {
  // Regression guard for the "period filter scoped to Categories only" contract.
  // Every thunk other than loadSummaryForPeriod must issue requests with NO
  // startDate/endDate query params, even when periodRange is set in state.
  // Failure here means period-filter state leaks into billing-window tiles.
  let axiosMock;
  let store;

  const summaryResponse = { consumed: 100, limit: 1000 };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    store = configureStore({ reducer: { usage: reducer } });
    // Seed a custom period range in state for every test in this suite.
    store.dispatch(actions.setPeriodRange({ startDate: '2026-04-01', endDate: '2026-06-30' }));
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('loadSummary: request URL has no startDate/endDate even with a custom period active', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, summaryResponse);

    await store.dispatch(actions.loadSummary());

    expect(axiosMock.history.get).toHaveLength(1);
    expect(axiosMock.history.get[0].url).not.toMatch(/startDate/);
    expect(axiosMock.history.get[0].url).not.toMatch(/endDate/);
  });

  it('loadHistoryBreakdown: request URL has no startDate/endDate even with a custom period active', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, []);

    await store.dispatch(actions.loadHistoryBreakdown('daily'));

    expect(axiosMock.history.get).toHaveLength(1);
    expect(axiosMock.history.get[0].url).not.toMatch(/startDate/);
    expect(axiosMock.history.get[0].url).not.toMatch(/endDate/);
  });

  it('loadSourceBreakdown: request URL has no startDate/endDate even with a custom period active', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-source/).reply(200, []);

    await store.dispatch(actions.loadSourceBreakdown());

    expect(axiosMock.history.get).toHaveLength(1);
    expect(axiosMock.history.get[0].url).not.toMatch(/startDate/);
    expect(axiosMock.history.get[0].url).not.toMatch(/endDate/);
  });

  it('loadStageBreakdown: request URL has no startDate/endDate even with a custom period active', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-stage/).reply(200, []);

    await store.dispatch(actions.loadStageBreakdown());

    expect(axiosMock.history.get).toHaveLength(1);
    expect(axiosMock.history.get[0].url).not.toMatch(/startDate/);
    expect(axiosMock.history.get[0].url).not.toMatch(/endDate/);
  });

  it('loadTopApps: request URL has no startDate/endDate even with a custom period active', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/top-apps/).reply(200, []);

    await store.dispatch(actions.loadTopApps());

    expect(axiosMock.history.get).toHaveLength(1);
    expect(axiosMock.history.get[0].url).not.toMatch(/startDate/);
    expect(axiosMock.history.get[0].url).not.toMatch(/endDate/);
  });

  it('loadDailyHistory: request URL has no startDate/endDate even with a custom period active', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/daily-history/).reply(200, null);

    await store.dispatch(actions.loadDailyHistory());

    expect(axiosMock.history.get).toHaveLength(1);
    expect(axiosMock.history.get[0].url).not.toMatch(/startDate/);
    expect(axiosMock.history.get[0].url).not.toMatch(/endDate/);
  });

  it('loadSummaryForPeriod: DOES carry startDate/endDate (the one thunk that should)', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, summaryResponse);

    await store.dispatch(actions.loadSummaryForPeriod());

    expect(axiosMock.history.get).toHaveLength(1);
    expect(axiosMock.history.get[0].url).toMatch(/startDate=2026-04-01/);
    expect(axiosMock.history.get[0].url).toMatch(/endDate=2026-06-30/);
  });
});

describe('usageSlice.refresh with active period range', () => {
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

  it('with currentBillingPeriod preset: fires exactly 6 requests, none with range params', async () => {
    // Default state: no custom period. Refresh must fire 6 billing-window requests.
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, summaryResponse);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-source/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-stage/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/top-apps/).reply(200, null);
    axiosMock.onGet(/\/api\/v2\/consumption\/daily-history/).reply(200, null);

    const result = await store.dispatch(actions.refresh());

    expect(result.type).toBe(actions.refresh.fulfilled.type);
    // Exactly 6 — the 6 bundled endpoints, no extra period-summary call.
    expect(axiosMock.history.get).toHaveLength(6);
    axiosMock.history.get.forEach((req) => {
      expect(req.url).not.toMatch(/startDate/);
      expect(req.url).not.toMatch(/endDate/);
    });
  });

  it('with custom range active: fires 6 unfiltered requests + 1 period /summary (7 total)', async () => {
    // Regression guard: when a custom period is active, refresh must keep the
    // Categories tile aligned by firing loadSummaryForPeriod in addition to the
    // 6 unfiltered loadAllUsageData requests.
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, summaryResponse);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-source/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-stage/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/top-apps/).reply(200, null);
    axiosMock.onGet(/\/api\/v2\/consumption\/daily-history/).reply(200, null);

    store.dispatch(actions.setPeriodRange({ startDate: '2026-04-01', endDate: '2026-06-30' }));
    const result = await store.dispatch(actions.refresh());

    expect(result.type).toBe(actions.refresh.fulfilled.type);
    // 7 requests total: 6 unfiltered + 1 period /summary
    expect(axiosMock.history.get).toHaveLength(7);

    const summaryRequests = axiosMock.history.get.filter((req) => req.url.includes('/api/v2/consumption/summary'));
    // Two /summary calls: one unfiltered (from loadAllUsageData), one with range (from loadSummaryForPeriod)
    expect(summaryRequests).toHaveLength(2);

    const unfiltered = summaryRequests.filter((req) => !req.url.includes('startDate'));
    const filtered = summaryRequests.filter((req) => req.url.includes('startDate'));
    expect(unfiltered).toHaveLength(1);
    expect(filtered).toHaveLength(1);
    expect(filtered[0].url).toMatch(/startDate=2026-04-01/);
    expect(filtered[0].url).toMatch(/endDate=2026-06-30/);

    // The 5 non-summary endpoints must have NO range params
    const nonSummaryRequests = axiosMock.history.get.filter((req) => !req.url.includes('/api/v2/consumption/summary'));
    expect(nonSummaryRequests).toHaveLength(5);
    nonSummaryRequests.forEach((req) => {
      expect(req.url).not.toMatch(/startDate/);
      expect(req.url).not.toMatch(/endDate/);
    });
  });
});

describe('usageSlice.allFulfilled seeds summaryForPeriod', () => {
  it('seeds summaryForPeriod from summary payload on loadAllUsageData success', () => {
    const summaryPayload = { consumed: 100, limit: 1000, activityBreakdown: { APIs: 10 } };
    const action = {
      type: actions.loadAllUsageData.fulfilled.type,
      payload: {
        aggregation: 'daily',
        summary: summaryPayload,
        historyBreakdown: [],
        sourceBreakdown: [],
        stageBreakdown: [],
        topApps: null,
        dailyHistory: null,
        loadedAt: Date.now(),
      },
    };
    const state = reducer(undefined, action);
    // summaryForPeriod must match summary so the Categories tile renders
    // correctly before the user ever touches the period filter.
    expect(state.summaryForPeriod).toEqual(summaryPayload);
    expect(state.summary).toEqual(summaryPayload);
  });
});
