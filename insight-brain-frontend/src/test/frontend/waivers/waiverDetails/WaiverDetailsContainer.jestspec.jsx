/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import WaiverDetailsContainer from 'MainRoot/waivers/waiverDetails/WaiverDetailsContainer';
import { screen, render } from 'TestRoot/SpecUtil';
import { set, lensPath } from 'ramda';

describe('WaiverDetailsContainer', () => {
  let renderComponent;

  const defaultPreloadedState = {
    router: {
      currentState: {
        name: 'waiver.details',
      },
      currentParams: { type: 'autoWaiver' },
    },
  };

  beforeEach(() => {
    renderComponent = (preloadedState) =>
      render(<WaiverDetailsContainer />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('should render AutoWaiverDetails when type is autoWaiver', () => {
    renderComponent();
    expect(screen.getByTestId('auto-waiver-details-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Auto-Waiver Details' })).toBeInTheDocument();
  });

  it('should render WaiverDetails when type is not autoWaiver', () => {
    const typeLens = lensPath(['router', 'currentParams', 'type']);
    const newState = set(typeLens, 'waiver', defaultPreloadedState);

    renderComponent(newState);
    expect(screen.getByTestId('waiver-details-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Waiver Detail View' })).toBeInTheDocument();
  });
});
