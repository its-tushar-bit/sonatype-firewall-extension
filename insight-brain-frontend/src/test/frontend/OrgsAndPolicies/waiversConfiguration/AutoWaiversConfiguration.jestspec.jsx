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
import { getAutoWaiversConfigurationURL, getAutoWaiversConfigurationURLWaiver } from 'MainRoot/util/CLMLocation';
import AutoWaiversConfiguration from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiversConfiguration';
import { fireEvent } from '@testing-library/react';

describe('Waivers Configuration Component', () => {
  let axiosMock, renderComponent;

  const preloadedState = {
    orgsAndPolicies: {
      root: {
        selectedOwner: {
          id: 'app',
          publicId: 'publicId',
          name: 'App',
        },
      },
    },
  };

  renderComponent = (initialState) =>
    render(<AutoWaiversConfiguration />, { preloadedState: preloadedState, ...initialState });

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

  it('displays validation error when saving without selecting at least one option when creating first auto policy waiver', async () => {
    render(<AutoWaiversConfiguration />);

    const pathForwardCheckbox = await screen.findByLabelText('No newer, non-violating component version is available');
    expect(pathForwardCheckbox).not.toBeChecked();

    const updateButton = await screen.findByRole('button', { name: 'Update' });
    updateButton.click();

    await waitFor(() => {
      expect(
        screen.getByText('There were validation errors. Can not save without selecting at least one option')
      ).toBeVisible();
    });
  });

  it('displays confirmation modal when disabling auto-waiver', async () => {
    axiosMock.onGet(getAutoWaiversConfigurationURL('application', 'app')).reply(200, {
      isInherited: false,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id',
      threatLevel: 7,
      autoPolicyWaiverOwnerId: 'some-owner-id',
      autoPolicyWaiverOwnerName: 'some-owner-name',
    });

    axiosMock.onGet(getAutoWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200, {
      pathForward: true,
      reachable: false,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
      creatorId: 'some-owner-id',
      creatorName: 'some-owner-name',
    });
    renderComponent();

    const pathForwardCheckbox = await screen.findByLabelText('No newer, non-violating component version is available');
    expect(pathForwardCheckbox).toBeChecked();
    fireEvent.click(pathForwardCheckbox);
    expect(pathForwardCheckbox).not.toBeChecked();

    const disableButton = await screen.findByRole('button', { name: 'Delete Auto Waiver' });
    disableButton.click();

    await waitFor(() => {
      expect(screen.getByText('Are you sure you want to delete this auto waiver configuration?')).toBeVisible();
    });
  });

  it('successfully saves waiver configuration when updating settings', async () => {
    axiosMock.onGet(getAutoWaiversConfigurationURL('application', 'app')).reply(200, {
      isInherited: false,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id',
      threatLevel: 7,
    });

    axiosMock.onGet(getAutoWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200, {
      pathForward: false,
      reachable: false,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    axiosMock.onPut(getAutoWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200);

    renderComponent();

    const pathForwardCheckbox = await screen.findByLabelText('No newer, non-violating component version is available');
    expect(pathForwardCheckbox).not.toBeChecked();

    // update path forward to true
    fireEvent.click(pathForwardCheckbox);
    expect(pathForwardCheckbox).toBeChecked();

    const updateButton = screen.getByRole('button', { name: 'Update' });
    fireEvent.click(updateButton);

    await waitFor(() => {
      expect(axiosMock.history.put.length).toBe(1);
      const putData = JSON.parse(axiosMock.history.put[0].data);
      expect(putData.pathForward).toBe(true);
    });
  });

  it('handles cancellation of disable waiver modal', async () => {
    axiosMock.onGet(getAutoWaiversConfigurationURL('application', 'app')).reply(200, {
      isInherited: false,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id',
      threatLevel: 7,
    });

    axiosMock.onGet(getAutoWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200, {
      pathForward: true,
      reachable: false,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    renderComponent();

    const pathForwardCheckbox = await screen.findByLabelText('No newer, non-violating component version is available');
    fireEvent.click(pathForwardCheckbox);

    const disableButton = screen.getByRole('button', { name: 'Delete Auto Waiver' });
    fireEvent.click(disableButton);

    // Find and click cancel button
    const cancelButton = screen.getByRole('button', { name: 'Delete' });
    fireEvent.click(cancelButton);

    // Modal should be closed
    expect(screen.queryByText('Are you sure you want to disable this auto waiver?')).not.toBeInTheDocument();
  });

  it('successfully deletes waiver when confirmed', async () => {
    axiosMock.onGet(getAutoWaiversConfigurationURL('application', 'app')).reply(200, {
      isInherited: false,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id',
      threatLevel: 7,
    });

    axiosMock.onGet(getAutoWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200, {
      pathForward: true,
      reachable: false,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    axiosMock.onDelete(getAutoWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200);

    renderComponent();

    const pathForwardCheckbox = await screen.findByLabelText('No newer, non-violating component version is available');
    fireEvent.click(pathForwardCheckbox);

    const disableButton = screen.getByRole('button', { name: 'Delete Auto Waiver' });
    fireEvent.click(disableButton);

    const confirmButton = screen.getByRole('button', { name: 'Delete' });
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(axiosMock.history.delete.length).toBe(1);
    });

    expect(screen.getByLabelText('No newer, non-violating component version is available')).not.toBeChecked();
  });

  it('displays error message when saving fails', async () => {
    axiosMock.onGet(getAutoWaiversConfigurationURL('application', 'app')).reply(200, {
      isInherited: false,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id',
      threatLevel: 7,
    });

    axiosMock.onGet(getAutoWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200, {
      pathForward: false,
      reachable: false,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    axiosMock.onPut(getAutoWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(500, {
      message: 'Failed to save configuration',
    });

    renderComponent();

    const pathForwardCheckbox = await screen.findByLabelText('No newer, non-violating component version is available');
    fireEvent.click(pathForwardCheckbox);

    const updateButton = screen.getByRole('button', { name: 'Update' });
    fireEvent.click(updateButton);

    await waitFor(() => {
      expect(screen.getByText('An error occurred saving data. Failed to save configuration')).toBeVisible();
    });
  });

  it('renders sublabel for Max Threat Level', async () => {
    render(<AutoWaiversConfiguration />);

    expect(await screen.findByText('Violations with higher threats will not be waived')).toBeVisible();
  });

  it('renders sublabel for Scope', async () => {
    render(<AutoWaiversConfiguration />);

    expect(await screen.findByText('Eligible violations will be waived if/when:')).toBeVisible();
    expect(await screen.findByText('No Upgrade Path')).toBeVisible();
  });

  it('shows default threat level of 7 when creating new waiver', async () => {
    render(<AutoWaiversConfiguration />);
    expect(await screen.findByRole('button', { name: /7 - Severe/i })).toBeVisible();
  });

  it('maintains selected threat level after form submission', async () => {
    render(<AutoWaiversConfiguration />);
    const dropdown = await screen.findByRole('button', { name: /7 - Severe/i });

    fireEvent.click(dropdown);
    fireEvent.click(screen.getByRole('button', { name: /3 - Moderate/i }));

    const updateButton = screen.getByRole('button', { name: 'Update' });
    fireEvent.click(updateButton);

    expect(await screen.findByRole('button', { name: /3 - Moderate/i })).toBeVisible();
  });
});
