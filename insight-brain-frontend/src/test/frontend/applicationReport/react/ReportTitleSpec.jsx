/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import moment from 'moment-timezone';

import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import ReportTitle from 'MainRoot/applicationReport/react/ReportTitle';

import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as applicationReportActions from 'MainRoot/applicationReport/applicationReportActions';
import * as routerActions from 'MainRoot/reduxUiRouter/routerActions';

describe('ReportTitle', () => {
  let renderComponent,
    mockedReevaluateReport,
    metadataDetails,
    selectApplicationReportMetaDataSpy,
    selectSelectedReportSpy;

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

    selectApplicationReportMetaDataSpy = spyOn(
      applicationReportSelectors,
      'selectApplicationReportMetaData'
    ).and.returnValue(metadataDetails);

    selectSelectedReportSpy = spyOn(applicationReportSelectors, 'selectSelectedReport').and.returnValue({
      reportVersion: 3,
    });

    mockedReevaluateReport = spyOn(applicationReportActions, 'reevaluateReport').and.callThrough();
    spyOn(routerActions, 'stateGo').and.callThrough();

    spyOn(routerSelectors, 'selectRouterCurrentParams').and.returnValue({
      publicId: 'publicId',
      scanId: 'scanId',
    });

    spyOn(applicationReportSelectors, 'selectReportParameters').and.returnValue({
      appId: 'appId',
      scanId: 'scanId',
    });

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

  it('options dropdown render 5 links', () => {
    renderComponent();
    const options = screen.getByRole('button', { name: 'Options' });
    expect(options).toBeInTheDocument();

    fireEvent.click(options);

    expect(screen.getByRole('link', { name: 'Generate PDF' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'View SBOM' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'View raw data' })).toBeVisible();

    const viewVulnerabilitiesLink = screen.getByRole('link', {
      name: 'Reevaluate the report in order to enable Vulnerabilities view',
    });
    expect(viewVulnerabilitiesLink).toBeVisible();
    expect(viewVulnerabilitiesLink).toHaveTextContent(/view vulnerabilities/i);

    expect(screen.getByRole('link', { name: 'View legacy report' })).toBeVisible();
  });

  it('renders a disabled view vulnerabilities link if report version is less than 5', async () => {
    renderComponent();
    const options = screen.getByRole('button', { name: 'Options' });
    expect(options).toBeVisible();

    fireEvent.click(options);

    const vulnerabilities = screen.getByRole('link', {
      name: 'Reevaluate the report in order to enable Vulnerabilities view',
    });
    expect(vulnerabilities).toHaveClassName('disabled');
    expect(vulnerabilities).toHaveTextContent(/view vulnerabilities/i);

    fireEvent.mouseOver(vulnerabilities);
    const tooltip = await screen.findByText('Reevaluate the report in order to enable Vulnerabilities view');
    expect(tooltip).not.toBeNull();
  });

  it('renders an enabled view vulnerabilities link if report version is greater than 5', () => {
    selectSelectedReportSpy.and.returnValue({
      reportVersion: 7,
    });
    renderComponent();
    const options = screen.getByText('Options');
    expect(options).toBeVisible();

    fireEvent.click(options);

    const vulnerabilitiesLink = screen.getByRole('link', { name: 'View vulnerabilities' });
    expect(vulnerabilitiesLink).not.toHaveClassName('disabled');
    expect(vulnerabilitiesLink).toHaveTextContent(/view vulnerabilities/i);

    fireEvent.click(vulnerabilitiesLink);
    expect(routerActions.stateGo).toHaveBeenCalledWith('applicationReport.vulnerabilities', {
      publicId: 'publicId',
      scanId: 'scanId',
    });
  });

  it('renders a disabled view vulnerabilities link if report version is lower than 5', () => {
    selectSelectedReportSpy.and.returnValue({
      reportVersion: 4,
    });
    renderComponent();
    const options = screen.getByText('Options');
    expect(options).toBeVisible();

    fireEvent.click(options);

    const vulnerabilitiesLink = screen.getByRole('link', {
      name: 'Reevaluate the report in order to enable Vulnerabilities view',
    });
    expect(vulnerabilitiesLink).toHaveClassName('disabled');
    expect(vulnerabilitiesLink).toHaveTextContent(/view vulnerabilities/i);

    fireEvent.click(vulnerabilitiesLink);
    expect(routerActions.stateGo).not.toHaveBeenCalled();
  });

  it('calls reevaluateReport when the reevaluateReport button is pressed', () => {
    renderComponent();
    const reevaluateReport = screen.getByRole('button', { name: 'Re-Evaluate Report' });
    expect(reevaluateReport).toBeVisible();

    fireEvent.click(reevaluateReport);
    expect(mockedReevaluateReport).toHaveBeenCalled();
  });

  it('renders a description with time value and triggered by scan type', () => {
    renderComponent();
    const description = screen.getByText(
      `Triggered by ${metadataDetails.scanTriggerType} on 2018-11-11 15:13:11 UTC-05:00`
    );
    expect(description).toBeVisible();
  });

  it('renders a description with triggered by scan type from continuous monitoring', () => {
    selectApplicationReportMetaDataSpy.and.returnValue({ ...metadataDetails, forMonitoring: true });
    renderComponent();
    const description = screen.getByText(
      `Triggered by ${metadataDetails.scanTriggerType} (Continuous Monitoring) on 2018-11-11 15:13:11 UTC-05:00`
    );
    expect(description).toBeVisible();
  });

  it('renders a description with triggered by scan type from re-evaluation', () => {
    selectApplicationReportMetaDataSpy.and.returnValue({ ...metadataDetails, reevaluation: true });
    renderComponent();
    const description = screen.getByText(
      `Triggered by ${metadataDetails.scanTriggerType} (Re-evaluation) on 2018-11-11 15:13:11 UTC-05:00`
    );
    expect(description).toBeVisible();
  });
});
