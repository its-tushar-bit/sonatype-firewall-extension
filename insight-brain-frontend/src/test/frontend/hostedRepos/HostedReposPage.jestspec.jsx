/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { act } from 'react';
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

  const recentActivityTime = Date.now() - 60 * 60 * 1000; // 1 hour ago

  const mockRepositoryManagers = [
    {
      id: 'rm-db-id-1',
      instanceId: 'nxrm-prod',
      name: null,
      baseUrl: 'http://nxrm-prod:8081',
      hostedRepositoryCount: 5,
      connectionStatus: 'CONNECTED',
      lastActivityTime: recentActivityTime,
    },
    {
      id: 'rm-db-id-2',
      instanceId: 'nxrm-dev',
      name: null,
      baseUrl: 'http://nxrm-dev:8081',
      hostedRepositoryCount: 0,
      connectionStatus: 'DISCONNECTED',
      lastActivityTime: null,
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

  it('should display friendly name as card title when name is set', async () => {
    const namedManager = { ...mockRepositoryManagers[0], name: 'My Local NXRM' };
    axiosMock.reset();
    axiosMock.onGet('/api/v2/lifecycle/repositoryManagers').reply(200, {
      repositoryManagers: [namedManager],
    });
    const namedState = {
      ...defaultPreloadedState,
      hostedRepos: { ...defaultPreloadedState.hostedRepos, repositoryManagers: [namedManager] },
    };

    renderComponent(namedState);

    await waitFor(() => {
      expect(screen.getByText('My Local NXRM')).toBeInTheDocument();
      expect(screen.queryByText('nxrm-prod')).not.toBeInTheDocument();
    });
  });

  it('should fall back to instanceId as card title when name is not set', async () => {
    const connectedState = {
      ...defaultPreloadedState,
      hostedRepos: {
        ...defaultPreloadedState.hostedRepos,
        repositoryManagers: [mockRepositoryManagers[0]],
      },
    };

    renderComponent(connectedState);

    await waitFor(() => {
      expect(screen.getByText('nxrm-prod')).toBeInTheDocument();
    });
  });

  it('should render repository manager card with recent activity', async () => {
    const connectedState = {
      ...defaultPreloadedState,
      hostedRepos: {
        ...defaultPreloadedState.hostedRepos,
        repositoryManagers: [mockRepositoryManagers[0]],
      },
    };

    renderComponent(connectedState);

    await waitFor(() => {
      expect(screen.getByText('nxrm-prod')).toBeInTheDocument();
      expect(screen.getByText('http://nxrm-prod:8081')).toBeInTheDocument();
      const label = screen.getByText(/Last activity:/i);
      expect(label).toBeInTheDocument();
      expect(label.closest('.iq-hosted-repos__card-status')).toHaveClass('iq-hosted-repos__status--active');
    });
  });

  it('should render "No activity recorded" when lastActivityTime is null', async () => {
    const noActivityState = {
      ...defaultPreloadedState,
      hostedRepos: {
        ...defaultPreloadedState.hostedRepos,
        repositoryManagers: [mockRepositoryManagers[1]],
      },
    };

    renderComponent(noActivityState);

    await waitFor(() => {
      expect(screen.getByText('nxrm-dev')).toBeInTheDocument();
      expect(screen.getByText('No activity recorded')).toBeInTheDocument();
    });
  });

  it('should render stale activity indicator when lastActivityTime is older than 7 days', async () => {
    // Use a timestamp from 2020 — always > 7 days ago
    const oldTimestamp = new Date('2020-01-01T00:00:00Z').getTime();

    const staleManager = {
      instanceId: 'nxrm-stale',
      baseUrl: 'http://nxrm-stale:8081',
      hostedRepositoryCount: 2,
      connectionStatus: 'CONNECTED',
      lastActivityTime: oldTimestamp,
    };

    axiosMock.reset();
    axiosMock.onGet('/api/v2/lifecycle/repositoryManagers').reply(200, {
      repositoryManagers: [staleManager],
    });

    const staleState = {
      ...defaultPreloadedState,
      hostedRepos: {
        ...defaultPreloadedState.hostedRepos,
        repositoryManagers: [staleManager],
      },
    };

    renderComponent(staleState);

    await waitFor(() => {
      const label = screen.getByText(/Last activity:/i);
      expect(label).toBeInTheDocument();
      expect(label.closest('.iq-hosted-repos__status--stale')).toBeInTheDocument();
    });
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

    const { store } = renderComponent(connectedState);

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

    // Manager info should be pre-populated in hostedReposList so the breadcrumb renders immediately
    const { hostedReposList } = store.getState();
    expect(hostedReposList.managerInstanceId).toBe('nxrm-prod');
    expect(hostedReposList.managerBaseUrl).toBe('http://nxrm-prod:8081');
    expect(hostedReposList.managerName).toBeNull();
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

  describe('EditNameModal', () => {
    const managerWithName = {
      id: 'rm-db-id-1',
      instanceId: 'nxrm-prod',
      name: 'Production NXRM',
      baseUrl: 'http://nxrm-prod:8081',
      hostedRepositoryCount: 5,
      connectionStatus: 'CONNECTED',
      lastActivityTime: recentActivityTime,
    };

    const stateWithManager = (manager) => ({
      ...defaultPreloadedState,
      hostedRepos: {
        ...defaultPreloadedState.hostedRepos,
        repositoryManagers: [manager],
      },
    });

    const renderWithManager = () => {
      axiosMock.reset();
      axiosMock.onGet('/api/v2/lifecycle/repositoryManagers').reply(200, {
        repositoryManagers: [managerWithName],
      });
      return renderComponent(stateWithManager(managerWithName));
    };

    const openEditModal = async (user, container) => {
      await waitFor(() => expect(screen.getByText('Production NXRM')).toBeInTheDocument());
      const dropdownToggle = container.querySelector('.nx-icon-dropdown__toggle');
      await user.click(dropdownToggle);
      await user.click(screen.getByRole('button', { name: /Edit Name/i }));
    };

    it('should open edit modal when Edit Name is clicked', async () => {
      const user = userEvent.setup();
      const { container } = renderWithManager();

      await openEditModal(user, container);

      expect(screen.getByRole('heading', { name: /Edit Repository Manager/i })).toBeInTheDocument();
      expect(screen.getByLabelText(/Repository Manager Name/i)).toBeInTheDocument();
    });

    it('should pre-fill input with current name', async () => {
      const user = userEvent.setup();
      const { container } = renderWithManager();

      await openEditModal(user, container);

      expect(screen.getByLabelText(/Repository Manager Name/i)).toHaveValue('Production NXRM');
    });

    it('should close modal when Cancel is clicked', async () => {
      const user = userEvent.setup();
      const { container } = renderWithManager();

      await openEditModal(user, container);
      await user.click(screen.getByRole('button', { name: /Cancel/i }));

      expect(screen.queryByRole('heading', { name: /Edit Repository Manager/i })).not.toBeInTheDocument();
    });

    it('should rename successfully, close modal and update card title', async () => {
      const user = userEvent.setup();
      const { container } = renderWithManager();
      axiosMock.onPut().reply(200);

      await openEditModal(user, container);

      const input = screen.getByLabelText(/Repository Manager Name/i);
      await user.clear(input);
      await user.type(input, 'New Name');
      await user.click(screen.getByRole('button', { name: /Update/i }));

      await waitFor(() => {
        expect(axiosMock.history.put.length).toBe(1);
      });

      await act(async () => {});

      expect(screen.queryByRole('heading', { name: /Edit Repository Manager/i })).not.toBeInTheDocument();
      expect(screen.getByText('New Name')).toBeInTheDocument();
    });

    it('should show error message when rename fails', async () => {
      const user = userEvent.setup();
      const { container } = renderWithManager();
      axiosMock.onPut().reply(500, { message: 'Name already in use' });

      await openEditModal(user, container);

      const input = screen.getByLabelText(/Repository Manager Name/i);
      await user.clear(input);
      await user.type(input, 'Duplicate Name');
      await user.click(screen.getByRole('button', { name: /Update/i }));

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /Edit Repository Manager/i })).toBeInTheDocument();
        expect(screen.getByRole('alert')).toBeInTheDocument();
      });
    });

    it('should show validation error and not submit when input is blank', async () => {
      const user = userEvent.setup();
      const { container } = renderWithManager();

      await openEditModal(user, container);

      const input = screen.getByLabelText(/Repository Manager Name/i);
      await user.clear(input);
      await user.click(screen.getByRole('button', { name: /Update/i }));

      await waitFor(() => {
        expect(axiosMock.history.put.length).toBe(0);
        expect(screen.getByRole('heading', { name: /Edit Repository Manager/i })).toBeInTheDocument();
        expect(screen.getByRole('alert', { name: /form validation errors/i })).toBeInTheDocument();
      });
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

});