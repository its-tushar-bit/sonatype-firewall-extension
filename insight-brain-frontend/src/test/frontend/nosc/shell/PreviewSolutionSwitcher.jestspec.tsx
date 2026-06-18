/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import userEvent from '@testing-library/user-event';
import { render, screen, within } from 'TestRoot/SpecUtil';
import PreviewSolutionSwitcher from 'MainRoot/nosc/shell/PreviewSolutionSwitcher';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

beforeAll(installRadixJsdomShims);

const lifecycleSolution = {
  id: 'lifecycle' as const,
  name: 'Sonatype Lifecycle',
  url: 'https://example.com/lifecycle',
};
const developerSolution = {
  id: 'developer' as const,
  name: 'Sonatype Developer',
  url: 'https://example.com/developer',
};
const sbomSolution = {
  id: 'sbom' as const,
  name: 'Sonatype SBOM Manager',
  url: 'https://example.com/sbom',
};

const loadedState = {
  solutionSwitcher: {
    licensedSolutions: [lifecycleSolution, developerSolution, sbomSolution],
    isFetched: true,
    loading: false,
    loadError: null,
  },
};

const loadingState = {
  solutionSwitcher: {
    licensedSolutions: [],
    isFetched: false,
    loading: true,
    loadError: null,
  },
};

const errorState = {
  solutionSwitcher: {
    licensedSolutions: [],
    isFetched: false,
    loading: false,
    loadError: 'Service unavailable',
  },
};

function renderInTheme(
  ui: React.ReactElement,
  appearance: 'light' | 'dark',
  preloadedState: object
) {
  return render(
    <Theme appearance={appearance} accentColor="blue" radius="medium">
      {ui}
    </Theme>,
    { preloadedState }
  );
}

describe('PreviewSolutionSwitcher', () => {
  it('renders trigger with aria-label "Solution switcher"', () => {
    renderInTheme(<PreviewSolutionSwitcher />, 'light', loadedState);
    expect(
      screen.getByRole('button', { name: 'Solution switcher' })
    ).toBeInTheDocument();
  });

  it('opens dropdown on click and lists licensed solutions + EXPLORE items', async () => {
    const user = userEvent.setup();
    renderInTheme(<PreviewSolutionSwitcher />, 'light', loadedState);
    await user.click(screen.getByRole('button', { name: 'Solution switcher' }));
    const menu = await screen.findByRole('menu');

    // The component delegates label text to the upstream
    // SolutionNameMap (Lifecycle, SBOM Manager, …) — short names, not
    // the "Sonatype X" full strings we passed in the test fixture.
    // useSolutionSwitcher additionally adds default EXPLORE entries
    // (Repository, Firewall, Guide) regardless of fixture, so the
    // menu has more items than just the licensed list.
    expect(within(menu).getByText('Lifecycle')).toBeInTheDocument();
    expect(within(menu).getByText('SBOM Manager')).toBeInTheDocument();
    // At least one EXPLORE entry renders too. Names come from the
    // upstream SolutionNameMap — Repository is "Nexus Repository
    // Manager", Firewall is "Repository Firewall", etc.
    expect(within(menu).getByText('Nexus Repository Manager')).toBeInTheDocument();
    // The MY SONATYPE / EXPLORE section labels are present.
    expect(within(menu).getByText(/MY SONATYPE SOLUTIONS/i)).toBeInTheDocument();
    expect(within(menu).getByText(/EXPLORE/i)).toBeInTheDocument();
    // At least 3 menuitems (the 2 licensed + ≥1 explore).
    expect(within(menu).getAllByRole('menuitem').length).toBeGreaterThanOrEqual(3);
  });

  it('shows Skeleton placeholders while loading', async () => {
    const user = userEvent.setup();
    renderInTheme(<PreviewSolutionSwitcher />, 'light', loadingState);
    await user.click(screen.getByRole('button', { name: 'Solution switcher' }));
    const menu = await screen.findByRole('menu');
    expect(
      within(menu).queryAllByTestId('nosc-solution-switcher-skeleton-row').length
    ).toBeGreaterThanOrEqual(3);
  });

  it('shows an inline error row when load failed', async () => {
    const user = userEvent.setup();
    renderInTheme(<PreviewSolutionSwitcher />, 'light', errorState);
    await user.click(screen.getByRole('button', { name: 'Solution switcher' }));
    const menu = await screen.findByRole('menu');
    expect(within(menu).getByText(/service unavailable/i)).toBeInTheDocument();
  });

  it('closes the dropdown when Escape is pressed', async () => {
    const user = userEvent.setup();
    renderInTheme(<PreviewSolutionSwitcher />, 'light', loadedState);
    await user.click(screen.getByRole('button', { name: 'Solution switcher' }));
    await screen.findByRole('menu');
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('renders inside a dark Theme without throwing', async () => {
    const user = userEvent.setup();
    renderInTheme(<PreviewSolutionSwitcher />, 'dark', loadedState);
    await user.click(screen.getByRole('button', { name: 'Solution switcher' }));
    expect(await screen.findByRole('menu')).toBeInTheDocument();
  });

  it('exposes the test-id used by Stage 2 e2e selectors', () => {
    renderInTheme(<PreviewSolutionSwitcher />, 'light', loadedState);
    expect(
      screen.getByTestId('nexus-one-top-nav-solution-switcher')
    ).toBeInTheDocument();
  });
});
