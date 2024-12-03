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
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import { getWaiversConfigurationURL, getWaiversConfigurationURLWaiver } from 'MainRoot/util/CLMLocation';
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
    render(<WaiversConfiguration />, { preloadedState: preloadedState, ...initialState });

  beforeEach(() => {
    jest.spyOn(ProductFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(true);
    jest.spyOn(ProductFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(true);
    axiosMock = new axiosMockAdapter(axios);
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
    const noPathForwardCheckbox = await screen.findByLabelText(
      'No newer, non-violating component version is available'
    );

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

  it('renders LicenseLockScreenForWaivers when isSbomManager is true', async () => {
    jest.spyOn(ProductFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(true);
    jest.spyOn(ProductFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(true);
    jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(true);
    render(<WaiversConfiguration />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Automated Waivers' })).toBeVisible();
      expect(screen.getByTestId('iq-integrations__missing-license')).toBeVisible();
    });
  });

  it('displays validation error when saving without selecting at least one option when creating first auto policy waiver', async () => {
    render(<WaiversConfiguration />);

    const reachableCheckbox = await screen.findByLabelText('Security vulnerability is Not Reachable');
    expect(reachableCheckbox).not.toBeChecked();

    const updateButton = await screen.findByRole('button', { name: 'Update' });
    updateButton.click();

    await waitFor(() => {
      expect(
        screen.getByText('There were validation errors. Can not save without selecting at least one option')
      ).toBeVisible();
    });
  });

  it('displays confirmation modal when disabling auto-waiver', async () => {
    axiosMock.onGet(getWaiversConfigurationURL('application', 'app')).reply(200, {
      isInherited: false,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id',
      threatLevel: 7,
      autoPolicyWaiverOwnerId: 'some-owner-id',
      autoPolicyWaiverOwnerName: 'some-owner-name',
    });

    axiosMock.onGet(getWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200, {
      pathForward: false,
      reachable: true,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
      creatorId: 'some-owner-id',
      creatorName: 'some-owner-name',
    });
    renderComponent();

    const reachableCheckbox = await screen.findByLabelText('Security vulnerability is Not Reachable');
    expect(reachableCheckbox).toBeChecked();
    fireEvent.click(reachableCheckbox);
    expect(reachableCheckbox).not.toBeChecked();

    const disableButton = await screen.findByRole('button', { name: 'Delete Auto Waiver' });
    disableButton.click();

    await waitFor(() => {
      expect(screen.getByText('Are you sure you want to delete this auto waiver configuration?')).toBeVisible();
    });
  });

  it('successfully saves waiver configuration when updating settings', async () => {
    axiosMock.onGet(getWaiversConfigurationURL('application', 'app')).reply(200, {
      isInherited: false,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id',
      threatLevel: 7,
    });

    axiosMock.onGet(getWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200, {
      pathForward: true,
      reachable: false,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    axiosMock.onPut(getWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200);

    renderComponent();

    const pathForwardCheckbox = await screen.findByLabelText('No newer, non-violating component version is available');
    expect(pathForwardCheckbox).toBeChecked();

    // update reachable to true
    const reachableCheckbox = await screen.findByLabelText('Security vulnerability is Not Reachable');
    fireEvent.click(reachableCheckbox);
    expect(reachableCheckbox).toBeChecked();

    const updateButton = screen.getByRole('button', { name: 'Update' });
    fireEvent.click(updateButton);

    await waitFor(() => {
      expect(axiosMock.history.put.length).toBe(1);
      const putData = JSON.parse(axiosMock.history.put[0].data);
      expect(putData.reachable).toBe(true);
    });
  });

  it('handles cancellation of disable waiver modal', async () => {
    axiosMock.onGet(getWaiversConfigurationURL('application', 'app')).reply(200, {
      isInherited: false,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id',
      threatLevel: 7,
    });

    axiosMock.onGet(getWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200, {
      pathForward: false,
      reachable: true,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    renderComponent();

    const reachableCheckbox = await screen.findByLabelText('Security vulnerability is Not Reachable');
    fireEvent.click(reachableCheckbox);

    const disableButton = screen.getByRole('button', { name: 'Delete Auto Waiver' });
    fireEvent.click(disableButton);

    // Find and click cancel button
    const cancelButton = screen.getByRole('button', { name: 'Delete' });
    fireEvent.click(cancelButton);

    // Modal should be closed
    expect(screen.queryByText('Are you sure you want to disable this auto waiver?')).not.toBeInTheDocument();
  });

  it('successfully deletes waiver when confirmed', async () => {
    axiosMock.onGet(getWaiversConfigurationURL('application', 'app')).reply(200, {
      isInherited: false,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id',
      threatLevel: 7,
    });

    axiosMock.onGet(getWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200, {
      pathForward: false,
      reachable: true,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    axiosMock.onDelete(getWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200);

    renderComponent();

    const reachableCheckbox = await screen.findByLabelText('Security vulnerability is Not Reachable');
    fireEvent.click(reachableCheckbox);

    const disableButton = screen.getByRole('button', { name: 'Delete Auto Waiver' });
    fireEvent.click(disableButton);

    const confirmButton = screen.getByRole('button', { name: 'Delete' });
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(axiosMock.history.delete.length).toBe(1);
    });

    expect(screen.getByLabelText('Security vulnerability is Not Reachable')).not.toBeChecked();
    expect(screen.getByLabelText('No newer, non-violating component version is available')).not.toBeChecked();
  });

  it('displays error message when saving fails', async () => {
    axiosMock.onGet(getWaiversConfigurationURL('application', 'app')).reply(200, {
      isInherited: false,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id',
      threatLevel: 7,
    });

    axiosMock.onGet(getWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200, {
      pathForward: false,
      reachable: false,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    axiosMock.onPut(getWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(500, {
      message: 'Failed to save configuration',
    });

    renderComponent();

    const reachableCheckbox = await screen.findByLabelText('Security vulnerability is Not Reachable');
    fireEvent.click(reachableCheckbox);

    const updateButton = screen.getByRole('button', { name: 'Update' });
    fireEvent.click(updateButton);

    await waitFor(() => {
      expect(screen.getByText('An error occurred saving data. Failed to save configuration')).toBeVisible();
    });
  });
});
