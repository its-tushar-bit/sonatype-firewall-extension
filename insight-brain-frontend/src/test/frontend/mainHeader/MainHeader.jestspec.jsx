/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from 'TestRoot/SpecUtil';
import { MainHeader } from 'MainRoot/mainHeader/MainHeader';
import * as userSessionSlice from 'MainRoot/user/userSessionSlice';
import * as routeStateUtilService from 'MainRoot/utility/services/routeStateUtilService';
import * as permissionService from 'MainRoot/util/permissionService';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';

describe('MainHeader', () => {
  let fetchUserSessionSpy, getValidPermissionsSpy, routerContextMock;

  const defaultState = {
    userSession: {
      data: null,
      loading: false,
      error: null,
      shouldDisplayPasswordWarning: false,
    },
    productFeatures: {
      productFeatures: {
        'webhooks-for-applications': true,
        automation: true,
      },
    },
    router: {
      currentParams: {},
      currentState: { name: 'dashboard.overview.violations' },
    },
    mainHeader: {
      permissions: {},
      shouldShowLoginButton: false,
      loading: false,
      loadError: null,
    },
  };

  beforeEach(() => {
    fetchUserSessionSpy = jest
      .spyOn(userSessionSlice.actions, 'fetchUserSession')
      .mockReturnValue(() => Promise.resolve());
    getValidPermissionsSpy = jest.spyOn(permissionService, 'getValidPermissions').mockResolvedValue([]);
    jest.spyOn(routeStateUtilService, 'stateRequiresAuthentication').mockResolvedValue(true);

    // Mock router context that MenuBar needs
    routerContextMock = {
      href: jest.fn((stateName) => `#/${stateName}`),
      get: jest.fn(),
      includes: jest.fn(),
    };
    jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue(routerContextMock);
  });

  const renderComponent = (state = defaultState, props = {}) => {
    return render(<MainHeader clmServerVersion="1.234.5" {...props} />, {
      preloadedState: state,
    });
  };

  it('renders the header', () => {
    renderComponent();

    // MenuBar should render - verify by checking for header landmark
    expect(screen.getByRole('banner')).toBeInTheDocument();
  });

  describe('login status', () => {
    it('does not show user menu when not authenticated', () => {
      renderComponent();

      // UserMenu should not be visible when not logged in
      // The actual user menu dropdown is only shown when logged in
      expect(screen.queryByRole('button', { name: /user/i })).not.toBeInTheDocument();
    });

    it('shows user menu when authenticated', () => {
      const stateWithUser = {
        ...defaultState,
        userSession: {
          ...defaultState.userSession,
          data: { username: 'testuser', displayName: 'Test User' },
        },
      };

      renderComponent(stateWithUser);

      // When logged in, there should be multiple buttons (user menu, help, system preferences, etc)
      // The presence of multiple buttons indicates the full menu bar is rendered (not just login)
      const buttons = screen.getAllByRole('button');
      expect(buttons.length).toBeGreaterThan(1);
    });
  });

  describe('login button', () => {
    it('shows login button when not authenticated and state does not require auth', async () => {
      jest.spyOn(routeStateUtilService, 'stateRequiresAuthentication').mockResolvedValue(false);
      const stateWithLoginButton = {
        ...defaultState,
        mainHeader: {
          ...defaultState.mainHeader,
          shouldShowLoginButton: true,
        },
      };
      renderComponent(stateWithLoginButton);

      await waitFor(() => {
        // LoginButton renders "Sign in" text
        expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
      });
    });

    it('does not show login button when authenticated', async () => {
      const stateWithUser = {
        ...defaultState,
        userSession: {
          ...defaultState.userSession,
          data: { username: 'testuser' },
        },
      };

      jest.spyOn(routeStateUtilService, 'stateRequiresAuthentication').mockResolvedValue(false);
      renderComponent(stateWithUser);

      await waitFor(() => {
        expect(screen.queryByRole('button', { name: /sign in/i })).not.toBeInTheDocument();
      });
    });

    it('does not show login button when state requires authentication', async () => {
      jest.spyOn(routeStateUtilService, 'stateRequiresAuthentication').mockResolvedValue(true);
      renderComponent();

      await waitFor(() => {
        expect(screen.queryByRole('button', { name: /sign in/i })).not.toBeInTheDocument();
      });
    });

    it('calls fetchUserSession when login button is clicked', async () => {
      jest.spyOn(routeStateUtilService, 'stateRequiresAuthentication').mockResolvedValue(false);
      renderComponent();

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
      });

      const loginButton = screen.getByRole('button', { name: /sign in/i });
      loginButton.click();

      expect(fetchUserSessionSpy).toHaveBeenCalledWith(true);
    });
  });

  describe('permissions', () => {
    it('does not load permissions until after login', () => {
      renderComponent();

      expect(getValidPermissionsSpy).not.toHaveBeenCalled();
    });

    it('loads permissions after login', async () => {
      getValidPermissionsSpy.mockResolvedValue(['CONFIGURE_SYSTEM', 'MANAGE_PROPRIETARY']);

      const stateWithUser = {
        ...defaultState,
        userSession: {
          ...defaultState.userSession,
          data: { username: 'testuser' },
        },
      };

      renderComponent(stateWithUser);

      await waitFor(() => {
        expect(getValidPermissionsSpy).toHaveBeenCalledWith([
          'CONFIGURE_SYSTEM',
          'MANAGE_PROPRIETARY',
          'VIEW_ROLES',
          'MANAGE_AUTOMATIC_APPLICATION_CREATION',
          'MANAGE_AUTOMATIC_SCM_CONFIGURATION',
        ]);
      });
    });

    it('handles permission loading errors gracefully', async () => {
      getValidPermissionsSpy.mockRejectedValue('Permission API error');

      const stateWithUser = {
        ...defaultState,
        userSession: {
          ...defaultState.userSession,
          data: { username: 'testuser' },
        },
      };

      const { store } = renderComponent(stateWithUser);

      await waitFor(() => {
        const state = store.getState();
        expect(state.mainHeader.loadError).toBe('Permission API error');
      });
    });
  });
});
