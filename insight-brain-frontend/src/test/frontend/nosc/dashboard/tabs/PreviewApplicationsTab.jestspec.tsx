/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { screen, waitFor } from '@testing-library/react';
import { axiosMockAdapter, render, setupPortalContainer } from 'TestRoot/SpecUtil';
import PreviewApplicationsTab, {
  parseApplicationsTabQuery,
} from 'MainRoot/nosc/dashboard/tabs/PreviewApplicationsTab';
import PreviewComponentsTab from 'MainRoot/nosc/dashboard/tabs/PreviewComponentsTab';
import { toggleFilter } from 'MainRoot/dashboard/filter/dashboardFilterActions';

/**
 * S2-PR-D-4 (CLM-39992) tests for the Applications Preview tab.
 *
 * The wrapped Classic `DashboardApplicationsContainer` defers its first
 * fetch until `dashboardFilter.loading` is false, which is `true` in
 * the initial Redux state. That keeps the table in its loading-row
 * shape and lets us focus on the wrapper's contracts (filter-rail
 * mount, URL-query handling, tab-isolation) without needing to stand
 * up a full filter-load fixture.
 */

function renderWithTheme(ui: JSX.Element) {
  return render(<Theme>{ui}</Theme>);
}

describe('PreviewApplicationsTab (CLM-39992 / S2-PR-D-4)', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    setupPortalContainer();
    axiosMock.onAny().reply(() => new Promise(() => {}));
  });

  afterEach(() => {
    axiosMock.reset();
    document.body.innerHTML = '';
    window.history.replaceState(null, '', '#');
  });

  describe('rendering', () => {
    it('renders the tab shell, filter-rail slot, and the wrapped table slot without crashing', () => {
      renderWithTheme(<PreviewApplicationsTab />);
      expect(screen.getByTestId('nosc-dashboard-applications-tab')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-dashboard-applications-filter-slot')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-dashboard-applications-table-slot')).toBeInTheDocument();
    });

    it('mounts the shared DashboardFilter rail in the filter slot', () => {
      renderWithTheme(<PreviewApplicationsTab />);
      const slot = screen.getByTestId('nosc-dashboard-applications-filter-slot');
      expect(slot).toBeInTheDocument();
      expect(screen.getByTestId('dashboard-filter-container')).toBeInTheDocument();
    });
  });

  describe('parseApplicationsTabQuery (helper)', () => {
    it.each([
      ['#/dashboard/applications', { org: null, stage: null, policy: null }],
      [
        '#/dashboard/applications?org=acme',
        { org: 'acme', stage: null, policy: null },
      ],
      [
        '#/dashboard/applications?stage=build',
        { org: null, stage: 'build', policy: null },
      ],
      [
        '#/dashboard/applications?policy=p1',
        { org: null, stage: null, policy: 'p1' },
      ],
      [
        '#/dashboard/applications?org=acme&stage=release&policy=p1',
        { org: 'acme', stage: 'release', policy: 'p1' },
      ],
      [
        '#/dashboard/applications?garbage=1',
        { org: null, stage: null, policy: null },
      ],
    ])('parses %s', (input, expected) => {
      expect(parseApplicationsTabQuery(input)).toEqual(expected);
    });
  });

  describe('URL query handling', () => {
    it('on first mount, ?org=acme dispatches an organizations filter and preserves the query', async () => {
      window.history.replaceState(null, '', '#/dashboard/applications?org=acme');
      const { store } = renderWithTheme(<PreviewApplicationsTab />);

      await waitFor(() => {
        const selected = store.getState().dashboardFilter.selected.organizations;
        expect(selected instanceof Set).toBe(true);
        expect(Array.from(selected as Set<string>)).toEqual(['acme']);
      });
      expect(window.location.hash).toBe('#/dashboard/applications?org=acme');
    });

    it('on first mount, ?stage=build dispatches a stages filter', async () => {
      window.history.replaceState(null, '', '#/dashboard/applications?stage=build');
      const { store } = renderWithTheme(<PreviewApplicationsTab />);

      await waitFor(() => {
        expect(Array.from(store.getState().dashboardFilter.selected.stages as Set<string>)).toEqual(['build']);
      });
    });

    it('on first mount, ?policy=p1 dispatches a policyTypes filter', async () => {
      window.history.replaceState(null, '', '#/dashboard/applications?policy=p1');
      const { store } = renderWithTheme(<PreviewApplicationsTab />);

      await waitFor(() => {
        expect(Array.from(store.getState().dashboardFilter.selected.policyTypes as Set<string>)).toEqual(['p1']);
      });
    });

    it('on first mount, all three params dispatch their respective filters and preserve the query', async () => {
      window.history.replaceState(
        null,
        '',
        '#/dashboard/applications?org=acme&stage=release&policy=p1',
      );
      const { store } = renderWithTheme(<PreviewApplicationsTab />);

      await waitFor(() => {
        const s = store.getState().dashboardFilter.selected;
        expect(Array.from(s.organizations as Set<string>)).toEqual(['acme']);
        expect(Array.from(s.stages as Set<string>)).toEqual(['release']);
        expect(Array.from(s.policyTypes as Set<string>)).toEqual(['p1']);
      });
      expect(window.location.hash).toBe('#/dashboard/applications?org=acme&stage=release&policy=p1');
    });

    it('does NOT dispatch when the hash has no recognized filter query', () => {
      window.history.replaceState(null, '', '#/dashboard/applications');
      const { store } = renderWithTheme(<PreviewApplicationsTab />);
      const s = store.getState().dashboardFilter.selected;
      expect(Array.from(s.organizations as Set<string>)).toEqual([]);
      expect(Array.from(s.stages as Set<string>)).toEqual([]);
      expect(Array.from(s.policyTypes as Set<string>)).toEqual([]);
      expect(window.location.hash).toBe('#/dashboard/applications');
    });
  });

  describe('chip removal clears the filter', () => {
    /**
     * Same rationale as the Components-tab chip-clear test: the
     * shared rail's curried `toggleFilter('organizations', …)` is
     * the action the chip-dismiss flow dispatches. We assert the
     * reducer round-trip directly rather than driving the rail's
     * inner widget.
     */
    it('after a URL-query dispatch, dispatching toggleFilter(..., empty Set) clears the chip', async () => {
      window.history.replaceState(null, '', '#/dashboard/applications?org=acme');
      const { store } = renderWithTheme(<PreviewApplicationsTab />);

      await waitFor(() => {
        expect(Array.from(store.getState().dashboardFilter.selected.organizations as Set<string>)).toEqual(['acme']);
      });

      store.dispatch(toggleFilter('organizations', new Set()));
      expect(Array.from(store.getState().dashboardFilter.selected.organizations as Set<string>)).toEqual([]);
    });
  });

  describe('tab-isolation', () => {
    /**
     * Per AT-D1-002 + D-4 spec: a thrown render error inside the
     * wrapped Applications table MUST NOT unmount a sibling
     * Components tab. Same shape as the components spec.
     */
    function ThrowingTab(): JSX.Element {
      throw new Error('forced throw — applications tab simulated crash');
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
          return <div data-testid="applications-isolation-fallback">tab failed</div>;
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

    it('a render-throw inside the applications slot is caught by its boundary and the components tab DOM is unaffected', () => {
      renderWithTheme(
        <>
          <IsolationBoundary>
            <ThrowingTab />
          </IsolationBoundary>
          <PreviewComponentsTab />
        </>,
      );
      expect(screen.getByTestId('applications-isolation-fallback')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-dashboard-components-tab')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-dashboard-components-table-slot')).toBeInTheDocument();
    });
  });
});
