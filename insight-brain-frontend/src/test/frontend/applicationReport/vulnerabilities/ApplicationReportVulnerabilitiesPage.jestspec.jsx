/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, within, fireEvent } from 'TestRoot/SpecUtil';
import ApplicationReportVulnerabilitiesPage from 'MainRoot/applicationReport/vulnerabilities/ApplicationReportVulnerabilitiesPage';
import { formatDate } from 'MainRoot/util/dateUtils';

const publicId = 'publicAppId';
const scanId = 'scanId';

describe('ApplicationReportVulnerabilitiesPage', function () {
  let renderComponent;

  const loadAllReportDataSpy = jest.fn();
  const minimalProps = {
    loadError: '',
    loading: false,
    vulnerabilitiesPageEnabled: true,
    metadata: {
      reportTitle: 'fooReport',
      reportTime: 0,
      application: { name: 'foo app' },
    },
    vulnerabilities: [
      {
        displayName: {
          parts: [
            {
              value: 'Foo',
            },
          ],
        },
        securityCode: 'CVE-12345',
        cvssScore: 8.0,
        threat: 10,
      },
    ],
    loadReportAllData: loadAllReportDataSpy,
  };
  const defaultPreloadedState = {
    router: {
      currentParams: {
        publicId,
        scanId,
      },
    },
  };

  beforeEach(function () {
    renderComponent = (props = minimalProps) =>
      render(<ApplicationReportVulnerabilitiesPage {...props} />, {
        preloadedState: defaultPreloadedState,
      });
  });

  it('renders a loading spinner if loading prop is true', function () {
    renderComponent({ ...minimalProps, loading: true });

    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  it('renders a loading spinner if metadata prop is falsey', function () {
    renderComponent({ ...minimalProps, metadata: null });

    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  it('renders an error if loadError prop is set', function () {
    renderComponent({ ...minimalProps, loadError: 'some_error' });

    const errorAlert = screen.getByRole('alert');
    expect(errorAlert).toBeInTheDocument();
    expect(errorAlert).toHaveTextContent('some_error');
  });

  it('renders an error if vulnerabilitiesPageEnabled is not true', function () {
    renderComponent({ ...minimalProps, vulnerabilitiesPageEnabled: false });

    const errorAlert = screen.getByRole('alert');
    expect(errorAlert).toBeInTheDocument();
  });

  it('renders a retry button on the error that calls loadReportAllData when clicked', function () {
    renderComponent({ ...minimalProps, loadError: 'some_error' });

    const errorAlert = screen.getByRole('alert');
    expect(errorAlert).toBeInTheDocument();
    const retryBtn = within(errorAlert).getByRole('button', { name: /retry/i });

    fireEvent.click(retryBtn);
    expect(loadAllReportDataSpy).toHaveBeenCalled();
  });

  it('renders a metadata info on the header', function () {
    renderComponent();
    const expectedDate = formatDate(minimalProps.metadata.reportTime);

    expect(screen.getByRole('heading')).toHaveTextContent(minimalProps.metadata.application.name);
    expect(screen.getByText(expectedDate)).toBeInTheDocument();
  });

  it('renders the vulnerabilities table correctly', function () {
    renderComponent();
    const table = screen.getByRole('table');
    expect(table).toBeInTheDocument();

    const columnHeaders = within(table).getAllByRole('columnheader');
    const rows = within(table).getAllByRole('row');

    expect(columnHeaders.length).toBe(4);
    expect(columnHeaders[0]).toHaveAccessibleName(/threat/i);
    expect(columnHeaders[1]).toHaveAccessibleName(/security issue/i);
    expect(columnHeaders[2]).toHaveAccessibleName(/cvss score/i);
    expect(columnHeaders[3]).toHaveAccessibleName(/component/i);

    expect(rows.length).toBe(2);
    expect(rows[1]).toHaveTextContent(minimalProps.vulnerabilities[0].cvssScore);
    expect(rows[1]).toHaveTextContent(minimalProps.vulnerabilities[0].securityCode);
  });

  it('calls loadReportAllData on render', function () {
    renderComponent();

    expect(loadAllReportDataSpy).toHaveBeenCalled();
  });
});
