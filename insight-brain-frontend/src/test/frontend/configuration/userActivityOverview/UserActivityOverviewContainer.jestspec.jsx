/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, setupPortalContainer, axiosMockAdapter } from 'TestRoot/SpecUtil';
import { waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import UserActivityOverviewContainer from 'MainRoot/configuration/userActivityOverview/UserActivityOverviewContainer';
import * as authorizationUtil from 'MainRoot/util/authorizationUtil';

describe('UserActivityOverviewContainer', () => {
  let preloadedState, axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    setupPortalContainer(); // Required for PortalDrawer

    // Mock the permission check for loadUserActivityPage
    jest.spyOn(authorizationUtil, 'checkPermissions').mockImplementation(() => Promise.resolve());

    preloadedState = {
      userActivity: {
        users: [
          { username: 'testuser1', loginCount: 5, lastActive: '2023-01-01T10:00:00Z' },
          { username: 'testuser2', loginCount: 3, lastActive: '2023-01-02T10:00:00Z' },
        ],
        totalUsers: 2,
        loading: false,
        loadError: null,
        exporting: false,
        exportError: null,
        searchFilter: '',
        filterDrawerOpen: false,
        selectedFilters: { selectedAge: 30 },
        appliedFilters: { selectedAge: 30 },
        filtersAreDirty: false,
      },
    };

    // Mock the API response for the useEffect applyFilters call
    axiosMock.onGet().reply(200, {
      users: preloadedState.userActivity.users,
      totalUsers: preloadedState.userActivity.totalUsers,
    });
  });

  it('should render user activity data from Redux store', async () => {
    render(<UserActivityOverviewContainer isAuthorized={true} />, { preloadedState });

    // Wait for the async action to complete and component to render data
    await waitFor(() => {
      expect(screen.getByText('testuser1')).toBeVisible();
    });

    expect(screen.getByText('testuser2')).toBeVisible();
    expect(screen.getByText('5')).toBeVisible(); // loginCount
    expect(screen.getByText('3')).toBeVisible(); // loginCount
  });

  it('should show loading state when loading is true', () => {
    const loadingState = {
      ...preloadedState,
      userActivity: {
        ...preloadedState.userActivity,
        loading: true,
      },
    };

    render(<UserActivityOverviewContainer isAuthorized={true} />, { preloadedState: loadingState });

    expect(screen.getByText(/Loading/)).toBeVisible(); // Loading text
  });

  it('should show error state when loadError is present', async () => {
    axiosMock.reset();
    axiosMock.onGet(/userActivity/).reply(500, 'Failed to load data');

    render(<UserActivityOverviewContainer isAuthorized={true} />, { preloadedState });

    await waitFor(
      () => {
        expect(screen.getByText(/Failed to load data/)).toBeVisible();
      },
      { timeout: 3000 }
    );
  });

  it('should show export error when exportError is present', async () => {
    const exportErrorState = {
      ...preloadedState,
      userActivity: {
        ...preloadedState.userActivity,
        exportError: 'Export failed',
      },
    };

    render(<UserActivityOverviewContainer isAuthorized={true} />, { preloadedState: exportErrorState });

    await waitFor(() => {
      expect(screen.getByText(/Export failed/)).toBeVisible();
    });
  });

  it('should open filter drawer when filter button is clicked', async () => {
    const user = userEvent.setup();
    render(<UserActivityOverviewContainer isAuthorized={true} />, { preloadedState });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Filter/ })).toBeVisible();
    });

    const filterButton = screen.getByRole('button', { name: /Filter/ });
    await user.click(filterButton);

    expect(document.querySelector('.nx-drawer')).toBeInTheDocument();
    expect(screen.getByText('Filters', { hidden: true })).toBeInTheDocument();
  });

  it('should handle search input changes', async () => {
    const user = userEvent.setup();
    render(<UserActivityOverviewContainer isAuthorized={true} />, { preloadedState });

    await waitFor(() => {
      expect(screen.getByPlaceholderText('Search by user name')).toBeVisible();
    });

    const searchInput = screen.getByPlaceholderText('Search by user name');
    await user.type(searchInput, 'test');

    expect(searchInput).toHaveValue('test');
  });

  it('should show info alert when filters are dirty', async () => {
    const dirtyFiltersState = {
      ...preloadedState,
      userActivity: {
        ...preloadedState.userActivity,
        filtersAreDirty: true,
        selectedFilters: { selectedAge: 90 },
        appliedFilters: { selectedAge: 30 },
      },
    };

    render(<UserActivityOverviewContainer isAuthorized={true} />, { preloadedState: dirtyFiltersState });

    await waitFor(() => {
      expect(screen.getByText('Please apply or revert filter to see results')).toBeVisible();
    });
  });

  it('should handle filter drawer interactions', async () => {
    const dirtyFiltersState = {
      ...preloadedState,
      userActivity: {
        ...preloadedState.userActivity,
        filterDrawerOpen: true,
        filtersAreDirty: true,
        selectedFilters: { selectedAge: 90 },
        appliedFilters: { selectedAge: 30 },
      },
    };

    render(<UserActivityOverviewContainer isAuthorized={true} />, { preloadedState: dirtyFiltersState });

    await waitFor(
      () => {
        // Drawer should be open
        expect(document.querySelector('.nx-drawer')).toBeInTheDocument();
      },
      { timeout: 3000 }
    );

    // Check that Apply and Reset buttons exist (they may be disabled initially)
    expect(screen.getByRole('button', { name: 'Apply', hidden: true })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reset', hidden: true })).toBeInTheDocument();
  });

  it('should handle age filter changes in drawer', async () => {
    const user = userEvent.setup();
    const openDrawerState = {
      ...preloadedState,
      userActivity: {
        ...preloadedState.userActivity,
        filterDrawerOpen: true,
        selectedFilters: { selectedAge: 30 },
      },
    };

    render(<UserActivityOverviewContainer isAuthorized={true} />, { preloadedState: openDrawerState });

    await waitFor(
      () => {
        expect(screen.getByText('Time Frame', { hidden: true })).toBeInTheDocument();
      },
      { timeout: 3000 }
    );

    // Find and expand the collapsible filter section
    const expandButton = screen.getByText('Time Frame', { hidden: true }).closest('button');
    await user.click(expandButton);

    await user.click(screen.getByRole('menuitemradio', { name: 'past 7 days', hidden: true }));

    // Past 7 days option should be selected
    expect(screen.getByRole('menuitemradio', { name: 'past 7 days', hidden: true })).toBeChecked();
  });

  it('should display correct user count summary', async () => {
    render(<UserActivityOverviewContainer isAuthorized={true} />, { preloadedState });

    await waitFor(() => {
      expect(screen.getByText('Showing 2 of 2 users')).toBeVisible();
    });
  });

  it('should disable export button when no users', async () => {
    const noUsersState = {
      ...preloadedState,
      userActivity: {
        ...preloadedState.userActivity,
        users: [],
        totalUsers: 0,
      },
    };

    // Mock empty response for this test
    axiosMock.reset();
    axiosMock.onGet().reply(200, {
      users: [],
      totalUsers: 0,
    });

    render(<UserActivityOverviewContainer isAuthorized={true} />, { preloadedState: noUsersState });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Export Activity/ })).toBeDisabled();
    });
  });

  it('should enable export button when users are present', async () => {
    render(<UserActivityOverviewContainer isAuthorized={true} />, { preloadedState });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Export Activity/ })).toBeEnabled();
    });
  });

  it('should have correct button styling', async () => {
    render(<UserActivityOverviewContainer isAuthorized={true} />, { preloadedState });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Export Activity/ })).toBeVisible();
    });

    const exportButton = screen.getByRole('button', { name: /Export Activity/ });
    const filterButton = screen.getByRole('button', { name: /Filter/ });

    expect(exportButton).toHaveClass('nx-btn--tertiary');
    expect(filterButton).toHaveClass('nx-btn--tertiary');
  });

  it('should show magnifying glass icon in search input', async () => {
    render(<UserActivityOverviewContainer isAuthorized={true} />, { preloadedState });

    await waitFor(() => {
      expect(screen.getByPlaceholderText('Search by user name')).toBeVisible();
    });

    const searchInput = screen.getByPlaceholderText('Search by user name');
    const searchContainer = searchInput.closest('.nx-text-input');

    expect(searchContainer).toHaveClass('nx-filter-input');
  });

  it('should render table headers correctly', async () => {
    render(<UserActivityOverviewContainer isAuthorized={true} />, { preloadedState });

    await waitFor(() => {
      expect(screen.getByRole('columnheader', { name: /Username/ })).toBeVisible();
    });

    expect(screen.getByRole('columnheader', { name: /Login Count/ })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: /Last Active/ })).toBeVisible();
  });

  it('should format last active dates correctly', async () => {
    render(<UserActivityOverviewContainer isAuthorized={true} />, { preloadedState });

    await waitFor(() => {
      // Check that dates are formatted (exact format may vary based on timezone)
      expect(screen.getByText(/Jan 01, 2023/)).toBeVisible();
    });

    expect(screen.getByText(/Jan 02, 2023/)).toBeVisible();
  });
});
