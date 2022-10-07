/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { render, screen } from 'TestRoot/SpecUtil';
import SAMLConfigurationPage from 'MainRoot/configuration/saml/SAMLConfigurationPage';
import * as samlConfigurationSelectors from 'MainRoot/configuration/saml/samlConfigurationSelectors';
import { actions as samlConfigurationActions } from 'MainRoot/configuration/saml/samlConfigurationSlice';
import { uriTemplate } from 'MainRoot/util/urlUtil';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('SAMLConfigurationPage', () => {
  let renderComponent, loadSAMLConfigurationSpy, samlConfigurationSlice;

  samlConfigurationSlice = {
    isLoading: false,
    submitState: null,
    submitMaskError: null,
    loadError: null,
    isConfigured: false,
    isDeleteModalShown: false,
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

  beforeEach(() => {
    spyOn(samlConfigurationSelectors, 'selectSAMLConfigurationSlice').and.returnValue(samlConfigurationSlice);

    loadSAMLConfigurationSpy = spyOn(samlConfigurationActions, 'loadSAMLConfiguration').and.callThrough();

    renderComponent = () => render(<SAMLConfigurationPage />);
  });

  it('sets default values in all fields', () => {
    renderComponent();

    const [idp, idpMetadata, entityId, username, firstName, lastName, email, groups] = screen.getAllByRole('textbox');
    const [validateResponseSignature, validateAssertionSignature] = screen.getAllByRole('combobox');

    // Check input values in order of their appearance on the page
    expect(idp.value).toBe('identity provider');
    expect(idpMetadata.value).toBe('');
    expect(entityId.value).toBe(uriTemplate`/api/v2/config/saml/metadata`);
    expect(username.value).toBe('username');
    expect(firstName.value).toBe('firstName');
    expect(lastName.value).toBe('lastName');
    expect(email.value).toBe('email');
    expect(groups.value).toBe('groups');

    expect(validateResponseSignature).toHaveTextContent('Default');
    expect(validateAssertionSignature).toHaveTextContent('Default');

    expect(loadSAMLConfigurationSpy).toHaveBeenCalled();
  });

  it('renders a disabled download IQ server metadata button', () => {
    renderComponent();

    expect(screen.getByRole('button', { name: 'Download IQ Server Metadata' })).toHaveClassName('disabled');
  });

  it('renders the "not configured" message', () => {
    renderComponent();

    expect(screen.getByText('* Currently not configured')).toBeVisible();
  });

  it('renders the save, cancel and delete buttons', () => {
    renderComponent();

    const saveButton = screen.getByRole('button', { name: 'Save' });
    const cancelButton = screen.getByRole('button', { name: 'Cancel' });
    const deleteButton = screen.getByRole('button', { name: 'Delete Configuration' });

    expect(saveButton).toBeVisible();
    expect(saveButton).toHaveClassName('disabled');
    expect(deleteButton).toBeVisible();
    expect(deleteButton).toHaveAttribute('disabled');
    expect(cancelButton).toBeVisible();
  });
});
