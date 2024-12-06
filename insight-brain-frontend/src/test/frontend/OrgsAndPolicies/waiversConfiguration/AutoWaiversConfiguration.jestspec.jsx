/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from 'TestRoot/SpecUtil';
import axiosMockAdapter from 'axios-mock-adapter';
import axios from 'axios';
import * as ProductFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import { getAutoWaiversConfigurationURL } from 'MainRoot/util/CLMLocation';
import AutoWaiversConfiguration from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiversConfiguration';

describe('Waivers Configuration Component', () => {
  let axiosMock;

  beforeEach(() => {
    jest.spyOn(ProductFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(true);
    jest.spyOn(ProductFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(true);
    axiosMock = new axiosMockAdapter(axios);

    axiosMock.onGet(getAutoWaiversConfigurationURL('organization', 'ROOT_ORGANIZATION_ID')).reply(200, {
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
    render(<AutoWaiversConfiguration />);

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
    render(<AutoWaiversConfiguration />);

    expect(await screen.findByText('Max. Threat Level')).toBeVisible();

    expect(axiosMock.history.get.length).toBe(2);
  });

  it('renders the update button', async () => {
    render(<AutoWaiversConfiguration />);
    expect(await screen.findByRole('button', { name: 'Update' })).toBeVisible();

    expect(axiosMock.history.get.length).toBe(2);
  });

  it('renders checkbox depending on the state of "noPathForward"', async () => {
    render(<AutoWaiversConfiguration />);
    const noPathForwardCheckbox = await screen.findByLabelText(
      'No newer, non-violating component version is available'
    );

    expect(noPathForwardCheckbox).toBeInTheDocument();
    expect(noPathForwardCheckbox).not.toBeChecked();

    expect(axiosMock.history.get.length).toBe(2);
  });

  it('renders LicenseLockScreenForWaivers when auto waiver feature flag is disabled', async () => {
    jest.spyOn(ProductFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(false);
    render(<AutoWaiversConfiguration />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Automated Waivers' })).toBeVisible();
      expect(screen.getByTestId('iq-integrations__missing-license')).toBeVisible();
    });
  });

  it('renders LicenseLockScreenForWaivers when developer dashboard feature flag is disabled', async () => {
    jest.spyOn(ProductFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(false);
    render(<AutoWaiversConfiguration />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Automated Waivers' })).toBeVisible();
      expect(screen.getByTestId('iq-integrations__missing-license')).toBeVisible();
    });
  });

  it('renders LicenseLockScreenForWaivers when one of feature flags is disabled', async () => {
    jest.spyOn(ProductFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(true);
    jest.spyOn(ProductFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(false);
    render(<AutoWaiversConfiguration />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Automated Waivers' })).toBeVisible();
      expect(screen.getByTestId('iq-integrations__missing-license')).toBeVisible();
    });
  });

  it('renders LicenseLockScreenForWaivers when isSbomManager is true', async () => {
    jest.spyOn(ProductFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(true);
    jest.spyOn(ProductFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(true);
    jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(true);
    render(<AutoWaiversConfiguration />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Automated Waivers' })).toBeVisible();
      expect(screen.getByTestId('iq-integrations__missing-license')).toBeVisible();
    });
  });
});
