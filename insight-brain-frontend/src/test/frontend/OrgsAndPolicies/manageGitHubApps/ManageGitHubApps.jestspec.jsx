/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render } from 'TestRoot/SpecUtil';
import ManageGitHubApps from 'MainRoot/OrgsAndPolicies/manageGitHubApps/ManageGitHubApps';

describe('ManageGitHubApps', () => {
  let axiosMock;
  const ownerId = 'org-123';
  const defaultState = {
    orgsAndPolicies: {
      root: { selectedOwner: { id: ownerId, name: 'Test Org' } },
    },
    manageGitHubApps: {
      githubApps: [],
      loading: false,
      error: null,
      deleteModal: { isOpen: false, app: null, isDeleting: false },
    },
  };

  const applicationState = {
    orgsAndPolicies: {
      root: { selectedOwner: { id: 'app-123', name: 'Test App', publicId: 'test-app-public-id' } },
    },
    manageGitHubApps: {
      githubApps: [],
      loading: false,
      error: null,
      deleteModal: { isOpen: false, app: null, isDeleting: false },
    },
  };

  const mockApps = [
    {
      id: 'uuid-1',
      appId: 101,
      slug: 'sonatype-iq-app1',
      githubOrganizationName: 'my-org',
      installationId: 12345,
      isActive: true,
      lastUpdatedAt: '2026-01-15T10:00:00Z',
      installationUrl: 'https://github.com/settings/installations/12345',
    },
    {
      id: 'uuid-2',
      appId: 102,
      slug: 'sonatype-iq-app2',
      githubOrganizationName: 'octocat(personal)',
      installationId: null,
      isActive: true,
      lastUpdatedAt: '2026-01-16T10:00:00Z',
      installationUrl: null,
    },
  ];

  const singleApp = [mockApps[0]];

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet(new RegExp('/api/v2/githubApp')).reply(200, mockApps);
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('renders table with apps from API', async () => {
    render(<ManageGitHubApps />, { preloadedState: defaultState });

    await waitFor(() => {
      expect(screen.getByText('sonatype-iq-app1')).toBeInTheDocument();
      expect(screen.getByText('sonatype-iq-app2')).toBeInTheDocument();
    });
  });

  it('renders empty state when no apps', async () => {
    axiosMock.onGet(new RegExp('/api/v2/githubApp')).reply(200, []);
    render(<ManageGitHubApps />, { preloadedState: defaultState });

    await waitFor(() => {
      expect(screen.getByText(/no GitHub Apps configured/i)).toBeInTheDocument();
    });
  });

  it('shows personal badge for personal accounts', async () => {
    render(<ManageGitHubApps />, { preloadedState: defaultState });

    await waitFor(() => {
      expect(screen.getByText('octocat')).toBeInTheDocument();
      expect(screen.getByText('(personal)', { exact: false })).toBeInTheDocument();
    });
  });

  it('disables Add GitHub App button for application with existing GitHub App', async () => {
    axiosMock.onGet(new RegExp('/api/v2/githubApp')).reply(200, singleApp);
    render(<ManageGitHubApps />, { preloadedState: applicationState });

    await waitFor(() => {
      expect(screen.getByText('sonatype-iq-app1')).toBeInTheDocument();
    });

    const addButton = screen.getByRole('button', { name: /add github app/i });
    expect(addButton).toBeDisabled();
  });

  it('shows tooltip on disabled Add GitHub App button for application', async () => {
    const user = userEvent.setup();
    axiosMock.onGet(new RegExp('/api/v2/githubApp')).reply(200, singleApp);
    render(<ManageGitHubApps />, { preloadedState: applicationState });

    await waitFor(() => {
      expect(screen.getByText('sonatype-iq-app1')).toBeInTheDocument();
    });

    const addButton = screen.getByRole('button', { name: /add github app/i });
    await user.hover(addButton);

    await waitFor(() => {
      expect(screen.getByText('IQ applications can only have one GitHub App associated')).toBeInTheDocument();
    });
  });

  it('does not disable Add GitHub App button for application without GitHub Apps', async () => {
    axiosMock.onGet(new RegExp('/api/v2/githubApp')).reply(200, []);
    render(<ManageGitHubApps />, { preloadedState: applicationState });

    await waitFor(() => {
      expect(screen.getByText(/no GitHub Apps configured/i)).toBeInTheDocument();
    });

    const addButton = screen.getByRole('button', { name: /add github app/i });
    expect(addButton).not.toBeDisabled();
  });

  it('does not disable Add GitHub App button for organization with multiple GitHub Apps', async () => {
    render(<ManageGitHubApps />, { preloadedState: defaultState });

    await waitFor(() => {
      expect(screen.getByText('sonatype-iq-app1')).toBeInTheDocument();
      expect(screen.getByText('sonatype-iq-app2')).toBeInTheDocument();
    });

    const addButton = screen.getByRole('button', { name: /add github app/i });
    expect(addButton).not.toBeDisabled();
  });
});
