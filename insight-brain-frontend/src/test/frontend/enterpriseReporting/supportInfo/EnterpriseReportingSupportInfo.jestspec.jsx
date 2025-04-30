/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, waitFor, fireEvent } from 'TestRoot/SpecUtil';
import { screen } from '@testing-library/dom';
import { getTelemetryStatusUrl } from 'MainRoot/util/CLMLocation';
import EnterpriseReportingSupportInfo from 'MainRoot/enterpriseReporting/supportInfo/EnterpiseReportingSupportInfo';

describe('EnterpriseReportingSupportInfo', () => {
  let axiosMock;

  const initialState = {
    loading: false,
    telemetryStatus: {},
    loadError: null,
  };

  const telemetryData = {
    telemetryId: '12345',
    clusterId: '12345-678',
    advancedReportingEnabled: true,
    enterpriseReportingFeatureExists: true,
    userApplicationCount: 50,
    totalApplicationCount: 100,
  };

  const renderSupportInfo = () => render(<EnterpriseReportingSupportInfo {...initialState} />);

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet(getTelemetryStatusUrl()).reply(200, telemetryData);
  });

  it('renders an error alert if telemetry not loaded', async () => {
    axiosMock.onGet(getTelemetryStatusUrl()).reply(400, 'Bad Request');
    renderSupportInfo();

    await waitFor(() => {
      expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });

    const errorAlert = screen.getByRole('alert');
    expect(errorAlert).toHaveTextContent('An error occurred loading data. Bad Request');
  });

  it('renders a button when telemetry is loaded', async () => {
    renderSupportInfo();
    expect(await screen.findByRole('button', { name: 'Copy Support Info to Clipboard' })).toBeInTheDocument();
  });

  it('renders a text link to help documentation', async () => {
    renderSupportInfo();
    expect(screen.getByRole('link', { name: 'Enterprise Reporting' })).toBeInTheDocument();
  });

  describe('copying', () => {
    beforeEach(() => {
      const writeText = jest.fn();
      Object.assign(navigator, {
        clipboard: {
          writeText,
        },
      });
    });

    it('copies telemetry status info with window.navigator.clipboard', async () => {
      renderSupportInfo();
      const copyButton = await screen.findByRole('button', { name: 'Copy Support Info to Clipboard' });
      await waitFor(() => {
        fireEvent.click(copyButton);
      });

      const stringifiedTelemetryData = JSON.stringify(telemetryData, null, ' ');
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith(stringifiedTelemetryData);
    });

    it('renders a validation message "Support info copied to clipboard" after copying', async () => {
      renderSupportInfo();
      const copyButton = await screen.findByRole('button', { name: 'Copy Support Info to Clipboard' });
      expect(screen.queryByText('Support info copied to clipboard')).not.toBeInTheDocument();
      await waitFor(() => {
        fireEvent.click(copyButton);
      });

      expect(screen.getByText('Support info copied to clipboard')).toBeInTheDocument();
    });
  });
});
