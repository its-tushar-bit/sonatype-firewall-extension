/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import moment from 'moment-timezone';

import { render, screen, fireEvent, within } from 'TestRoot/SpecUtil';
import ReportTitle from 'MainRoot/applicationReport/ReportTitle';

import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as applicationReportActions from 'MainRoot/applicationReport/applicationReportActions';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import * as latestReportSelectors from 'MainRoot/applicationReport/latestReportForStageSelectors';

describe('ReportTitle', () => {
  const givenScanIdForReport = 'scanId';
  const givenPublicId = 'publicId';

  let renderComponent,
    routerContextMock,
    mockedReevaluateReport,
    metadataDetails,
    selectApplicationReportMetaDataSpy,
    selectSelectedReportSpy,
    selectIsLatestReportForStageRequestPendingSpy,
    selectLatestReportForStageIdSpy;

  beforeAll(() => {
    moment.tz.setDefault('America/New_York');
  });

  afterAll(() => {
    moment.tz.setDefault();
  });

  beforeEach(() => {
    metadataDetails = {
      scanTriggerType: 'Unknown',
      reportTitle: 'Title',
      reportTime: moment('2018-11-11 15:13:11').toDate().getTime(),
      application: {
        id: 'metadataApplicationId',
        name: 'App Name',
      },
    };

    selectApplicationReportMetaDataSpy = jest
      .spyOn(applicationReportSelectors, 'selectApplicationReportMetaData')
      .mockReturnValue(metadataDetails);

    jest
      .spyOn(applicationReportSelectors, 'selectIsContainerImagesEvaluationEnabledAndProxyStage')
      .mockReturnValue(false);

    selectIsLatestReportForStageRequestPendingSpy = jest
      .spyOn(latestReportSelectors, 'selectIsLatestReportForStageRequestPending')
      .mockReturnValue(false);

    selectLatestReportForStageIdSpy = jest
      .spyOn(latestReportSelectors, 'selectLatestReportForStageId')
      .mockReturnValue(givenScanIdForReport);

    selectSelectedReportSpy = jest.spyOn(applicationReportSelectors, 'selectSelectedReport').mockReturnValue({
      reportVersion: 3,
    });

    mockedReevaluateReport = jest.spyOn(applicationReportActions, 'reevaluateReport');

    jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
      publicId: givenPublicId,
      scanId: givenScanIdForReport,
    });

    jest.spyOn(applicationReportSelectors, 'selectReportParameters').mockReturnValue({
      appId: 'appId',
      scanId: givenScanIdForReport,
    });

    jest.spyOn(productFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(false);

    routerContextMock = {
      href: jest.fn('href').mockReturnValue('mockValue'),
      get: jest.fn('get').mockReturnValue('mockGetValue'),
    };
    jest.spyOn(routerContext, 'useRouterState').mockReturnValue(routerContextMock);

    renderComponent = () => {
      render(<ReportTitle />);
    };
  });

  it('renders a title a dropdown and a button', () => {
    renderComponent();
    expect(screen.getByRole('heading', { name: 'App Name Title' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Re-Evaluate Report' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Options' })).toBeVisible();
  });

  it('options dropdown render 6 links', async () => {
    renderComponent();
    const options = screen.getByRole('button', { name: 'Options' });
    expect(options).toBeInTheDocument();

    fireEvent.click(options);

    expect(screen.getByRole('link', { name: 'Export PDF' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Export CycloneDX' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Export SPDX' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'View raw data' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'View Latest Evaluations' })).toBeVisible();

    const viewVulnerabilitiesLink = await screen.findByRole('link', {
      name: /view vulnerabilities/i,
    });
    expect(viewVulnerabilitiesLink).toBeVisible();
  });

  it('options dropdown renders 6 links if developer dashboard is enabled and firewall for docker product', async () => {
    productFeaturesSelectors.selectIsDeveloperDashboardEnabled.mockReturnValue(true);
    applicationReportSelectors.selectIsContainerImagesEvaluationEnabledAndProxyStage.mockReturnValue(true);

    renderComponent();
    const options = screen.getByRole('button', { name: 'Options' });
    expect(options).toBeInTheDocument();

    fireEvent.click(options);

    expect(screen.getByRole('link', { name: 'Export PDF' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Export CycloneDX' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Export SPDX' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'View raw data' })).toBeVisible();
    expect(screen.queryByRole('link', { name: 'Priorities' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'View Latest Evaluations' })).toBeVisible();
  });

  it('options dropdown renders 7 links if developer dashboard is enabled', async () => {
    productFeaturesSelectors.selectIsDeveloperDashboardEnabled.mockReturnValue(true);

    renderComponent();
    const options = screen.getByRole('button', { name: 'Options' });
    expect(options).toBeInTheDocument();

    fireEvent.click(options);

    expect(screen.getByRole('link', { name: 'Export PDF' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Export CycloneDX' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Export SPDX' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'View raw data' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Priorities' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'View Latest Evaluations' })).toBeVisible();

    const viewVulnerabilitiesLink = await screen.findByRole('link', {
      name: /view vulnerabilities/i,
    });
    expect(viewVulnerabilitiesLink).toBeVisible();
  });

  it('options dropdown priorities link redirects to an external tab', async () => {
    productFeaturesSelectors.selectIsDeveloperDashboardEnabled.mockReturnValue(true);

    renderComponent();
    const options = screen.getByRole('button', { name: 'Options' });

    fireEvent.click(options);

    expect(screen.getByRole('link', { name: 'Priorities' })).toHaveAttribute('target', '_blank');
  });

  it('renders a disabled view vulnerabilities link if report version is less than 5', async () => {
    renderComponent();
    const options = screen.getByRole('button', { name: 'Options' });
    expect(options).toBeVisible();

    fireEvent.click(options);

    const vulnerabilities = await screen.findByRole('link', {
      name: /view vulnerabilities/i,
    });
    expect(vulnerabilities).toHaveAttribute('aria-disabled', 'true');

    fireEvent.mouseOver(vulnerabilities);
    const tooltip = await screen.findByText('Reevaluate the report in order to enable Vulnerabilities view');
    expect(tooltip).not.toBeNull();
  });

  it('renders an enabled view vulnerabilities link if report version is greater than 5', () => {
    selectSelectedReportSpy.mockReturnValue({
      reportVersion: 7,
    });
    renderComponent();
    const options = screen.getByText('Options');
    expect(options).toBeVisible();

    fireEvent.click(options);

    const vulnerabilitiesLink = screen.getByRole('link', { name: 'View vulnerabilities' });
    expect(vulnerabilitiesLink).toHaveAttribute('aria-disabled', 'false');

    expect(vulnerabilitiesLink).toHaveTextContent(/view vulnerabilities/i);
  });

  it('renders a disabled view vulnerabilities link if report version is lower than 5 ', async () => {
    selectSelectedReportSpy.mockReturnValue({
      reportVersion: 4,
    });

    renderComponent();
    const options = screen.getByText('Options');
    expect(options).toBeVisible();

    fireEvent.click(options);

    const vulnerabilitiesLink = await screen.findByRole('link', {
      name: /view vulnerabilities/i,
    });
    expect(vulnerabilitiesLink).toHaveAttribute('aria-disabled', 'true');
  });

  it('calls reevaluateReport when the reevaluateReport button is pressed', () => {
    renderComponent();
    const reevaluateReport = screen.getByRole('button', { name: 'Re-Evaluate Report' });
    expect(reevaluateReport).toBeVisible();
    expect(reevaluateReport).not.toBeDisabled();

    fireEvent.click(reevaluateReport);
    expect(mockedReevaluateReport).toHaveBeenCalled();
  });

  it('show reevaluation modal when the auto waiver feature flag is enabled', () => {
    jest.spyOn(productFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(true);
    renderComponent();
    const reevaluateReport = screen.getByRole('button', { name: 'Re-Evaluate Report' });
    expect(reevaluateReport).toBeVisible();
    expect(reevaluateReport).not.toBeDisabled();

    fireEvent.click(reevaluateReport);
    expect(screen.getByRole('heading', { name: 'Re-Evaluate Report' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Quick Re-Evaluate' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Re-Evaluate' })).toBeInTheDocument();
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByRole('heading', { name: 'Re-Evaluate Report' })).toBeInTheDocument();
  });

  it('should disable reevaluateReport button when there is a newer report for the same stage', () => {
    selectIsLatestReportForStageRequestPendingSpy.mockReturnValue(false);
    selectLatestReportForStageIdSpy.mockReturnValue('some-other-report-id');
    renderComponent();
    const reevaluateReport = screen.getByRole('button', { name: 'Re-Evaluate Report' });

    expect(reevaluateReport).toBeVisible();
    expect(reevaluateReport).toBeDisabled();
  });

  it('should have a tooltip when reevaluateReport button is disabled', async () => {
    selectIsLatestReportForStageRequestPendingSpy.mockReturnValue(false);
    selectLatestReportForStageIdSpy.mockReturnValue('some-other-report-id');
    renderComponent();

    const reevaluateReport = screen.getByRole('button', { name: 'Re-Evaluate Report' });

    fireEvent.mouseOver(reevaluateReport.firstElementChild);

    const tooltip = await screen.findByRole('tooltip', {
      name: 'Re-Evaluation is only allowed on the latest scan of a given stage.',
    });

    expect(tooltip).toBeInTheDocument();
  });

  it('renders a description with time value and triggered by scan type', () => {
    renderComponent();
    const description = screen.getByText(
      `Triggered by ${metadataDetails.scanTriggerType} on 2018-11-11 15:13:11 UTC-0500`
    );

    expect(description).toBeVisible();
  });

  it('renders a description with triggered by scan type from continuous monitoring', () => {
    selectApplicationReportMetaDataSpy.mockReturnValue({ ...metadataDetails, forMonitoring: true });
    renderComponent();
    const description = screen.getByText(
      `Triggered by ${metadataDetails.scanTriggerType} (Continuous Monitoring) on 2018-11-11 15:13:11 UTC-0500`
    );

    expect(description).toBeVisible();
  });

  it('renders a description with triggered by scan type from re-evaluation', () => {
    selectApplicationReportMetaDataSpy.mockReturnValue({ ...metadataDetails, reevaluation: true });
    renderComponent();
    const description = screen.getByText(
      `Triggered by ${metadataDetails.scanTriggerType} (Re-evaluation) on 2018-11-11 15:13:11 UTC-0500`
    );

    expect(description).toBeVisible();
  });

  it('renders a description with container scanning mode', () => {
    selectApplicationReportMetaDataSpy.mockReturnValue({
      ...metadataDetails,
      containerScanningMode: 'sonatype',
    });

    renderComponent();

    const description = screen.getByText(
      `Triggered by ${metadataDetails.scanTriggerType} (Sonatype Container) on 2018-11-11 15:13:11 UTC-0500`
    );

    expect(description).toBeVisible();
  });
});
