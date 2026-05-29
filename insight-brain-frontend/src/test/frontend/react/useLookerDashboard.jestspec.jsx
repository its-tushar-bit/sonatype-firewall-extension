/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { axiosMockAdapter, renderHook, act, configureStore } from 'TestRoot/SpecUtil';
import { Provider } from 'react-redux';
import reducers from 'MainRoot/reduxConfig/reducers';
import { LookerEmbedSDK } from '@looker/embed-sdk';

import useLookerDashboard from 'MainRoot/react/useLookerDashboard';
import { getEnterpriseReportingGenerateEmbedTokensUrl } from 'MainRoot/util/CLMLocation';
import { actions } from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSlice';
import { actions as filterActions, FILTER_STATES } from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSlice';

// mockConnectDeferreds is module-level (mock-prefixed) so jest.mock() factory can reference it
const mockConnectDeferreds = [];

jest.mock('@looker/embed-sdk', () => {
  const mockBuildChain = {
    appendTo: jest.fn().mockReturnThis(),
    withFilters: jest.fn().mockReturnThis(),
    withParams: jest.fn().mockReturnThis(),
    withDynamicIFrameHeight: jest.fn().mockReturnThis(),
    on: jest.fn().mockReturnThis(),
    build: jest.fn().mockReturnThis(),
    connect: jest.fn().mockImplementation(() => {
      let resolve, reject;
      const promise = new Promise((res, rej) => {
        resolve = res;
        reject = rej;
      });
      mockConnectDeferreds.push({ promise, resolve, reject });
      return promise;
    }),
  };

  return {
    LookerEmbedSDK: {
      initCookieless: jest.fn(),
      createDashboardWithId: jest.fn().mockReturnValue(mockBuildChain),
    },
  };
});

const firewallDashboard = {
  dashboardId: 'firewall-dash',
  dashboardPath: '42',
  title: 'Firewall Dashboard',
  category: 'firewall',
};

const firewallDashboard2 = {
  dashboardId: 'firewall-dash-2',
  dashboardPath: '43',
  title: 'Firewall Dashboard 2',
  category: 'firewall',
};

const simpleDataDashboard = {
  dashboardId: 'data-dash',
  dashboardPath: '99',
  title: 'Data Dashboard',
  category: 'data',
};

const simpleDataDashboard2 = {
  dashboardId: 'data-dash-2',
  dashboardPath: '100',
  title: 'Data Dashboard 2',
  category: 'data',
};

const makePreloadedState = (selectedDashboard) => ({
  enterpriseReportingDashboard: {
    loading: false,
    loadError: null,
    baseUrl: 'looker.example.com',
    selectedDashboard,
    selectedDashboardName: selectedDashboard?.title || null,
    dashboardsData: null,
    dashboardTabs: [],
    activeDashboardTab: 0,
    iqVersion: null,
  },
  enterpriseReportingFilter: {
    isOpen: false,
    loadingIframe: false,
    appliedFilterName: null,
    appliedFilter: null,
    previewFilterName: null,
    previewFilter: null,
    defaultFilter: null,
    filterState: 'clean',
    filtersInitialized: true,
    loadingAllFilters: false,
    savedFilters: [],
  },
});

function renderUseLookerDashboard(preloadedState, iframeContainerId = '#dashboard') {
  const store = configureStore({ reducer: reducers, preloadedState });
  const wrapper = ({ children }) => <Provider store={store}>{children}</Provider>;
  return { ...renderHook(() => useLookerDashboard(iframeContainerId), { wrapper }), store };
}

// Advance fake timers past the 300ms debounce and flush all pending microtasks
async function flushDebounce() {
  await act(async () => {
    jest.advanceTimersByTime(350);
  });
}

// Simulate the Looker SDK appending an iframe (what connect() does in production)
function appendSdkIframe(container) {
  const iframe = document.createElement('iframe');
  container.appendChild(iframe);
  return iframe;
}

