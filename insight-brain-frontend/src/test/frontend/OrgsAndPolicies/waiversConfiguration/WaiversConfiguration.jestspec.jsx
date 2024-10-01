/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import WaiversConfiguration from 'MainRoot/OrgsAndPolicies/waiversConfiguration/WaiversConfiguration';
import { render, screen, waitFor } from 'TestRoot/SpecUtil';
import axiosMockAdapter from 'axios-mock-adapter';
import axios from 'axios';
import * as ProductFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import { getWaiversConfigurationURL } from 'MainRoot/util/CLMLocation';

describe('Waivers Configuration Component', () => {
  let axiosMock;

  beforeEach(() => {
    jest.spyOn(ProductFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(true);
    jest.spyOn(ProductFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(true);
    axiosMock = new axiosMockAdapter(axios);

    axiosMock.onGet(getWaiversConfigurationURL('organization', 'ROOT_ORGANIZATION_ID')).reply(200, {
      isInherited: false,
      isAutoWaiverEnabled: true,
      pathForward: false,
      reachable: false,
      threatLevel: 7,
    });
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('renders the page title and description', async () => {
    render(<WaiversConfiguration />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Automated Waivers' })).toBeVisible();
      expect(
        screen.getByText(
          'Limit disruptions by deprioritizing low-threat violations until a remediation path is available.'
        )
      ).toBeVisible();
    });

    expect(axiosMock.history.get.length).toBe(2);
  });

  it('renders the "Max. Threat Level" label', async () => {
    render(<WaiversConfiguration />);

    expect(await screen.findByText('Max. Threat Level')).toBeVisible();

    expect(axiosMock.history.get.length).toBe(2);
  });

  it('renders the update button', async () => {
    render(<WaiversConfiguration />);
    expect(await screen.findByRole('button', { name: 'Update' })).toBeVisible();

    expect(axiosMock.history.get.length).toBe(2);
  });

  it('renders checkboxes depending on the state of "reachable" and "noPathForward"', async () => {
    render(<WaiversConfiguration />);

    const notReachableCheckbox = await screen.findByLabelText('Security vulnerability is Not Reachable');
    const noPathForwardCheckbox = await screen.findByLabelText('Component version is current or latest non-violating');

    expect(notReachableCheckbox).toBeInTheDocument();
    expect(noPathForwardCheckbox).toBeInTheDocument();
    expect(notReachableCheckbox).not.toBeChecked();
    expect(noPathForwardCheckbox).not.toBeChecked();

    expect(axiosMock.history.get.length).toBe(2);
  });

  it('renders LicenseLockScreenForWaivers when auto waiver feature flag is disabled', async () => {
    jest.spyOn(ProductFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(false);
    render(<WaiversConfiguration />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Automated Waivers' })).toBeVisible();
      expect(screen.getByTestId('iq-integrations__missing-license')).toBeVisible();
    });
  });

  it('renders LicenseLockScreenForWaivers when developer dashboard feature flag is disabled', async () => {
    jest.spyOn(ProductFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(false);
    render(<WaiversConfiguration />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Automated Waivers' })).toBeVisible();
      expect(screen.getByTestId('iq-integrations__missing-license')).toBeVisible();
    });
  });

  it('renders LicenseLockScreenForWaivers when one of feature flags is disabled', async () => {
    jest.spyOn(ProductFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(true);
    jest.spyOn(ProductFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(false);
    render(<WaiversConfiguration />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Automated Waivers' })).toBeVisible();
      expect(screen.getByTestId('iq-integrations__missing-license')).toBeVisible();
    });
  });
});
