/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { Theme } from '@radix-ui/themes';
import { NavigationProvider } from '@guide/ui-core';
import SidebarNavigation from 'GuideRoot/layout/SidebarNavigation';
import { SIDEBAR_GROUPS } from 'GuideRoot/layout/constants';
import { FeatureFlagProvider } from 'GuideRoot/feature-flags/FeatureFlagProvider';
import { useReactRouterAdapter } from 'GuideRoot/reactRouterAdapter';
import * as featureFlagsApi from 'GuideRoot/feature-flags/featureFlagsApi';

jest.mock('GuideRoot/feature-flags/featureFlagsApi');

function AdapterBridge({ children }: { children: React.ReactNode }) {
  const adapter = useReactRouterAdapter();
  return <NavigationProvider adapter={adapter}>{children}</NavigationProvider>;
}

function renderSidebar() {
  return render(
    <Theme>
      <MemoryRouter initialEntries={['/']}>
        <FeatureFlagProvider>
          <AdapterBridge>
            <SidebarNavigation groups={SIDEBAR_GROUPS} expanded={true} />
          </AdapterBridge>
        </FeatureFlagProvider>
      </MemoryRouter>
    </Theme>
  );
}

describe('SidebarNavigation feature flag filtering', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('shows Components and Vulnerabilities when guide-ui flag is enabled', async () => {
    jest.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockResolvedValue(['guide-ui']);

    renderSidebar();

    await waitFor(() => {
      expect(screen.getByRole('link', { name: 'Components' })).toBeInTheDocument();
    });
    expect(screen.getByRole('link', { name: 'Vulnerabilities' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Home' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'MCP' })).toBeInTheDocument();
  });

  it('hides Components and Vulnerabilities when guide-ui flag is disabled', async () => {
    jest.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockResolvedValue([]);

    renderSidebar();

    await waitFor(() => {
      expect(screen.getByRole('link', { name: 'Home' })).toBeInTheDocument();
    });
    expect(screen.getByRole('link', { name: 'MCP' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Components' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Vulnerabilities' })).not.toBeInTheDocument();
  });

  it('hides flagged items when the fetch fails (default-disabled)', async () => {
    jest
      .spyOn(featureFlagsApi, 'fetchFeatureFlags')
      .mockRejectedValue(new Error('500'));

    renderSidebar();

    await waitFor(() => {
      expect(screen.getByRole('link', { name: 'Home' })).toBeInTheDocument();
    });
    expect(screen.queryByRole('link', { name: 'Components' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Vulnerabilities' })).not.toBeInTheDocument();
  });
});
