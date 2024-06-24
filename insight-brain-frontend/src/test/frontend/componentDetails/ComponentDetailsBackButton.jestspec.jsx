/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

import { render, screen, setupPortalContainer, removePortalContainer } from 'TestRoot/SpecUtil';
import ComponentDetailsBackButton from 'MainRoot/componentDetails/ComponentDetailsBackButton';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import * as RouterSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

describe('ComponentDetailsBackButton', () => {
  let renderComponent, minimalProps, routerContextMock, selectIsPrioritiesPageContainerSpy;

  beforeEach(() => {
    setupPortalContainer();

    renderComponent = (additionalProps, preloadedState) =>
      render(<ComponentDetailsBackButton {...minimalProps} {...additionalProps} />, { preloadedState });

    routerContextMock = {
      href: jest.fn('href').mockReturnValue('href'),
      get: jest.fn('get').mockImplementation((stateName) => {
        if (stateName === 'applicationReport.policy') {
          return { data: { title: 'Application Report' } };
        }
        return stateName;
      }),
    };

    jest.spyOn(routerContext, 'useRouterState').mockReturnValue(routerContextMock);

    selectIsPrioritiesPageContainerSpy = jest
      .spyOn(RouterSelectors, 'selectIsPrioritiesPageContainer')
      .mockReturnValue(false);
  });

  afterEach(() => removePortalContainer());

  it('renders "Back to Dependency Tree" if scanId and publicId props are provided', () => {
    renderComponent({ scanId: 'scanId', publicId: 'testId' });

    expect(screen.getByRole('link', { name: 'Back To Dependency Tree' })).toBeInTheDocument();
  });

  describe('if rendered within prioritiesPageContainer', () => {
    beforeEach(() => {
      selectIsPrioritiesPageContainerSpy.mockReturnValue(true);
    });

    it('renders "Back to Priorities" if navigated Developer Dashboard -> Priorities Page -> Component Details', () => {
      const routerPreloadedState = {
        router: {
          currentState: {
            name: 'appReportPageWithinPrioritiesPageContainerFromDashboard.componentDetails.overview',
          },
          currentParams: {
            publicId: 'testPublicAppId',
            scanId: 'testScanId',
          },
        },
      };
      renderComponent(null, routerPreloadedState);

      const backBtn = screen.getByRole('link', { name: 'Back to Priorities' });

      expect(backBtn).toBeInTheDocument();
    });

    it('renders "Back to Application Report" if navigated from App Report -> Priorities Page -> View Full Report -> Component Details', () => {
      const routerPreloadedState = {
        router: {
          currentState: {
            name: 'appReportPageWithinPrioritiesPageContainerFromAppReport.componentDetailsFromReport.overview',
          },
          currentParams: {
            publicId: 'testPublicAppId',
            scanId: 'testScanId',
          },
        },
      };
      renderComponent(null, routerPreloadedState);

      const backBtn = screen.getByRole('link', { name: 'Back to Application Report' });

      expect(backBtn).toBeInTheDocument();
    });
  });

  it('renders "Back to Application Report" by default', () => {
    renderComponent();

    expect(screen.getByRole('link', { name: 'Back to Application Report' })).toBeInTheDocument();
  });
});
