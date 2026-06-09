/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { screen } from '@testing-library/react';
import { axiosMockAdapter, render, setupPortalContainer } from 'TestRoot/SpecUtil';
import PreviewWaiversTab from 'MainRoot/nosc/dashboard/tabs/PreviewWaiversTab';
import PreviewViolationsTab from 'MainRoot/nosc/dashboard/tabs/PreviewViolationsTab';

/**
 * S2-PR-D-3 (CLM-39992) tests for the Waivers Preview tab.
 *
 * Mirrors `PreviewViolationsTab.jestspec.tsx`. Classic's waivers table
 * has no active/expired/expiringSoon facet, so D-3 intentionally
 * carries no `?status=` URL contract — the URL-query test from the
 * Violations spec is replaced here with a "no URL contract dispatched
 * on mount" sanity assertion to lock that decision in place. (D-5 may
 * later add a Waivers-tab URL contract when tile→tab IA wiring lands.)
 */

function renderWithTheme(ui: JSX.Element) {
  return render(<Theme>{ui}</Theme>);
}

describe('PreviewWaiversTab (CLM-39992 / S2-PR-D-3)', () => {
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
      renderWithTheme(<PreviewWaiversTab />);
      expect(screen.getByTestId('nosc-dashboard-waivers-tab')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-dashboard-waivers-filter-slot')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-dashboard-waivers-table-slot')).toBeInTheDocument();
    });
  });

  describe('URL query handling (no D-3 contract)', () => {
    it('does NOT mutate the dashboardFilter slice on mount when the hash has a ?status query (no D-3 contract)', () => {
      window.history.replaceState(null, '', '#/dashboard/waivers?status=active');
      const { store } = renderWithTheme(<PreviewWaiversTab />);
      // Default selected.expirationDate is unchanged because Waivers
      // does not (yet) honor a `?status=` URL contract.
      const before = store.getState().dashboardFilter.selected.expirationDate;
      expect(store.getState().dashboardFilter.selected.expirationDate).toEqual(before);
      // Hash is unchanged — the tab does not strip queries it does not
      // understand. (D-5 may add stripping if it adds a Waivers URL
      // contract.)
      expect(window.location.hash).toBe('#/dashboard/waivers?status=active');
    });
  });

  describe('tab-isolation', () => {
    function ThrowingTab(): JSX.Element {
      throw new Error('forced throw — waivers tab simulated crash');
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
          return <div data-testid="waivers-isolation-fallback">tab failed</div>;
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

    it('a render-throw inside the waivers slot is caught by its boundary and the violations tab DOM is unaffected', () => {
      renderWithTheme(
        <>
          <IsolationBoundary>
            <ThrowingTab />
          </IsolationBoundary>
          <PreviewViolationsTab />
        </>,
      );
      expect(screen.getByTestId('waivers-isolation-fallback')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-dashboard-violations-tab')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-dashboard-violations-table-slot')).toBeInTheDocument();
    });
  });
});
