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
import { NexusOneRouterProvider } from 'TestRoot/nosc/renderNexusOneRoute';

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
  return render(
    <NexusOneRouterProvider>
      <Theme>{ui}</Theme>
    </NexusOneRouterProvider>,
  );
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
    it('renders the tab shell + native nosc WaiversTable without crashing', () => {
      renderWithTheme(<PreviewWaiversTab />);
      expect(screen.getByTestId('nosc-dashboard-waivers-tab')).toBeInTheDocument();
      // The native nosc WaiversTable mounts a loading skeleton while the
      // axios request is in flight; the test-id is suffixed with -loading.
      // Its presence proves the Classic-table → native-table swap landed.
      expect(
        screen.getByTestId('nosc-dashboard-waivers-table-loading'),
      ).toBeInTheDocument();
    });

    it('does NOT render the Classic filter-rail (intentional UX choice — see component doc-comment)', () => {
      renderWithTheme(<PreviewWaiversTab />);
      // The Classic DashboardFilter is intentionally absent from this tab.
      // The previous implementation mounted it next to the Classic
      // DashboardWaiversTable; this implementation drops it because the
      // native nosc WaiversTable reads from a different code path and
      // the filter rail would be visible but non-functional.
      expect(
        screen.queryByTestId('nosc-dashboard-waivers-filter-slot'),
      ).not.toBeInTheDocument();
    });
  });

  describe('URL query handling (no D-3 contract)', () => {
    it('preserves ?status= in the hash on mount because the waivers tab has no URL filter contract', () => {
      window.history.replaceState(null, '', '#/dashboard/waivers?status=active');
      renderWithTheme(<PreviewWaiversTab />);
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
