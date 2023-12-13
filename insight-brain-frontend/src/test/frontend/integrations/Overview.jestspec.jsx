/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { axiosMockAdapter, render, screen, waitForElementToBeRemoved } from 'TestRoot/SpecUtil';
import Overview from 'MainRoot/integrations/sections/overview/Overview';
import { getUsageOverTimeChartVisibility } from 'MainRoot/util/CLMLocation';

describe('Overview', () => {
  const ADOPTION_GRAPH_TEXT = 'Adoption Graph Placeholder';
  const MTTR_GRAPH_TEXT = 'MTTR Graph Graph Placeholder';
  const RISK_AND_REMEDIATION_GRAPH_TEXT = 'Risk And Remediation Graph Placeholder';
  const LOADING = 'Loading…';

  let axiosMock;

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
  });

  it('should render charts given backend indicates they are visible for the user profile', async () => {
    givenChartsShownForUser();

    renderComponent();

    await waitForChartVisibilityRequestToLoad();

    expect(screen.queryByText(ADOPTION_GRAPH_TEXT)).toBeInTheDocument();
    expect(screen.queryByText(MTTR_GRAPH_TEXT)).toBeInTheDocument();
    expect(screen.queryByText(RISK_AND_REMEDIATION_GRAPH_TEXT)).toBeInTheDocument();
  });

  it('should not render charts given backend indicates they are not visible for the user profile', async () => {
    givenChartsNotShownForUser();

    renderComponent();

    await waitForChartVisibilityRequestToLoad();

    expect(screen.queryByText(ADOPTION_GRAPH_TEXT)).not.toBeInTheDocument();
    expect(screen.queryByText(MTTR_GRAPH_TEXT)).not.toBeInTheDocument();
    expect(screen.queryByText(RISK_AND_REMEDIATION_GRAPH_TEXT)).not.toBeInTheDocument();
  });

  it('should show an error message given visiblity info for the user could not be fetched', async () => {
    givenChartVisiblityRequestFails();

    renderComponent();

    await waitForChartVisibilityRequestToLoad();

    expect(screen.queryByText(ADOPTION_GRAPH_TEXT)).not.toBeInTheDocument();

    const errorAlert = await screen.findByRole('alert');
    expect(errorAlert).toBeInTheDocument();
    expect(errorAlert).toHaveTextContent('Error');
  });

  function givenChartsShownForUser() {
    axiosMock.onGet(getUsageOverTimeChartVisibility()).reply(200, { usageOverTimeChartsShown: true });
  }

  function givenChartsNotShownForUser() {
    axiosMock.onGet(getUsageOverTimeChartVisibility()).reply(200, { usageOverTimeChartsShown: false });
  }

  function givenChartVisiblityRequestFails() {
    axiosMock.onGet(getUsageOverTimeChartVisibility()).reply(500, 'Error');
  }

  async function waitForChartVisibilityRequestToLoad() {
    expect(screen.getByText('Loading…')).toBeInTheDocument();
    await waitForElementToBeRemoved(() => screen.queryByText(LOADING));
  }

  function renderComponent() {
    return render(<Overview />);
  }
});

jest.mock('MainRoot/integrations/sections/Graphs/AdoptionGraph', () => {
  const MockAdoptionGraph = () => {
    return <div>Adoption Graph Placeholder</div>;
  };

  return MockAdoptionGraph;
});

jest.mock('MainRoot/integrations/sections/Graphs/RiskRemediationGraph', () => {
  const MockRiskRemediationGraph = () => {
    return <div>Risk And Remediation Graph Placeholder</div>;
  };

  return MockRiskRemediationGraph;
});

jest.mock('MainRoot/integrations/sections/Graphs/MTTRGraph', () => {
  const MockMTTRGraph = () => {
    return <div>MTTR Graph Graph Placeholder</div>;
  };

  return MockMTTRGraph;
});
