/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, render, fireEvent } from 'TestRoot/SpecUtil';
import HelpMenu from 'MainRoot/mainHeader/MenuBar/HelpMenu/HelpMenu';
import * as RouterStateContextModule from 'MainRoot/react/RouterStateContext';

describe('HelpMenu', () => {
  let renderComponent;
  beforeEach(() => {
    const defaultPreloadedState = {};

    const mockRouterState = {
      href: jest.fn().mockReturnValue('some-href)'),
      get: jest.fn(),
      includes: jest.fn(),
    };
    jest.spyOn(RouterStateContextModule, 'useRouterState').mockImplementation(() => mockRouterState);

    renderComponent = (preloadedState) =>
      render(<HelpMenu />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('renders a heading with title Support Options', () => {
    renderComponent();
    const btn = screen.getByRole('button');
    fireEvent.click(btn);
    expect(screen.getByRole('heading', { name: 'Support Options' })).toBeInTheDocument();
  });

  it('renders 3 links', () => {
    renderComponent();
    const btn = screen.getByRole('button');
    fireEvent.click(btn);
    expect(screen.getAllByRole('link').length).toBe(3);
  });

  it('renders a getting started link', () => {
    renderComponent();
    const btn = screen.getByRole('button');
    fireEvent.click(btn);

    expect(screen.getByRole('link', { name: /getting started/i })).toBeInTheDocument();
  });

  describe('online help link', () => {
    it('renders a link', () => {
      renderComponent();
      const btn = screen.getByRole('button');
      fireEvent.click(btn);

      expect(screen.getByRole('link', { name: /online help/i })).toBeInTheDocument();
    });

    it('renders a link to lifecycle documentation url by default', () => {
      renderComponent();
      const btn = screen.getByRole('button');
      fireEvent.click(btn);

      const link = screen.getByRole('link', { name: /online help/i });
      expect(link).toHaveAttribute('href', 'http://links.sonatype.com/products/clm/doc/');
    });

    it('renders a link to sbom manager documentation when isSbomManager is true', () => {
      renderComponent({ router: { currentState: { name: 'sbomManager.dashboard' } } });
      const btn = screen.getByRole('button');
      fireEvent.click(btn);

      const link = screen.getByRole('link', { name: /online help/i });
      expect(link).toHaveAttribute('href', 'http://links.sonatype.com/products/sbom/doc');
    });

    it('renders a link to sonatype developer documentation when isDeveloper is true', () => {
      renderComponent({ router: { currentState: { name: 'developer.dashboard' } } });
      const btn = screen.getByRole('button');
      fireEvent.click(btn);

      const link = screen.getByRole('link', { name: /online help/i });
      expect(link).toHaveAttribute('href', 'http://links.sonatype.com/products/nxiq/doc/sonatype-developer');
    });

    it('does not render getting started link if isDeveloper is true', () => {
      renderComponent({ router: { currentState: { name: 'developer.dashboard' } } });
      const btn = screen.getByRole('button');
      fireEvent.click(btn);

      expect(screen.queryByRole('link', { name: /getting started/i })).not.toBeInTheDocument();
    });
  });

  it('renders a request support link', () => {
    renderComponent();
    const btn = screen.getByRole('button');
    fireEvent.click(btn);

    const link = screen.getByRole('link', { name: /request support/i });

    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', 'http://links.sonatype.com/products/clm/support');
  });
});
