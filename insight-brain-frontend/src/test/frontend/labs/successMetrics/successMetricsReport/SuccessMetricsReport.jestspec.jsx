/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { axiosMockAdapter, render, screen, waitFor, within, fireEvent } from 'TestRoot/SpecUtil';
import {
  getApplicationsUrl,
  getSuccessMetricsChartDataUrl,
  getSuccessMetricsComponentCountsUrl,
  getSuccessMetricsConfigUrl,
  getSuccessMetricsReportsUrl,
  getSuccessMetricsReportUrl,
  getSuccessMetricsStageIdUrl,
} from 'MainRoot/util/CLMLocation';
import SuccessMetricsReportContainer from 'MainRoot/labs/successMetrics/successMetricsReport/SuccessMetricsReportContainer';
import { Components } from 'plottable';

// jest.spyOn will not let you override methods on Components unless we mock the module here
// we need to mock it because, it invokes some native dom apis that are not represented in jsdom
jest.mock('plottable', () => {
  const originalModule = jest.requireActual('plottable');
  return {
    ...originalModule,
    Components: {
      ...originalModule.Components,
      Table: jest.fn(),
      Group: jest.fn(),
    },
  };
});

describe('SuccessMetricsReport', () => {
  const successMetricsReportId = 'some-report-id';
  const reportName = 'report-rtl-test';
  const numActiveApplications = 42;
  const givenMonthCount = 9;

  let renderToTable;
  let renderToGroup;

  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    renderToTable = jest.fn();
    renderToGroup = jest.fn();

    jest.spyOn(Components, 'Table').mockReturnValue({ renderTo: renderToTable });
    jest
      .spyOn(Components, 'Group')
      .mockReturnValue({ renderTo: renderToGroup, onAnchor: jest.fn(), someRandomData: 'foo' });

    givenSuccessMetricsIsEnabledForAMultipleApplicationReport();
  });

  it('renders multiple application report', async () => {
    await renderComponent();

    await assertSpinnerShownAndRemoved();

    const expectedReportDescriptionText = `
    This report contains data for ${numActiveApplications} applications, evaluated over the past ${givenMonthCount}
    months, aggregated and deduplicated over the source, build, stage release, release, and operate stages. Last updated
    Jan 01, 2025.
    `;

    await assertCorrectHeaderShown(reportName, expectedReportDescriptionText);

    // should make all required network calls
    expect(axiosMock.history.get.length).toEqual(5);
    expect(axiosMock.history.get[0].url).toEqual(getSuccessMetricsConfigUrl());
    expect(axiosMock.history.get[1].url).toEqual(getSuccessMetricsChartDataUrl(successMetricsReportId));
    expect(axiosMock.history.get[2].url).toEqual(getSuccessMetricsReportsUrl());
    expect(axiosMock.history.get[3].url).toEqual(getSuccessMetricsComponentCountsUrl(successMetricsReportId));
    expect(axiosMock.history.get[4].url).toEqual(getSuccessMetricsStageIdUrl());

    await assertChartsRendered();
  });

  it('renders multiple application report when includeLatestData is enabled', async () => {
    givenSuccessMetricsReportReturned(200, [
      {
        id: successMetricsReportId,
        name: reportName,
        scope: { applicationIds: null, organizationIds: null },
        includeLatestData: true, // include latest data is true
      },
    ]);

    await renderComponent();

    await assertSpinnerShownAndRemoved();

    // includes the additional timestamp (12:00:00 AM) when includeLatestData is enabled
    const expectedReportDescriptionText = `
    This report contains data for ${numActiveApplications} applications, evaluated over the past ${givenMonthCount}
    months, aggregated and deduplicated over the source, build, stage release, release, and operate stages. Last
    updated Jan 01, 2025 12:00:00 AM.
    `;

    await assertCorrectHeaderShown(reportName, expectedReportDescriptionText);

    // should make all required network calls
    expect(axiosMock.history.get.length).toEqual(5);
    expect(axiosMock.history.get[0].url).toEqual(getSuccessMetricsConfigUrl());
    expect(axiosMock.history.get[1].url).toEqual(getSuccessMetricsChartDataUrl(successMetricsReportId));
    expect(axiosMock.history.get[2].url).toEqual(getSuccessMetricsReportsUrl());
    expect(axiosMock.history.get[3].url).toEqual(getSuccessMetricsComponentCountsUrl(successMetricsReportId));
    expect(axiosMock.history.get[4].url).toEqual(getSuccessMetricsStageIdUrl());

    await assertChartsRendered();
  });

  it('renders multiple application report with singular month in description when only 1 month of data', async () => {
    givenChartDataReturned(
      200,
      getChartData({
        lastUpdated: 1735705000000,
        monthCount: 1,
      })
    );

    await renderComponent();

    await assertSpinnerShownAndRemoved();

    const expectedReportDescriptionText = `
    This report contains data for ${numActiveApplications} applications, evaluated over the past 1 month, aggregated
    and deduplicated over the source, build, stage release, release, and operate stages. Last updated Dec 31, 2024.
    `;

    await assertCorrectHeaderShown(reportName, expectedReportDescriptionText);

    // should make all required network calls
    expect(axiosMock.history.get.length).toEqual(5);
    expect(axiosMock.history.get[0].url).toEqual(getSuccessMetricsConfigUrl());
    expect(axiosMock.history.get[1].url).toEqual(getSuccessMetricsChartDataUrl(successMetricsReportId));
    expect(axiosMock.history.get[2].url).toEqual(getSuccessMetricsReportsUrl());
    expect(axiosMock.history.get[3].url).toEqual(getSuccessMetricsComponentCountsUrl(successMetricsReportId));
    expect(axiosMock.history.get[4].url).toEqual(getSuccessMetricsStageIdUrl());

    await assertChartsRendered();
  });

  it('renders single application report', async () => {
    // override response to simulate single-application report
    givenChartDataReturned(
      200,
      getChartData({
        applicationCounts: {
          ...getChartData().applicationCounts,
          activeApplications: 1,
        },
      })
    );
    givenSuccessMetricsReportReturned(200, [
      {
        id: successMetricsReportId,
        name: reportName,
        scope: { applicationIds: ['some-application-id'], organizationIds: null },
        includeLatestData: false,
      },
    ]);

    await renderComponent();

    await assertSpinnerShownAndRemoved();

    await screen.findByRole('heading', { name: reportName });

    const expectedReportDescriptionText = `
    This report contains data for 1 application, evaluated over the past ${givenMonthCount} months, aggregated and deduplicated over the
    source, build, stage release, release, and operate stages. Last updated Jan 01, 2025.
    `;

    await assertCorrectHeaderShown(reportName, expectedReportDescriptionText);

    // should make all required network calls -- makes an extra call to fetch applications for single application report
    expect(axiosMock.history.get.length).toEqual(6);
    expect(axiosMock.history.get[0].url).toEqual(getSuccessMetricsConfigUrl());
    expect(axiosMock.history.get[1].url).toEqual(getSuccessMetricsChartDataUrl(successMetricsReportId));
    expect(axiosMock.history.get[2].url).toEqual(getSuccessMetricsReportsUrl());
    expect(axiosMock.history.get[3].url).toEqual(getSuccessMetricsComponentCountsUrl(successMetricsReportId));
    expect(axiosMock.history.get[4].url).toEqual(getSuccessMetricsStageIdUrl());
    expect(axiosMock.history.get[5].url).toEqual(getApplicationsUrl());

    await assertChartsRendered(true);
  });

  it('renders message based selected stage if a custom success metrics stage is set', async () => {
    givenSuccessMetricsStageIdReturned(200, 'release');

    await renderComponent();

    await assertSpinnerShownAndRemoved();

    await screen.findByRole('heading', { name: reportName });

    const expectedReportDescriptionText = `
    This report contains data for ${numActiveApplications} applications, evaluated over the past ${givenMonthCount} months,
    for evaluations of the release stage. Last updated Jan 01, 2025.
    `;

    await assertCorrectHeaderShown(reportName, expectedReportDescriptionText);
    await assertChartsRendered();
  });

  it('renders appropriate message and avoids un-needed network call when success metrics feature is not enabled', async () => {
    givenSuccessMetricsDisabled();

    await renderComponent();

    await screen.findByText(
      'An error occurred loading data. Success metrics have been disabled by your system administrator.'
    );

    // should not make and additional network calls if we find that the feature is disabled
    expect(axiosMock.history.get[0].url).toEqual(getSuccessMetricsConfigUrl());
    expect(axiosMock.history.get.length).toEqual(1);
  });

  it('renders error message if check for success metrics enabled fails', async () => {
    givenSuccessMetricsEnabled(403);

    await renderComponent();

    await screen.findByText('An error occurred loading data. Request failed with status code 403');
  });

  it('renders error message if chart data request fails', async () => {
    givenChartDataReturned(500);

    await renderComponent();

    await screen.findByText('An error occurred loading data. Request failed with status code 500');
  });

  it('renders error message if report data request fails', async () => {
    givenSuccessMetricsReportReturned(501);

    await renderComponent();

    await screen.findByText('An error occurred loading data. Request failed with status code 501');
  });

  it('renders error message if component count request fails', async () => {
    givenSuccessMetricsComponentCountsReturned(502);

    await renderComponent();

    await screen.findByText('An error occurred loading data. Request failed with status code 502');
  });

  it('renders error message if success metrics stage id request fails', async () => {
    givenSuccessMetricsStageIdReturned(503);

    await renderComponent();

    await screen.findByText('An error occurred loading data. Request failed with status code 503');
  });

  it('renders error message if application request fails and its a single application report', async () => {
    givenSuccessMetricsReportReturned(200, [
      {
        id: successMetricsReportId,
        name: reportName,
        scope: { applicationIds: ['some-application-id'], organizationIds: null },
        includeLatestData: false,
      },
    ]);
    givenApplicationsReturned(504);

    await renderComponent();

    await screen.findByText('An error occurred loading data. Request failed with status code 504');
  });

  it('can delete report', async () => {
    axiosMock.onDelete(getSuccessMetricsReportUrl(successMetricsReportId)).reply(200);
    await renderComponent();

    // make sure form has loaded for good measure
    await assertSpinnerShownAndRemoved();
    await screen.findByRole('heading', { name: reportName });
    await assertChartsRendered();

    // find the delete report button and click it
    const deleteButton = screen.queryByRole('button', { name: 'Delete Report' });
    expect(deleteButton).toBeInTheDocument();
    fireEvent.click(deleteButton);

    // a dialog should be shown with a message and an addition confirmation delete button to be clicked
    const deleteDialog = await screen.queryByRole('dialog');
    within(deleteDialog).queryByText(`You are about to delete ${reportName}. This action cannot be undone.`);
    const applyDeleteButton = within(deleteDialog).queryByRole('button', { name: 'Delete' });
    expect(applyDeleteButton).toBeInTheDocument();
    fireEvent.click(applyDeleteButton);

    expect(await within(deleteDialog).findByText('Success!')).toBeInTheDocument();

    // after some time (800 ms) another action should cause the success message to be removed
    await waitFor(() => {
      expect(within(deleteDialog).queryByText('Success!')).not.toBeInTheDocument();
    });

    // we should have made the correct request to delete the specified report
    expect(axiosMock.history.delete.length).toEqual(1);
    expect(axiosMock.history.delete[0].url).toEqual(getSuccessMetricsReportUrl(successMetricsReportId));
  });

  describe('when no data to show', () => {
    beforeEach(() => {
      givenChartDataReturned(200, getChartData({ applicationCounts: {} }));
    });

    it('and most recent evaluations is enabled, renders no data message', async () => {
      await renderComponent();
      await assertSpinnerShownAndRemoved();
      await assertCorrectHeaderShown(
        reportName,
        "There's not enough data to generate Success Metrics. Run some evaluations and check " +
          "again next month. Create a Success Metrics report using the 'include most recent evaluations' option " +
          'to see the latest data.'
      );

      assertChartsNotRendered();
    });

    it('and most recent evaluations is disabled, renders no data message', async () => {
      givenSuccessMetricsReportReturned(200, [
        {
          id: successMetricsReportId,
          name: reportName,
          scope: { applicationIds: null, organizationIds: null },
          includeLatestData: true, // include latest data is true
        },
      ]);

      await renderComponent();
      await assertSpinnerShownAndRemoved();
      await assertCorrectHeaderShown(
        reportName,
        "There's not enough data to generate Success Metrics. Run some evaluations and check again."
      );

      assertChartsNotRendered();
    });
  });

  function renderComponent(preloadStateOverrides = {}) {
    const preloadedState = {
      router: {
        currentParams: { successMetricsReportId },
      },
      ...preloadStateOverrides,
    };

    return render(<SuccessMetricsReportContainer />, { preloadedState });
  }

  function givenSuccessMetricsIsEnabledForAMultipleApplicationReport() {
    givenSuccessMetricsEnabled();
    givenChartDataReturned();
    givenSuccessMetricsReportReturned();
    givenSuccessMetricsComponentCountsReturned();
    givenSuccessMetricsStageIdReturned();

    // not strictly needed in single application case but easier to provide default mock here and override if needed
    // for special cases such as simulating failure
    givenApplicationsReturned();
  }

  function givenSuccessMetricsEnabled(statusCode = 200) {
    axiosMock.onGet(getSuccessMetricsConfigUrl()).reply(statusCode, { enabled: true });
  }

  function givenSuccessMetricsDisabled() {
    axiosMock.onGet(getSuccessMetricsConfigUrl()).reply(200, { enabled: false });
  }

  function givenChartDataReturned(statusCode = 200, overrides = {}) {
    axiosMock.onGet(getSuccessMetricsChartDataUrl(successMetricsReportId)).reply(statusCode, getChartData(overrides));
  }

  function givenSuccessMetricsReportReturned(statusCode = 200, respOverride) {
    const resp = respOverride
      ? respOverride
      : [
          {
            id: successMetricsReportId,
            name: reportName,
            scope: { applicationIds: null, organizationIds: null },
            includeLatestData: false,
          },
        ];

    axiosMock.onGet(getSuccessMetricsReportsUrl()).reply(statusCode, resp);
  }

  function givenSuccessMetricsComponentCountsReturned(statusCode = 200, respOverrides = {}) {
    const resp = {
      componentsPerApplication: 68,
      componentsInTheMostApplications: [
        {
          componentDisplayName: 'org.apache.tomcat.embed : tomcat-embed-core : 8.5.0',
          hash: '3c250f6e7b0299a6944f',
          count: 539,
        },
      ],
      componentsWithTheMostViolations: [
        {
          componentDisplayName: 'org.apache.tomcat.embed : tomcat-embed-core : 8.5.0',
          hash: '3c250f6e7b0299a6944f',
          count: 27489,
        },
      ],
      ...respOverrides,
    };

    axiosMock.onGet(getSuccessMetricsComponentCountsUrl(successMetricsReportId)).reply(statusCode, resp);
  }

  function givenApplicationsReturned(statusCode = 200, applicationsOverride) {
    const resp = applicationsOverride || [
      {
        id: 'some-application-id',
        publicId: 'some_public_application_id',
        name: 'Some Application For Jest Testing',
        organizationId: 'some-org-id',
        organizationName: 'some-org',
        contact: null,
      },
    ];

    axiosMock.onGet(getApplicationsUrl()).reply(statusCode, resp);
  }

  function givenSuccessMetricsStageIdReturned(statusCode = 200, stageIdReturned) {
    axiosMock.onGet(getSuccessMetricsStageIdUrl()).reply(statusCode, { successMetricsStageId: stageIdReturned });
  }

  async function assertSpinnerShownAndRemoved() {
    expect(screen.getByText('Loading…')).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByText('Loading…')).not.toBeInTheDocument());
  }

  async function assertCorrectHeaderShown(expectedTitle, expectedDescription) {
    expect(await screen.findByRole('heading', { name: expectedTitle })).toBeInTheDocument();

    expect(
      screen.getByRole('heading', {
        name: (content) => {
          const normalizedContent = content.replace(/\s+/g, ' ').trim();
          const normalizedExpectedText = expectedDescription.replace(/\s+/g, ' ').trim();
          return normalizedContent === normalizedExpectedText;
        },
      })
    ).toBeInTheDocument();
  }

  async function assertChartsRendered(isSingleApplicationReport = false) {
    // wait until we've invoked all the plotter render mocks, these happen
    // async if you don't wait you can get some weird behavior where errors
    // happen after the test finishes, causing the next test to fail instead
    // of the one that caused the errors
    await waitFor(() => expect(renderToTable).toHaveBeenCalled());
    await waitFor(() => expect(renderToGroup).toHaveBeenCalled());

    assertChartPresent('Violation Trends Chart', '12 Week Policy Violation Activity');
    assertChartPresent('Violation By Category Chart', '12 Week Open Violation Totals');
    assertChartPresent(
      'Violation Averages Chart',
      'Average Number of Violations Discovered Per Month, Per Application'
    );
    assertChartPresent(
      'MTTR Chart',
      `This data represents the average age of violations that were resolved each month in 1 application over the past ${givenMonthCount} months. A violation that does not reappear in a subsequent evaluation is considered resolved.`
    );

    if (isSingleApplicationReport) {
      assertChartNotPresent('Applications Chart');
    } else {
      assertChartPresent(
        'Applications Chart',
        `Over the past ${givenMonthCount} months, 1 out of 1 applications contained violations, and 1 contained critical violations.`
      );
    }

    assertChartPresent(
      'Component Counts Chart',
      'This data is based on the latest Lifecycle evaluations of 1 applications. On average, there are 68 components per application.'
    );
  }

  function assertChartsNotRendered() {
    expect(renderToTable).not.toHaveBeenCalled();
    expect(renderToGroup).not.toHaveBeenCalled();

    assertChartNotPresent('Violation Trends Chart');
    assertChartNotPresent('Violation By Category Chart');
    assertChartNotPresent('Violation Averages Chart');
    assertChartNotPresent('MTTR Chart');
    assertChartNotPresent('Applications Chart');
    assertChartNotPresent('Component Counts Chart');
  }

  function assertChartPresent(chartName, chartDescription) {
    const chart = screen.queryByRole('region', { name: chartName });
    expect(chart).toBeInTheDocument();
    within(chart).queryByRole('heading', { name: chartDescription });
  }

  function assertChartNotPresent(chartName) {
    expect(screen.queryByRole('region', { name: chartName })).not.toBeInTheDocument();
  }

  function getChartData(overrides = {}) {
    return {
      mttrs: [
        { timePeriodName: 'Jun', mttrInSeconds: null, criticalMttrInSeconds: null },
        { timePeriodName: 'Jul', mttrInSeconds: 308, criticalMttrInSeconds: 308 },
      ],
      averages: {
        evaluationCount: 0.6666666666666666,
        totalViolations: { averageDiscovered: 2.0, averageDiscoveredCritical: 0.7777777777777778 },
        securityViolations: { averageDiscovered: 1.2222222222222223, averageDiscoveredCritical: 0.7777777777777778 },
        licenseViolations: { averageDiscovered: 0.2222222222222222, averageDiscoveredCritical: 0.0 },
        qualityViolations: { averageDiscovered: 0.5555555555555556, averageDiscoveredCritical: 0.0 },
        otherViolations: { averageDiscovered: 0.0, averageDiscoveredCritical: 0.0 },
      },
      applicationCounts: {
        totalApplications: 2037,
        activeApplications: numActiveApplications,
        total: { applicationsWithViolations: 1, applicationsWithCriticalViolations: 1 },
        security: { applicationsWithViolations: 1, applicationsWithCriticalViolations: 1 },
        license: { applicationsWithViolations: 1, applicationsWithCriticalViolations: 0 },
        quality: { applicationsWithViolations: 1, applicationsWithCriticalViolations: 0 },
        other: { applicationsWithViolations: 0, applicationsWithCriticalViolations: 0 },
      },
      violationCounts: [
        {
          timePeriodName: 'Week of December 16th',
          discoveredCounts: {
            SECURITY: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            LICENSE: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            QUALITY: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            OTHER: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
          },
          waivedCounts: {
            SECURITY: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            LICENSE: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            QUALITY: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            OTHER: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
          },
          fixedCounts: {
            SECURITY: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            LICENSE: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            QUALITY: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            OTHER: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
          },
        },
        {
          timePeriodName: 'Week of December 23rd',
          discoveredCounts: {
            SECURITY: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            LICENSE: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            QUALITY: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            OTHER: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
          },
          waivedCounts: {
            SECURITY: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            LICENSE: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            QUALITY: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            OTHER: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
          },
          fixedCounts: {
            SECURITY: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            LICENSE: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            QUALITY: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
            OTHER: { LOW: 0, MODERATE: 0, SEVERE: 0, CRITICAL: 0 },
          },
        },
      ],
      violationsByCategoryWeeks: [
        { timePeriodName: '16 Dec', security: 9, license: 2, quality: 5, other: 0 },
        { timePeriodName: '23 Dec', security: 9, license: 2, quality: 5, other: 0 },
      ],
      lastUpdated: 1735707600000,
      monthCount: givenMonthCount,
      ...overrides,
    };
  }
});
