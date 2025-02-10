/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as R from 'ramda';
import { axiosMockAdapter, render, waitFor, screen } from 'TestRoot/SpecUtil';
import EditRoiConfigurationPage from 'MainRoot/configuration/editRoiConfiguration/EditRoiConfigurationPage';
import { generateDefaultNumericState } from 'MainRoot/configuration/editRoiConfiguration/editRoiConfigurationPageSlice';

import { getPermissionContextTestUrl } from 'MainRoot/utilAngular/CLMContextLocation';

describe('editRoiConfigurationPage', () => {
  let axiosMock;

  const sampleConfiguration = Object.freeze({
    developerHourlyRate: generateDefaultNumericState(true, 0, 1000.11),
    fixRate: generateDefaultNumericState(true, 0, 5),

    securityViolation: {
      critical: generateDefaultNumericState(true, 0, 2000.22),
      high: generateDefaultNumericState(true, 0, 3000.33),
      medium: generateDefaultNumericState(false, 0, 4000.44),
      low: generateDefaultNumericState(false, 0, 5000.55),
    },

    supplyChainAttacksBlocked: generateDefaultNumericState(true, 0, 6000.66),
    namespaceAttacksBlocked: generateDefaultNumericState(true, 0, 7000.77),
    safeComponentsAutoSelected: generateDefaultNumericState(true, 0, 8000.88),

    waivedViolations: true,
  });

  const bootstrapState = ({
    loading = true,
    error = null,
    configuration = {},
    withLifecycleLicense = true,
    withFirewallLicense = true,
    showModal = false,
  } = {}) =>
    Object.freeze({
      editRoiConfigurationPage: {
        loading,
        error,
        showRestoreDefaultsModal: showModal,
        configuration: R.mergeDeepRight({ ...sampleConfiguration })(configuration),
      },
      productLicense: {
        license: {
          products: R.compose(
            R.when(R.always(withLifecycleLicense), R.append('Sonatype Lifecycle SaaS')),
            R.when(R.always(withFirewallLicense), R.append('Sonatype Lifecycle Firewall SaaS'))
          )([]),
        },
      },
      router: { currentState: { name: 'firewall.roiConfiguration' } },
    });

  const renderComponent = (preloadedState = bootstrapState()) =>
    render(<EditRoiConfigurationPage />, { preloadedState });

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
  });

  it('renders the correct content', async () => {
    axiosMock.onPut(getPermissionContextTestUrl('global', 'global')).reply(200, ['CONFIGURE_SYSTEM']);

    renderComponent();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByRole('heading', { name: /Return on Investment Configuration/i })).toBeVisible();

    expect(screen.getByTestId('edit-roi-configuration-page__input__developer-hourly-rate')).toHaveValue('1000.11');

    expect(screen.getByTestId('edit-roi-configuration-page__input__fix-rate')).toHaveValue('5');

    expect(screen.getByTestId('edit-roi-configuration-page__security-violation-checkbox__critical')).toBeChecked();
    expect(screen.getByTestId('edit-roi-configuration-page__input__security-violation-critical')).toHaveValue(
      '2000.22'
    );
    expect(screen.getByTestId('edit-roi-configuration-page__input__security-violation-critical')).not.toBeDisabled();

    expect(screen.getByTestId('edit-roi-configuration-page__security-violation-checkbox__high')).toBeChecked();
    expect(screen.getByTestId('edit-roi-configuration-page__input__security-violation-high')).toHaveValue('3000.33');
    expect(screen.getByTestId('edit-roi-configuration-page__input__security-violation-high')).not.toBeDisabled();

    expect(screen.getByTestId('edit-roi-configuration-page__security-violation-checkbox__medium')).not.toBeChecked();
    expect(screen.getByTestId('edit-roi-configuration-page__input__security-violation-medium')).toHaveValue('4000.44');
    expect(screen.getByTestId('edit-roi-configuration-page__input__security-violation-medium')).toBeDisabled();

    expect(screen.getByTestId('edit-roi-configuration-page__security-violation-checkbox__low')).not.toBeChecked();
    expect(screen.getByTestId('edit-roi-configuration-page__input__security-violation-low')).toHaveValue('5000.55');
    expect(screen.getByTestId('edit-roi-configuration-page__input__security-violation-low')).toBeDisabled();

    expect(screen.getByTestId('edit-roi-configuration-page__input__supply-chain-attacks-blocked')).toHaveValue(
      '6000.66'
    );
    expect(screen.getByTestId('edit-roi-configuration-page__input__namespace-attacks-blocked')).toHaveValue('7000.77');
    expect(screen.getByTestId('edit-roi-configuration-page__input__safe-components-auto-selected')).toHaveValue(
      '8000.88'
    );

    expect(screen.getByTestId('edit-roi-configuration-page__checkbox__waived-violations')).toBeChecked();
  });

  describe('Restore Defaults Modal', () => {
    it('renders modal', async () => {
      axiosMock.onPut(getPermissionContextTestUrl('global', 'global')).reply(200, ['CONFIGURE_SYSTEM']);

      renderComponent();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const openModalButton = screen.getByRole('button', { name: /Restore Default Values/i });
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

      openModalButton.click();

      await waitFor(() => {
        expect(screen.queryByRole('dialog')).toBeInTheDocument();
      });

      expect(screen.getByRole('heading', { name: /Restore Default Values/i })).toBeVisible();
    });
  });

  describe('license rendering', () => {
    it('should not render when both Lifecycle and Repository Firewal licenses are not present', async () => {
      axiosMock.onPut(getPermissionContextTestUrl('global', 'global')).reply(200, ['CONFIGURE_SYSTEM']);

      renderComponent(
        bootstrapState({
          withLifecycleLicense: false,
          withFirewallLicense: false,
        })
      );

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      expect(
        screen.getByText(/Must have Lifecycle or Repository Firewall license to configure ROI metrics./i)
      ).toBeVisible();
    });

    it('should only render Lifecycle Metrics content when only Lifecycle license is present', async () => {
      axiosMock.onPut(getPermissionContextTestUrl('global', 'global')).reply(200, ['CONFIGURE_SYSTEM']);

      renderComponent(
        bootstrapState({
          withLifecycleLicense: true,
          withFirewallLicense: false,
        })
      );

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      expect(
        screen.queryByText(/Must have Lifecycle or Repository Firewall license to configure ROI metrics./i)
      ).not.toBeInTheDocument();

      expect(screen.getByRole('heading', { name: /Lifecycle Metrics/i })).toBeVisible();
      expect(screen.queryByText(/Repository Firewall Metrics/i)).not.toBeInTheDocument();
    });

    it('should only render Repository Firewall content when only Firewall license is present', async () => {
      axiosMock.onPut(getPermissionContextTestUrl('global', 'global')).reply(200, ['CONFIGURE_SYSTEM']);

      renderComponent(
        bootstrapState({
          withLifecycleLicense: false,
          withFirewallLicense: true,
        })
      );

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      expect(
        screen.queryByText(/Must have Lifecycle or Repository Firewall license to configure ROI metrics./i)
      ).not.toBeInTheDocument();

      expect(screen.queryByText(/Lifecycle Metrics/i)).not.toBeInTheDocument();
      expect(screen.getByRole('heading', { name: /Repository Firewall Metrics/i })).toBeVisible();
    });

    it('should render both Lifecycle and Firewall contents when both license are present', async () => {
      axiosMock.onPut(getPermissionContextTestUrl('global', 'global')).reply(200, ['CONFIGURE_SYSTEM']);

      renderComponent(
        bootstrapState({
          withLifecycleLicense: true,
          withFirewallLicense: true,
        })
      );

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      expect(
        screen.queryByText(/Must have Lifecycle or Repository Firewall license to configure ROI metrics./i)
      ).not.toBeInTheDocument();

      expect(screen.getByRole('heading', { name: /Lifecycle Metrics/i })).toBeVisible();
      expect(screen.getByRole('heading', { name: /Repository Firewall Metrics/i })).toBeVisible();
    });
  });
});