describe('useLookerDashboard', () => {
  let container, axiosMock;
  let getLookerMocks;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    jest.useFakeTimers();
    mockConnectDeferreds.length = 0;

    const tokenResponse = { api_token: 't', navigation_token: 'n', session_reference_token: 'r' };
    axiosMock.onGet(/acquire-embed-session/).reply(200, tokenResponse);
    axiosMock.onPut(getEnterpriseReportingGenerateEmbedTokensUrl()).reply(200, tokenResponse);

    container = document.createElement('div');
    container.id = 'dashboard';
    document.body.appendChild(container);

    LookerEmbedSDK.initCookieless.mockClear();
    // Use mockReset + re-setup so the build chain is fresh and results are reliable
    const mockBuildChain = {
      appendTo: jest.fn().mockReturnThis(),
      withFilters: jest.fn().mockReturnThis(),
      withParams: jest.fn().mockReturnThis(),
      withDynamicIFrameHeight: jest.fn().mockReturnThis(),
      on: jest.fn().mockReturnThis(),
      build: jest.fn().mockReturnThis(),
      connect: jest.fn().mockImplementation(() => {
        let resolve, reject;
        const promise = new Promise((res, rej) => {
          resolve = res;
          reject = rej;
        });
        mockConnectDeferreds.push({ promise, resolve, reject });
        return promise;
      }),
    };
    LookerEmbedSDK.createDashboardWithId.mockReset();
    LookerEmbedSDK.createDashboardWithId.mockReturnValue(mockBuildChain);

    getLookerMocks = () => {
      const buildChain = LookerEmbedSDK.createDashboardWithId.mock.results[0]?.value;
      return { sdk: LookerEmbedSDK, buildChain };
    };
  });

  afterEach(() => {
    jest.useRealTimers();
    document.body.removeChild(container);
  });

  describe('stale iframe race condition', () => {
    it('removes the stale iframe when a superseded connect() resolves after the active embed (stale-after-active)', async () => {
      // Scenario: gen=1 connect() starts. User switches dashboards (gen=2 starts, container cleared).
      // gen=2 connect() resolves first → appends its iframe. Then gen=1 stale connect() resolves
      // → appends as lastChild. Container has [gen2Active, gen1Stale]. removeStaleIframe() removes lastChild.

      const { store } = renderUseLookerDashboard(makePreloadedState(firewallDashboard));
      await flushDebounce();

      // gen=1 connect() is in-flight
      expect(mockConnectDeferreds.length).toBe(1);
      const gen1Connect = mockConnectDeferreds[0];

      // Trigger gen=2: user switches to a different dashboard — the hook calls clearIframeContainer()
      // which increments embedGeneration and clears the DOM, then starts a new connect().
      await act(async () => {
        store.dispatch(actions.setSelectedDashboard(firewallDashboard2));
      });
      await flushDebounce();

      // gen=2 connect() has been called
      expect(mockConnectDeferreds.length).toBe(2);
      const gen2Connect = mockConnectDeferreds[1];

      // gen=2 (active) resolves first — SDK appends its iframe
      const gen2ActiveIframe = appendSdkIframe(container);
      await act(async () => {
        gen2Connect.resolve({});
      });

      expect(container.children.length).toBe(1);

      // gen=1 (stale) resolves after — SDK appends its iframe as lastChild
      const gen1StaleIframe = appendSdkIframe(container);
      await act(async () => {
        gen1Connect.resolve({});
      });

      // removeStaleIframe() should have removed lastChild (gen1Stale)
      expect(container.children.length).toBe(1);
      expect(container.lastChild).toBe(gen2ActiveIframe);
      expect(container.lastChild).not.toBe(gen1StaleIframe);
    });

    it('removes the leading stale iframe when a stale connect() resolved before the active embed (stale-before-active)', async () => {
      // Scenario: gen=1 stale connect() resolves first (container has 1 child → > 1 guard skips).
      // Then gen=2 active connect() resolves → SDK appends iframe. Container now has [gen1Stale, gen2Active].
      // removeLeadingStaleIframe() removes firstChild (gen1Stale).

      const { store } = renderUseLookerDashboard(makePreloadedState(firewallDashboard));
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(1);
      const gen1Connect = mockConnectDeferreds[0];

      // Trigger gen=2
      await act(async () => {
        store.dispatch(actions.setSelectedDashboard(firewallDashboard2));
      });
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(2);
      const gen2Connect = mockConnectDeferreds[1];

      // gen=1 (stale) resolves first — SDK appends its iframe. Container has 1 child.
      const gen1StaleIframe = appendSdkIframe(container);
      await act(async () => {
        gen1Connect.resolve({});
      });

      // Only 1 child → > 1 guard skipped → gen1Stale still present
      expect(container.children.length).toBe(1);

      // gen=2 (active) resolves — SDK appends its iframe
      const gen2ActiveIframe = appendSdkIframe(container);
      await act(async () => {
        gen2Connect.resolve({});
      });

      // removeLeadingStaleIframe() should have removed firstChild (gen1Stale)
      expect(container.children.length).toBe(1);
      expect(container.firstChild).toBe(gen2ActiveIframe);
      expect(container.firstChild).not.toBe(gen1StaleIframe);
    });

    it('does NOT remove the active iframe when only 1 child is present (triple-rapid-switch guard)', async () => {
      // Scenario: 3 rapid switches. gen=1 stale connect() resolves when gen=3's active iframe
      // is the only child. > 1 guard must prevent removing gen=3's iframe.
      // pendingEmbeds is reset on each clearIframeContainer(), so gen=1/gen=2 cannot hold the spinner.

      const { result, store } = renderUseLookerDashboard(makePreloadedState(firewallDashboard));
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(1);
      const gen1Connect = mockConnectDeferreds[0];

      // Switch to dashboard 2 (gen=2) — pendingEmbeds reset to 0 then incremented to 1
      await act(async () => {
        store.dispatch(actions.setSelectedDashboard(firewallDashboard2));
      });
      await flushDebounce();

      // Switch back to dashboard 1 (gen=3) — pendingEmbeds reset to 0 then incremented to 1
      await act(async () => {
        store.dispatch(actions.setSelectedDashboard(firewallDashboard));
      });
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(3);

      // gen=3 (active) resolves and appends its iframe — only 1 child present
      const gen3ActiveIframe = appendSdkIframe(container);
      const gen3Connect = mockConnectDeferreds[2];
      await act(async () => {
        gen3Connect.resolve({});
      });

      expect(container.children.length).toBe(1);
      // gen=3 resolved — pendingEmbeds drops to 0, spinner clears
      expect(result.current.loadingDashboard).toBe(false);

      // gen=1 (stale, from a cleared generation) finally fires — floor guard keeps counter at 0
      await act(async () => {
        gen1Connect.resolve({});
      });

      // > 1 guard prevented iframe removal — gen=3's active iframe survives
      expect(container.children.length).toBe(1);
      expect(container.firstChild).toBe(gen3ActiveIframe);
      // Spinner remains clear — stale gen=1 finally cannot re-toggle it
      expect(result.current.loadingDashboard).toBe(false);
    });

    it('clears the spinner even when a stale connect() hangs forever', async () => {
      // Regression guard: before the pendingEmbeds reset-on-switch fix, a hung gen=1 connect()
      // would hold pendingEmbeds at 1 permanently, leaving the spinner stuck after gen=2 resolved.

      const { result, store } = renderUseLookerDashboard(makePreloadedState(firewallDashboard));
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(1);
      // gen=1 connect() starts but never resolves (simulates a hung SDK call)

      // User switches dashboards — clearIframeContainer resets pendingEmbeds to 0
      await act(async () => {
        store.dispatch(actions.setSelectedDashboard(firewallDashboard2));
      });
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(2);
      const gen2Connect = mockConnectDeferreds[1];

      // gen=2 (active) resolves — pendingEmbeds was reset to 0 then incremented to 1; now drops to 0
      await act(async () => {
        gen2Connect.resolve({});
      });

      // Spinner must clear even though gen=1 is still hanging
      expect(result.current.loadingDashboard).toBe(false);
    });
  });

  describe('non-filter dashboard (embedDashboard path)', () => {
    it('does not register Looker event handlers for non-enterprise/firewall dashboards', async () => {
      renderUseLookerDashboard(makePreloadedState(simpleDataDashboard));
      await flushDebounce();

      const { sdk } = getLookerMocks();
      // Verify the SDK was called with the correct dashboard path
      expect(sdk.createDashboardWithId).toHaveBeenCalledWith(simpleDataDashboard.dashboardPath);

      // embedDashboard path does not register .on() event handlers; results[0] is the build chain
      expect(sdk.createDashboardWithId.mock.results.length).toBeGreaterThan(0);
      const buildChain = sdk.createDashboardWithId.mock.results[0].value;
      expect(buildChain.on).not.toHaveBeenCalled();
    });

    it('removes the stale iframe when a superseded connect() resolves after the active embed (stale-after-active)', async () => {
      // Same race as the filter path but exercises embedDashboard (non-enterprise/firewall category).
      const { store } = renderUseLookerDashboard(makePreloadedState(simpleDataDashboard));
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(1);
      const gen1Connect = mockConnectDeferreds[0];

      await act(async () => {
        store.dispatch(actions.setSelectedDashboard(simpleDataDashboard2));
      });
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(2);
      const gen2Connect = mockConnectDeferreds[1];

      // gen=2 (active) resolves first — SDK appends its iframe
      const gen2ActiveIframe = appendSdkIframe(container);
      await act(async () => {
        gen2Connect.resolve({});
      });

      expect(container.children.length).toBe(1);

      // gen=1 (stale) resolves after — SDK appends its iframe as lastChild
      const gen1StaleIframe = appendSdkIframe(container);
      await act(async () => {
        gen1Connect.resolve({});
      });

      // removeStaleIframe() should have removed lastElementChild (gen1Stale)
      expect(container.children.length).toBe(1);
      expect(container.lastChild).toBe(gen2ActiveIframe);
      expect(container.lastChild).not.toBe(gen1StaleIframe);
    });

    it('keeps loadingDashboard true while gen=2 is in-flight and clears only after gen=2 resolves', async () => {
      // Regression: same scenario for dataInsight/AI_group dashboards (category !== enterprise/firewall).
      // pendingEmbeds ensures the stale gen=1 finally does not prematurely hide the spinner.
      const { result, store } = renderUseLookerDashboard(makePreloadedState(simpleDataDashboard));
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(1);
      const gen1Connect = mockConnectDeferreds[0];

      // User switches to another data dashboard (advances generation; pendingEmbeds reset to 0, then incremented to 1)
      await act(async () => {
        store.dispatch(actions.setSelectedDashboard(simpleDataDashboard2));
      });
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(2);
      const gen2Connect = mockConnectDeferreds[1];

      // gen=1 stale resolves — pendingEmbeds drops to 0, but generation mismatch keeps spinner true
      await act(async () => {
        gen1Connect.resolve({});
      });

      expect(result.current.loadingDashboard).toBe(true);

      // gen=2 (active) resolves — pendingEmbeds drops to 0 and generation matches, spinner clears
      await act(async () => {
        gen2Connect.resolve({});
      });

      expect(result.current.loadingDashboard).toBe(false);
    });
  });

  describe('filter-supporting dashboard (embedDashboardWithFilters path)', () => {
    it('registers all four Looker filter event handlers for enterprise/firewall dashboards', async () => {
      renderUseLookerDashboard(makePreloadedState(firewallDashboard));
      await flushDebounce();

      const { sdk } = getLookerMocks();
      expect(sdk.createDashboardWithId).toHaveBeenCalledWith(firewallDashboard.dashboardPath);

      const buildChain = sdk.createDashboardWithId.mock.results[0].value;
      expect(buildChain.on).toHaveBeenCalledWith('dashboard:loaded', expect.any(Function));
      expect(buildChain.on).toHaveBeenCalledWith('dashboard:filters:changed', expect.any(Function));
      expect(buildChain.on).toHaveBeenCalledWith('dashboard:run:start', expect.any(Function));
      expect(buildChain.on).toHaveBeenCalledWith('dashboard:run:complete', expect.any(Function));
    });

    it('keeps loadingDashboard true while gen=2 is in-flight and clears only after gen=2 resolves', async () => {
      // Regression: gen=1 connect() is in-flight; user switches tabs → gen=2 starts.
      // clearIframeContainer resets pendingEmbeds to 0 then increments to 1 for gen=2.
      // gen=1 stale finally fires: pendingEmbeds drops to 0, but generation mismatch blocks spinner clear.
      // Only gen=2 resolving satisfies both pendingEmbeds<=0 AND generation match.
      const { result, store } = renderUseLookerDashboard(makePreloadedState(firewallDashboard));
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(1);
      const gen1Connect = mockConnectDeferreds[0];

      // User switches to a different dashboard (advances generation; pendingEmbeds reset to 0, then incremented to 1)
      await act(async () => {
        store.dispatch(actions.setSelectedDashboard(firewallDashboard2));
      });
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(2);
      const gen2Connect = mockConnectDeferreds[1];

      // gen=1 stale resolves — pendingEmbeds drops to 0, but generation mismatch keeps spinner true
      await act(async () => {
        gen1Connect.resolve({});
      });

      expect(result.current.loadingDashboard).toBe(true);

      // gen=2 (active) resolves — pendingEmbeds drops to 0 and generation matches, spinner clears
      await act(async () => {
        gen2Connect.resolve({});
      });

      expect(result.current.loadingDashboard).toBe(false);
    });

    it('does not re-embed when loadingAllFilters cycles true→false for an already-embedded dashboard', async () => {
      // Production regression: switching enterprise→enterprise grouped dashboard tabs causes
      // EnterpriseReportingFilter to unmount (reset → loadingAllFilters=true) then remount
      // (initializeFilters → loadingAllFilters=false). The loadingAllFilters transition fires
      // the useEffect a second time for the same dashboardId, producing two iframes and a
      // permanently stuck spinner. lastEmbeddedDashboardId guard prevents the second embed.

      const { result, store } = renderUseLookerDashboard(makePreloadedState(firewallDashboard));
      await flushDebounce();

      // First embed completes — dashboard is active
      expect(mockConnectDeferreds.length).toBe(1);
      await act(async () => {
        mockConnectDeferreds[0].resolve({});
      });
      expect(result.current.loadingDashboard).toBe(false);

      // Simulate filter slice reset (unmount of EnterpriseReportingFilter) → loadingAllFilters=true
      await act(async () => {
        store.dispatch(filterActions.reset());
      });
      await flushDebounce();

      // Simulate initializeFilters completing → loadingAllFilters=false, same dashboardId still selected
      await act(async () => {
        store.dispatch(filterActions.initializeFilters.fulfilled(null));
      });
      await flushDebounce();

      // Must NOT have triggered a second embed — only 1 connect() call total
      expect(mockConnectDeferreds.length).toBe(1);
      // Spinner must remain cleared
      expect(result.current.loadingDashboard).toBe(false);
    });

    it('preserves filter communication (dashboardCommunicationRef) when loadingAllFilters cycles for an already-embedded dashboard', async () => {
      // Regression guard: before the ref-null fix, currentDashboardId.current and
      // dashboardCommunicationRef.current were nulled unconditionally before the alreadyEmbedded
      // guard. When alreadyEmbedded was true, refs were null for the life of the active embed,
      // silently dropping any subsequent filter-apply send() calls.

      const mockSend = jest.fn();
      const dashboardHandle = { send: mockSend };

      const { store } = renderUseLookerDashboard(makePreloadedState(firewallDashboard));
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(1);
      // Resolve with a handle that has send() so dashboardCommunicationRef.current is populated
      await act(async () => {
        mockConnectDeferreds[0].resolve(dashboardHandle);
      });

      // Simulate loadingAllFilters cycling true→false (EnterpriseReportingFilter unmount/remount)
      await act(async () => {
        store.dispatch(filterActions.reset());
      });
      await flushDebounce();
      await act(async () => {
        store.dispatch(filterActions.initializeFilters.fulfilled(null));
      });
      await flushDebounce();

      // Guard fired — still only 1 embed
      expect(mockConnectDeferreds.length).toBe(1);

      // Now simulate a filter apply — refs must still be populated for send() to fire
      await act(async () => {
        store.dispatch(filterActions.setAppliedFilter({ Application: 'test-app' }));
        store.dispatch(filterActions.setFilterState(FILTER_STATES.APPLYING));
      });

      expect(mockSend).toHaveBeenCalledWith('dashboard:filters:update', {
        filters: { Application: 'test-app' },
      });
      expect(mockSend).toHaveBeenCalledWith('dashboard:run');
    });

    it('triggers a new embed when switching to a different dashboard after alreadyEmbedded guard fired (A→cycle→B)', async () => {
      // Regression guard: after alreadyEmbedded suppresses a re-embed for dashboard A,
      // switching to a different dashboard B must still trigger a new embed.
      // lastEmbeddedDashboardId must be updated to B's ID when runLookerQuery fires for B.

      const { result, store } = renderUseLookerDashboard(makePreloadedState(firewallDashboard));
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(1);
      await act(async () => {
        mockConnectDeferreds[0].resolve({});
      });
      expect(result.current.loadingDashboard).toBe(false);

      // Cycle loadingAllFilters for dashboard A — alreadyEmbedded guard fires, no second embed
      await act(async () => {
        store.dispatch(filterActions.reset());
      });
      await flushDebounce();
      await act(async () => {
        store.dispatch(filterActions.initializeFilters.fulfilled(null));
      });
      await flushDebounce();
      expect(mockConnectDeferreds.length).toBe(1);

      // Now switch to a different dashboard B — must trigger a new embed
      await act(async () => {
        store.dispatch(actions.setSelectedDashboard(firewallDashboard2));
      });
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(2);

      // Resolve B's embed and verify spinner clears
      await act(async () => {
        mockConnectDeferreds[1].resolve({});
      });
      expect(result.current.loadingDashboard).toBe(false);
    });

    it('sets iframeError and clears spinner when connect() rejects', async () => {
      // Regression guard: finally always runs on rejection, so pendingEmbeds must decrement
      // and setLoadingDashboard(false) must fire when the generation matches.

      const { result } = renderUseLookerDashboard(makePreloadedState(firewallDashboard));
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(1);
      await act(async () => {
        mockConnectDeferreds[0].reject(new Error('connect failed'));
      });

      expect(result.current.iframeError).toBe(true);
      expect(result.current.loadingDashboard).toBe(false);
    });

    it('does not set iframeError when a stale connect() rejects after the active embed succeeds', async () => {
      // Regression guard: setIframeError is now generation-gated; a stale rejected connect()
      // must not overwrite the healthy error state of the active embed.

      const { result, store } = renderUseLookerDashboard(makePreloadedState(firewallDashboard));
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(1);
      const gen1Connect = mockConnectDeferreds[0];

      // Switch dashboards — gen=2 starts
      await act(async () => {
        store.dispatch(actions.setSelectedDashboard(firewallDashboard2));
      });
      await flushDebounce();

      expect(mockConnectDeferreds.length).toBe(2);
      const gen2Connect = mockConnectDeferreds[1];

      // gen=2 (active) resolves successfully
      await act(async () => {
        gen2Connect.resolve({});
      });
      expect(result.current.iframeError).toBe(false);
      expect(result.current.loadingDashboard).toBe(false);

      // gen=1 (stale) rejects — must NOT set iframeError
      await act(async () => {
        gen1Connect.reject(new Error('stale failure'));
      });
      expect(result.current.iframeError).toBe(false);
    });
  });
});
