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
import * as featureFlagsApi from 'GuideRoot/feature-flags/featureFlagsApi';

jest.mock('GuideRoot/feature-flags/featureFlagsApi');

describe('TopNavigation', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    global.fetch = jest.fn().mockResolvedValue({ ok: true, status: 200, headers: new Headers() });
    jest.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockResolvedValue(['guide-ui']);
  });

  afterEach(() => {
    global.fetch = originalFetch;
    jest.restoreAllMocks();
  });

  it('renders a log out button next to the avatar', () => {
    render(
      <FeatureFlagProvider>
        <TopNavigation onSidebarToggle={() => {}} />
      </FeatureFlagProvider>
    );

    expect(screen.getByRole('button', { name: 'Log out' })).toBeInTheDocument();
  });

  it('calls DELETE /rest/user/session/logout when log out is clicked', async () => {
    const user = userEvent.setup();
    render(
      <FeatureFlagProvider>
        <TopNavigation onSidebarToggle={() => {}} />
      </FeatureFlagProvider>
    );

    await user.click(screen.getByRole('button', { name: 'Log out' }));

    await waitFor(() => {
      const deleteCall = (global.fetch as jest.Mock).mock.calls.find(
        ([, init]) => init?.method === 'DELETE'
      );
      expect(deleteCall).toBeDefined();
      expect(deleteCall![0]).toBe('/rest/user/session/logout');
    });
  });

  it('renders the global search input with placeholder', async () => {
    render(
      <FeatureFlagProvider>
        <TopNavigation onSidebarToggle={() => {}} />
      </FeatureFlagProvider>
    );

    await waitFor(() => {
      const searchInput = screen.getByPlaceholderText(/search components and vulnerabilities/i);
      expect(searchInput).toBeInTheDocument();
    });
  });

  it('search submission targets /search via the form action', async () => {
    render(
      <FeatureFlagProvider>
        <TopNavigation onSidebarToggle={() => {}} />
      </FeatureFlagProvider>
    );

    await waitFor(() => {
      const input = screen.getByPlaceholderText(/search components and vulnerabilities/i);
      const form = input.closest('form');
      expect(form).not.toBeNull();
      expect(form?.getAttribute('action')).toBe('/search');
    });
  });

  it('does not render search when feature flag is disabled', async () => {
    jest.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockResolvedValue([]);

    render(
      <FeatureFlagProvider>
        <TopNavigation onSidebarToggle={() => {}} />
      </FeatureFlagProvider>
    );

    await waitFor(() => {
      expect(screen.queryByPlaceholderText(/search components and vulnerabilities/i)).not.toBeInTheDocument();
    });
  });
});
