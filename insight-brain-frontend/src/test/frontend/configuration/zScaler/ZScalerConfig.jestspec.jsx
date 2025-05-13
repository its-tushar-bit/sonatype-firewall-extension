/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, waitFor, screen, fireEvent, within } from 'TestRoot/SpecUtil';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

import ZScalerConfig from 'MainRoot/configuration/zscaler/ZScalerConfig';
import { FAKE_PASSWORD } from 'MainRoot/configuration/zscaler/zscalerConfigSlice';

const { initialState: initUserInput, userInput } = nxTextInputStateHelpers;

describe('ZScalerConfig', () => {
  const initialProps = {
    serverData: null,
    hostnameState: initUserInput(''),
    usernameState: initUserInput(''),
    passwordState: initUserInput(''),
    apiKeyState: initUserInput(''),
    eulaState: {
      value: false,
      isPristine: true,
      validationErrors: 'This field is required',
      disabled: false,
    },
    isDirty: false,
    isValid: false,
    hasAllRequiredData: false,
    loading: false,
    submitMaskState: null,
    submitMaskMessage: null,
    loadError: null,
    saveError: null,
    deleteError: null,
    testConfigError: false,
    testConfigSuccess: false,
    showDeleteModal: false,
    mustReenterPassword: false,
    isAuthorized: true,
    load: jest.fn(),
    save: jest.fn(),
    del: jest.fn(),
    testConfig: jest.fn(),
    resetForm: jest.fn(),
    setUsername: jest.fn(),
    setPassword: jest.fn(),
    setHostname: jest.fn(),
    setApiKey: jest.fn(),
    setEulaCheckbox: jest.fn(),
    setShowDeleteModal: jest.fn(),
  };

  const setState = (additionalProps = {}) => Object.freeze({ ...initialProps, ...additionalProps });

  const renderComponent = (props = setState()) => render(<ZScalerConfig {...props} />);

  const CHECKBOX_NAME =
    'I acknowledge that access to and use of Sonatype products is governed by either 1) the terms ' +
    "of company's negotiated license agreement with Sonatype or, in the absence of a negotiated license, 2) " +
    'Sonatype’s End User License Agreement';

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
    const apiKeyInput = screen.getByLabelText('Zscaler API Key');
    const eulaCheckboxInput = screen.getByRole('checkbox', { name: CHECKBOX_NAME });
    const saveButton = screen.getByRole('button', { name: 'Save' });
    const cancelButton = screen.getByRole('button', { name: 'Cancel' });
    const deleteButton = screen.getByRole('button', { name: 'Delete Configuration' });
    const testConfigButton = screen.getByRole('button', { name: 'Test Configuration' });
    const description = 'To protect users at the network level, integrate our data with your Zscaler infrastructure.';
    const apiKeyDesc =
      'You can generate one in the Zscaler Admin Portal under API Management. Learn how to retrieve your API Key';
    const learnMoreLink = screen.getByRole('link', { name: /Learn more about the Zscaler integration/i });
    const apiKeyLink = screen.getByRole('link', { name: /Learn how to retrieve your API Key/i });

    expect(screen.getByRole('heading', { level: 2, name: /ZScaler Configuration/i })).toBeInTheDocument();
    expect(screen.getByText(description)).toBeInTheDocument();
    expect(learnMoreLink).toBeInTheDocument();
    expect(learnMoreLink).toHaveAttribute(
      'href',
      'https://links.sonatype.com/products/nxrm3/docs/zscaler/configuration'
    );
    expect(apiKeyLink).toBeInTheDocument();
    expect(apiKeyLink).toHaveAttribute('href', 'https://links.sonatype.com/products/nxrm3/docs/zscaler/api-keys');
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
    expect(apiKeyInput).toHaveAccessibleDescription(apiKeyDesc);
    expect(apiKeyInput).toHaveAttribute('placeholder', '465');
    expect(apiKeyInput).toHaveValue('');
    expect(eulaCheckboxInput).toBeInTheDocument();
    expect(eulaCheckboxInput).not.toBeChecked();
    // buttons
    expect(saveButton).toBeInTheDocument();
    expect(saveButton).not.toBeDisabled();
    expect(cancelButton).toBeInTheDocument();
    expect(cancelButton).toBeDisabled();
    expect(deleteButton).toBeInTheDocument();
    expect(deleteButton).toBeDisabled();
    expect(testConfigButton).toBeInTheDocument();
    expect(testConfigButton).toHaveClass('disabled');
  });

  it('renders the correct content when zscaler is configured', async () => {
    renderComponent(
      setState({
        usernameState: initUserInput('testUser'),
        passwordState: initUserInput(FAKE_PASSWORD), // password is null
        hostnameState: initUserInput('https://zsapi.zscalertwo.net'),
        apiKeyState: initUserInput('123'),
        eulaState: {
          value: true,
          isPristine: false,
          validationErrors: null,
          disabled: true,
        },
        hasAllRequiredData: true,
        serverData: {
          username: 'testUser',
          password: FAKE_PASSWORD,
          hostname: 'https://zsapi.zscalertwo.net',
          apiKeyState: '123',
        },
      })
    );
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByLabelText('Username')).toHaveValue('testUser');
    expect(screen.getByLabelText('Password')).toHaveValue(FAKE_PASSWORD);
    expect(screen.getByLabelText('Hostname')).toHaveValue('https://zsapi.zscalertwo.net');
    expect(screen.getByLabelText('Zscaler API Key')).toHaveValue('123');
    expect(screen.getByRole('checkbox', { name: CHECKBOX_NAME })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: CHECKBOX_NAME })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Update' })).toBeInTheDocument();
  });

  it('calls setting methods when updating the input fields', async () => {
    const setUsernameMock = jest.fn();
    const setPasswordMock = jest.fn();
    const setHostnameMock = jest.fn();
    const setApiKeyMock = jest.fn();
    const setEulaCheckboxMock = jest.fn();

    renderComponent(
      setState({
        setUsername: setUsernameMock,
        setPassword: setPasswordMock,
        setHostname: setHostnameMock,
        setApiKey: setApiKeyMock,
        setEulaCheckbox: setEulaCheckboxMock,
      })
    );
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const usernameInput = screen.getByLabelText('Username');
    const passwordInput = screen.getByLabelText('Password');
    const hostnameInput = screen.getByLabelText('Hostname');
    const apiKeyInput = screen.getByLabelText('Zscaler API Key');
    const eulaCheckboxInput = screen.getByRole('checkbox', { name: CHECKBOX_NAME });

    fireEvent.change(usernameInput, { target: { value: 'admin' } });
    expect(setUsernameMock).toHaveBeenCalledWith('admin', expect.anything());

    fireEvent.change(passwordInput, { target: { value: 'asdf' } });
    expect(setPasswordMock).toHaveBeenCalledWith('asdf', expect.anything());

    fireEvent.change(hostnameInput, { target: { value: 'https://zsapi.zscalertwo.test.net' } });
    expect(setHostnameMock).toHaveBeenCalledWith('https://zsapi.zscalertwo.test.net', expect.anything());

    fireEvent.change(apiKeyInput, { target: { value: '111' } });
    expect(setApiKeyMock).toHaveBeenCalledWith('111', expect.anything());

    fireEvent.click(eulaCheckboxInput);
    expect(setEulaCheckboxMock).toHaveBeenCalledWith(true);
  });

  it('renders password sub-label when hasAllRequiredData and mustReenterPassword are true', async () => {
    renderComponent();
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.queryByText('Password must be re-entered when any fields are modified.')).not.toBeInTheDocument();

    renderComponent(setState({ hasAllRequiredData: true, mustReenterPassword: true }));
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByText('Password must be re-entered when any fields are modified.')).toBeInTheDocument();
  });

  it('renders delete configuration model when showDeleteModal is true', async () => {
    renderComponent(setState({ showDeleteModal: true }));
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const modal = screen.getByRole('dialog');
    expect(modal).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: /Delete Zscaler Configuration?/i })).toBeInTheDocument();
    expect(modal).toHaveTextContent(
      'This action cannot be undone. Are you sure you want to delete this configuration?'
    );
  });

  it('renders testConfig button with tooltips when not provide all required fields', async () => {
    renderComponent();
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());
    const testConfigButton = screen.getByRole('button', { name: 'Test Configuration' });

    fireEvent.mouseOver(testConfigButton);
    const tooltip = await screen.findByRole('tooltip');
    expect(
      within(tooltip).getByText('Username, Password, Hostname and Zscaler API Key are required details.')
    ).toBeInTheDocument();
  });

  it('renders testConfig button with tooltips when not re-enter password', async () => {
    renderComponent(setState({ hasAllRequiredData: true }));
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());
    const testConfigButton = screen.getByRole('button', { name: 'Test Configuration' });

    fireEvent.mouseOver(testConfigButton);
    const tooltip = await screen.findByRole('tooltip');
    expect(within(tooltip).getByText('Password must be re-entered for testing configuration.')).toBeInTheDocument();
  });

  it('calls testConfig when testConfig button is clicked', async () => {
    const testConfigMock = jest.fn();
    renderComponent(
      setState({
        usernameState: userInput(null, 'testUser'),
        passwordState: userInput(null, 'password'),
        hostnameState: userInput(null, 'https://zsapi.zscalertwo.net'),
        apiKeyState: userInput(null, '123'),
        testConfig: testConfigMock,
        hasAllRequiredData: true,
        isDirty: true,
      })
    );
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const testConfigButton = screen.getByRole('button', { name: 'Test Configuration' });
    fireEvent.click(testConfigButton);
    expect(testConfigMock).toHaveBeenCalled();
  });

  it('renders success alert when testConfigSuccess is true', async () => {
    renderComponent(
      setState({
        testConfigSuccess: true,
        hasAllRequiredData: true,
        isDirty: true,
      })
    );
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const alert = screen.getByRole('status');
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent(
      'The connection to Zscaler was successfully established. Test Zscaler configuration succeed.'
    );
  });

  it('renders error alert when testConfigError is true', async () => {
    renderComponent(
      setState({
        testConfigError: true,
        hasAllRequiredData: true,
        isDirty: true,
        eulaState: {
          value: true,
          isPristine: false,
          validationErrors: null,
        },
      })
    );
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const alert = screen.getByRole('alert');
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent(
      'Unable to establish the connection to Zscaler as the connection is not configured. Test Zscaler ' +
        'configuration failed. Learn more about the Zscaler integrationRetry'
    );
  });
});
