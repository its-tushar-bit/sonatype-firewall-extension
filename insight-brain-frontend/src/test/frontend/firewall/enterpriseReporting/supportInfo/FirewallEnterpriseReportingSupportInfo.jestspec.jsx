/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, waitFor, fireEvent } from 'TestRoot/SpecUtil';
import { screen } from '@testing-library/dom';
import { getTelemetryStatusUrl } from 'MainRoot/util/CLMLocation';
import FirewallEnterpriseReportingSupportInfo from 'MainRoot/firewall/enterpriseReporting/supportInfo/FirewallEnterpriseReportingSupportInfo';

describe('FirewallEnterpriseReportingSupportInfo', () => {
  let axiosMock;

  const defaultPreloadedState = {
    enterpriseReportingSupportInfo: {
      loading: false,
      telemetryStatus: {},
      loadError: null,
    },
  };

  const telemetryData = {
    telemetryId: '12345',
    clusterId: '12345-678',
    advancedReportingEnabled: true,
    enterpriseReportingFeatureExists: true,
    userApplicationCount: 50,
    totalApplicationCount: 100,
  };

  const renderSupportInfo = (preloadedState) =>
    render(<FirewallEnterpriseReportingSupportInfo />, {
      preloadedState: preloadedState || defaultPreloadedState,
    });

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet(getTelemetryStatusUrl()).reply(200, telemetryData);
  });

  afterEach(() => {
    jest.restoreAllMocks();
    axiosMock.reset();
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
    const loadedState = {
      enterpriseReportingSupportInfo: {
        loading: false,
        telemetryStatus: telemetryData,
        loadError: null,
      },
    };
    renderSupportInfo(loadedState);
    expect(await screen.findByRole('button', { name: 'Copy Support Info to Clipboard' })).toBeInTheDocument();
  });

  it('renders a text link to help documentation', async () => {
    renderSupportInfo();
    expect(screen.getByRole('link', { name: 'Enterprise Reporting' })).toBeInTheDocument();
  });

  it('renders the correct help documentation link for Firewall', () => {
    renderSupportInfo();
    const link = screen.getByRole('link', { name: 'Enterprise Reporting' });
    expect(link).toHaveAttribute('href', 'https://links.sonatype.com/products/firewall/enterprise-reporting');
  });

  it('renders Help Documentation and Support Information headings', () => {
    renderSupportInfo();
    expect(screen.getByRole('heading', { name: 'Help Documentation' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Support Information' })).toBeInTheDocument();
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
      const loadedState = {
        enterpriseReportingSupportInfo: {
          loading: false,
          telemetryStatus: telemetryData,
          loadError: null,
        },
      };
      renderSupportInfo(loadedState);
      const copyButton = await screen.findByRole('button', { name: 'Copy Support Info to Clipboard' });
      await waitFor(() => {
        fireEvent.click(copyButton);
      });

      const stringifiedTelemetryData = JSON.stringify(telemetryData, null, ' ');
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith(stringifiedTelemetryData);
    });

    it('renders a validation message "Support info copied to clipboard" after copying', async () => {
      const loadedState = {
        enterpriseReportingSupportInfo: {
          loading: false,
          telemetryStatus: telemetryData,
          loadError: null,
        },
      };
      renderSupportInfo(loadedState);
      const copyButton = await screen.findByRole('button', { name: 'Copy Support Info to Clipboard' });
      expect(screen.queryByText('Support info copied to clipboard')).not.toBeInTheDocument();
      await waitFor(() => {
        fireEvent.click(copyButton);
      });

      expect(screen.getByText('Support info copied to clipboard')).toBeInTheDocument();
    });

    it('displays check icon when copy is successful', async () => {
      const loadedState = {
        enterpriseReportingSupportInfo: {
          loading: false,
          telemetryStatus: telemetryData,
          loadError: null,
        },
      };
      renderSupportInfo(loadedState);
      const copyButton = await screen.findByRole('button', { name: 'Copy Support Info to Clipboard' });

      await waitFor(() => {
        fireEvent.click(copyButton);
      });

      // After clicking, the icon should change (check for the icon via class)
      const iconElement = copyButton.querySelector('.nx-icon.copied');
      expect(iconElement).toBeInTheDocument();
    });

    it('handles clipboard write error gracefully', async () => {
      const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      const clipboardError = new Error('Permission denied');
      navigator.clipboard.writeText.mockRejectedValueOnce(clipboardError);

      const loadedState = {
        enterpriseReportingSupportInfo: {
          loading: false,
          telemetryStatus: telemetryData,
          loadError: null,
        },
      };
      renderSupportInfo(loadedState);
      const copyButton = await screen.findByRole('button', { name: 'Copy Support Info to Clipboard' });

      fireEvent.click(copyButton);

      await waitFor(() => {
        expect(consoleErrorSpy).toHaveBeenCalledWith('Failed to copy to clipboard:', clipboardError);
      });

      // Should not show success message
      expect(screen.queryByText('Support info copied to clipboard')).not.toBeInTheDocument();

      consoleErrorSpy.mockRestore();
    });
  });

  it('shows loading state initially', () => {
    const loadingState = {
      enterpriseReportingSupportInfo: {
        loading: true,
        telemetryStatus: null,
        loadError: null,
      },
    };
    renderSupportInfo(loadingState);

    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  it('calls load action on component mount', async () => {
    renderSupportInfo();

    await waitFor(() => {
      expect(axiosMock.history.get.length).toBeGreaterThanOrEqual(1);
    });

    const telemetryRequest = axiosMock.history.get.find((req) => req.url.includes('telemetry/status'));
    expect(telemetryRequest).toBeDefined();
  });
});
