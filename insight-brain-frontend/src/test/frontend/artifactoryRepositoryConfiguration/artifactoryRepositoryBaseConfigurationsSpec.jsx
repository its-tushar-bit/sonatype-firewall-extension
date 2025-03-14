/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ArtifactoryRepositoryBaseConfigurations from 'MainRoot/artifactoryRepositoryConfiguration/ArtifactoryRepositoryBaseConfigurations';
import * as artifactoryRepositoryBaseConfigurationsSelectors from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsSelectors';
import * as artifactoryRepositoryConfigurationModalSelectors from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSelectors';
import { render, screen, fireEvent, within, setupPortalContainer, removePortalContainer } from 'TestRoot/SpecUtil';
import React from 'react';
import { getInitialState } from 'TestRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsTestData';
import {
  MUST_UPDATE_ENABLED_ADD_MESSAGE,
  MUST_UPDATE_ENABLED_EDIT_MESSAGE,
  PARENT_ORGANIZATIONS_MUST_ALLOW_OVERRIDE,
} from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsSlice';

describe('ArtifactoryRepositoryBaseConfigurations', function () {
  let renderComponent,
    spySelectArtifactoryRepositoryBaseConfigurationsSlice,
    spySelectFormState,
    spySelectInheritedFromOrganizationName,
    spySelectEnabled,
    spySelectInheritedFromOrgEnabled,
    spySelectAllowChange,
    spySelectArtifactoryConnection,
    spySelectValidationErrors,
    spySelectOwnerTypeAndOwnerId,
    spySelectOwnerPublicId;

  beforeEach(() => {
    setupPortalContainer();
    spySelectArtifactoryRepositoryBaseConfigurationsSlice = spyOn(
      artifactoryRepositoryBaseConfigurationsSelectors,
      'selectArtifactoryRepositoryBaseConfigurationsSlice'
    ).and.callThrough();
    spySelectFormState = spyOn(artifactoryRepositoryBaseConfigurationsSelectors, 'selectFormState').and.callThrough();
    spySelectInheritedFromOrganizationName = spyOn(
      artifactoryRepositoryBaseConfigurationsSelectors,
      'selectInheritedFromOrganizationName'
    ).and.callThrough();
    spySelectEnabled = spyOn(artifactoryRepositoryBaseConfigurationsSelectors, 'selectEnabled').and.callThrough();
    spySelectInheritedFromOrgEnabled = spyOn(
      artifactoryRepositoryBaseConfigurationsSelectors,
      'selectInheritedFromOrgEnabled'
    ).and.callThrough();
    spySelectAllowChange = spyOn(
      artifactoryRepositoryBaseConfigurationsSelectors,
      'selectAllowChange'
    ).and.callThrough();
    spySelectArtifactoryConnection = spyOn(
      artifactoryRepositoryBaseConfigurationsSelectors,
      'selectArtifactoryConnection'
    ).and.callThrough();
    spySelectValidationErrors = spyOn(
      artifactoryRepositoryBaseConfigurationsSelectors,
      'selectValidationErrors'
    ).and.callThrough();
    spySelectOwnerTypeAndOwnerId = spyOn(
      artifactoryRepositoryConfigurationModalSelectors,
      'selectOwnerTypeAndOwnerId'
    ).and.callThrough();
    spySelectOwnerPublicId = spyOn(
      artifactoryRepositoryBaseConfigurationsSelectors,
      'selectOwnerPublicId'
    ).and.callThrough();
    renderComponent = () => render(<ArtifactoryRepositoryBaseConfigurations />);
  });

  afterEach(() => removePortalContainer());

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

    it('shows inherit and no repository', function () {
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
      expect(screen.queryByText('No Artifactory repository connection is configured')).toBeNull();
    });

    it('shows disable and no repository', function () {
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
      expect(screen.queryByText('No Artifactory repository connection is configured')).toBeNull();
    });

    it('shows enable with a repository', function () {
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
      expect(screen.getByText('No Artifactory repository connection is configured')).toBeVisible();
    });

    it('disables the update button if there is a validation error', async () => {
      spySelectValidationErrors.and.returnValue('someValidationError');
      SpecUtil.requestIdleCallbackInvokeImmediate();

      renderComponent();
      const updateButton = screen.getByText('Update');
      expect(updateButton).toBeVisible();
      fireEvent.click(updateButton);
      const alert = await screen.findByRole('alert');
      expect(alert).toHaveTextContent('There were validation errors. someValidationError');
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
      spySelectArtifactoryRepositoryBaseConfigurationsSlice.and.returnValue(state);
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
      spySelectArtifactoryRepositoryBaseConfigurationsSlice.and.returnValue(state);
      spySelectOwnerTypeAndOwnerId.and.returnValue({ ownerType: 'application', ownerId: 'someOwnerId' });
      renderComponent();
      const allowOverride = screen.queryByLabelText('Allow Override');
      expect(allowOverride).toBeNull();
    });

    it('shows a load error', function () {
      const state = { ...getInitialState(), loadError: 'someLoadError' };
      spySelectArtifactoryRepositoryBaseConfigurationsSlice.and.returnValue(state);
      renderComponent();
      const loadError = screen.getByText('An error occurred loading data. someLoadError');
      expect(loadError).toBeVisible();
    });

    it(
      'enables adding an artifactory connection if changes are allowed and enabled is saved and there is no' +
        ' existing artifactory connection',
      function () {
        spySelectArtifactoryConnection.and.returnValue(null);
        spySelectAllowChange.and.returnValue(true);
        spySelectEnabled.and.returnValue(true);
        spySelectFormState.and.returnValue({ enabled: true });
        renderComponent();
        const addButton = screen.getByText('Add a Repository').closest('button');
        expect(addButton).toBeVisible();
        expect(addButton).not.toHaveClassName('disabled');
      }
    );

    it('disables adding new artifactory connection if one already exists', async () => {
      spySelectArtifactoryConnection.and.returnValue({
        artifactoryConnectionId: 'someArtifactoryConnectionId',
        baseUrl: 'someBaseUrl',
      });
      spySelectAllowChange.and.returnValue(true);
      spySelectFormState.and.returnValue({ enabled: true });
      SpecUtil.requestIdleCallbackInvokeImmediate();

      renderComponent();

      const addButton = screen.getByText('Add a Repository').closest('button');
      expect(addButton).toBeVisible();
      expect(addButton).toHaveClassName('disabled');
      fireEvent.mouseOver(addButton);
      const tooltip = await screen.findByRole('tooltip');
      expect(within(tooltip).getByText(MUST_UPDATE_ENABLED_ADD_MESSAGE)).toBeInTheDocument();
    });

    it('disables adding artifactory connection if enabled is not saved', async () => {
      spySelectArtifactoryConnection.and.returnValue({});
      spySelectAllowChange.and.returnValue(true);
      spySelectFormState.and.returnValue({ enabled: true });
      SpecUtil.requestIdleCallbackInvokeImmediate();

      renderComponent();

      const addButton = screen.getByText('Add a Repository').closest('button');
      expect(addButton).toBeVisible();
      expect(addButton).toHaveClassName('disabled');
      fireEvent.mouseOver(addButton);
      const tooltip = await screen.findByRole('tooltip');
      expect(within(tooltip).getByText(MUST_UPDATE_ENABLED_ADD_MESSAGE)).toBeInTheDocument();
    });

    it('enables editing an artifactory connection if changes are allowed and enabled is saved', function () {
      spySelectArtifactoryConnection.and.returnValue({
        artifactoryConnectionId: 'someArtifactoryConnectionId',
        baseUrl: 'someBaseUrl',
      });
      spySelectAllowChange.and.returnValue(true);
      spySelectEnabled.and.returnValue(true);
      spySelectFormState.and.returnValue({ enabled: true });
      renderComponent();
      const editButton = screen
        .getAllByRole('button')
        .find((b) => b.getAttribute('class').includes('nx-btn--icon-only'));
      expect(editButton).toBeVisible();
      expect(editButton).not.toHaveClassName('disabled');
    });

    it('disables editing artifactory connection if enabled is not saved', async () => {
      spySelectArtifactoryConnection.and.returnValue({});
      spySelectAllowChange.and.returnValue(true);
      spySelectFormState.and.returnValue({ enabled: true });
      SpecUtil.requestIdleCallbackInvokeImmediate();
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

    it('disables adding artifactory connection if changes are not allowed', async () => {
      spySelectArtifactoryConnection.and.returnValue({});
      spySelectFormState.and.returnValue({ enabled: true });
      SpecUtil.requestIdleCallbackInvokeImmediate();

      renderComponent();

      const addButton = screen.getByText('Add a Repository').closest('button');
      expect(addButton).toBeVisible();
      expect(addButton).toHaveClassName('disabled');
      fireEvent.mouseOver(addButton);
      const tooltip = await screen.findByRole('tooltip');
      expect(within(tooltip).getByText(PARENT_ORGANIZATIONS_MUST_ALLOW_OVERRIDE)).toBeInTheDocument();
    });

    it('disables editing artifactory connection if changes are not allowed', async () => {
      spySelectArtifactoryConnection.and.returnValue({});
      spySelectFormState.and.returnValue({ enabled: true });
      SpecUtil.requestIdleCallbackInvokeImmediate();

      renderComponent();

      const editButton = screen
        .getAllByRole('button')
        .find((b) => b.getAttribute('class').includes('nx-btn--icon-only'));
      expect(editButton).toBeVisible();
      fireEvent.mouseOver(editButton);
      const tooltip = await screen.findByRole('tooltip');
      expect(within(tooltip).getByText(PARENT_ORGANIZATIONS_MUST_ALLOW_OVERRIDE)).toBeInTheDocument();
    });
  });

  describe('saving', function () {
    it('disables the update button if no changes have been made', async () => {
      SpecUtil.requestIdleCallbackInvokeImmediate();

      renderComponent();
      const updateButton = screen.getByText('Update');
      expect(updateButton).toBeVisible();
      fireEvent.click(updateButton);
      const alert = await screen.findByRole('alert');
      expect(alert).toHaveTextContent('There were validation errors. No changes have been made.');
    });

    it('shows a save error', function () {
      const state = { ...getInitialState(), saveError: 'someSaveError' };
      spySelectArtifactoryRepositoryBaseConfigurationsSlice.and.returnValue(state);
      renderComponent();
      const saveError = screen.getByText('An error occurred saving data. someSaveError');
      expect(saveError).toBeVisible();
    });
  });
});
