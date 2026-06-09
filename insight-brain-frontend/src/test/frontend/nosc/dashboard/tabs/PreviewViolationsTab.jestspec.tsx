/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { screen, waitFor } from '@testing-library/react';
import { axiosMockAdapter, render, setupPortalContainer } from 'TestRoot/SpecUtil';
import PreviewViolationsTab from 'MainRoot/nosc/dashboard/tabs/PreviewViolationsTab';
import { parseViolationsTabQuery } from 'MainRoot/nosc/dashboard/tabs/previewDashboardTabQuery';
import PreviewWaiversTab from 'MainRoot/nosc/dashboard/tabs/PreviewWaiversTab';

/**
 * S2-PR-D-3 (CLM-39992) tests for the Violations Preview tab.
 *
 * The wrapped Classic `DashboardViolationsContainer` defers its first
 * fetch until `dashboardFilter.loading` is false, which is `true` in
 * the initial Redux state. That keeps the table in its loading-row
 * shape during these tests and lets us focus on the wrapper itself
 * (filter-rail mount, URL-query handling, tab-isolation) without
 * needing to stand up a full filter-load fixture.
 */

function renderWithTheme(ui: JSX.Element) {
  return render(<Theme>{ui}</Theme>);
}

describe('PreviewViolationsTab (CLM-39992 / S2-PR-D-3)', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    setupPortalContainer();
    // Catch-all: pend any axios call the wrapped Classic table fires
    // (it won't fire in this state, but a future Classic change might).
    axiosMock.onAny().reply(() => new Promise(() => {}));
  });

  afterEach(() => {
    axiosMock.reset();
    document.body.innerHTML = '';
    window.history.replaceState(null, '', '#');
  });

  describe('rendering', () => {
    it('renders the tab shell, filter-rail slot, and the wrapped table slot without crashing', () => {
      renderWithTheme(<PreviewViolationsTab />);
      expect(screen.getByTestId('nosc-dashboard-violations-tab')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-dashboard-violations-filter-slot')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-dashboard-violations-table-slot')).toBeInTheDocument();
    });
  });

  describe('parseViolationsTabQuery (helper)', () => {
    it.each([
      ['#/dashboard/violations', { severity: null, ltg: null, policy: null }],
      [
        '#/dashboard/violations?severity=critical',
        { severity: 'critical', ltg: null, policy: null },
      ],
      [
        '#/dashboard/violations?ltg=copyleft&policy=p1',
        { severity: null, ltg: 'copyleft', policy: 'p1' },
      ],
      ['#/dashboard/violations?garbage=1', { severity: null, ltg: null, policy: null }],
    ])('parses %s', (input, expected) => {
      expect(parseViolationsTabQuery(input)).toEqual(expected);
    });
  });

  describe('URL query handling', () => {
    it('on first mount, ?severity=critical dispatches a policyThreatLevels filter and preserves the query', async () => {
      window.history.replaceState(
        null,
        '',
        '#/dashboard/violations?severity=critical',
      );
      const { store } = renderWithTheme(<PreviewViolationsTab />);

      // The wrapper dispatches a TOGGLE_FILTER for policyThreatLevels
      // = [8, 10] (Critical band). The reducer immediately reflects
      // that into `dashboardFilter.selected.policyThreatLevels`.
      await waitFor(() => {
        const range = store.getState().dashboardFilter.selected.policyThreatLevels;
        expect(range).toEqual([8, 10]);
      });

      expect(window.location.hash).toBe('#/dashboard/violations?severity=critical');
    });

    it('does NOT dispatch when the hash has no recognized filter query', () => {
      window.history.replaceState(null, '', '#/dashboard/violations');
      const { store } = renderWithTheme(<PreviewViolationsTab />);
      // Selected stays at the default range [2, 10].
      expect(store.getState().dashboardFilter.selected.policyThreatLevels).toEqual([2, 10]);
      expect(window.location.hash).toBe('#/dashboard/violations');
    });

    it('handles unknown severity values without dispatching and preserves the query', () => {
      window.history.replaceState(
        null,
        '',
        '#/dashboard/violations?severity=banana',
      );
      const { store } = renderWithTheme(<PreviewViolationsTab />);
      expect(store.getState().dashboardFilter.selected.policyThreatLevels).toEqual([2, 10]);
      expect(window.location.hash).toBe('#/dashboard/violations?severity=banana');
    });

    it('preserves unrecognized query keys without dispatching', () => {
      window.history.replaceState(null, '', '#/dashboard/violations?garbage=1');
      const { store } = renderWithTheme(<PreviewViolationsTab />);
      expect(store.getState().dashboardFilter.selected.policyThreatLevels).toEqual([2, 10]);
      expect(window.location.hash).toBe('#/dashboard/violations?garbage=1');
    });
  });

  describe('tab-isolation', () => {
    /**
     * Per AT-D1-002 + D-3 spec: a thrown render error inside the
     * wrapped Violations table MUST NOT unmount a sibling Waivers
     * tab. The parent `PreviewDashboardPage` wraps each tab content
     * area in a `TabErrorBoundary`, but D-3 mounts both tab bodies
     * inside `Tabs.Content` panels (only the active one is rendered
     * by Radix). To exercise the contract without booting the
     * full tabbed page, this test simulates the same isolation
     * contract directly: we render a forced-throw violations tab
     * inside its own boundary and a healthy waivers tab next to
     * it, and assert the waivers DOM survives the violations crash.
     */
    function ThrowingTab(): JSX.Element {
      throw new Error('forced throw — violations tab simulated crash');
    }

    class IsolationBoundary extends React.Component<
      { children: React.ReactNode },
      { hasError: boolean }
    > {
      constructor(props: { children: React.ReactNode }) {
        super(props);
        this.state = { hasError: false };
      }
      static getDerivedStateFromError(): { hasError: boolean } {
        return { hasError: true };
      }
      render(): React.ReactNode {
        if (this.state.hasError) {
          return <div data-testid="violations-isolation-fallback">tab failed</div>;
        }
        return this.props.children;
      }
    }

    let originalConsoleError: typeof console.error;
    beforeEach(() => {
      originalConsoleError = console.error;
      console.error = jest.fn();
    });
    afterEach(() => {
      console.error = originalConsoleError;
    });

    it('a render-throw inside the violations slot is caught by its boundary and the waivers tab DOM is unaffected', () => {
      renderWithTheme(
        <>
          <IsolationBoundary>
            <ThrowingTab />
          </IsolationBoundary>
          <PreviewWaiversTab />
        </>,
      );
      expect(screen.getByTestId('violations-isolation-fallback')).toBeInTheDocument();
      // The waivers tab's wrapper + slots are still in the DOM.
      expect(screen.getByTestId('nosc-dashboard-waivers-tab')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-dashboard-waivers-table-slot')).toBeInTheDocument();
    });
  });
});
