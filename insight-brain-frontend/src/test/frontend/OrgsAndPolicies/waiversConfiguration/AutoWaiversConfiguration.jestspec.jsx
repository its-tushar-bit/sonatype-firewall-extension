/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor, axiosMockAdapter, within } from 'TestRoot/SpecUtil';
import {
  getAutoWaiversConfigurationURL,
  getAutoWaiversConfigurationURLWaiver,
  getProductFeaturesUrl,
  getAutoWaiverExclusionsByAutoWaiverIdUrl,
  getAutoWaiverExclusionsByExclusionIdUrl,
} from 'MainRoot/util/CLMLocation';
import AutoWaiversConfiguration from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiversConfiguration';
import userEvent from '@testing-library/user-event';
import { DEVELOPER_FEATURE_DISABLED_MESSAGE_WAIVERS } from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/LicenseLockScreenForAutoWaivers';

describe('Auto Waivers Configuration Component', () => {
  let axiosMock, renderComponent, user;

  const defaultPreloadedState = {
    orgsAndPolicies: {
      root: {
        selectedOwner: {
          id: 'app',
          publicId: 'publicId',
          name: 'App',
        },
      },
    },
    router: {
      currentState: {
        name: 'management.edit.application.edit-waivers',
      },
      currentParams: {
        applicationPublicId: 'publicId',
      },
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    user = userEvent.setup();
  });

  beforeEach(() => {
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['developer-dashboard', 'auto-waivers', 'new-scan-process']);

    renderComponent = (preloadedState) =>
      render(<AutoWaiversConfiguration />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('renders a loading spinner', () => {
    renderComponent();

    const loading = screen.getByText('Loading…');
    expect(loading).toBeInTheDocument();

    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getProductFeaturesUrl());
  });

  describe('missing features', () => {
    it('displays LicenseLockScreen if all features are missing', async () => {
      axiosMock.onGet(getProductFeaturesUrl()).reply(200, []);

      renderComponent();

      const alert = await screen.findByRole('alert');
      expect(alert).toBeInTheDocument();
      expect(alert).toHaveTextContent(DEVELOPER_FEATURE_DISABLED_MESSAGE_WAIVERS);
    });

    it('displays LicenseLockScreen if developer-dashboard feature is missing', async () => {
      axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['auto-waivers']);

      renderComponent();

      const alert = await screen.findByRole('alert');
      expect(alert).toBeInTheDocument();
      expect(alert).toHaveTextContent(DEVELOPER_FEATURE_DISABLED_MESSAGE_WAIVERS);
    });

    it('displays LicenseLockScreen if auto-waiver feature is missing', async () => {
      axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['developer-dashboard']);

      renderComponent();

      const alert = await screen.findByRole('alert');
      expect(alert).toBeInTheDocument();
      expect(alert).toHaveTextContent(DEVELOPER_FEATURE_DISABLED_MESSAGE_WAIVERS);
    });

    it('displays contents but hides Reachability section if new-scan-process feature is missing', async () => {
      axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['developer-dashboard', 'auto-waivers']);

      renderComponent();

      expect(await screen.findByTestId('auto-waivers-configuration')).toBeInTheDocument();
      expect(screen.queryByText(DEVELOPER_FEATURE_DISABLED_MESSAGE_WAIVERS)).not.toBeInTheDocument();
      expect(screen.queryByText('Reachability Analysis')).not.toBeInTheDocument();
      const checkboxes = await screen.findAllByRole('checkbox');
      expect(checkboxes.length).toBe(1);
    });
  });

  it('renders the content when the feature is enabled for the license', async () => {
    renderComponent();

    expect(await screen.findByTestId('auto-waivers-configuration')).toBeInTheDocument();
    expect(screen.queryByText(DEVELOPER_FEATURE_DISABLED_MESSAGE_WAIVERS)).not.toBeInTheDocument();

    expect(axiosMock.history.get.length).toBe(2);
    expect(axiosMock.history.get[0].url).toBe(getProductFeaturesUrl());
    expect(axiosMock.history.get[1].url).toBe(getAutoWaiversConfigurationURL('application', 'app'));
  });

  it('renders the page title and description', async () => {
    renderComponent();

    expect(await screen.findByRole('heading', { name: 'Automated Waivers' })).toBeVisible();
    expect(
      await screen.findByText(
        'Limit disruptions by deprioritizing low-threat violations until a remediation path is available.'
      )
    ).toBeVisible();
  });

  it('renders content for "Max. Threat Level"', async () => {
    renderComponent();

    expect(await screen.findByText('Max. Threat Level')).toBeVisible();
    expect(await screen.findByText('Violations with higher threats will not be waived')).toBeVisible();
  });

  it('renders the update button', async () => {
    renderComponent();
    expect(await screen.findByRole('button', { name: 'Update' })).toBeVisible();
  });

  it('renders checkboxes correctly based on the state', async () => {
    renderComponent();

    const noPathForwardCheckbox = await screen.findByLabelText(
      'No newer, non-violating component version is available'
    );
    expect(noPathForwardCheckbox).toBeInTheDocument();
    expect(noPathForwardCheckbox).not.toBeChecked();

    const reachabilityCheckbox = await screen.findByLabelText('Security vulnerability is Not reachable');
    expect(reachabilityCheckbox).toBeInTheDocument();
    expect(reachabilityCheckbox).not.toBeChecked();

    const infoIcon = await screen.findByTestId('auto-waivers-configuration-reachability-icon');
    expect(infoIcon).toBeInTheDocument();

    await user.hover(infoIcon);
    expect(await screen.findByRole('tooltip')).toHaveTextContent(
      'Callflow must be enabled via Jenkins or Sonatype CLI'
    );
  });

  it('displays validation error when saving without selecting at least one option when creating first auto policy waiver', async () => {
    renderComponent();

    const pathForwardCheckbox = await screen.findByLabelText('No newer, non-violating component version is available');
    expect(pathForwardCheckbox).not.toBeChecked();

    const reachabilityCheckbox = await screen.findByLabelText('Security vulnerability is Not reachable');
    expect(reachabilityCheckbox).not.toBeChecked();

    const updateButton = await screen.findByRole('button', { name: 'Update' });
    updateButton.click();

    expect(
      await screen.findByText('There were validation errors. Can not save without selecting at least one option')
    ).toBeVisible();
  });

  it('displays delete auto waiver when newScanProcess FF is turned off but reachability is on and pathForward is off', async () => {
    axiosMock.onGet(getAutoWaiversConfigurationURL('application', 'app')).reply(200, {
      isInherited: false,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id',
      threatLevel: 7,
      autoPolicyWaiverOwnerId: 'some-owner-id',
      autoPolicyWaiverOwnerName: 'some-owner-name',
    });

    axiosMock.onGet(getAutoWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200, {
      pathForward: false,
      reachability: true,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
      creatorId: 'some-owner-id',
      creatorName: 'some-owner-name',
    });

    axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['developer-dashboard', 'auto-waivers']);

    renderComponent();

    const pathForwardCheckbox = await screen.findByLabelText('No newer, non-violating component version is available');
    expect(pathForwardCheckbox).not.toBeChecked();

    expect(screen.queryByLabelText('Security vulnerability is Not reachable')).not.toBeInTheDocument();

    const deleteButton = await screen.findByRole('button', { name: 'Delete Auto Waiver' });
    deleteButton.click();

    const dialog = await screen.findByRole('dialog');
    expect(dialog).toBeInTheDocument();
    expect(within(dialog).getByText('Are you sure you want to delete this auto waiver configuration?')).toBeVisible();
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
      reachability: false,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
      creatorId: 'some-owner-id',
      creatorName: 'some-owner-name',
    });
    renderComponent();

    const pathForwardCheckbox = await screen.findByLabelText('No newer, non-violating component version is available');
    expect(pathForwardCheckbox).toBeChecked();
    await user.click(pathForwardCheckbox);
    expect(pathForwardCheckbox).not.toBeChecked();

    const deleteButton = await screen.findByRole('button', { name: 'Delete Auto Waiver' });
    deleteButton.click();

    const dialog = await screen.findByRole('dialog');
    expect(dialog).toBeInTheDocument();
    expect(within(dialog).getByText('Are you sure you want to delete this auto waiver configuration?')).toBeVisible();
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
      reachability: false,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    axiosMock.onPut(getAutoWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200);

    renderComponent();

    const pathForwardCheckbox = await screen.findByLabelText('No newer, non-violating component version is available');
    expect(pathForwardCheckbox).not.toBeChecked();

    const reachabilityCheckbox = await screen.findByLabelText('Security vulnerability is Not reachable');
    expect(reachabilityCheckbox).not.toBeChecked();

    // update path forward to true
    await user.click(pathForwardCheckbox);
    expect(pathForwardCheckbox).toBeChecked();

    // update reachability to true
    await user.click(reachabilityCheckbox);
    expect(reachabilityCheckbox).toBeChecked();

    const updateButton = screen.getByRole('button', { name: 'Update' });
    await user.click(updateButton);

    expect(axiosMock.history.put.length).toBe(1);
    const putData = JSON.parse(axiosMock.history.put[0].data);
    expect(putData.pathForward).toBe(true);
    expect(putData.reachability).toBe(true);
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
      reachability: false,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    renderComponent();

    const pathForwardCheckbox = await screen.findByLabelText('No newer, non-violating component version is available');
    await user.click(pathForwardCheckbox);

    const disableButton = screen.getByRole('button', { name: 'Delete Auto Waiver' });
    await user.click(disableButton);

    // Find and click cancel button
    const cancelButton = screen.getByRole('button', { name: 'Delete' });
    await user.click(cancelButton);

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
      reachability: false,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    axiosMock.onDelete(getAutoWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200);

    renderComponent();

    expect(axiosMock.history.get.length).toBe(1);

    const pathForwardCheckbox = await screen.findByLabelText('No newer, non-violating component version is available');
    expect(pathForwardCheckbox).toBeChecked();
    await user.click(pathForwardCheckbox);

    const disableButton = screen.getByRole('button', { name: 'Delete Auto Waiver' });
    await user.click(disableButton);

    const confirmButton = screen.getByRole('button', { name: 'Delete' });
    await user.click(confirmButton);

    expect(axiosMock.history.delete.length).toBe(1);

    expect(screen.getByLabelText('No newer, non-violating component version is available')).not.toBeChecked();

    const getCalls = axiosMock.history.get;
    expect(getCalls.some((call) => call.url === '/api/v2/autoPolicyWaiverExclusions/application/app/some-id')).toBe(
      true
    );
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
      reachability: false,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    axiosMock.onPut(getAutoWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(500, {
      message: 'Failed to save configuration',
    });

    renderComponent();

    const pathForwardCheckbox = await screen.findByLabelText('No newer, non-violating component version is available');
    await user.click(pathForwardCheckbox);

    const updateButton = screen.getByRole('button', { name: 'Update' });
    await user.click(updateButton);

    expect(await screen.findByText('An error occurred saving data. Failed to save configuration')).toBeVisible();
  });

  it('renders sublabel for Scope', async () => {
    renderComponent();

    expect(await screen.findByText('Eligible violations will be waived if/when:')).toBeVisible();
    expect(await screen.findByText('No Upgrade Path')).toBeVisible();
  });

  it('shows default threat level of 7 when creating new waiver', async () => {
    renderComponent();
    expect(await screen.findByRole('button', { name: /7 - Severe/i })).toBeVisible();
  });

  it('maintains selected threat level after form submission', async () => {
    renderComponent();
    const dropdown = await screen.findByRole('button', { name: /7 - Severe/i });

    await user.click(dropdown);
    await user.click(screen.getByRole('button', { name: /3 - Moderate/i }));

    const updateButton = screen.getByRole('button', { name: 'Update' });
    await user.click(updateButton);

    expect(await screen.findByRole('button', { name: /3 - Moderate/i })).toBeVisible();
  });

  it('disables checkboxes when configuration is inherited from organization', async () => {
    axiosMock.onGet(getAutoWaiversConfigurationURL('application', 'app')).reply(200, {
      isInherited: true,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id',
      threatLevel: 7,
      autoPolicyWaiverOwnerId: 'orgId',
      autoPolicyWaiverOwnerType: 'organization',
    });

    axiosMock.onGet(getAutoWaiversConfigurationURLWaiver('organization', 'orgId', 'some-id')).reply(200, {
      pathForward: true,
      reachability: true,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    renderComponent();

    const pathForwardCheckbox = await screen.findByLabelText('No newer, non-violating component version is available');
    expect(pathForwardCheckbox).toBeVisible();
    expect(pathForwardCheckbox).toBeChecked();
    expect(pathForwardCheckbox).toBeDisabled();

    const reachabilityCheckbox = await screen.findByLabelText('Security vulnerability is Not reachable');
    expect(reachabilityCheckbox).toBeVisible();
    expect(reachabilityCheckbox).toBeChecked();
    expect(reachabilityCheckbox).toBeDisabled();

    expect(
      screen.getByText(
        'Automated waivers are enabled for the parent organization. Changes made here will only affect this application.'
      )
    ).toBeVisible();

    //click should have no effect
    await user.click(pathForwardCheckbox);
    expect(pathForwardCheckbox).toBeChecked();
    expect(pathForwardCheckbox).toBeDisabled();

    await user.click(reachabilityCheckbox);
    expect(reachabilityCheckbox).toBeChecked();
    expect(reachabilityCheckbox).toBeDisabled();
  });

  it('adjust threatLevel should override the auto waiver when configuration is inherited from organization', async () => {
    axiosMock.onGet(getAutoWaiversConfigurationURL('application', 'app')).replyOnce(200, {
      isInherited: true,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id',
      threatLevel: 7,
      autoPolicyWaiverOwnerId: 'orgId',
      autoPolicyWaiverOwnerType: 'organization',
    });

    axiosMock.onGet(getAutoWaiversConfigurationURLWaiver('organization', 'orgId', 'some-id')).reply(200, {
      pathForward: true,
      reachability: false,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    // Response after creating new waiver
    axiosMock.onPost(getAutoWaiversConfigurationURL('application', 'app')).reply(200, {
      autoPolicyWaiverId: 'some-id-2',
    });

    // Response after creation to reload configuration
    axiosMock.onGet(getAutoWaiversConfigurationURL('application', 'app')).reply(200, {
      isInherited: false,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id-2',
      threatLevel: 3,
      autoPolicyWaiverOwnerId: 'app',
      autoPolicyWaiverOwnerType: 'application',
      pathForward: true,
      reachability: false,
    });

    axiosMock.onGet(getAutoWaiversConfigurationURLWaiver('application', 'app', 'some-id-2')).reply(200, {
      pathForward: true,
      reachability: false,
      threatLevel: 3,
      autoPolicyWaiverId: 'some-id-2',
    });

    renderComponent();

    const pathForwardCheckbox = await screen.findByLabelText('No newer, non-violating component version is available');
    expect(pathForwardCheckbox).toBeVisible();
    expect(pathForwardCheckbox).toBeChecked();
    expect(pathForwardCheckbox).toBeDisabled();

    const reachabilityCheckbox = await screen.findByLabelText('Security vulnerability is Not reachable');
    expect(reachabilityCheckbox).toBeVisible();
    expect(reachabilityCheckbox).not.toBeChecked();
    expect(reachabilityCheckbox).toBeDisabled();

    expect(
      screen.getByText(
        'Automated waivers are enabled for the parent organization. Changes made here will only affect this application.'
      )
    ).toBeVisible();

    // Change threat level
    const dropdown = await screen.findByRole('button', { name: /7 - Severe/i });
    await user.click(dropdown);
    await user.click(screen.getByRole('button', { name: /3 - Moderate/i }));

    const updateButton = screen.getByRole('button', { name: 'Update' });
    await user.click(updateButton);

    await waitFor(async () => {
      const updatedCheckbox = screen.getByLabelText('No newer, non-violating component version is available');
      expect(updatedCheckbox).not.toBeDisabled();
    });

    expect(screen.getByRole('button', { name: /3 - Moderate/i })).toBeVisible();
    expect(
      screen.queryByText(
        'Automated waivers are enabled for the parent organization. Changes made here will only affect this application.'
      )
    ).not.toBeInTheDocument();
  });

  it('renders exclusion log section', async () => {
    axiosMock.onGet(getAutoWaiversConfigurationURL('application', 'app')).reply(200, {
      isInherited: false,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id',
      threatLevel: 7,
    });

    axiosMock.onGet(getAutoWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200, {
      pathForward: true,
      reachability: false,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    axiosMock.onGet(getAutoWaiverExclusionsByAutoWaiverIdUrl('application', 'app', 'some-id')).reply(200, [
      {
        createTime: '2024-12-15T03:26:55.418+0000',
        threatLevel: 7,
        policyName: 'Security-Medium',
        componentDisplayName: 'com.example:test:1.0.0',
        vulnerabilityIdentifiers: 'SONATYPE-1234',
        autoPolicyWaiverId: 'some-id',
        autoPolicyWaiverExclusionId: 'exc-id-1',
      },
    ]);

    renderComponent();

    expect(await screen.findByRole('heading', { name: 'Exclusion Log' })).toBeVisible();
    expect(await screen.findByText('Security-Medium')).toBeVisible();
    expect(await screen.findByText('com.example:test:1.0.0')).toBeVisible();
    expect(await screen.findByText('SONATYPE-1234')).toBeVisible();
  });

  it('handles empty exclusion log', async () => {
    axiosMock.onGet(getAutoWaiversConfigurationURL('application', 'app')).reply(200, {
      isInherited: false,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id',
      threatLevel: 7,
    });

    axiosMock.onGet(getAutoWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200, {
      pathForward: true,
      reachability: false,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    axiosMock.onGet(getAutoWaiverExclusionsByAutoWaiverIdUrl('application', 'app', 'some-id')).reply(200, []);

    renderComponent();

    expect(await screen.findByText('No exclusions found')).toBeVisible();
  });

  it('successfully deletes an exclusion', async () => {
    axiosMock.onGet(getAutoWaiversConfigurationURL('application', 'app')).reply(200, {
      isInherited: false,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id',
      threatLevel: 7,
    });

    axiosMock.onGet(getAutoWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200, {
      pathForward: true,
      reachability: false,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    const exclusion = {
      createTime: '2024-12-15T03:26:55.418+0000',
      threatLevel: 7,
      policyName: 'Security-Medium',
      componentDisplayName: 'com.example:test:1.0.0',
      vulnerabilityIdentifiers: 'SONATYPE-1234',
      autoPolicyWaiverId: 'some-id',
      autoPolicyWaiverExclusionId: 'exc-id-1',
    };

    axiosMock
      .onGet(getAutoWaiverExclusionsByAutoWaiverIdUrl('application', 'app', 'some-id'))
      .replyOnce(200, [exclusion])
      .onGet(getAutoWaiverExclusionsByAutoWaiverIdUrl('application', 'app', 'some-id'))
      .reply(200, []);
    axiosMock.onDelete(getAutoWaiverExclusionsByExclusionIdUrl('application', 'app', 'some-id', 'exc-id-1')).reply(200);

    renderComponent();
    expect(
      screen.queryByText('Click Continue to resume automated waiver eligibility for this violation')
    ).not.toBeInTheDocument();

    const deleteButton = await screen.findByRole('button', { name: /delete/i });
    await user.click(deleteButton);

    expect(
      await screen.findByText('Click Continue to resume automated waiver eligibility for this violation')
    ).toBeVisible();

    const confirmButton = await screen.findByRole('button', { name: 'Continue' });
    await user.click(confirmButton);

    expect(axiosMock.history.delete.length).toBe(1);
    expect(axiosMock.history.delete[0].url).toBe(
      getAutoWaiverExclusionsByExclusionIdUrl('application', 'app', 'some-id', 'exc-id-1')
    );

    expect(await screen.findByText('No exclusions found')).toBeVisible();
    expect(screen.queryByText('Security-Medium')).not.toBeInTheDocument();
    expect(screen.queryByText('com.example:test:1.0.0')).not.toBeInTheDocument();
    expect(screen.queryByText('SONATYPE-1234')).not.toBeInTheDocument();
  });

  it('handles exclusion deletion error', async () => {
    axiosMock.onGet(getAutoWaiversConfigurationURL('application', 'app')).reply(200, {
      isInherited: false,
      isAutoWaiverEnabled: true,
      autoPolicyWaiverId: 'some-id',
      threatLevel: 7,
    });

    axiosMock.onGet(getAutoWaiversConfigurationURLWaiver('application', 'app', 'some-id')).reply(200, {
      pathForward: true,
      reachability: false,
      threatLevel: 7,
      autoPolicyWaiverId: 'some-id',
    });

    const exclusions = [
      {
        createTime: '2024-12-15T03:26:55.418+0000',
        threatLevel: 7,
        policyName: 'Security-Medium',
        componentDisplayName: 'com.example:test:1.0.0',
        vulnerabilityIdentifiers: 'SONATYPE-1234',
        autoPolicyWaiverId: 'some-id',
        autoPolicyWaiverExclusionId: 'exc-id-1',
      },
    ];

    axiosMock.onGet(getAutoWaiverExclusionsByAutoWaiverIdUrl('application', 'app', 'some-id')).reply(200, exclusions);
    axiosMock
      .onDelete(getAutoWaiverExclusionsByExclusionIdUrl('application', 'app', 'some-id', 'exc-id-1'))
      .reply(400, { message: 'Failed to delete exclusion' });

    renderComponent();
    expect(
      screen.queryByText('Click Continue to resume automated waiver eligibility for this violation')
    ).not.toBeInTheDocument();

    const deleteButton = await screen.findByRole('button', { name: /delete/i });
    await user.click(deleteButton);

    expect(
      await screen.findByText('Click Continue to resume automated waiver eligibility for this violation')
    ).toBeVisible();

    const confirmButton = await screen.findByRole('button', { name: 'Continue' });
    await user.click(confirmButton);

    expect(await screen.findByText('An error occurred saving data. Failed to delete exclusion')).toBeVisible();
  });
});
