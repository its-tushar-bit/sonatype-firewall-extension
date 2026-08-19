/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { render, screen, fireEvent, within } from 'TestRoot/SpecUtil';
import SAMLConfigurationForm from 'MainRoot/configuration/saml/SAMLConfigurationForm';
import { uriTemplate } from 'MainRoot/util/urlUtil';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('SAMLConfigurationForm', () => {
  let renderComponent,
    samlConfigurationSlice,
    props,
    onCancelSpy,
    onSubmitSpy,
    deleteConfigurationSpy,
    toggleDeleteModalSpy;

  samlConfigurationSlice = {
    isLoading: false,
    submitState: null,
    submitMaskError: null,
    loadError: null,
    isConfigured: false,
    configurationValues: {
      identityProviderName: initUserInput('identity provider'),
      entityId: initUserInput(uriTemplate`/api/v2/config/saml/metadata`),
      usernameAttributeName: initUserInput('username'),
      firstNameAttributeName: initUserInput('firstName'),
      lastNameAttributeName: initUserInput('lastName'),
      emailAttributeName: initUserInput('email'),
      groupsAttributeName: initUserInput('groups'),
      identityProviderMetadataXml: initUserInput(''),
      validateResponseSignature: 'null',
      validateAssertionSignature: 'null',
    },
    loadedConfigurationValues: null,
  };

  onCancelSpy = jest.fn();
  onSubmitSpy = jest.fn();
  deleteConfigurationSpy = jest.fn();
  toggleDeleteModalSpy = jest.fn();

  props = {
    onCancel: onCancelSpy,
    onChangeSelect: () => {},
    onChange: () => {},
    onBlur: () => {},
    onSubmit: onSubmitSpy,
    deleteConfiguration: deleteConfigurationSpy,
    readIdentityProviderMetadataXml: () => {},
    isConfigured: samlConfigurationSlice.isConfigured,
    submitState: samlConfigurationSlice.submitState,
    isSubmitButtonDisabled: !samlConfigurationSlice.configurationValues.identityProviderMetadataXml.value,
    submitMaskError: samlConfigurationSlice.submitMaskError,
    configurationValues: samlConfigurationSlice.configurationValues,
    metaDataUrl: 'test/url',
    toggleDeleteModal: toggleDeleteModalSpy,
    isDeleteModalShown: false,
  };

  beforeEach(() => {
    renderComponent = (props) => render(<SAMLConfigurationForm {...props} />);
  });

  describe('when saml is not configured', () => {
    it('renders a download, save and delete buttons', () => {
      renderComponent(props);

      const saveButton = screen.getByRole('button', { name: 'Save' });
      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      const deleteButton = screen.getByRole('button', { name: 'Delete Configuration' });
      const download = screen.getByRole('button', { name: 'Download IQ Server Metadata' });

      expect(saveButton).toBeVisible();
      expect(saveButton).toHaveClass('disabled');

      expect(deleteButton).toBeVisible();
      expect(deleteButton).toHaveAttribute('disabled');
      fireEvent.click(deleteButton);
      expect(deleteConfigurationSpy).not.toHaveBeenCalled();

      expect(download).toBeVisible();
      expect(download).toHaveClass('disabled');

      expect(cancelButton).toBeVisible();
      fireEvent.click(cancelButton);
      expect(onCancelSpy).toHaveBeenCalled();
    });

    it('renders a "* Currently not configured" message', () => {
      renderComponent(props);

      expect(screen.getByText('* Currently not configured')).toBeVisible();
    });
  });

  describe('when saml is configured', () => {
    beforeEach(() => {
      props = {
        ...props,
        configurationValues: { ...props.configurationValues, identityProviderMetadataXml: initUserInput('test xml') },
        isConfigured: true,
        isSubmitButtonDisabled: false,
      };
    });

    it('renders a download, save and delete buttons', () => {
      renderComponent(props);

      const saveButton = screen.getByRole('button', { name: 'Save' });
      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      const deleteButton = screen.getByRole('button', { name: 'Delete Configuration' });
      const download = screen.getByRole('button', { name: 'Download IQ Server Metadata' });

      expect(saveButton).toBeVisible();
      expect(saveButton).not.toHaveClass('disabled');
      fireEvent.click(saveButton);
      expect(onSubmitSpy).toHaveBeenCalled();

      expect(deleteButton).toBeVisible();
      expect(deleteButton).not.toHaveAttribute('disabled');
      fireEvent.click(deleteButton);
      expect(toggleDeleteModalSpy).toHaveBeenCalled();

      expect(download).toBeVisible();
      expect(download).not.toHaveClass('disabled');

      expect(cancelButton).toBeVisible();
      fireEvent.click(cancelButton);
      expect(onCancelSpy).toHaveBeenCalled();
    });

    it('renders saml delete modal and calls delete configuration function', () => {
      renderComponent({ ...props, isDeleteModalShown: true });

      const deleteModal = screen.getByRole('dialog');
      const modalCancelButton = within(deleteModal).getByRole('button', { name: 'Cancel' });
      const modalDeleteButton = within(deleteModal).getByRole('button', { name: 'Delete' });

      expect(
        screen.queryByText(
          'Clicking "delete" will permanently remove this SAML configuration from the system. Are you sure you want to delete it?'
        )
      ).toBeInTheDocument();

      expect(modalCancelButton).toBeVisible();
      fireEvent.click(modalCancelButton);
      expect(toggleDeleteModalSpy).toHaveBeenCalled();

      expect(modalDeleteButton).toBeVisible();
      fireEvent.click(modalDeleteButton);
      expect(deleteConfigurationSpy).toHaveBeenCalled();
    });

    it('do not render a "* Currently not configured" message', () => {
      renderComponent(props);

      expect(screen.queryByText('* Currently not configured')).not.toBeInTheDocument();
    });
  });

  describe('when submit mask is shown', () => {
    it('when mask state is false', () => {
      props = {
        ...props,
        submitState: false,
      };
      renderComponent(props);
      expect(screen.getByText('Submitting…')).toBeVisible();
    });

    it('when mask state is true', () => {
      props = {
        ...props,
        submitState: true,
      };
      renderComponent(props);
      expect(screen.getByText('Success!')).toBeVisible();
    });

    it('when mask state is null', () => {
      props = {
        ...props,
        submitState: null,
      };
      renderComponent(props);
      expect(screen.queryByText('Success!')).not.toBeInTheDocument();
    });
  });

  describe('when submit fails', () => {
    it('renders an error section with a retry button', () => {
      props = {
        ...props,
        submitMaskError: 'Error',
      };
      renderComponent(props);
      fireEvent.click(screen.queryByText('Retry'));
      expect(onSubmitSpy).toHaveBeenCalled();
    });

    it('do not render an error section', () => {
      props = {
        ...props,
        submitMaskError: null,
      };
      renderComponent(props);
      expect(screen.queryByText('Retry')).not.toBeInTheDocument();
    });
  });
});
