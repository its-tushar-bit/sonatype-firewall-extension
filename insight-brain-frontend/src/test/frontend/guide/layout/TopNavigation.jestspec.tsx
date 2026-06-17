/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from '../test-utils';
import { FeatureFlagProvider } from 'GuideRoot/feature-flags/FeatureFlagProvider';
import { TopNavigation } from 'GuideRoot/layout/TopNavigation';
import { LicenseProvider } from 'GuideRoot/license/LicenseProvider';
import * as featureFlagsApi from 'GuideRoot/feature-flags/featureFlagsApi';

jest.mock('GuideRoot/feature-flags/featureFlagsApi');

// TopNavigation embeds ProductSwitcher, which reads licensed solutions from LicenseProvider.
function renderTopNav() {
  return render(
    <LicenseProvider>
      <FeatureFlagProvider>
        <TopNavigation onSidebarToggle={() => {}} />
      </FeatureFlagProvider>
    </LicenseProvider>
  );
}

describe('TopNavigation', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers(),
      json: async () => ({ hits: [], total: 0, offset: 0, limit: 25, aggregations: {} }),
    });
    jest.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockResolvedValue(['guide-search']);
  });

  afterEach(() => {
    global.fetch = originalFetch;
    jest.restoreAllMocks();
  });

  it('renders a log out button next to the avatar', () => {
    renderTopNav();

    expect(screen.getByRole('button', { name: 'Log out' })).toBeInTheDocument();
  });

  it('calls DELETE /rest/user/session/logout when log out is clicked', async () => {
    const user = userEvent.setup();
    renderTopNav();

    await user.click(screen.getByRole('button', { name: 'Log out' }));

    await waitFor(() => {
      const deleteCall = (global.fetch as jest.Mock).mock.calls.find(
        ([, init]) => init?.method === 'DELETE'
      );
      expect(deleteCall).toBeDefined();
      expect(deleteCall![0]).toBe('/rest/user/session/logout');
    });
  });

  // guide-ui is intentionally absent — search visibility is controlled by guide-search only
  it('renders the global search input with placeholder', async () => {
    renderTopNav();

    await waitFor(() => {
      const searchInput = screen.getByPlaceholderText(/search components and vulnerabilities/i);
      expect(searchInput).toBeInTheDocument();
    });
  });

  it('search submission targets /search via the form action', async () => {
    renderTopNav();

    await waitFor(() => {
      const input = screen.getByPlaceholderText(/search components and vulnerabilities/i);
      const form = input.closest('form');
      expect(form).not.toBeNull();
      expect(form?.getAttribute('action')).toBe('/search');
    });
  });

  it('forwards typed query to the global search endpoint as URLSearchParams', async () => {
    const fetchMock = global.fetch as jest.Mock;

    renderTopNav();

    const input = await screen.findByPlaceholderText(/search components and vulnerabilities/i);
    const user = userEvent.setup();
    await user.type(input, 'lodash');

    await waitFor(() => {
      const searchCall = fetchMock.mock.calls.find(
        ([url]) => typeof url === 'string' && url.startsWith('/api/v2/guide/global/search')
      );
      expect(searchCall).toBeDefined();
      const url = searchCall![0] as string;
      const params = new URLSearchParams(url.split('?')[1] ?? '');
      expect(params.get('query')).toBe('lodash');
      expect(url).not.toContain('[object Object]');
    });
  });

  it('does not render search when feature flag is disabled', async () => {
    jest.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockResolvedValue([]);

    renderTopNav();

    await waitFor(() => {
      expect(screen.queryByPlaceholderText(/search components and vulnerabilities/i)).not.toBeInTheDocument();
    });
  });

  it('hides search when guide-search is absent even if guide-ui is present', async () => {
    jest.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockResolvedValue(['guide-ui']);

    renderTopNav();

    // Wait for the component to finish loading flags (logout button is always present)
    await waitFor(() => expect(screen.getByRole('button', { name: 'Log out' })).toBeInTheDocument());
    expect(screen.queryByPlaceholderText(/search components and vulnerabilities/i)).not.toBeInTheDocument();
  });
});
