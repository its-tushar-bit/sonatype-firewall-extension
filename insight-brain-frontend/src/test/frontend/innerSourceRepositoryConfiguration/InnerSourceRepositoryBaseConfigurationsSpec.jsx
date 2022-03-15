/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import InnerSourceRepositoryBaseConfigurations from 'MainRoot/innerSourceRepositoryConfiguration/InnerSourceRepositoryBaseConfigurations';
import * as innerSourceRepositoryBaseConfigurationsSelectors from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSelectors';
import * as innerSourceRepositoryConfigurationModalSelectors from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalSelectors';
import { render, screen, fireEvent, within } from 'TestRoot/SpecUtil';
import React from 'react';
import { getInitialState } from 'TestRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsTestData';
import {
  MUST_UPDATE_ENABLED_ADD_MESSAGE,
  MUST_UPDATE_ENABLED_EDIT_MESSAGE,
  PARENT_ORGANIZATIONS_MUST_ALLOW_OVERRIDE,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSlice';

describe('InnerSourceRepositoryBaseConfigurations', function () {
  let renderComponent,
    spySelectInnerSourceRepositoryBaseConfigurationsSlice,
    spySelectFormState,
    spySelectInheritedFromOrganizationName,
    spySelectEnabled,
    spySelectInheritedFromOrgEnabled,
    spySelectAllowChange,
    spySelectRepositoryConnections,
    spySelectIsDirty,
    spySelectValidationErrors,
    spySelectOwnerTypeAndOwnerId,
    spySelectOwnerPublicId;

  beforeEach(() => {
    spySelectInnerSourceRepositoryBaseConfigurationsSlice = spyOn(
      innerSourceRepositoryBaseConfigurationsSelectors,
      'selectInnerSourceRepositoryBaseConfigurationsSlice'
    ).and.callThrough();
    spySelectFormState = spyOn(innerSourceRepositoryBaseConfigurationsSelectors, 'selectFormState').and.callThrough();
    spySelectInheritedFromOrganizationName = spyOn(
      innerSourceRepositoryBaseConfigurationsSelectors,
      'selectInheritedFromOrganizationName'
    ).and.callThrough();
    spySelectEnabled = spyOn(innerSourceRepositoryBaseConfigurationsSelectors, 'selectEnabled').and.callThrough();
    spySelectInheritedFromOrgEnabled = spyOn(
      innerSourceRepositoryBaseConfigurationsSelectors,
      'selectInheritedFromOrgEnabled'
    ).and.callThrough();
    spySelectAllowChange = spyOn(
      innerSourceRepositoryBaseConfigurationsSelectors,
      'selectAllowChange'
    ).and.callThrough();
    spySelectRepositoryConnections = spyOn(
      innerSourceRepositoryBaseConfigurationsSelectors,
      'selectRepositoryConnections'
    ).and.callThrough();
    spySelectIsDirty = spyOn(innerSourceRepositoryBaseConfigurationsSelectors, 'selectIsDirty').and.callThrough();
    spySelectValidationErrors = spyOn(
      innerSourceRepositoryBaseConfigurationsSelectors,
      'selectValidationErrors'
    ).and.callThrough();
    spySelectOwnerTypeAndOwnerId = spyOn(
      innerSourceRepositoryConfigurationModalSelectors,
      'selectOwnerTypeAndOwnerId'
    ).and.callThrough();
    spySelectOwnerPublicId = spyOn(
      innerSourceRepositoryBaseConfigurationsSelectors,
      'selectOwnerPublicId'
    ).and.callThrough();
    renderComponent = () => render(<InnerSourceRepositoryBaseConfigurations />);
  });

  describe('configuration load', function () {
    it('has the correct back link for an org', function () {
      spySelectEnabled.and.returnValue(null);
      spySelectInheritedFromOrgEnabled.and.returnValue(true);
      spySelectInheritedFromOrganizationName.and.returnValue('someOrganizationName');
      spySelectOwnerTypeAndOwnerId.and.returnValue({ ownerType: 'organization', ownerId: 'someOwnerId' });
      renderComponent();
      const back = screen.getByText('Back');
      const link = back.closest('a');
      expect(link).toBeVisible();
      expect(link.href.split('#')[1]).toEqual('/management/view/organization/someOwnerId');
    });

    it('has the correct back link for an app', function () {
      spySelectEnabled.and.returnValue(null);
      spySelectInheritedFromOrgEnabled.and.returnValue(true);
      spySelectInheritedFromOrganizationName.and.returnValue('someOrganizationName');
      spySelectOwnerTypeAndOwnerId.and.returnValue({ ownerType: 'application', ownerId: 'someOwnerId' });
      spySelectOwnerPublicId.and.returnValue('appPublicId');
      renderComponent();
      const back = screen.getByText('Back');
      const link = back.closest('a');
      expect(link).toBeVisible();
      expect(link.href.split('#')[1]).toEqual('/management/view/application/appPublicId');
    });

    it('shows status enabled and inherited', function () {
      spySelectEnabled.and.returnValue(null);
      spySelectInheritedFromOrgEnabled.and.returnValue(true);
      spySelectInheritedFromOrganizationName.and.returnValue('someOrganizationName');
      renderComponent();
      const status = screen.getByText('Enabled (inherited from someOrganizationName)');
      expect(status).toBeVisible();
    });

    it('shows status disabled and inherited', function () {
      spySelectEnabled.and.returnValue(null);
      spySelectInheritedFromOrgEnabled.and.returnValue(false);
      spySelectInheritedFromOrganizationName.and.returnValue('someOrganizationName');
      renderComponent();
      const status = screen.getByText('Disabled (inherited from someOrganizationName)');
      expect(status).toBeVisible();
    });

    it('shows status enabled and not inherited', function () {
      spySelectInheritedFromOrgEnabled.and.returnValue(null);
      spySelectEnabled.and.returnValue(true);
      renderComponent();
      const status = screen.getByText('Enabled');
      expect(status).toBeVisible();
    });

    it('shows status disabled and not inherited', function () {
      renderComponent();
      const status = screen.getByText('Disabled');
      expect(status).toBeVisible();
    });

    it('shows allow override checked', function () {
      spySelectFormState.and.returnValue({ allowOverride: true });
      renderComponent();
      const allowOverride = screen.getByLabelText('Allow Override');
      expect(allowOverride).toBeChecked();
    });

    it('shows allow override unchecked', function () {
      spySelectFormState.and.returnValue({ allowOverride: false });
      renderComponent();
      const allowOverride = screen.getByLabelText('Allow Override');
      expect(allowOverride).not.toBeChecked();
    });

    it('shows inherit and no repositories list', function () {
      spySelectFormState.and.returnValue({ enabled: null });
      renderComponent();
      const inherit = screen.getByLabelText('Inherit');
      expect(inherit).toBeChecked();
      const disable = screen.getByLabelText('Disable');
      expect(disable).not.toBeChecked();
      const enable = screen.getByLabelText('Enable and Override Repository Connections');
      expect(enable).not.toBeChecked();

      expect(screen.queryByText('LOCAL')).toBeNull();
      expect(screen.queryByText('Add a Repository')).toBeNull();
      expect(screen.queryByText('No repositories are configured')).toBeNull();
    });

    it('shows disable and no repositories list', function () {
      spySelectFormState.and.returnValue({ enabled: false });
      renderComponent();
      const inherit = screen.getByLabelText('Inherit');
      expect(inherit).not.toBeChecked();
      const disable = screen.getByLabelText('Disable');
      expect(disable).toBeChecked();
      const enable = screen.getByLabelText('Enable and Override Repository Connections');
      expect(enable).not.toBeChecked();

      expect(screen.queryByText('LOCAL')).toBeNull();
      expect(screen.queryByText('Add a Repository')).toBeNull();
      expect(screen.queryByText('No repositories are configured')).toBeNull();
    });

    it('shows enable with a repositories list', function () {
      spySelectFormState.and.returnValue({ enabled: true });
      renderComponent();
      const inherit = screen.getByLabelText('Inherit');
      expect(inherit).not.toBeChecked();
      const disable = screen.getByLabelText('Disable');
      expect(disable).not.toBeChecked();
      const enable = screen.getByLabelText('Enable and Override Repository Connections');
      expect(enable).toBeChecked();

      expect(screen.getByText('LOCAL')).toBeVisible();
      expect(screen.getByText('Add a Repository')).toBeVisible();
      expect(screen.getByText('No repositories are configured')).toBeVisible();
    });

    it('disables the cancel button if not dirty', function () {
      renderComponent();
      const cancelButton = screen.getByText('Cancel');
      expect(cancelButton).toBeVisible();
      expect(cancelButton).toBeDisabled();
    });

    it('enables the cancel button if dirty', function () {
      spySelectIsDirty.and.returnValue(true);
      renderComponent();
      const cancelButton = screen.getByText('Cancel');
      expect(cancelButton).toBeVisible();
      expect(cancelButton).toBeEnabled();
    });

    it('disables the update button if there is a validation error', async () => {
      spySelectValidationErrors.and.returnValue('someValidationError');
      renderComponent();
      const updateButton = screen.getByText('Update');
      expect(updateButton).toBeVisible();
      expect(updateButton).toHaveClassName('disabled');
      fireEvent.mouseOver(updateButton);
      const tooltip = await screen.findByRole('tooltip');
      expect(within(tooltip).getByText('someValidationError')).toBeInTheDocument();
    });

    it('enables the update button if there is no validation error', function () {
      spySelectValidationErrors.and.returnValue(null);
      renderComponent();
      const updateButton = screen.getByText('Update');
      expect(updateButton).toBeVisible();
      expect(updateButton).not.toHaveClassName('disabled');
    });

    it('shows an info alert and disables the checkbox and radio buttons if `allowChange` is false', function () {
      renderComponent();
      const infoAlert = screen.getByText('The inherited configuration cannot be overridden.');
      expect(infoAlert).toBeVisible();
      const allowOverride = screen.getByLabelText('Allow Override');
      expect(allowOverride).toBeVisible();
      expect(allowOverride).toBeDisabled();
      const inherit = screen.getByLabelText('Inherit');
      expect(inherit).toBeVisible();
      expect(inherit).toBeDisabled();
      const disable = screen.getByLabelText('Disable');
      expect(disable).toBeVisible();
      expect(disable).toBeDisabled();
      const enable = screen.getByLabelText('Enable and Override Repository Connections');
      expect(enable).toBeVisible();
      expect(enable).toBeDisabled();
    });

    it('does not show an info alert and enables the checkbox and radio buttons if `allowChange` is true', function () {
      spySelectAllowChange.and.returnValue(true);
      renderComponent();
      const infoAlert = screen.queryByText('The inherited configuration cannot be overridden.');
      expect(infoAlert).toBeNull();
      const allowOverride = screen.getByLabelText('Allow Override');
      expect(allowOverride).toBeVisible();
      expect(allowOverride).toBeEnabled();
      const inherit = screen.getByLabelText('Inherit');
      expect(inherit).toBeVisible();
      expect(inherit).toBeEnabled();
      const disable = screen.getByLabelText('Disable');
      expect(disable).toBeVisible();
      expect(disable).toBeEnabled();
      const enable = screen.getByLabelText('Enable and Override Repository Connections');
      expect(enable).toBeVisible();
      expect(enable).toBeEnabled();
    });

    it('does not show inherit for the root organization and omits override text', function () {
      const state = { ...getInitialState(), serverData: {} };
      spySelectInnerSourceRepositoryBaseConfigurationsSlice.and.returnValue(state);
      spySelectOwnerTypeAndOwnerId.and.returnValue({ ownerType: 'organization', ownerId: 'ROOT_ORGANIZATION_ID' });
      renderComponent();
      const inherit = screen.queryByLabelText('Inherit');
      expect(inherit).toBeNull();
      const disable = screen.getByLabelText('Disable');
      expect(disable).toBeVisible();
      const enable = screen.getByLabelText('Enable');
      expect(enable).toBeVisible();
    });

    it('does not show allow override for an application', function () {
      const state = { ...getInitialState(), serverData: {} };
      spySelectInnerSourceRepositoryBaseConfigurationsSlice.and.returnValue(state);
      spySelectOwnerTypeAndOwnerId.and.returnValue({ ownerType: 'application', ownerId: 'someOwnerId' });
      renderComponent();
      const allowOverride = screen.queryByLabelText('Allow Override');
      expect(allowOverride).toBeNull();
    });

    it('shows a load error', function () {
      const state = { ...getInitialState(), loadError: 'someLoadError' };
      spySelectInnerSourceRepositoryBaseConfigurationsSlice.and.returnValue(state);
      renderComponent();
      const loadError = screen.getByText('An error occurred loading data. someLoadError');
      expect(loadError).toBeVisible();
    });

    it('enables adding/editing repository connections if changes are allowed and enabled is saved', function () {
      spySelectRepositoryConnections.and.returnValue([
        { repositoryConnectionId: 'someRepositoryConnectionId', baseUrl: 'someBaseUrl', format: 'someFormat' },
      ]);
      spySelectAllowChange.and.returnValue(true);
      spySelectEnabled.and.returnValue(true);
      spySelectFormState.and.returnValue({ enabled: true });
      renderComponent();
      const addButton = screen.getByText('Add a Repository').closest('button');
      expect(addButton).toBeVisible();
      expect(addButton).not.toHaveClassName('disabled');
      const editButton = screen
        .getAllByRole('button')
        .find((b) => b.getAttribute('class').includes('nx-btn--icon-only'));
      expect(editButton).toBeVisible();
      expect(editButton).not.toHaveClassName('disabled');
    });

    it('disables adding repository connections if enabled is not saved', async () => {
      spySelectRepositoryConnections.and.returnValue([
        { repositoryConnectionId: 'someRepositoryConnectionId', baseUrl: 'someBaseUrl', format: 'someFormat' },
      ]);
      spySelectAllowChange.and.returnValue(true);
      spySelectFormState.and.returnValue({ enabled: true });
      renderComponent();
      const addButton = screen.getByText('Add a Repository').closest('button');
      expect(addButton).toBeVisible();
      expect(addButton).toHaveClassName('disabled');
      fireEvent.mouseOver(addButton);
      const tooltip = await screen.findByRole('tooltip');
      expect(within(tooltip).getByText(MUST_UPDATE_ENABLED_ADD_MESSAGE)).toBeInTheDocument();
    });

    it('disables editing repository connections if enabled is not saved', async () => {
      spySelectRepositoryConnections.and.returnValue([
        { repositoryConnectionId: 'someRepositoryConnectionId', baseUrl: 'someBaseUrl', format: 'someFormat' },
      ]);
      spySelectAllowChange.and.returnValue(true);
      spySelectFormState.and.returnValue({ enabled: true });
      renderComponent();
      const editButton = screen
        .getAllByRole('button')
        .find((b) => b.getAttribute('class').includes('nx-btn--icon-only'));
      expect(editButton).toBeVisible();
      expect(editButton).toHaveClassName('disabled');
      fireEvent.mouseOver(editButton);
      const tooltip = await screen.findByRole('tooltip');
      expect(within(tooltip).getByText(MUST_UPDATE_ENABLED_EDIT_MESSAGE)).toBeInTheDocument();
    });

    it('disables adding repository connections if changes are not allowed', async () => {
      spySelectRepositoryConnections.and.returnValue([
        { repositoryConnectionId: 'someRepositoryConnectionId', baseUrl: 'someBaseUrl', format: 'someFormat' },
      ]);
      spySelectFormState.and.returnValue({ enabled: true });
      renderComponent();
      const addButton = screen.getByText('Add a Repository').closest('button');
      expect(addButton).toBeVisible();
      expect(addButton).toHaveClassName('disabled');
      fireEvent.mouseOver(addButton);
      const tooltip = await screen.findByRole('tooltip');
      expect(within(tooltip).getByText(PARENT_ORGANIZATIONS_MUST_ALLOW_OVERRIDE)).toBeInTheDocument();
    });

    it('disables editing repository connections if changes are not allowed', async () => {
      spySelectRepositoryConnections.and.returnValue([
        { repositoryConnectionId: 'someRepositoryConnectionId', baseUrl: 'someBaseUrl', format: 'someFormat' },
      ]);
      spySelectFormState.and.returnValue({ enabled: true });
      renderComponent();
      const editButton = screen
        .getAllByRole('button')
        .find((b) => b.getAttribute('class').includes('nx-btn--icon-only'));
      expect(editButton).toBeVisible();
      expect(editButton).toHaveClassName('disabled');
      fireEvent.mouseOver(editButton);
      const tooltip = await screen.findByRole('tooltip');
      expect(within(tooltip).getByText(PARENT_ORGANIZATIONS_MUST_ALLOW_OVERRIDE)).toBeInTheDocument();
    });
  });

  describe('saving', function () {
    it('disables the update button if no changes have been made', async () => {
      renderComponent();
      const updateButton = screen.getByText('Update');
      expect(updateButton).toBeVisible();
      expect(updateButton).toHaveClassName('disabled');

      fireEvent.mouseOver(updateButton);
      const tooltip = await screen.findByRole('tooltip');
      expect(within(tooltip).getByText('No changes have been made.')).toBeInTheDocument();
    });

    it('shows a save error', function () {
      const state = { ...getInitialState(), saveError: 'someSaveError' };
      spySelectInnerSourceRepositoryBaseConfigurationsSlice.and.returnValue(state);
      renderComponent();
      const saveError = screen.getByText('An error occurred saving data. someSaveError');
      expect(saveError).toBeVisible();
    });
  });
});
