/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, within } from 'TestRoot/SpecUtil';
import MenuBar from 'MainRoot/mainHeader/MenuBar/MenuBar';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';

jest.mock('MainRoot/mainHeader/MenuBar/SystemPreferencesMenu/SystemPreferencesMenu', () => {
  const SystemPreferencesMenu = () => {
    return <div>SystemPreferencesMenu</div>;
  };

  return SystemPreferencesMenu;
});

jest.mock('MainRoot/mainHeader/MenuBar/LoginButton/LoginButton', () => {
  const LoginButton = () => {
    return <div>LoginButton</div>;
  };

  return LoginButton;
});

jest.mock('MainRoot/mainHeader/MenuBar/UserMenu/UserMenuContainer', () => {
  const UserMenu = () => {
    return <div>UserMenu</div>;
  };

  return UserMenu;
});

jest.mock('MainRoot/mainHeader/MenuBar/NotificationsMenu/NotificationsMenuContainer', () => {
  const NotificationsMenuContainer = () => {
    return <div>NotificationsMenuContainer</div>;
  };

  return NotificationsMenuContainer;
});

jest.mock('MainRoot/mainHeader/MenuBar/SolutionSwitcherContainer/SolutionSwitcherContainer', () => {
  const SolutionSwitcherContainer = () => {
    return <div>SolutionSwitcherContainer</div>;
  };

  return SolutionSwitcherContainer;
});

jest.mock('MainRoot/mainHeader/MenuBar/SystemPreferencesMenu/SystemPreferencesMenu', () => {
  const SystemPreferencesMenu = () => {
    return <div>SystemPreferencesMenu</div>;
  };

  return SystemPreferencesMenu;
});

describe('MenuBar', () => {
  let renderComponent, hrefSpy, getSpy;

  const minimalProps = {
    userActions: {
      loadUser: jest.fn(),
      logout: jest.fn(),
      changePassword: jest.fn(),
      resetPasswordStatus: jest.fn(),
    },
    permissions: { testPermissions: true },
    login: jest.fn(),
    isLoggedIn: true,
    shouldShowLoginButton: true,
    isShowNotificationMenuEnabled: true,
  };

  beforeEach(() => {
    renderComponent = (props) => {
      render(<MenuBar {...props} {...minimalProps} />);
    };

    hrefSpy = jest.fn('href').mockImplementation((stateName) => stateName);
    getSpy = jest.fn('get').mockImplementation((state) => state);
    const routerContextMock = { href: hrefSpy, get: getSpy };
    jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue(routerContextMock);
  });

  it('renders UserMenu', () => {
    renderComponent();
    const userMenu = screen.getByText('UserMenu');
    expect(userMenu).toBeInTheDocument();
  });

  it('renders SystemPreferencesMenu', () => {
    renderComponent();
    const systemPreferencesMenu = screen.getByText('SystemPreferencesMenu');
    expect(systemPreferencesMenu).toBeInTheDocument();
  });

  it('renders NotificationsMenuContainer', () => {
    renderComponent();
    const notificationsMenu = screen.getByText('NotificationsMenuContainer');
    expect(notificationsMenu).toBeInTheDocument();
  });

  it('renders SolutionSwitcherContainer', () => {
    renderComponent();
    const solutionSwitcher = screen.getByText('SolutionSwitcherContainer');
    expect(solutionSwitcher).toBeInTheDocument();
  });

  describe('product logo and homeHref', () => {
    it('renders correct logo and homeHref for developer', () => {
      renderComponent({ isStandaloneDeveloper: true });

      const link = screen.getByRole('link');
      expect(link).toHaveProperty('href', 'http://localhost/developer.dashboard');

      // There are 2 images, one for light and one for dark mode. Pick the first one.
      const logo = within(link).getAllByRole('img')[0];
      expect(logo).toHaveAttribute('alt', 'sonatype developer');
    });

    it('renders correct logo and homeHref for firewall', () => {
      renderComponent({ isStandaloneFirewall: true });

      const link = screen.getByRole('link');
      expect(link).toHaveProperty('href', 'http://localhost/firewall.firewallPage');

      const logo = within(link).getAllByRole('img')[0];
      expect(logo).toHaveAttribute('alt', 'sonatype firewall');
    });

    it('renders correct logo and homeHref for sbomManager', () => {
      renderComponent({ isStandaloneSbomManager: true });

      const link = screen.getByRole('link');
      expect(link).toHaveProperty('href', 'http://localhost/sbomManager.dashboard');

      const logo = within(link).getAllByRole('img')[0];
      expect(logo).toHaveAttribute('alt', 'sonatype sbom manager');
    });

    it('renders correct logo and homeHref for lifecycle (default)', () => {
      renderComponent();

      const link = screen.getByRole('link');
      expect(link).toHaveProperty('href', 'http://localhost/dashboard.overview.violations');

      const logo = within(link).getAllByRole('img')[0];
      expect(logo).toHaveAttribute('alt', 'Lifecycle');
    });
  });
});
