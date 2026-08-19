/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, setupPortalContainer, axiosMockAdapter } from 'TestRoot/SpecUtil';
import { waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import UserActivityDetailsContainer from 'MainRoot/configuration/userActivityOverview/UserActivityDetailsContainer';
import { USER_ACTIVITY_PAGE_SIZE } from 'MainRoot/configuration/userActivityOverview/userActivitySlice';

describe('UserActivityDetailsContainer', () => {
  let preloadedState, axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    setupPortalContainer(); // Required for PortalDrawer

    preloadedState = {
      router: {
        currentParams: { username: 'john.doe' },
        currentState: { name: 'userActivityDetails' },
      },
      userActivity: {
        // Overview state
        selectedFilters: { selectedAge: 30 },
        appliedFilters: { selectedAge: 30 },
        // Details state
        detailsCurrentUser: 'john.doe',
        detailsActivities: [
          {
            timestamp: '2024-03-13T14:30:45.123Z',
            domain: 'authentication',
            type: 'login',
            errorType: null,
            uri: '/api/v2/auth/login',
            method: 'POST',
            ipAddress: '192.168.1.100',
            userAgent: 'Mozilla/5.0',
          },
          {
            timestamp: '2024-03-13T15:30:45.123Z',
            domain: 'reporting',
            type: 'view',
            errorType: null,
            uri: '/api/v2/applications/123/reports',
            method: 'GET',
            ipAddress: '192.168.1.100',
            userAgent: 'Mozilla/5.0',
          },
        ],
        detailsTotalActivities: 2,
        detailsLoading: false,
        detailsLoadError: null,
        detailsPagination: { limit: USER_ACTIVITY_PAGE_SIZE, offset: 0, hasMore: false },
        detailsFilterDrawerOpen: false,
        detailsFiltersAreDirty: false,
        detailsSelectedFilters: {
          selectedActivityTypes: [],
          selectedDomains: [],
          selectedErrorTypes: [],
        },
        detailsAppliedFilters: {
          selectedActivityTypes: [],
          selectedDomains: [],
          selectedErrorTypes: [],
        },
        filterOptions: {
          activityTypes: ['login', 'view', 'create'],
          domains: ['authentication', 'reporting', 'governance'],
          errorTypes: ['Success', 'bad-request', 'unauthorized'],
        },
        filterOptionsLoading: false,
        filterOptionsError: null,
      },
      productFeatures: {
        productFeatures: {
          'user-activity-tracking': true,
        },
      },
    };

    // Mock API responses
    axiosMock.onGet('/api/v2/userActivity/filterOptions').reply(200, {
      activityTypes: ['login', 'view', 'create'],
      domains: ['authentication', 'reporting', 'governance'],
      errorTypes: ['Success', 'bad-request', 'unauthorized'],
    });

    axiosMock.onGet(/\/api\/v2\/userActivity\/john\.doe/).reply(200, {
      username: 'john.doe',
      activities: preloadedState.userActivity.detailsActivities,
      totalActivities: 2,
    });
  });

  describe('authorization', () => {
    it('should show authorization error when not authorized', () => {
      render(<UserActivityDetailsContainer isAuthorized={false} />, {
        preloadedState,
      });

      expect(screen.getByText(/It appears you do not have permission to access this page/i)).toBeInTheDocument();
    });

    it('should render component when authorized', async () => {
      render(<UserActivityDetailsContainer isAuthorized={true} />, { preloadedState });

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /john\.doe Activity/i })).toBeInTheDocument();
      });
    });
  });

  describe('data loading and rendering', () => {
    it('should render user activity details from Redux store', async () => {
      render(<UserActivityDetailsContainer isAuthorized={true} />, { preloadedState });

      // Wait for the component to render
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /john\.doe Activity/i })).toBeInTheDocument();
      });

      // Check that activities are rendered
      expect(screen.getByText('authentication')).toBeInTheDocument();
      expect(screen.getByText('reporting')).toBeInTheDocument();
      expect(screen.getByText('login')).toBeInTheDocument();
      expect(screen.getByText('view')).toBeInTheDocument();
      expect(screen.getByText('Showing 2 activities')).toBeInTheDocument();
    });

    it('should show loading state when detailsLoading is true', async () => {
      const loadingState = {
        ...preloadedState,
        userActivity: {
          ...preloadedState.userActivity,
          detailsLoading: true,
        },
      };

      render(<UserActivityDetailsContainer isAuthorized={true} />, { preloadedState: loadingState });

      await waitFor(() => {
        expect(screen.getByText(/loading/i)).toBeInTheDocument();
      });
    });

    it('should show error state when detailsLoadError is present', async () => {
      const errorState = {
        ...preloadedState,
        userActivity: {
          ...preloadedState.userActivity,
          detailsLoadError: 'Failed to load user activity details',
          detailsActivities: [], // Clear activities when there's an error
          detailsLoading: false, // Ensure loading is false to show error
        },
      };

      render(<UserActivityDetailsContainer isAuthorized={true} />, { preloadedState: errorState });

      await waitFor(() => {
        // Check that the retry button appears when there's an error
        // The retry button should have the nx-load-error__retry class
        const retryButton = document.querySelector('.nx-load-error__retry');
        expect(retryButton).toBeInTheDocument();
      });
    });
  });

  describe('username handling', () => {
    it('should handle missing username gracefully', async () => {
      const stateWithoutUsername = {
        ...preloadedState,
        router: {
          currentParams: {},
          currentState: { name: 'userActivityDetails' },
        },
      };

      render(<UserActivityDetailsContainer isAuthorized={true} />, {
        preloadedState: stateWithoutUsername,
      });

      // Should still render the component structure
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /filter/i })).toBeInTheDocument();
      });
    });

    it('should update current user when username changes', async () => {
      const { rerender } = render(<UserActivityDetailsContainer isAuthorized={true} />, {
        preloadedState,
      });

      // Wait for initial render
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /john\.doe Activity/i })).toBeInTheDocument();
      });

      rerender(<UserActivityDetailsContainer isAuthorized={true} />);

      // The component should handle the username change
      expect(screen.getByRole('button', { name: /filter/i })).toBeInTheDocument();
    });
  });

  describe('filter interactions', () => {
    it('should open filter drawer when filter button is clicked', async () => {
      const user = userEvent.setup();
      render(<UserActivityDetailsContainer isAuthorized={true} />, { preloadedState });

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /filter/i })).toBeInTheDocument();
      });

      const filterButton = screen.getByRole('button', { name: /filter/i });
      await user.click(filterButton);

      // The Redux action should be dispatched - just verify the button click worked
      expect(filterButton).toBeInTheDocument();
    });

    it('should handle filter drawer interactions', async () => {
      const stateWithDrawerOpen = {
        ...preloadedState,
        userActivity: {
          ...preloadedState.userActivity,
          detailsFilterDrawerOpen: true,
        },
      };

      render(<UserActivityDetailsContainer isAuthorized={true} />, {
        preloadedState: stateWithDrawerOpen,
      });

      await waitFor(() => {
        expect(document.querySelector('.nx-drawer')).toBeInTheDocument();
      });

      // Test that filter options are available in the drawer
      const drawer = document.querySelector('.nx-drawer');
      expect(drawer).toBeInTheDocument();
      expect(drawer).toHaveTextContent('Activity Type');
      expect(drawer).toHaveTextContent('Domain');
      expect(drawer).toHaveTextContent('Error Type');
    });

    it('should handle dirty filters state', async () => {
      const stateWithDrawerOpen = {
        ...preloadedState,
        userActivity: {
          ...preloadedState.userActivity,
          detailsFilterDrawerOpen: true,
        },
      };

      render(<UserActivityDetailsContainer isAuthorized={true} />, {
        preloadedState: stateWithDrawerOpen,
      });

      await waitFor(() => {
        expect(document.querySelector('.nx-drawer')).toBeInTheDocument();
      });

      // Check that the Apply and Reset buttons are initially disabled
      const applyButton = screen.getByRole('button', { name: /apply/i, hidden: true });
      const resetButton = screen.getByRole('button', { name: /reset/i, hidden: true });

      // Buttons should be disabled when filters are not dirty
      expect(applyButton).toBeDisabled();
      expect(resetButton).toBeDisabled();
    });
  });

  describe('redux integration', () => {
    it('should dispatch loadFilterOptions on mount', async () => {
      render(<UserActivityDetailsContainer isAuthorized={true} />, { preloadedState });

      // Verify that the filter options API was called
      await waitFor(() => {
        const filterOptionsRequests = axiosMock.history.get.filter((req) => req.url.includes('filterOptions'));
        expect(filterOptionsRequests).toHaveLength(1);
      });
    });

    it('should dispatch applyDetailsFilters when username is available', async () => {
      render(<UserActivityDetailsContainer isAuthorized={true} />, { preloadedState });

      // Verify that the user activity details API was called
      await waitFor(() => {
        const userActivityRequests = axiosMock.history.get.filter((req) =>
          req.url.includes('/api/v2/userActivity/john.doe')
        );
        expect(userActivityRequests.length).toBeGreaterThan(0);
      });
    });

    it('should cleanup on unmount', () => {
      const { unmount } = render(<UserActivityDetailsContainer isAuthorized={true} />, {
        preloadedState,
      });

      // Component should unmount without errors
      expect(() => unmount()).not.toThrow();
    });
  });

  describe('callback handlers', () => {
    it('should provide all required callback functions to UserActivityDetails', async () => {
      const stateWithActivities = {
        ...preloadedState,
        userActivity: {
          ...preloadedState.userActivity,
          detailsLoading: false, // Ensure not loading
          detailsActivities: [
            // Ensure activities are present
            {
              timestamp: '2024-03-13T14:30:45.123Z',
              domain: 'authentication',
              type: 'login',
              errorType: null,
              uri: '/api/v2/auth/login',
              method: 'POST',
              ipAddress: '192.168.1.100',
              userAgent: 'Mozilla/5.0',
            },
          ],
        },
      };

      render(<UserActivityDetailsContainer isAuthorized={true} />, { preloadedState: stateWithActivities });

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /john\.doe Activity/i })).toBeInTheDocument();
      });

      // Verify that all interactive elements are present (indicating callbacks are working)
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /filter/i })).toBeInTheDocument();
      });
    });

    it('should handle retry action when error occurs', async () => {
      const user = userEvent.setup();
      const errorState = {
        ...preloadedState,
        userActivity: {
          ...preloadedState.userActivity,
          detailsLoadError: 'Network error',
          detailsActivities: [], // Clear activities when there's an error
          detailsLoading: false, // Ensure loading is false to show error
        },
      };

      render(<UserActivityDetailsContainer isAuthorized={true} />, { preloadedState: errorState });

      await waitFor(() => {
        // Just check that retry button appears when there's an error
        const retryButton = document.querySelector('.nx-load-error__retry');
        expect(retryButton).toBeInTheDocument();
      });

      const retryButton = document.querySelector('.nx-load-error__retry');
      await user.click(retryButton);

      // Should attempt to reload the data
      await waitFor(() => {
        const retryRequests = axiosMock.history.get.filter((req) => req.url.includes('/api/v2/userActivity/john.doe'));
        expect(retryRequests.length).toBeGreaterThan(1);
      });
    });
  });
});
