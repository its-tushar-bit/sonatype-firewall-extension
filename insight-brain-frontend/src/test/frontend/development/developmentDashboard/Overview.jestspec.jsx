/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { axiosMockAdapter, render, screen, waitForElementToBeRemoved, fireEvent } from 'TestRoot/SpecUtil';
import Overview from 'MainRoot/development/developmentDashboard/sections/overview/Overview';
import { getUsageOverTimeChartVisibility, getAppIntegrationsAndRisk } from 'MainRoot/util/CLMLocation';
import { map, range } from 'ramda';

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

  describe('Overview component contains filtering button for Application Configuration Summary table', () => {
    beforeEach(() => {
      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(function (config) {
        if (config.params.optionalFilterScmIsIntegrated === true) {
          return createIntegrationResponse(2, false, true);
        } else if (config.params.optionalFilterScmIsIntegrated === false) {
          return createIntegrationResponse(5, false, false);
        } else if (config.params.optionalFilterCiCdIsIntegrated === true) {
          return createIntegrationResponse(20, true, false);
        } else if (config.params.optionalFilterCiCdIsIntegrated === false) {
          return createIntegrationResponse(11, false, false);
        } else {
          return createIntegrationResponse(15, false, false);
        }
      });
    });

    it('should render it a filter button for the table', async () => {
      givenChartsShownForUser();
      renderComponent();

      await waitForChartVisibilityRequestToLoad();
      const button = await screen.findByRole('button', {
        name: /Filter/i,
      });
      expect(button).toBeInTheDocument();
    });

    it('should render the filtering sidebar when the Filter button is clicked', async () => {
      givenChartsShownForUser();
      renderComponent();
      await waitForChartVisibilityRequestToLoad();
      expect(screen.queryByText('Filter')).toBeInTheDocument();
      const button = await screen.findByRole('button', {
        name: /Filter/i,
      });
      expect(button).toBeInTheDocument();
      fireEvent.click(button);

      expect(await screen.findByText('CI/CD Configuration')).toBeInTheDocument();
      expect(await screen.findByText('SCM Feedback Configuration')).toBeInTheDocument();
    });

    it('should render correct number of rows when SCM filter is true', async () => {
      givenChartsShownForUser();
      renderComponent();
      await waitForChartVisibilityRequestToLoad();
      const button = await screen.findByRole('button', {
        name: /Filter/i,
      });
      fireEvent.click(button);
      const configuredInput = await screen.queryAllByLabelText('Configured apps')[1];
      expect(configuredInput).toBeInTheDocument();
      fireEvent.focus(configuredInput);
      fireEvent.click(configuredInput);
      expect(configuredInput).toBeChecked();

      const applyButton = await screen.getByText('Apply');
      expect(applyButton).toBeInTheDocument();
      fireEvent.click(applyButton);
      expect(await screen.findByRole('table')).toBeInTheDocument();
      const configuredRows = await screen.findAllByRole('row');
      expect(configuredRows.length).toBe(4);

      let allRows = await screen.findAllByRole('row');
      expect(allRows.length).toBe(4); // 2 data rows, 1 filter row and 1 header
    });

    it('should render correct number of rows when SCM filter is false', async () => {
      givenChartsShownForUser();
      renderComponent();
      await waitForChartVisibilityRequestToLoad();
      const button = await screen.findByRole('button', {
        name: /Filter/i,
      });

      fireEvent.click(button);
      const unconfiguredApps = await screen.queryAllByLabelText('Non-configured apps')[1];
      expect(unconfiguredApps).toBeInTheDocument();
      fireEvent.focus(unconfiguredApps);
      fireEvent.click(unconfiguredApps);
      expect(unconfiguredApps).toBeChecked();

      const applyButton = await screen.getByText('Apply');
      expect(applyButton).toBeInTheDocument();
      fireEvent.click(applyButton);
      expect(await screen.findByRole('table')).toBeInTheDocument();
      const configuredRows = await screen.findAllByRole('row');
      expect(configuredRows.length).toBe(7); // 5 data rows, 1 filter row and 1 header
    });

    it('should render correct number of rows when CI filter is true', async () => {
      givenChartsShownForUser();
      renderComponent();
      await waitForChartVisibilityRequestToLoad();
      const button = await screen.findByRole('button', {
        name: /Filter/i,
      });
      fireEvent.click(button);
      const configuredInput = await screen.queryAllByLabelText('Configured apps')[0];
      expect(configuredInput).toBeInTheDocument();
      fireEvent.focus(configuredInput);
      fireEvent.click(configuredInput);
      expect(configuredInput).toBeChecked();

      const applyButton = await screen.getByText('Apply');
      expect(applyButton).toBeInTheDocument();
      fireEvent.click(applyButton);
      expect(await screen.findByRole('table')).toBeInTheDocument();
      const configuredRows = await screen.findAllByRole('row');
      expect(configuredRows.length).toBe(22); // 20 data rows, 1 fitler row and 1 header
    });

    it('should render correct number of rows when CI filter is false', async () => {
      givenChartsShownForUser();
      renderComponent();
      await waitForChartVisibilityRequestToLoad();
      const button = await screen.findByRole('button', {
        name: /Filter/i,
      });

      fireEvent.click(button);
      const unconfiguredApps = await screen.queryAllByLabelText('Non-configured apps')[0];
      expect(unconfiguredApps).toBeInTheDocument();
      fireEvent.focus(unconfiguredApps);
      fireEvent.click(unconfiguredApps);
      expect(unconfiguredApps).toBeChecked();

      const applyButton = await screen.getByText('Apply');
      expect(applyButton).toBeInTheDocument();
      fireEvent.click(applyButton);
      expect(await screen.findByRole('table')).toBeInTheDocument();
      const configuredRows = await screen.findAllByRole('row');
      expect(configuredRows.length).toBe(13); // 11 data rows, 1 filter row and 1 header
    });

    it('should render the correct number of rows when CI and SCM filters are not applied', async () => {
      givenChartsShownForUser();
      renderComponent();
      await waitForChartVisibilityRequestToLoad();
      const button = await screen.findByRole('button', {
        name: /Filter/i,
      });

      fireEvent.click(button);
      const allApps = await screen.queryAllByLabelText('All'); // two radio inputs: one for CI and one for SCM
      fireEvent.click(allApps[0]);
      fireEvent.click(allApps[1]);
      const applyButton = await screen.getByText('Apply');
      expect(applyButton).toBeInTheDocument();
      fireEvent.click(applyButton);
      expect(await screen.findByRole('table')).toBeInTheDocument();
      const configuredRows = await screen.findAllByRole('row');
      expect(configuredRows.length).toBe(17); // 15 data rows, 1 filter row and 1 header
    });
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

jest.mock('MainRoot/development/developmentDashboard/sections/Graphs/AdoptionGraph', () => {
  const MockAdoptionGraph = () => {
    return <div>Adoption Graph Placeholder</div>;
  };

  return MockAdoptionGraph;
});

jest.mock('MainRoot/development/developmentDashboard/sections/Graphs/RiskRemediationGraph', () => {
  const MockRiskRemediationGraph = () => {
    return <div>Risk And Remediation Graph Placeholder</div>;
  };

  return MockRiskRemediationGraph;
});

jest.mock('MainRoot/development/developmentDashboard/sections/Graphs/MTTRGraph', () => {
  const MockMTTRGraph = () => {
    return <div>MTTR Graph Graph Placeholder</div>;
  };

  return MockMTTRGraph;
});

function createIntegrationResponse(appCount, ciIntegrated, scmIntegrated) {
  return [
    200,
    {
      results: createAppArrayWithLength(appCount, 0, ciIntegrated, scmIntegrated, false),
      total: appCount,
      page: 1,
      pageSize: 10,
      pageCount: Math.ceil(appCount / 10),
    },
  ];
}

function createAppArrayWithLength(
  length,
  startIndex = 0,
  cicdEnabled = false,
  scmEnabled = true,
  hasSastReport = false
) {
  // Create a date object for January 1, 2023
  const date = new Date('January 1, 2023');

  // Get the timestamp in milliseconds
  const timestamp = date.getTime();

  const oneDayMilliseconds = 24 * 60 * 60 * 1000;

  return map(
    (i) => ({
      applicationName: `App${i}`,
      applicationId: `AppId${i}`,
      applicationPublicId: `App${i}`,
      lastCommitTimestamp: timestamp + i * oneDayMilliseconds,
      lastEvaluationTimestamp: timestamp + i * oneDayMilliseconds,
      totalRiskScore: i,
      ciIntegrationEnabled: cicdEnabled,
      automatedSourceControlFeedbackEnabled: scmEnabled,
      organizationId: `OrgId${i}`,
      hasSastReport: hasSastReport,
      lastSastReportId: hasSastReport ? `lastSastReportId${i}` : null,
      lastSastReportTime: hasSastReport ? timestamp + i * oneDayMilliseconds : null,
    }),
    range(startIndex, startIndex + length)
  );
}
