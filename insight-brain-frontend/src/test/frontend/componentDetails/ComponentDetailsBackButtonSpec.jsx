/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

import { render, screen } from 'TestRoot/SpecUtil';
import ComponentDetailsBackButton from 'MainRoot/componentDetails/ComponentDetailsBackButton';
import * as routerContext from 'MainRoot/react/RouterStateContext';

describe('ComponentDetailsBackButton', () => {
  let renderComponent, minimalProps, routerContextMock;

  beforeEach(() => {
    renderComponent = (additionalProps, preloadedState) =>
      render(<ComponentDetailsBackButton {...minimalProps} {...additionalProps} />, { preloadedState });
    routerContextMock = {
      href: jasmine.createSpy('href').and.returnValue('mockValue'),
      get: jasmine.createSpy('get').and.callFake((stateName) => {
        if (stateName !== 'applicationReport.policy') {
          return null;
        }
        return { data: { title: 'Application Report' } };
      }),
    };
    spyOn(routerContext, 'useRouterState').and.returnValue(routerContextMock);
  });

  it('renders `Back to Dependency Tree', () => {
    renderComponent({ scanId: 'scanId', publicId: 'testId' });

    expect(screen.getByText('Back To Dependency Tree')).toBeVisible();
  });

  it('renders `Back to Application Report', () => {
    renderComponent();

    expect(screen.getByText('Back to Application Report')).toBeVisible();
  });

  it('renders `Back to Priorities', () => {
    const routerPreloadedState = {
      router: {
        prevParams: {
          '#': null,
          publicAppId: 'testPublicAppId',
          scanId: 'testScanId',
        },
        prevState: {
          name: 'prioritiesPage',
          url: '/development/priorities/{publicAppId}/{scanId}',
          data: {
            title: 'Priorities',
          },
        },
      },
    };
    renderComponent(null, routerPreloadedState);

    const backBtn = screen.getByRole('link', { name: 'Back to Priorities' });

    expect(backBtn).toBeInTheDocument();
  });
});
