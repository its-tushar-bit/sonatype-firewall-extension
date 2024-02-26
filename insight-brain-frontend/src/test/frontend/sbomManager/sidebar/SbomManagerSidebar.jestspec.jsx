/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render } from '@testing-library/react';
import '@testing-library/jest-dom';

import SbomManagerSidebar from 'MainRoot/sbomManager/sidebar/SbomManagerSidebar';
import * as RouterStateContextModule from 'MainRoot/react/RouterStateContext';

describe('SbomManagerSidebar', () => {
  beforeEach(() => {
    const mockRouterState = {
      href: jest.fn().mockImplementation((stateName) => {
        switch (stateName) {
          case 'sbomManager':
            return '#/sbomManager/dashboard';
          case 'sbomManager.dashboard':
            return '#/sbomManager/dashboard';
          default:
            return '/mocked-default-href';
        }
      }),
      includes: jest.fn(),
      get: jest.fn(),
      router: {
        globals: {
          current: {
            name: 'sbomManager.dashboard',
          },
        },
        transitionService: {
          onSuccess: jest.fn().mockImplementation(() => jest.fn()),
        },
      },
    };
    jest.spyOn(RouterStateContextModule, 'useRouterState').mockImplementation(() => mockRouterState);
  });
  it('renders correctly when user is logged in and sbomManager is enabled', () => {
    const { getByText } = render(<SbomManagerSidebar isLoggedIn={true} isSbomManagerEnabled={true} />);
    const dashboardText = getByText('Dashboard');
    const dashboardLink = dashboardText.closest('a');
    expect(dashboardLink).toHaveClass('nx-global-sidebar__navigation-link nx-text-link selected');
    expect(dashboardLink).toHaveAttribute('href', '#/sbomManager/dashboard');
    expect(dashboardLink).toHaveTextContent('Dashboard');

    const homeIcon = dashboardLink.querySelector('svg.fa-home');
    expect(homeIcon).toBeInTheDocument();
    expect(homeIcon).toHaveAttribute('data-icon', 'home');
    expect(homeIcon).toHaveAttribute('role', 'img');
  });

  it('does not render the dashboard link when the user is not logged in', () => {
    const { queryByText } = render(<SbomManagerSidebar isLoggedIn={false} isSbomManagerEnabled={true} />);
    expect(queryByText('Dashboard')).not.toBeInTheDocument();
  });

  it('does not show sidebar when isSbomManagerEnabled is disabled', () => {
    const { queryByText } = render(<SbomManagerSidebar isLoggedIn={true} isSbomManagerEnabled={false} />);
    expect(queryByText('Dashboard')).not.toBeInTheDocument();
  });
});
