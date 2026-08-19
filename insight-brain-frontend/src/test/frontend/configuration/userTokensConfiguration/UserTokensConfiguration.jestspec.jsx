/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';

import { axiosMockAdapter, fireEvent, render, screen, waitFor } from 'TestRoot/SpecUtil';
import UserTokensConfiguration from 'MainRoot/configuration/userTokensConfiguration/UserTokensConfiguration';
import { getConfigurationUrl } from 'MainRoot/util/CLMLocation';
import { CONFIG_PROPERTIES_PARAMS } from 'MainRoot/configuration/userTokensConfiguration/userTokensConfigurationSlice';

describe('UserTokensConfiguration', () => {
  let renderComponent, axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet(getConfigurationUrl().concat(CONFIG_PROPERTIES_PARAMS)).reply(200, {
      userTokenDefaultExpirationDays: null,
    });

    renderComponent = (preloadedState = {}) =>
      render(<UserTokensConfiguration />, {
        preloadedState: {
          userTokensConfiguration: {
            loading: false,
            loadError: null,
            updateError: null,
            isDirty: false,
            submitMaskState: null,
            formState: {
              userTokensEnabled: true,
              expirationEnabled: false,
              expirationDays: { value: '30', isPristine: true, validationErrors: null },
            },
            serverData: {
              userTokenDefaultExpirationDays: null,
            },
          },
          ...preloadedState,
        },
      });
  });

  it('should render UserTokensConfiguration with all sections', async () => {
    renderComponent();

    expect(await screen.findByText('Token Configuration')).toBeVisible();
    expect(screen.getByRole('heading', { name: 'User Tokens' })).toBeVisible();
    expect(screen.getByText('Manage user token configuration')).toBeVisible();
    expect(screen.getByText(/The user tokens feature allows users to authenticate securely/)).toBeVisible();
    expect(screen.getByLabelText('Enable User Tokens')).toBeVisible();
    expect(screen.getByLabelText('Enable User Token Expiration')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Update' })).not.toBeDisabled();
  });

  it('should have user tokens toggle enabled and disabled', async () => {
    renderComponent();

    const userTokensToggle = await screen.findByLabelText('Enable User Tokens');
    expect(userTokensToggle).toBeChecked();
    expect(userTokensToggle).toBeDisabled();
  });

  it('should toggle expiration enabled', async () => {
    renderComponent();

    const expirationToggle = await screen.findByLabelText('Enable User Token Expiration');
    expect(expirationToggle).not.toBeChecked();

    fireEvent.click(expirationToggle);
    expect(expirationToggle).toBeChecked();
    expect(screen.getByRole('button', { name: 'Cancel' })).not.toBeDisabled();
  });

  it('should enable/disable expiration days input based on expiration toggle', async () => {
    renderComponent();

    const expirationToggle = await screen.findByLabelText('Enable User Token Expiration');
    const expirationDaysInput = screen.getByRole('textbox');

    expect(expirationDaysInput).toBeDisabled();

    fireEvent.click(expirationToggle);
    expect(expirationDaysInput).not.toBeDisabled();
  });

  it('should update expiration days value', async () => {
    const user = userEvent.setup();
    renderComponent();

    const expirationToggle = await screen.findByLabelText('Enable User Token Expiration');
    fireEvent.click(expirationToggle);

    const expirationDaysInput = screen.getByRole('textbox');
    await user.clear(expirationDaysInput);
    await user.type(expirationDaysInput, '90');

    expect(expirationDaysInput).toHaveValue('90');
  });

  it('should show validation error for invalid input', async () => {
    const user = userEvent.setup();
    renderComponent();

    const expirationToggle = await screen.findByLabelText('Enable User Token Expiration');
    fireEvent.click(expirationToggle);

    const expirationDaysInput = screen.getByRole('textbox');
    await user.clear(expirationDaysInput);
    await user.type(expirationDaysInput, '500');

    const updateBtn = screen.getByRole('button', { name: 'Update' });
    fireEvent.click(updateBtn);

    await waitFor(() => {
      expect(screen.getAllByText(/Must be at most 365 days/).length).toBeGreaterThan(0);
    });
  });

  it('should show validation error for empty input when expiration enabled', async () => {
    const user = userEvent.setup();
    renderComponent();

    const expirationToggle = await screen.findByLabelText('Enable User Token Expiration');
    fireEvent.click(expirationToggle);

    const expirationDaysInput = screen.getByRole('textbox');
    await user.clear(expirationDaysInput);

    const updateBtn = screen.getByRole('button', { name: 'Update' });
    fireEvent.click(updateBtn);

    await waitFor(() => {
      expect(screen.getByText('Must be non-empty.')).toBeVisible();
    });
  });

  it('should show no changes error when form is not dirty', async () => {
    renderComponent();

    const updateBtn = await screen.findByRole('button', { name: 'Update' });
    fireEvent.click(updateBtn);

    expect(screen.getByText('There were validation errors. There are no changes to update.')).toBeVisible();
  });

  it('should reset form on cancel', async () => {
    renderComponent();

    const expirationToggle = await screen.findByLabelText('Enable User Token Expiration');
    fireEvent.click(expirationToggle);

    expect(expirationToggle).toBeChecked();
    expect(screen.getByRole('button', { name: 'Cancel' })).not.toBeDisabled();

    const cancelBtn = screen.getByRole('button', { name: 'Cancel' });
    fireEvent.click(cancelBtn);

    expect(expirationToggle).not.toBeChecked();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
  });

  it('should render with loading state', () => {
    renderComponent({
      userTokensConfiguration: {
        loading: true,
        loadError: null,
        updateError: null,
        isDirty: false,
        submitMaskState: null,
        formState: {
          userTokensEnabled: true,
          expirationEnabled: false,
          expirationDays: { value: '30', isPristine: true, validationErrors: null },
        },
        serverData: null,
      },
    });

    expect(screen.getByText('Loading…')).toBeVisible();
  });
});
