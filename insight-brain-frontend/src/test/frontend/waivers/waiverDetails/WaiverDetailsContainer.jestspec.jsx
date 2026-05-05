/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import WaiverDetailsContainer from 'MainRoot/waivers/waiverDetails/WaiverDetailsContainer';
import { screen, render } from 'TestRoot/SpecUtil';
import { set, lensPath } from 'ramda';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';

describe('WaiverDetailsContainer', () => {
  let renderComponent, routerContextMock;

  const defaultPreloadedState = {
    productFeatures: {
      productFeatures: { 'auto-waiver-management': true },
    },
    router: {
      currentState: {
        name: 'waiver.details',
      },
      currentParams: { type: 'autoWaiver' },
    },
  };

  beforeEach(() => {
    routerContextMock = {
      href: jest.fn('href').mockImplementation((stateName) => {
        if (stateName.includes('firewall')) {
          return '#/firewall/dashboard/components/waivers';
        }
        return '#/dashboard/waivers';
      }),
      get: jest.fn('get').mockImplementation((state) => state),
      includes: jest.fn(),
    };

    jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue(routerContextMock);

    renderComponent = (preloadedState) =>
      render(<WaiverDetailsContainer />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('should render AutoWaiverDetails when type is autoWaiver', () => {
    renderComponent();
    expect(screen.getByTestId('auto-waiver-details')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Auto-Waiver Details' })).toBeInTheDocument();
  });

  it('should render AutoWaiverDetails when type is autoWaiver on FIREWALL_WAIVER_DETAILS route', () => {
    const firewallAutoWaiverState = set(
      lensPath(['router', 'currentState', 'name']),
      'firewall.waiver.details',
      defaultPreloadedState
    );

    renderComponent(firewallAutoWaiverState);
    expect(screen.getByTestId('auto-waiver-details')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Auto-Waiver Details' })).toBeInTheDocument();
  });

  it('should render WaiverDetails when type is not autoWaiver', () => {
    const typeLens = lensPath(['router', 'currentParams', 'type']);
    const newState = set(typeLens, 'waiver', defaultPreloadedState);

    renderComponent(newState);
    expect(screen.getByTestId('waiver-details-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Waiver Detail View' })).toBeInTheDocument();
  });

  it('renders a back button with correct href for lifecycle', () => {
    renderComponent();
    const backButton = screen.getByRole('link');
    expect(backButton).toBeInTheDocument();
    expect(backButton).toHaveAttribute('href', '#/dashboard/waivers');
  });

  it('renders a back button with correct href for firewall', () => {
    const firewallPreloadedState = set(
      lensPath(['router', 'currentState', 'name']),
      'firewall.firewallPage.components.waivers',
      defaultPreloadedState
    );

    renderComponent(firewallPreloadedState);
    const backButton = screen.getByRole('link');
    expect(backButton).toBeInTheDocument();
    expect(backButton).toHaveAttribute('href', '#/firewall/dashboard/components/waivers');
  });
});
