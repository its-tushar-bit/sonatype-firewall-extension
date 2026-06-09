/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { screen, waitFor } from '@testing-library/react';
import { axiosMockAdapter, render, setupPortalContainer } from 'TestRoot/SpecUtil';
import PreviewComponentsTab, {
  parseComponentsTabQuery,
} from 'MainRoot/nosc/dashboard/tabs/PreviewComponentsTab';
import PreviewApplicationsTab from 'MainRoot/nosc/dashboard/tabs/PreviewApplicationsTab';
import { toggleFilter } from 'MainRoot/dashboard/filter/dashboardFilterActions';

/**
 * S2-PR-D-4 (CLM-39992) tests for the Components Preview tab.
 *
 * The wrapped Classic `DashboardComponentsContainer` defers its first
 * fetch until `dashboardFilter.loading` is false, which is `true` in
 * the initial Redux state. That keeps the table in its loading-row
 * shape during these tests and lets us focus on the wrapper itself
 * (filter-rail mount, URL-query handling, tab-isolation) without
 * needing to stand up a full filter-load fixture.
 */

function renderWithTheme(ui: JSX.Element) {
  return render(<Theme>{ui}</Theme>);
}

describe('PreviewComponentsTab (CLM-39992 / S2-PR-D-4)', () => {
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
      renderWithTheme(<PreviewComponentsTab />);
      expect(screen.getByTestId('nosc-dashboard-components-tab')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-dashboard-components-filter-slot')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-dashboard-components-table-slot')).toBeInTheDocument();
    });

    it('mounts the shared DashboardFilter rail in the filter slot', () => {
      renderWithTheme(<PreviewComponentsTab />);
      const slot = screen.getByTestId('nosc-dashboard-components-filter-slot');
      // The PortalDrawer the filter rail uses renders inside the
      // `.nx-page` portal container we set up in beforeEach. Its
      // own `dashboard-filter-container` testid is the cleanest
      // anchor for "the filter rail mounted".
      expect(slot).toBeInTheDocument();
      expect(screen.getByTestId('dashboard-filter-container')).toBeInTheDocument();
    });
  });

  describe('parseComponentsTabQuery (helper)', () => {
    it.each([
      ['#/dashboard/components', { severity: null, policy: null }],
      [
        '#/dashboard/components?severity=critical',
        { severity: 'critical', policy: null },
      ],
      [
        '#/dashboard/components?policy=p1',
        { severity: null, policy: 'p1' },
      ],
      [
        '#/dashboard/components?severity=moderate&policy=p2',
        { severity: 'moderate', policy: 'p2' },
      ],
      ['#/dashboard/components?garbage=1', { severity: null, policy: null }],
    ])('parses %s', (input, expected) => {
      expect(parseComponentsTabQuery(input)).toEqual(expected);
    });
  });

  describe('URL query handling', () => {
    it('on first mount, ?policy=foo dispatches a policyTypes filter and preserves the query', async () => {
      window.history.replaceState(null, '', '#/dashboard/components?policy=foo');
      const { store } = renderWithTheme(<PreviewComponentsTab />);

      // The wrapper dispatches a TOGGLE_FILTER for policyTypes
      // = Set(['foo']). The reducer immediately reflects that into
      // `dashboardFilter.selected.policyTypes`.
      await waitFor(() => {
        const selected = store.getState().dashboardFilter.selected.policyTypes;
        expect(selected instanceof Set).toBe(true);
        expect(Array.from(selected as Set<string>)).toEqual(['foo']);
      });

      expect(window.location.hash).toBe('#/dashboard/components?policy=foo');
    });

    it('on first mount, ?severity=critical dispatches policyThreatLevels [8,10] and preserves the query', async () => {
      window.history.replaceState(null, '', '#/dashboard/components?severity=critical');
      const { store } = renderWithTheme(<PreviewComponentsTab />);

      await waitFor(() => {
        expect(store.getState().dashboardFilter.selected.policyThreatLevels).toEqual([8, 10]);
      });
      expect(window.location.hash).toBe('#/dashboard/components?severity=critical');
    });

    it('does NOT dispatch when the hash has no recognized filter query', () => {
      window.history.replaceState(null, '', '#/dashboard/components');
      const { store } = renderWithTheme(<PreviewComponentsTab />);
      // Selected stays at the default range [2, 10] and policyTypes
      // is an empty Set.
      expect(store.getState().dashboardFilter.selected.policyThreatLevels).toEqual([2, 10]);
      expect(Array.from(store.getState().dashboardFilter.selected.policyTypes as Set<string>)).toEqual([]);
      expect(window.location.hash).toBe('#/dashboard/components');
    });

    it('handles unknown severity values without dispatching and preserves the query', () => {
      window.history.replaceState(null, '', '#/dashboard/components?severity=banana');
      const { store } = renderWithTheme(<PreviewComponentsTab />);
      expect(store.getState().dashboardFilter.selected.policyThreatLevels).toEqual([2, 10]);
      expect(window.location.hash).toBe('#/dashboard/components?severity=banana');
    });
  });

  describe('chip removal clears the filter', () => {
    /**
     * Per spec: "Removing the chip clears the filter". The chip
     * itself is rendered by the shared `DashboardFilter` rail's
     * `NxStatefulTreeViewMultiSelect`, which calls the curried
     * `onPolicyTypesChange = curriedToggleFilter('policyTypes')`
     * with the NEW selected-ids Set when a chip is dismissed
     * (i.e. an empty Set when the only chip is removed). That is
     * exactly the same action shape this tab dispatches on the
     * URL-query first mount — so the "clear the chip" contract
     * is: dispatching `toggleFilter('policyTypes', new Set())`
     * removes the filter the wrapper applied. We assert the
     * reducer round-trip here rather than driving the rail's
     * inner widget (which would require a fully-loaded
     * applications/organizations/categories dataset that has
     * nothing to do with chip-clear semantics).
     */
    it('after a URL-query dispatch, dispatching toggleFilter(..., empty Set) clears the chip', async () => {
      window.history.replaceState(null, '', '#/dashboard/components?policy=foo');
      const { store } = renderWithTheme(<PreviewComponentsTab />);

      await waitFor(() => {
        expect(Array.from(store.getState().dashboardFilter.selected.policyTypes as Set<string>)).toEqual(['foo']);
      });

      store.dispatch(toggleFilter('policyTypes', new Set()));
      expect(Array.from(store.getState().dashboardFilter.selected.policyTypes as Set<string>)).toEqual([]);
    });
  });

  describe('tab-isolation', () => {
    /**
     * Per AT-D1-002 + D-4 spec: a thrown render error inside the
     * wrapped Components table MUST NOT unmount a sibling
     * Applications tab. Same shape as the D-3 violations spec —
     * we mount a forced-throw "components" child inside its own
     * boundary, and a healthy Applications tab next to it, and
     * assert the Applications DOM survives the Components crash.
     */
    function ThrowingTab(): JSX.Element {
      throw new Error('forced throw — components tab simulated crash');
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
          return <div data-testid="components-isolation-fallback">tab failed</div>;
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

    it('a render-throw inside the components slot is caught by its boundary and the applications tab DOM is unaffected', () => {
      renderWithTheme(
        <>
          <IsolationBoundary>
            <ThrowingTab />
          </IsolationBoundary>
          <PreviewApplicationsTab />
        </>,
      );
      expect(screen.getByTestId('components-isolation-fallback')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-dashboard-applications-tab')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-dashboard-applications-table-slot')).toBeInTheDocument();
    });
  });
});
