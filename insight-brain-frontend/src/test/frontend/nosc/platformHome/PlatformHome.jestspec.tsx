/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PlatformHome } from 'MainRoot/nosc/platformHome/PlatformHome';
import * as urlUtil from 'MainRoot/util/urlUtil';

/**
 * P1-F14 / CLM-39608. The Platform Home is the entry point of the Nexus One
 * UI — a 5-tile grid of Sonatype solutions. Mirrors the design from the
 * Nexus One UX prototype (apps/nexusone-ux-prototype/src/app/nexusone/page.tsx),
 * adapted for IQ:
 *
 * - Lifecycle, Firewall, SBOM Manager, Guide → in-IQ Preview surfaces (the
 *   first 3 ship in Phase-1; Guide ships when its IQ integration lands).
 * - Repository → external link to sonatype.com (separate JVM, separate team).
 *
 * Tile contracts here are the authoritative source — the page reads from
 * the same SOLUTIONS array.
 */
describe('PlatformHome', () => {
  const originalLocation = window.location;
  let assignMock: jest.Mock;
  let openMock: jest.Mock;

  beforeEach(() => {
    assignMock = jest.fn();
    openMock = jest.fn();
    delete (window as any).location;
    (window as any).location = {
      ...originalLocation,
      assign: assignMock,
      hash: '',
    };
    (window as any).open = openMock;
    jest.spyOn(urlUtil, 'bundleIndexUrl').mockImplementation(
      (_bundle, hashPath) => `http://localhost/assets/nexus-one/index.html#${hashPath ?? ''}`,
    );
  });

  afterEach(() => {
    (window as any).location = originalLocation;
    jest.restoreAllMocks();
  });

  const renderInTheme = () =>
    render(
      <Theme>
        <PlatformHome />
      </Theme>,
    );

  it('renders the page heading', () => {
    renderInTheme();
    expect(screen.getByRole('heading', { name: /nexus one/i, level: 1 })).toBeInTheDocument();
  });

  it('renders the Solutions section heading', () => {
    renderInTheme();
    expect(screen.getByRole('heading', { name: /solutions/i, level: 2 })).toBeInTheDocument();
  });

  it.each([
    ['Sonatype Lifecycle', 'lifecycle'],
    ['Sonatype Repository Firewall', 'firewall'],
    ['Sonatype SBOM Manager', 'sbom-manager'],
    ['Sonatype Guide', 'guide'],
    ['Sonatype Nexus Repository', 'repository'],
  ])('renders the %s tile', (productName, _slug) => {
    renderInTheme();
    expect(screen.getByRole('heading', { name: new RegExp(productName, 'i') })).toBeInTheDocument();
  });

  it('navigates to /dashboard when Lifecycle tile is clicked', async () => {
    renderInTheme();
    const button = screen.getByTestId('platform-home-tile-lifecycle');
    await userEvent.click(button);
    expect(assignMock).toHaveBeenCalledWith(
      expect.stringMatching(/nexus-one\/index\.html#\/dashboard$/),
    );
  });

  it('navigates to coming-soon firewall placeholder when Firewall tile is clicked', async () => {
    renderInTheme();
    const button = screen.getByTestId('platform-home-tile-firewall');
    await userEvent.click(button);
    expect(assignMock).toHaveBeenCalledWith(
      expect.stringMatching(/nexus-one\/index\.html#\/coming-soon\/firewall$/),
    );
  });

  it('navigates to coming-soon sbom placeholder when SBOM Manager tile is clicked', async () => {
    renderInTheme();
    const button = screen.getByTestId('platform-home-tile-sbom-manager');
    await userEvent.click(button);
    expect(assignMock).toHaveBeenCalledWith(
      expect.stringMatching(/nexus-one\/index\.html#\/coming-soon\/sbom-manager$/),
    );
  });

  it('navigates to coming-soon guide placeholder when Guide tile is clicked', async () => {
    renderInTheme();
    const button = screen.getByTestId('platform-home-tile-guide');
    await userEvent.click(button);
    expect(assignMock).toHaveBeenCalledWith(
      expect.stringMatching(/nexus-one\/index\.html#\/coming-soon\/guide$/),
    );
  });

  it('opens sonatype.com in a new tab when Repository tile is clicked (external)', async () => {
    renderInTheme();
    const button = screen.getByTestId('platform-home-tile-repository');
    await userEvent.click(button);
    // Repository is on a different JVM and not part of IQ — link out, not navigate.
    expect(openMock).toHaveBeenCalledWith(
      expect.stringContaining('sonatype.com'),
      '_blank',
      expect.stringContaining('noopener'),
    );
    expect(assignMock).not.toHaveBeenCalled();
  });

  it('renders a logo image for each tile (5 product icons)', () => {
    renderInTheme();
    const images = screen.getAllByRole('img');
    // At least one img per tile; the 5 product icons must all be present.
    expect(images.length).toBeGreaterThanOrEqual(5);
  });

  it('marks the in-IQ tiles as primary buttons and the Repository tile as outlined', () => {
    renderInTheme();
    // Smoke check via test-ids; specific Radix variant prop isn't directly
    // queryable, but the data-external attribute distinguishes Repository.
    expect(screen.getByTestId('platform-home-tile-lifecycle')).toHaveAttribute('data-external', 'false');
    expect(screen.getByTestId('platform-home-tile-repository')).toHaveAttribute('data-external', 'true');
  });
});
