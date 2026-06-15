/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor, within } from '@testing-library/dom';
import { render } from 'TestRoot/SpecUtil';
import HostedReposListPage from 'MainRoot/hostedRepos/HostedReposListPage';

describe('HostedReposListPage', () => {
  const defaultPreloadedState = {
    hostedReposList: {
      repositories: [],
      totalCount: 0,
      loading: false,
      loadError: null,
      sortConfiguration: [],
      repositoryFormatsFilter: '',
      availableFormats: [],
      availableFormatsLoading: false,
      managerInstanceId: 'nxrm-1',
      managerBaseUrl: 'http://localhost:8081',
      managerName: null,
      searchText: '',
    },
    router: {
      currentParams: { repositoryManagerId: 'nxrm-1' },
      currentState: { name: 'hostedRepositories' },
    },
    productFeatures: {
      productFeatures: { 'hosted-repository-evaluation': true },
      loading: false,
      loadError: null,
    },
  };

  const renderComponent = (preloadedState) =>
    render(<HostedReposListPage />, { preloadedState: preloadedState || defaultPreloadedState });

  it('renders the friendly name in H1 when managerName is set', async () => {
    const stateWithName = {
      ...defaultPreloadedState,
      hostedReposList: { ...defaultPreloadedState.hostedReposList, managerName: 'My Local NXRM' },
    };

    renderComponent(stateWithName);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'My Local NXRM' })).toBeInTheDocument();
    });
  });

  it('falls back to instanceId in H1 when managerName is null', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'nxrm-1' })).toBeInTheDocument();
    });
  });

  it('falls back to "Repository Manager" in H1 when both name and instanceId are null', async () => {
    const stateNoManager = {
      ...defaultPreloadedState,
      hostedReposList: {
        ...defaultPreloadedState.hostedReposList,
        managerInstanceId: null,
        managerName: null,
      },
    };

    renderComponent(stateNoManager);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Repository Manager' })).toBeInTheDocument();
    });
  });

  it('shows friendly name in breadcrumb second crumb when managerName is set', async () => {
    const stateWithName = {
      ...defaultPreloadedState,
      hostedReposList: { ...defaultPreloadedState.hostedReposList, managerName: 'My Local NXRM' },
    };

    renderComponent(stateWithName);

    await waitFor(() => {
      const nav = screen.getByRole('navigation');
      expect(within(nav).getByText('My Local NXRM')).toBeInTheDocument();
    });
  });

  it('shows instanceId in breadcrumb second crumb when managerName is null', async () => {
    renderComponent();

    await waitFor(() => {
      const nav = screen.getByRole('navigation');
      expect(within(nav).getByText('nxrm-1')).toBeInTheDocument();
    });
  });
});
