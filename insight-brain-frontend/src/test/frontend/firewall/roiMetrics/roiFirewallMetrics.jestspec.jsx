/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, waitFor, screen, fireEvent } from 'TestRoot/SpecUtil';
import RoiFirewallMetrics from 'MainRoot/firewall/roiMetrics/RoiFirewallMetrics';

import { getPermissionContextTestUrl } from 'MainRoot/util/CLMContextLocation';

describe('roiFirewallMetrics', () => {
  let axiosMock;
  const ROI_DESCRIPTION_TEXT_CONFIG_PERMISSION_TRUE =
    'The metrics below highlights the Return on Investment (ROI) ' +
    'of your organization’s partnership with Sonatype. Configure the values for each category based on your industry ' +
    'to provide accurate results. Configure ROI values';
  const ROI_DESCRIPTION_TEXT_CONFIG_PERMISSION_FALSE =
    'The metrics below highlights the Return on Investment (ROI) ' +
    'of your organization’s partnership with Sonatype.';

  const renderComponent = () => render(<RoiFirewallMetrics />);

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
  });

  it('renders the static content correctly', async () => {
    axiosMock.onPut(getPermissionContextTestUrl('global', 'global')).reply(200, ['CONFIGURE_SYSTEM']);

    const { container } = renderComponent();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByRole('heading', { name: 'Return on Investment (ROI)' })).toBeVisible();
    expect(screen.getByTestId('roi-firewall-metrics-description')).toHaveTextContent(
      ROI_DESCRIPTION_TEXT_CONFIG_PERMISSION_TRUE
    );
    expect(screen.getByTestId('roi-firewall-metrics-total')).toHaveTextContent('Total USD Saved$600,000');

    expect(screen.getByTestId('roi-firewall-metrics-content__title__malware-attacks-prevented')).toHaveTextContent(
      'Malware attacks prevented'
    );
    expect(screen.getByTestId('roi-firewall-metrics-content__title__namespace-attacks-prevented')).toHaveTextContent(
      'Namespace attacks prevented'
    );
    expect(screen.getByTestId('roi-firewall-metrics-content__title__safe-components-auto-selected')).toHaveTextContent(
      'Safe Components Auto-selected'
    );

    let tooltip;
    const icons = container.querySelectorAll('.roi-firewall-metrics-content__icon');
    fireEvent.mouseOver(icons[0]);
    tooltip = await screen.findByRole('tooltip', {
      name: 'Determined based on the number of Malware attacks prevented and the ROI value configured per attack.',
    });
    expect(tooltip).toBeInTheDocument();
    fireEvent.mouseOver(icons[1]);
    tooltip = await screen.findByRole('tooltip', {
      name: 'Determined based on the number of namespace attacks protected and the ROI value configured per attack.',
    });
    expect(tooltip).toBeInTheDocument();
    fireEvent.mouseOver(icons[2]);
    tooltip = await screen.findByRole('tooltip', {
      name: 'Determined based on the number of safe components auto-selected and the ROI value configured per attack.',
    });
    expect(tooltip).toBeInTheDocument();

    expect(screen.getByTestId('roi-firewall-metrics-content__value__malware-attacks-prevented')).toHaveTextContent(
      '$100,000'
    );
    expect(screen.getByTestId('roi-firewall-metrics-content__value__namespace-attacks-prevented')).toHaveTextContent(
      '$200,000'
    );
    expect(screen.getByTestId('roi-firewall-metrics-content__value__safe-components-auto-selected')).toHaveTextContent(
      '$300,000'
    );
  });

  describe('renders the ROI description text correctly when', () => {
    it('configure system permission is true', async () => {
      axiosMock.onPut(getPermissionContextTestUrl('global', 'global')).reply(200, ['CONFIGURE_SYSTEM']);

      renderComponent();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      expect(screen.getByTestId('roi-firewall-metrics-description')).toHaveTextContent(
        ROI_DESCRIPTION_TEXT_CONFIG_PERMISSION_TRUE
      );
    });

    it('configure system permission is false', async () => {
      axiosMock.onPut(getPermissionContextTestUrl('global', 'global')).reply(200, []);

      renderComponent();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      expect(screen.getByTestId('roi-firewall-metrics-description')).toHaveTextContent(
        ROI_DESCRIPTION_TEXT_CONFIG_PERMISSION_FALSE
      );
    });
  });

  it('renders error message when loading fails', async () => {
    axiosMock.onPut(getPermissionContextTestUrl('global', 'global')).reply(500, 'Error');

    renderComponent();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const alert = await screen.findByRole('alert');
    expect(alert).toBeVisible();
    expect(alert).toHaveTextContent('An error occurred loading data. Error');
  });
});
