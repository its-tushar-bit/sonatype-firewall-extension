/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, waitFor, screen } from 'TestRoot/SpecUtil';

import ZscalerConfigLimits from 'MainRoot/configuration/zscaler/ZscalerConfigLimits';

describe('ZscalerConfigLimits', () => {
  const initialProps = {
    zscalerConfigLimitsState: {
      loading: false,
      error: null,
      limits: {
        totalAllowedUrls: 10000,
        remainingUrls: 4000,
        status: 'under',
      },
    },
    loadLimits: jest.fn(),
  };

  const setState = (additionalProps = {}) => Object.freeze({ ...initialProps, ...additionalProps });

  const renderComponent = (props = setState()) => render(<ZscalerConfigLimits {...props} />);

  it('renders the component with correct data', async () => {
    renderComponent();
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByRole('heading', { level: 3, name: /Total Purchased/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 3, name: /Remaining/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 3, name: /Status/i })).toBeInTheDocument();
    expect(screen.getByText('10,000')).toBeInTheDocument();
    expect(screen.getByText('4,000')).toBeInTheDocument();
    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('OSS Malware Catalog Synced');
  });

  it('renders the correct indicator for not configured limits', async () => {
    const props = setState({
      zscalerConfigLimitsState: {
        limits: {
          totalAllowedUrls: 10000,
          remainingUrls: 0,
          status: 'none',
        },
      },
    });
    renderComponent(props);
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByRole('status')).toHaveTextContent('Not Configured');
  });

  it('renders the correct indicator and an error alert for over limits', async () => {
    const props = setState({
      zscalerConfigLimitsState: {
        limits: {
          totalAllowedUrls: 10000,
          remainingUrls: 1000,
          status: 'over',
        },
      },
    });
    renderComponent(props);
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByRole('status')).toHaveTextContent('Zscaler Custom URL Limit Exceeded');
    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Zscaler Custom URL limit exceeded. Some URLs were not added to the Sonatype Custom URL Categories. ' +
        'Review your configured formats or contact Zscaler to increase your Custom URL allowance for full ' +
        'protection against malware in open-source components.Learn more about Zscaler Custom URL limits'
    );
    expect(screen.getByRole('link')).toHaveTextContent('Learn more about Zscaler Custom URL limits');
    expect(screen.getByRole('link')).toHaveAttribute(
      'href',
      'https://links.sonatype.com/products/nxrm3/docs/zscaler/main'
    );
  });
});
