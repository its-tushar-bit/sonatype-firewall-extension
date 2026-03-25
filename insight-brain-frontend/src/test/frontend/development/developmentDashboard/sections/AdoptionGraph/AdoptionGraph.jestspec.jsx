/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, within, axiosMockAdapter } from 'TestRoot/SpecUtil';
import AdoptionGraph from 'MainRoot/development/developmentDashboard/sections/Graphs/AdoptionGraph';
import { fireEvent } from '@testing-library/react';
import { getDeveloperDashboardGraphsData } from 'MainRoot/util/CLMLocation';

describe('AdoptionGraph', () => {
  let axiosMock, renderComponent;

  const defaultPreloadedState = {
    integrations: {
      developerDashboardGraphs: {
        graphData: [
          {
            dateTimeMillis: 1701350755891,
            totalNumberOfApps: 8,
            totalNumberOfAppsUsingCiCd: 2,
            totalNumberOfAppsWithScmEnabled: 4,
          },
          {
            dateTimeMillis: 1701955555891,
            totalNumberOfApps: 8,
            totalNumberOfAppsUsingCiCd: 2,
            totalNumberOfAppsWithScmEnabled: 5,
          },
        ],
        loading: false,
        loadError: null,
      },
    },
  };

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
    renderComponent = (preloadedState) =>
      render(<AdoptionGraph />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('should render a Loading... message when network call is pending', () => {
    const loadingState = {
      integrations: {
        developerDashboardGraphs: {
          graphData: null,
          loading: true,
          loadError: null,
        },
      },
    };

    renderComponent(loadingState);

    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  it('should render an error message when network call is failed', async () => {
    const errorState = {
      integrations: {
        developerDashboardGraphs: {
          graphData: null,
          loading: false,
          loadError: 'error',
        },
      },
    };

    renderComponent(errorState);

    const errorAlert = await screen.findByRole('alert');
    expect(errorAlert).toBeInTheDocument();
  });

  it('should trigger correct network request when retry button of alert is clicked', async () => {
    axiosMock.onGet(getDeveloperDashboardGraphsData()).reply(404, 'Error');

    const errorState = {
      integrations: {
        developerDashboardGraphs: {
          graphData: null,
          loading: false,
          loadError: 'error',
        },
      },
    };

    renderComponent(errorState);

    expect(axiosMock.history.get.length).toBe(0);

    const errorAlert = await screen.findByRole('alert');
    expect(errorAlert).toBeInTheDocument();

    const retryBytton = within(errorAlert).getByRole('button');
    fireEvent.click(retryBytton);

    expect(axiosMock.history.get.length).toBe(1);

    const request = axiosMock.history.get[0];

    expect(request.url).toBe(getDeveloperDashboardGraphsData());
    expect(request.params).toBe(undefined);
  });

  it('should render a graph', async () => {
    renderComponent();

    expect(screen.getByRole('heading', { name: /adoption profile/i })).toBeInTheDocument();
    expect(screen.getByTestId('auto-sizer')).toBeInTheDocument();
  });
});
