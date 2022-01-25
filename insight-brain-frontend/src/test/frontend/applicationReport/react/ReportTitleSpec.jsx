/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import moment from 'moment-timezone';

import { render, screen, fireEvent } from '../../SpecUtil';
import ReportTitle from 'MainRoot/applicationReport/react/ReportTitle';

describe('ReportTitle', () => {
  let renderComponent, minimalProps, mockedReevaluateReport, metadataDetails;

  beforeAll(() => {
    moment.tz.setDefault('America/New_York');
  });

  afterAll(() => {
    moment.tz.setDefault();
  });

  beforeEach(() => {
    mockedReevaluateReport = jasmine.createSpy('reevaluateReport');
    metadataDetails = {
      scanTriggerType: 'Unknown',
      reportTitle: 'Title',
      reportTime: moment('2018-11-11 15:13:11').toDate().getTime(),
      application: {
        id: 'metadataApplicationId',
        name: 'App Name',
      },
    };
    minimalProps = {
      metadataDetails,
      publicId: 'publicId',
      scanId: 'scanId',
      selectedReport: {
        reportVersion: 3,
      },
      reevaluateReport: mockedReevaluateReport,
    };

    renderComponent = (optionalProps) => {
      render(<ReportTitle {...minimalProps} {...optionalProps} />);
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
    expect(screen.getByRole('link', { name: 'View vulnerabilities' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'View legacy report' })).toBeVisible();
  });

  it('renders a disabled view vulnerabilities link if report version is less than 5', async () => {
    renderComponent();
    const options = screen.getByRole('button', { name: 'Options' });
    expect(options).toBeVisible();
    fireEvent.click(options);

    const vulnerabilities = screen.getByRole('link', { name: 'View vulnerabilities' });
    expect(vulnerabilities).toHaveClassName('disabled');

    fireEvent.mouseOver(vulnerabilities);
    const tooltip = await screen.findByText('Reevaluate the report in order to enable Vulnerabilities view');
    expect(tooltip).not.toBeNull();
  });

  it('renders an enabled view vulnerabilities link if report version is greater than 5', () => {
    const props = {
      metadataDetails: {
        reportTitle: 'Title',
        application: {
          name: 'App Name',
        },
      },
      publicId: 'publicId',
      scanId: 'scanId',
      selectedReport: {
        reportVersion: 7,
      },
    };

    renderComponent(props);
    const options = screen.getByText('Options');
    expect(options).toBeVisible();
    fireEvent.click(options);

    const vulnerabilities = screen.getByRole('link', { name: 'View vulnerabilities' });
    expect(vulnerabilities).not.toHaveClassName('disabled');
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
    const props = {
      metadataDetails: { ...metadataDetails, forMonitoring: true },
    };
    renderComponent(props);
    const description = screen.getByText(
      `Triggered by ${metadataDetails.scanTriggerType} (Continuous Monitoring) on 2018-11-11 15:13:11 UTC-05:00`
    );
    expect(description).toBeVisible();
  });

  it('renders a description with triggered by scan type from re-evaluation', () => {
    const props = {
      metadataDetails: { ...metadataDetails, reevaluation: true },
    };
    renderComponent(props);
    const description = screen.getByText(
      `Triggered by ${metadataDetails.scanTriggerType} (Re-evaluation) on 2018-11-11 15:13:11 UTC-05:00`
    );
    expect(description).toBeVisible();
  });
});
