/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, axiosMockAdapter, within } from 'TestRoot/SpecUtil';
import { getRiskRemediationAndMttrGraphData } from 'MainRoot/util/CLMLocation';
import RiskRemediationGraph from 'MainRoot/integrations/sections/Graphs/RiskRemediationGraph';
import { act, fireEvent } from '@testing-library/react';

let listener = null;
const originalResizeObserver = window.ResizeObserver;

window.ResizeObserver = class ResizeObserver {
  constructor(ls) {
    listener = ls;
  }

  observe() {
    return this;
  }

  disconnect() {
    return this;
  }
};

describe('RiskRemediationGraph', () => {
  let axiosMock, renderComponent;

  const defaultPreloadedState = {
    integrations: {
      riskRemediationAndMttrGraph: {
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
      render(<RiskRemediationGraph />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  afterAll(() => {
    window.ResizeObserver = originalResizeObserver;
  });

  it('should render a Loading... message when network call is pending', () => {
    const loadingState = {
      integrations: {
        riskRemediationAndMttrGraph: {
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
        riskRemediationAndMttrGraph: {
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
    axiosMock.onGet(getRiskRemediationAndMttrGraphData()).reply(404, 'Error');

    const errorState = {
      integrations: {
        riskRemediationAndMttrGraph: {
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

    expect(axiosMock.history.get[0].url).toBe(getRiskRemediationAndMttrGraphData());
    expect(axiosMock.history.get[0].params).toBe(undefined);
  });

  it('should render a graph', async () => {
    renderComponent();

    const header = await screen.findByRole('heading', { name: /risk & remediation timeline/i });
    expect(header).toBeInTheDocument();

    act(() => {
      listener([
        {
          contentRect: {
            width: 800,
            height: 400,
          },
        },
      ]);
    });

    const graph = await screen.findByRole('img');
    expect(graph).toBeInTheDocument();
  });
});
