/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, axiosMockAdapter, within } from 'TestRoot/SpecUtil';
import { getDeveloperDashboardGraphsData } from 'MainRoot/util/CLMLocation';
import MTTRGraph from 'MainRoot/development/developmentDashboard/sections/Graphs/MTTRGraph';
import { fireEvent } from '@testing-library/react';

describe('MTTRGraph', () => {
  let axiosMock, renderComponent;

  const defaultPreloadedState = {
    integrations: {
      developerDashboardGraphs: {
        graphData: [
          {
            dateTimeMillis: 1701350755925,
            totalNumberOfApps: 8,
            totalNumberOfAppsWithScmEnabled: 3,
            totalNumberOfPolicyActionFailuresByAppCount: 7,
            totalNumberOfWaivers: 16,
            meanTimeToRemediateMs: 900000000,
            totalNumberOfAppsUsingCiCd: 2,
          },
          {
            dateTimeMillis: 1701955555925,
            totalNumberOfApps: 8,
            totalNumberOfAppsWithScmEnabled: 3,
            totalNumberOfPolicyActionFailuresByAppCount: 5,
            totalNumberOfWaivers: 4,
            meanTimeToRemediateMs: 400000000,
            totalNumberOfAppsUsingCiCd: 2,
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
      render(<MTTRGraph />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('should render a Loading... message when network call is pending', () => {
    const loadingState = {
      integrations: {
        developerDashboardGraphs: {
          graphData: [],
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
          graphData: [],
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
          graphData: [],
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

    expect(axiosMock.history.get[0].url).toBe(getDeveloperDashboardGraphsData());
    expect(axiosMock.history.get[0].params).toBe(undefined);
  });

  it('should render a graph', async () => {
    renderComponent();

    expect(screen.getByRole('heading', { name: /mean time to remediate/i })).toBeInTheDocument();
    expect(screen.getByTestId('auto-sizer')).toBeInTheDocument();
  });
});
