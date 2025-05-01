/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, waitFor, screen, fireEvent } from 'TestRoot/SpecUtil';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

import ZScalerConfig from 'MainRoot/configuration/zscaler/ZScalerConfig';
import { FAKE_PASSWORD } from 'MainRoot/configuration/zscaler/zscalerConfigSlice';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('ZScalerConfig', () => {
  const initialProps = {
    serverData: null,
    hostnameState: initUserInput(''),
    usernameState: initUserInput(''),
    passwordState: initUserInput(''),
    apiKeyState: initUserInput(''),
    isDirty: false,
    isValid: false,
    hasAllRequiredData: false,
    loading: false,
    submitMaskState: null,
    submitMaskMessage: null,
    loadError: null,
    saveError: null,
    deleteError: null,
    showDeleteModal: false,
    mustReenterPassword: false,
    isAuthorized: true,
    load: jest.fn(),
    save: jest.fn(),
    del: jest.fn(),
    resetForm: jest.fn(),
    setUsername: jest.fn(),
    setPassword: jest.fn(),
    setHostname: jest.fn(),
    setApiKey: jest.fn(),
    setShowDeleteModal: jest.fn(),
  };

  const setState = (additionalProps = {}) => Object.freeze({ ...initialProps, ...additionalProps });

  const renderComponent = (props = setState()) => render(<ZScalerConfig {...props} />);

  it('renders error alert with message when not authorized', async () => {
    renderComponent(setState({ isAuthorized: false }));
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const alert = screen.getByRole('alert');
    const authErrorMessageRegex = new RegExp(
      'It appears you do not have permission to access this page\\. ' +
        'If you believe this to be incorrect please contact your administrator\\.'
    );
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent(authErrorMessageRegex);
  });

  it('renders the correct initial content when zscaler is not configured', async () => {
    renderComponent();
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const usernameInput = screen.getByLabelText('Username');
    const passwordInput = screen.getByLabelText('Password');
    const hostnameInput = screen.getByLabelText('Hostname');
    const apiKeyInput = screen.getByLabelText('ApiKey');
    const saveButton = screen.getByRole('button', { name: 'Save' });
    const cancelButton = screen.getByRole('button', { name: 'Cancel' });
    const deleteButton = screen.getByRole('button', { name: 'Delete Configuration' });
    const description = new RegExp(
      'To protect users at the network level, integrate our data with your zScaler infrastructure\\. ' +
        'For further details see the'
    );

    expect(screen.getByRole('heading', { level: 2, name: /zScaler Configuration/i })).toBeInTheDocument();
    expect(screen.getByText(description)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /documentation/i })).toBeInTheDocument();
    // input fields
    expect(usernameInput).toBeInTheDocument();
    expect(usernameInput).toHaveAttribute('placeholder', 'user');
    expect(usernameInput).toHaveValue('');
    expect(passwordInput).toBeInTheDocument();
    expect(passwordInput).toHaveValue('');
    expect(hostnameInput).toBeInTheDocument();
    expect(hostnameInput).toHaveAttribute('placeholder', 'https://zsapi.zscalertwo.net');
    expect(hostnameInput).toHaveValue('');
    expect(apiKeyInput).toBeInTheDocument();
    expect(apiKeyInput).toHaveAttribute('placeholder', '465');
    expect(apiKeyInput).toHaveValue('');
    // buttons
    expect(saveButton).toBeInTheDocument();
    expect(saveButton).not.toBeDisabled();
    expect(cancelButton).toBeInTheDocument();
    expect(cancelButton).toBeDisabled();
    expect(deleteButton).toBeInTheDocument();
    expect(deleteButton).toBeDisabled();
  });

  it('renders the correct content when zscaler is configured', async () => {
    renderComponent(
      setState({
        usernameState: initUserInput('testUser'),
        passwordState: initUserInput(FAKE_PASSWORD), // password is null
        hostnameState: initUserInput('https://zsapi.zscalertwo.net'),
        apiKeyState: initUserInput('123'),
      })
    );
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByLabelText('Username')).toHaveValue('testUser');
    expect(screen.getByLabelText('Password')).toHaveValue(FAKE_PASSWORD);
    expect(screen.getByLabelText('Hostname')).toHaveValue('https://zsapi.zscalertwo.net');
    expect(screen.getByLabelText('ApiKey')).toHaveValue('123');
  });

  it('calls setting methods when updating the input fields', async () => {
    const setUsernameMock = jest.fn();
    const setPasswordMock = jest.fn();
    const setHostnameMock = jest.fn();
    const setApiKeyMock = jest.fn();

    renderComponent(
      setState({
        setUsername: setUsernameMock,
        setPassword: setPasswordMock,
        setHostname: setHostnameMock,
        setApiKey: setApiKeyMock,
      })
    );
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const usernameInput = screen.getByLabelText('Username');
    const passwordInput = screen.getByLabelText('Password');
    const hostnameInput = screen.getByLabelText('Hostname');
    const apiKeyInput = screen.getByLabelText('ApiKey');

    fireEvent.change(usernameInput, { target: { value: 'admin' } });
    expect(setUsernameMock).toHaveBeenCalledWith('admin', expect.anything());

    fireEvent.change(passwordInput, { target: { value: 'asdf' } });
    expect(setPasswordMock).toHaveBeenCalledWith('asdf', expect.anything());

    fireEvent.change(hostnameInput, { target: { value: 'https://zsapi.zscalertwo.test.net' } });
    expect(setHostnameMock).toHaveBeenCalledWith('https://zsapi.zscalertwo.test.net', expect.anything());

    fireEvent.change(apiKeyInput, { target: { value: '111' } });
    expect(setApiKeyMock).toHaveBeenCalledWith('111', expect.anything());
  });

  it('renders password sub-label when hasAllRequiredData and mustReenterPassword are true', async () => {
    renderComponent();
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.queryByText('Must be re-entered when any fields are modified.')).not.toBeInTheDocument();

    renderComponent(setState({ hasAllRequiredData: true, mustReenterPassword: true }));
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByText('Must be re-entered when any fields are modified.')).toBeInTheDocument();
  });

  it('renders delete configuration model when showDeleteModal is true', async () => {
    renderComponent(setState({ showDeleteModal: true }));
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const modal = screen.getByRole('dialog');
    expect(modal).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: /Delete zScaler Configuration?/i })).toBeInTheDocument();
    expect(modal).toHaveTextContent(
      'This action cannot be undone. Are you sure you want to delete this configuration?'
    );
  });
});
