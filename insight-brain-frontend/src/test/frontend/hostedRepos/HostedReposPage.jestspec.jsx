/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor } from '@testing-library/dom';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render } from 'TestRoot/SpecUtil';
import HostedReposPage from 'MainRoot/hostedRepos/HostedReposPage';

describe('HostedReposPage', () => {
  let axiosMock;

  const defaultPreloadedState = {
    hostedRepos: {
      repositoryManagers: [],
      loading: false,
      error: null,
    },
    productFeatures: {
      productFeatures: {
        'hosted-repository-evaluation': true,
      },
      loading: false,
      loadError: null,
    },
  };

  const mockRepositoryManagers = [
    {
      instanceId: 'nxrm-prod',
      baseUrl: 'http://nxrm-prod:8081',
      hostedRepositoryCount: 5,
      connectionStatus: 'CONNECTED',
    },
    {
      instanceId: 'nxrm-dev',
      baseUrl: 'http://nxrm-dev:8081',
      hostedRepositoryCount: 0,
      connectionStatus: 'DISCONNECTED',
    },
  ];

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet('/api/v2/lifecycle/repositoryManagers').reply(200, {
      repositoryManagers: mockRepositoryManagers,
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
    axiosMock.reset();
  });

  const renderComponent = (preloadedState) => {
    return render(<HostedReposPage />, {
      preloadedState: preloadedState || defaultPreloadedState,
    });
  };

  it('should render page title and description', () => {
    renderComponent();

    expect(screen.getByRole('heading', { name: /Repository Managers/i })).toBeInTheDocument();
    expect(
      screen.getByText(/Select a Nexus Repository Manager instance to view its hosted repositories/i)
    ).toBeInTheDocument();
  });

  it('should render learn more link', () => {
    renderComponent();

    const learnMoreLink = screen.getByText(/Learn more about hosted repository evaluation/i);
    expect(learnMoreLink).toBeInTheDocument();
    expect(learnMoreLink.closest('a')).toHaveAttribute('href', '#');
  });

  it('should display loading state', () => {
    const loadingState = {
      ...defaultPreloadedState,
      hostedRepos: {
        ...defaultPreloadedState.hostedRepos,
        loading: true,
      },
    };

    renderComponent(loadingState);

    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  it('should display error state with retry', async () => {
    const user = userEvent.setup();

    // Mock API to return error
    axiosMock.reset();
    axiosMock.onGet('/api/v2/lifecycle/repositoryManagers').reply(500, {
      message: 'Failed to load repository managers',
    });

    renderComponent();

    // Wait for error to appear after the fetch fails
    await waitFor(() => {
      expect(screen.getByText(/An error occurred/i)).toBeInTheDocument();
    });

    const retryButton = screen.getByRole('button', { name: /Retry/i });
    expect(retryButton).toBeInTheDocument();

    // Mock successful response for retry
    axiosMock.reset();
    axiosMock.onGet('/api/v2/lifecycle/repositoryManagers').reply(200, {
      repositoryManagers: mockRepositoryManagers,
    });

    // Click retry should trigger new fetch
    await user.click(retryButton);
    await waitFor(() => {
      expect(axiosMock.history.get.length).toBeGreaterThan(0);
    });
  });

  it('should display empty state when no repository managers exist', async () => {
    // Mock API to return empty list
    axiosMock.reset();
    axiosMock.onGet('/api/v2/lifecycle/repositoryManagers').reply(200, {
      repositoryManagers: [],
    });

    renderComponent();

    // Wait for empty state to render after fetch completes
    await waitFor(() => {
      expect(
        screen.getByRole('heading', { name: /No Nexus Repository Managers are currently connected/i })
      ).toBeInTheDocument();
    });

    expect(
      screen.getByText(/To connect a Repository Manager, open the desired Nexus Repository Manager/i)
    ).toBeInTheDocument();
  });

  it('should fetch repository managers on mount', async () => {
    renderComponent();

    await waitFor(() => {
      expect(axiosMock.history.get.length).toBe(1);
      expect(axiosMock.history.get[0].url).toBe('/api/v2/lifecycle/repositoryManagers');
    });
  });

  it('should render connected repository manager card', async () => {
    const connectedState = {
      ...defaultPreloadedState,
      hostedRepos: {
        ...defaultPreloadedState.hostedRepos,
        repositoryManagers: [mockRepositoryManagers[0]],
      },
    };

    renderComponent(connectedState);

    // Wait for component's useEffect fetch to complete
    await waitFor(() => {
      expect(axiosMock.history.get.length).toBe(1);
    });

    await waitFor(() => {
      expect(screen.getByText('nxrm-prod')).toBeInTheDocument();
      expect(screen.getByText('http://nxrm-prod:8081')).toBeInTheDocument();
      expect(screen.getByText('Hosted Repositories')).toBeInTheDocument();
      expect(screen.getByText('5')).toBeInTheDocument();
    });
  });

  it('should render disconnected repository manager card', async () => {
    // Override the default mock to return only disconnected manager
    axiosMock.reset();
    axiosMock.onGet('/api/v2/lifecycle/repositoryManagers').reply(200, {
      repositoryManagers: [mockRepositoryManagers[1]],
    });

    const disconnectedState = {
      ...defaultPreloadedState,
      hostedRepos: {
        ...defaultPreloadedState.hostedRepos,
        repositoryManagers: [mockRepositoryManagers[1]],
      },
    };

    renderComponent(disconnectedState);

    await waitFor(() => {
      expect(screen.getByText('nxrm-dev')).toBeInTheDocument();
      expect(screen.getByText('http://nxrm-dev:8081')).toBeInTheDocument();
    });
    expect(screen.queryByText('Hosted Repositories')).not.toBeInTheDocument();
  });

  it('should render multiple repository manager cards', async () => {
    const multipleState = {
      ...defaultPreloadedState,
      hostedRepos: {
        ...defaultPreloadedState.hostedRepos,
        repositoryManagers: mockRepositoryManagers,
      },
    };

    renderComponent(multipleState);

    // Wait for component's useEffect fetch to complete
    await waitFor(() => {
      expect(axiosMock.history.get.length).toBe(1);
    });

    await waitFor(() => {
      expect(screen.getByText('nxrm-prod')).toBeInTheDocument();
      expect(screen.getByText('nxrm-dev')).toBeInTheDocument();
    });
  });

  it('should handle card click event', async () => {
    const user = userEvent.setup();
    const stateGoSpy = jest.spyOn(require('MainRoot/reduxUiRouter/routerActions'), 'stateGo');

    const connectedState = {
      ...defaultPreloadedState,
      hostedRepos: {
        ...defaultPreloadedState.hostedRepos,
        repositoryManagers: [mockRepositoryManagers[0]],
      },
    };

    renderComponent(connectedState);

    // Wait for component's useEffect fetch to complete
    await waitFor(() => {
      expect(axiosMock.history.get.length).toBe(1);
    });

    await waitFor(() => {
      expect(screen.getByText('nxrm-prod')).toBeInTheDocument();
    });

    // Find the card and click it
    const card = screen.getByText('nxrm-prod').closest('.iq-hosted-repos__card');
    await user.click(card);

    expect(stateGoSpy).toHaveBeenCalledWith('hostedRepositories', { repositoryManagerId: 'nxrm-prod' });
    stateGoSpy.mockRestore();
  });

  it('should display base URL for both connected and disconnected states', async () => {
    const multipleState = {
      ...defaultPreloadedState,
      hostedRepos: {
        ...defaultPreloadedState.hostedRepos,
        repositoryManagers: mockRepositoryManagers,
      },
    };

    renderComponent(multipleState);

    // Wait for component's useEffect fetch to complete
    await waitFor(() => {
      expect(axiosMock.history.get.length).toBe(1);
    });

    await waitFor(() => {
      expect(screen.getByText('http://nxrm-prod:8081')).toBeInTheDocument();
      expect(screen.getByText('http://nxrm-dev:8081')).toBeInTheDocument();
    });
  });

  it('should not display base URL when it is null or empty', async () => {
    const mockNoUrlManager = {
      instanceId: 'nxrm-no-url',
      baseUrl: null,
      hostedRepositoryCount: 3,
      connectionStatus: 'CONNECTED',
    };

    // Override the default mock to return only manager with null URL
    axiosMock.reset();
    axiosMock.onGet('/api/v2/lifecycle/repositoryManagers').reply(200, {
      repositoryManagers: [mockNoUrlManager],
    });

    const noUrlState = {
      ...defaultPreloadedState,
      hostedRepos: {
        ...defaultPreloadedState.hostedRepos,
        repositoryManagers: [mockNoUrlManager],
      },
    };

    renderComponent(noUrlState);

    await waitFor(() => {
      expect(screen.getByText('nxrm-no-url')).toBeInTheDocument();
      expect(screen.queryByText(/http/i)).not.toBeInTheDocument();
    });
  });

  it('should render cards in a grid layout', async () => {
    const multipleState = {
      ...defaultPreloadedState,
      hostedRepos: {
        ...defaultPreloadedState.hostedRepos,
        repositoryManagers: mockRepositoryManagers,
      },
    };

    const { container } = renderComponent(multipleState);

    // Wait for component's useEffect fetch to complete
    await waitFor(() => {
      expect(axiosMock.history.get.length).toBe(1);
    });

    await waitFor(() => {
      const grid = container.querySelector('.iq-hosted-repos__grid');
      expect(grid).toBeInTheDocument();

      const cards = container.querySelectorAll('.iq-hosted-repos__card');
      expect(cards.length).toBe(2);
    });
  });

  it('should not display "Hosted Repositories" section for disconnected state', async () => {
    // Override the default mock to return only disconnected manager
    axiosMock.reset();
    axiosMock.onGet('/api/v2/lifecycle/repositoryManagers').reply(200, {
      repositoryManagers: [mockRepositoryManagers[1]],
    });

    const disconnectedState = {
      ...defaultPreloadedState,
      hostedRepos: {
        ...defaultPreloadedState.hostedRepos,
        repositoryManagers: [mockRepositoryManagers[1]],
      },
    };

    renderComponent(disconnectedState);

    await waitFor(() => {
      expect(screen.getByText('nxrm-dev')).toBeInTheDocument();
      expect(screen.queryByText('Hosted Repositories')).not.toBeInTheDocument();
    });
  });
});