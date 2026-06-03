/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen, userEvent, act } from 'TestRoot/SpecUtil';
import { TopNav } from 'MainRoot/nosc/shell/TopNav';
import * as urlUtil from 'MainRoot/util/urlUtil';

// TopNav embeds the live Classic <SolutionSwitcherContainer>, which is
// a Redux-connected component reading /api/v2/solutions/licensed via the
// solutionSwitcher slice. Using the Redux-aware render from SpecUtil
// supplies the full reducer set so the container can mount in tests.

/**
 * CLM-39639 symmetric Switch-to-Classic.
 *
 * Mirrors the Repo same-page-toggle pattern: clicking "Switch to Classic UI"
 * lands the user on the Classic equivalent of their current Preview page,
 * not always on the Classic root. Falls back to /dashboard/violations for
 * Preview-only surfaces (search, platform home, etc.).
 *
 * Render-rule tests for the rest of the TopNav (header, badge, brand mark)
 * live in Shell.a11y.jestspec.tsx; this file is focused on the toggle
 * navigation behavior.
 */
describe('TopNav — Switch to Classic UI symmetric mapping', () => {
  const originalLocation = window.location;
  let assignMock: jest.Mock;

  beforeEach(() => {
    assignMock = jest.fn();
    delete (window as any).location;
    (window as any).location = {
      ...originalLocation,
      assign: assignMock,
      hash: '',
    };
    jest.spyOn(urlUtil, 'bundleIndexUrl').mockImplementation((bundle, hashPath) => {
      const base =
        bundle === 'classic'
          ? 'http://localhost/assets/index.html'
          : 'http://localhost/assets/nexus-one/index.html';
      return hashPath ? `${base}#${hashPath.startsWith('/') ? hashPath : `/${hashPath}`}` : base;
    });
  });

  afterEach(() => {
    (window as any).location = originalLocation;
    jest.restoreAllMocks();
  });

  const setHash = (hash: string) => {
    (window as any).location.hash = hash;
    act(() => {
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    });
  };

  const renderInTheme = () =>
    render(
      <Theme>
        <TopNav />
      </Theme>,
    );

  it('navigates to /assets/#/dashboard/violations from /dashboard', async () => {
    setHash('#/dashboard');
    renderInTheme();

    const button = screen.getByRole('button', { name: /switch to classic ui/i });
    await userEvent.click(button);

    expect(assignMock).toHaveBeenCalledWith("http://localhost/assets/index.html#/dashboard/violations");
  });

  it('navigates to /assets/#/dashboard/applications from /applications (the canonical Classic apps-list)', async () => {
    setHash('#/applications');
    renderInTheme();

    const button = screen.getByRole('button', { name: /switch to classic ui/i });
    await userEvent.click(button);

    // /management/view/application requires a {publicId} segment; the
    // bare list view in Classic lives at /dashboard/applications. The
    // toggle therefore sends users there to avoid UI-Router's "Unknown
    // Address" error.
    expect(assignMock).toHaveBeenCalledWith('http://localhost/assets/index.html#/dashboard/applications');
  });

  it('falls back to /assets/#/dashboard/violations from Preview-only surfaces', async () => {
    // /home, /search, /preview/guide, etc. have no Classic
    // equivalent — fall back to Classic's safe landing.
    setHash('#/home');
    renderInTheme();

    const button = screen.getByRole('button', { name: /switch to classic ui/i });
    await userEvent.click(button);

    expect(assignMock).toHaveBeenCalledWith("http://localhost/assets/index.html#/dashboard/violations");
  });

  it('updates the click target reactively when the hash changes', async () => {
    setHash('#/dashboard');
    renderInTheme();

    let button = screen.getByRole('button', { name: /switch to classic ui/i });
    await userEvent.click(button);
    expect(assignMock).toHaveBeenLastCalledWith("http://localhost/assets/index.html#/dashboard/violations");

    setHash('#/applications');
    button = screen.getByRole('button', { name: /switch to classic ui/i });
    await userEvent.click(button);
    expect(assignMock).toHaveBeenLastCalledWith('http://localhost/assets/index.html#/dashboard/applications');
  });

  it('exposes the resolved Classic href in the aria-label for screen readers', () => {
    setHash('#/dashboard');
    renderInTheme();

    const button = screen.getByRole('button', { name: /switch to classic ui \(\/dashboard\/violations\)/i });
    expect(button).toBeInTheDocument();
  });

  describe('Sonatype brand mark — Platform Home navigation (CLM-39608)', () => {
    it('navigates to /home when clicked', async () => {
      setHash('#/dashboard');
      renderInTheme();

      const brand = screen.getByTestId('nexus-one-top-nav-brand');
      await userEvent.click(brand);

      expect(assignMock).toHaveBeenCalledWith('http://localhost/assets/nexus-one/index.html#/home');
    });

    it('has an accessible label describing it as a navigation control', () => {
      setHash('#/dashboard');
      renderInTheme();

      // Brand mark is the real Sonatype Lifecycle wordmark in the
      // rebuilt TopNav; matches Classic IQ's logo asset.
      const brand = screen.getByRole('button', { name: /sonatype lifecycle.*platform home/i });
      expect(brand).toBeInTheDocument();
    });
  });
});
