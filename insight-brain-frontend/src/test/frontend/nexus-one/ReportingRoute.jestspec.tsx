/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import { ReportingRoute } from 'MainRoot/nexus-one/ReportingRoute';

jest.mock('MainRoot/enterpriseReporting/EnterpriseReportingLandingPage', () => ({
  __esModule: true,
  default: () => <div>Enterprise Reporting Page</div>,
}));

jest.mock('MainRoot/operationalReporting/OperationalReportingLandingPage', () => ({
  __esModule: true,
  default: () => <div>Operational Reporting Page</div>,
}));

const preloadedState = (loading: boolean, productFeatures: Record<string, boolean>) => ({
  productFeatures: {
    loading,
    productFeatures,
  },
});

describe('ReportingRoute', () => {
  it('renders Enterprise Reporting when integrated enterprise reporting is supported', () => {
    render(<ReportingRoute />, { preloadedState: preloadedState(false, { 'integrated-enterprise-reporting': true }) });

    expect(screen.getByText('Enterprise Reporting Page')).toBeInTheDocument();
    expect(screen.queryByText('Operational Reporting Page')).not.toBeInTheDocument();
  });

  it('renders Operational Reporting when integrated enterprise reporting is not supported', () => {
    render(<ReportingRoute />, { preloadedState: preloadedState(false, { 'integrated-enterprise-reporting': false }) });

    expect(screen.getByText('Operational Reporting Page')).toBeInTheDocument();
    expect(screen.queryByText('Enterprise Reporting Page')).not.toBeInTheDocument();
  });

  it('renders the loading spinner only while product features are not yet loaded', () => {
    render(<ReportingRoute />, { preloadedState: preloadedState(true, {}) });

    expect(screen.getByText(/Loading/)).toBeInTheDocument();
    expect(screen.queryByText('Enterprise Reporting Page')).not.toBeInTheDocument();
    expect(screen.queryByText('Operational Reporting Page')).not.toBeInTheDocument();
  });

  it('keeps the landing page mounted when features reload after first load', () => {
    render(<ReportingRoute />, { preloadedState: preloadedState(true, { 'integrated-enterprise-reporting': true }) });

    expect(screen.getByText('Enterprise Reporting Page')).toBeInTheDocument();
  });
});
