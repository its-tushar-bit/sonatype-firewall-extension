/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';

import SbomManagerSidebar from 'MainRoot/sbomManager/sidebar/SbomManagerSidebar';
import * as RouterStateContextModule from 'MainRoot/react/RouterStateContext';

describe('SbomManagerSidebar', () => {
  let renderComponent;
  beforeEach(() => {
    const defaultPreloadedState = {
      router: {
        currentState: {
          name: 'sbomManager.dashboard',
        },
      },
    };

    const mockRouterState = {
      href: jest.fn().mockImplementation((stateName) => {
        switch (stateName) {
          case 'sbomManager.dashboard':
            return '#/sbomManager/dashboard';
          case 'sbomManager.applications':
            return '#/sbomManager/applications';
          case 'sbomManager.management.view':
            return '#/sbomManager/management/view';
          case 'sbomManager.management.tree':
            return '#/sbomManager/management/tree';
          case 'sbomManager.advancedSearch':
            return '#/sbomManager/advancedSearch';
          case 'sbomManager.legal.dashboard':
            return '#/sbomManager/legal/dashboard';
          case 'sbomManager.api':
            return '#/sbomManager/api';
          default:
            return '/mocked-default-href';
        }
      }),
    };
    jest.spyOn(RouterStateContextModule, 'useRouterState').mockImplementation(() => mockRouterState);

    const props = { isLoggedIn: true, isSbomManagerEnabled: true };
    renderComponent = (additionalProps, preloadedState) =>
      render(<SbomManagerSidebar {...props} {...additionalProps} />, {
        preloadedState: { ...defaultPreloadedState, ...preloadedState },
      });
  });

  const verifyLinks = (expectedLinks) => {
    const sidebarLinks = screen.getAllByRole('link');
    expect(sidebarLinks.length).toBe(expectedLinks.length);
    expectedLinks.forEach((link, index) => {
      expect(sidebarLinks[index]).toHaveTextContent(link.text);
      expect(sidebarLinks[index]).toHaveAttribute('href', link.href);
    });
  };

  it('renders correctly when user is logged in and sbomManager is enabled', () => {
    renderComponent();
    verifyLinks([
      { text: 'Dashboard', href: '#/sbomManager/dashboard' },
      { text: 'Applications', href: '#/sbomManager/applications' },
      { text: 'Organizations', href: '#/sbomManager/management/view' },
      { text: 'Advanced Search', href: '#/sbomManager/advancedSearch' },
    ]);
  });

  it('does not render the api link when isApiPageEnabled is false', () => {
    renderComponent({ isApiPageEnabled: false });
    verifyLinks([
      { text: 'Dashboard', href: '#/sbomManager/dashboard' },
      { text: 'Applications', href: '#/sbomManager/applications' },
      { text: 'Organizations', href: '#/sbomManager/management/view' },
      { text: 'Advanced Search', href: '#/sbomManager/advancedSearch' },
    ]);
  });

  it('does render the api link when isApiPageEnabled is true', () => {
    renderComponent({ isApiPageEnabled: true });
    verifyLinks([
      { text: 'Dashboard', href: '#/sbomManager/dashboard' },
      { text: 'Applications', href: '#/sbomManager/applications' },
      { text: 'Organizations', href: '#/sbomManager/management/view' },
      { text: 'Advanced Search', href: '#/sbomManager/advancedSearch' },
      { text: 'API', href: '#/sbomManager/api' },
    ]);
  });

  it('does not render the legal link when isAlpForSbomManagerEnabled is false and isLegalEnabled is false', () => {
    renderComponent({ isAlpForSbomManagerEnabled: false, isLegalEnabled: false });
    verifyLinks([
      { text: 'Dashboard', href: '#/sbomManager/dashboard' },
      { text: 'Applications', href: '#/sbomManager/applications' },
      { text: 'Organizations', href: '#/sbomManager/management/view' },
      { text: 'Advanced Search', href: '#/sbomManager/advancedSearch' },
    ]);
  });

  it('does not render the legal link when isAlpForSbomManagerEnabled is false and isLegalEnabled is true', () => {
    renderComponent({ isAlpForSbomManagerEnabled: false, isLegalEnabled: true });
    verifyLinks([
      { text: 'Dashboard', href: '#/sbomManager/dashboard' },
      { text: 'Applications', href: '#/sbomManager/applications' },
      { text: 'Organizations', href: '#/sbomManager/management/view' },
      { text: 'Advanced Search', href: '#/sbomManager/advancedSearch' },
    ]);
  });

  it('does render the legal link when isAlpForSbomManagerEnabled is true and isLegalEnabled is true', () => {
    renderComponent({ isAlpForSbomManagerEnabled: true, isLegalEnabled: true });
    verifyLinks([
      { text: 'Dashboard', href: '#/sbomManager/dashboard' },
      { text: 'Applications', href: '#/sbomManager/applications' },
      { text: 'Organizations', href: '#/sbomManager/management/view' },
      { text: 'Advanced Search', href: '#/sbomManager/advancedSearch' },
      { text: 'Legal', href: '#/sbomManager/legal/dashboard' },
    ]);
  });

  it('does not render the legal link when isAlpForSbomManagerEnabled is true and isLegalEnabled is false', () => {
    renderComponent({ isAlpForSbomManagerEnabled: true, isLegalEnabled: false });
    verifyLinks([
      { text: 'Dashboard', href: '#/sbomManager/dashboard' },
      { text: 'Applications', href: '#/sbomManager/applications' },
      { text: 'Organizations', href: '#/sbomManager/management/view' },
      { text: 'Advanced Search', href: '#/sbomManager/advancedSearch' },
    ]);
  });
});
