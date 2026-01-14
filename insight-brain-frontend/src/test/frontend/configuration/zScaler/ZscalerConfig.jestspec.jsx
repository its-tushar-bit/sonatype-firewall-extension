/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, waitFor, screen, fireEvent } from 'TestRoot/SpecUtil';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

import ZscalerConfig from 'MainRoot/configuration/zscaler/ZscalerConfig';
import { FAKE_PASSWORD } from 'MainRoot/configuration/zscaler/zscalerConfigSlice';

const { initialState: initUserInput, userInput } = nxTextInputStateHelpers;

describe('ZscalerConfig', () => {
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
    configuredFormatState: {
      formats: new Set(),
      isPristine: true,
      validationErrors: 'At least one format must be selected',
    },
    isDirty: false,
    isValid: false,
    hasAllRequiredData: false,
    hasAllRequiredDataForTestConfig: false,
    loading: false,
    submitMaskState: null,
    submitMaskMessage: null,
    loadError: null,
    saveError: null,
    deleteError: null,
    testConfigError: null,
    testConfigSuccess: false,
    showDeleteModal: false,
    mustReenterPassword: false,
    isAuthorized: true,
    loadAll: jest.fn(),
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
    setConfiguredFormats: jest.fn(),
    zscalerConfigLimitsState: {
      loading: false,
      error: null,
      limits: null,
    },
    loadLimits: jest.fn(),
  };

  const setState = (additionalProps = {}) => Object.freeze({ ...initialProps, ...additionalProps });

  const renderComponent = (props = setState()) => render(<ZscalerConfig {...props} />);

  const CHECKBOX_NAME =
    `By clicking "Save" below, I hereby acknowledge and agree that ` +
    `access to and use of Sonatype's Zscaler integration is subject to ` +
    `and governed by these License Terms.`;

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
    const description =
      'To protect users at the network level, integrate Sonatype data with your Zscaler infrastructure.';
    const apiKeyDesc =
      'Generate a Zscaler API Key through the Admin Portal under API Management. Learn how to retrieve Zscaler API Key';
    const learnMoreLink = screen.getByRole('link', { name: /Learn more about the Zscaler integration/i });
    const apiKeyLink = screen.getByRole('link', { name: /Learn how to retrieve Zscaler API Key/i });
    const licenseTermsLink = screen.getByRole('link', { name: /License Terms/i });
    const configuredFormatLabel = screen.getByText('Configured Formats');
    const configuredFormatSubLabel = screen.getByText('Limit the number of urls pushed to Zscaler.');
    const formatDropdownButton = screen.getByRole('button', { name: 'Formats' });
    const configuredFormatsTooltip = screen.getByTestId('tooltip-icon');

    // heading and description
    expect(screen.getByRole('heading', { level: 2, name: /ZScaler Configuration/i })).toBeInTheDocument();
    expect(screen.getByText(description)).toBeInTheDocument();
    // links
    expect(learnMoreLink).toBeInTheDocument();
    expect(learnMoreLink).toHaveAttribute(
      'href',
      'https://links.sonatype.com/products/nxrm3/docs/zscaler/configuration'
    );
    expect(apiKeyLink).toBeInTheDocument();
    expect(apiKeyLink).toHaveAttribute('href', 'https://links.sonatype.com/products/nxrm3/docs/zscaler/api-keys');
    expect(licenseTermsLink).toBeInTheDocument();
    expect(licenseTermsLink).toHaveAttribute(
      'href',
      'https://links.sonatype.com/products/firewall/docs/zscaler/zscaler-eula'
    );
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
    // formats dropdown
    expect(configuredFormatLabel).toBeInTheDocument();
    expect(configuredFormatSubLabel).toBeInTheDocument();
    expect(formatDropdownButton).toBeInTheDocument();
    expect(configuredFormatsTooltip).toBeInTheDocument();
    fireEvent.mouseOver(configuredFormatsTooltip);
    const tooltip = await screen.findByRole('tooltip');
    expect(tooltip).toHaveTextContent(
      'URLs pushed to Zscaler are based on official package sources. Limiting formats ' +
        'reduces noise and optimizes security rules. Dependencies from unofficial or custom sources are not fully protected ' +
        'by this integration.'
    );
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
        configuredFormatState: {
          formats: new Set(['mavenFormatEnabled', 'npmFormatEnabled']),
          isPristine: true,
          validationErrors: null,
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
    expect(screen.getByRole('button', { name: '2 of 4' })).toBeInTheDocument();
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
    const setConfiguredFormatsMock = jest.fn();

    renderComponent(
      setState({
        setUsername: setUsernameMock,
        setPassword: setPasswordMock,
        setHostname: setHostnameMock,
        setApiKey: setApiKeyMock,
        setEulaCheckbox: setEulaCheckboxMock,
        setConfiguredFormats: setConfiguredFormatsMock,
      })
    );
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const usernameInput = screen.getByLabelText('Username');
    const passwordInput = screen.getByLabelText('Password');
    const hostnameInput = screen.getByLabelText('Hostname');
    const apiKeyInput = screen.getByLabelText('Zscaler API Key');
    const eulaCheckboxInput = screen.getByRole('checkbox', { name: CHECKBOX_NAME });
    const formatDropdownButton = screen.getByRole('button', { name: 'Formats' });

    fireEvent.change(usernameInput, { target: { value: 'admin' } });
    expect(setUsernameMock).toHaveBeenCalledWith('admin', expect.anything());

    fireEvent.change(passwordInput, { target: { value: 'asdf' } });
    expect(setPasswordMock).toHaveBeenCalledWith('asdf', expect.anything());

    fireEvent.change(hostnameInput, { target: { value: 'https://zsapi.zscalertwo.test.net' } });
    expect(setHostnameMock).toHaveBeenCalledWith('https://zsapi.zscalertwo.test.net', expect.anything());

    fireEvent.change(apiKeyInput, { target: { value: '111' } });
    expect(setApiKeyMock).toHaveBeenCalledWith('111', expect.anything());

    fireEvent.click(formatDropdownButton);
    const mavenCheckbox = screen.getByRole('checkbox', { name: 'Maven' });
    const npmCheckbox = screen.getByRole('checkbox', { name: 'Npm' });
    const nugetCheckbox = screen.getByRole('checkbox', { name: 'Nuget' });
    const pypiCheckbox = screen.getByRole('checkbox', { name: 'Pypi' });
    fireEvent.click(mavenCheckbox);
    expect(setConfiguredFormatsMock).toHaveBeenCalledWith(new Set(['mavenFormatEnabled']), 'mavenFormatEnabled');
    fireEvent.click(npmCheckbox);
    expect(setConfiguredFormatsMock).toHaveBeenCalledWith(new Set(['npmFormatEnabled']), 'npmFormatEnabled');
    fireEvent.click(nugetCheckbox);
    expect(setConfiguredFormatsMock).toHaveBeenCalledWith(new Set(['nugetFormatEnabled']), 'nugetFormatEnabled');
    fireEvent.click(pypiCheckbox);
    expect(setConfiguredFormatsMock).toHaveBeenCalledWith(new Set(['pypiFormatEnabled']), 'pypiFormatEnabled');

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
    expect(tooltip).toHaveTextContent('Username, Password, Hostname and Zscaler API Key are required details.');
  });

  it('renders testConfig button with tooltips when not re-enter password', async () => {
    renderComponent(setState({ hasAllRequiredDataForTestConfig: true }));
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());
    const testConfigButton = screen.getByRole('button', { name: 'Test Configuration' });

    fireEvent.mouseOver(testConfigButton);
    const tooltip = await screen.findByRole('tooltip');
    expect(tooltip).toHaveTextContent('Password must be re-entered for testing configuration.');
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
        hasAllRequiredDataForTestConfig: true,
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

    const alert = screen.getAllByRole('status');
    expect(alert[1]).toBeInTheDocument();
    expect(alert[1]).toHaveTextContent('Connection to Zscaler successful.');
  });

  it('renders error alert when testConfigError has an error message', async () => {
    renderComponent(
      setState({
        testConfigError:
          'Insufficient ZScaler permissions. The user account must have both CUSTOM_URL_CAT and OVERRIDE_EXISTING_CAT permissions with READ_WRITE access.',
        hasAllRequiredData: true,
        isDirty: true,
        eulaState: {
          value: true,
          isPristine: false,
          validationErrors: null,
          disabled: false,
        },
      })
    );
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const alert = screen.getByRole('alert');
    expect(alert).toBeInTheDocument();
    // Now shows the actual backend error message
    expect(alert).toHaveTextContent('Test Zscaler configuration failed.');
    expect(alert).toHaveTextContent('Insufficient ZScaler permissions');
  });
});
