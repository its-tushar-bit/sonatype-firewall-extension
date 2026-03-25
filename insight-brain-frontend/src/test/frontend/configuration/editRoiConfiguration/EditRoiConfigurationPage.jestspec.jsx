/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as R from 'ramda';
import userEvent from '@testing-library/user-event';

import { axiosMockAdapter, render, waitFor, screen, within } from 'TestRoot/SpecUtil';
import EditRoiConfigurationPage from 'MainRoot/configuration/editRoiConfiguration/EditRoiConfigurationPage';
import { instantiateNumericState } from 'MainRoot/configuration/editRoiConfiguration/editRoiConfigurationPageSlice';

import { getPermissionContextTestUrl } from 'MainRoot/util/CLMContextLocation';
import { getRoiConfigurationUrl } from 'MainRoot/util/CLMLocation';

describe('editRoiConfigurationPage', () => {
  let axiosMock;

  const mockGetPayload = Object.freeze({
    baselineDaysToResolveViolationMinimum: 100,
    baselineDaysToResolveViolation: 150,
    dailyRiskCostOfUnfixedViolationMinimum: 1000,
    dailyRiskCostOfUnfixedViolation: 1234.56,
    malwareAttacksPreventedMinimum: 1000,
    malwareAttacksPrevented: 1111.11,
    namespaceAttacksPreventedMinimum: 1000,
    namespaceAttacksPrevented: 2222.22,
    safeComponentsAutoSelectedMinimum: 1000,
    safeComponentsAutoSelected: 3333.33,
  });

  const initialConfiguration = Object.freeze({
    baselineDaysToResolveViolation: instantiateNumericState(true, 0, 0),
    dailyRiskCostOfUnfixedViolation: instantiateNumericState(true, 0, 0),
    malwareAttacksPrevented: instantiateNumericState(true, 0, 0),
    namespaceAttacksPrevented: instantiateNumericState(true, 0, 0),
    safeComponentsAutoSelected: instantiateNumericState(true, 0, 0),
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
        configuration: R.mergeDeepRight({ ...initialConfiguration })(configuration),
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
    axiosMock.onGet(getRoiConfigurationUrl('usd')).reply(200, mockGetPayload);

    renderComponent();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByRole('heading', { name: /Return on Investment Configuration/i })).toBeVisible();

    const baselineDaysWrapper = screen.getByTestId(
      'edit-roi-configuration-page__input__baseline-days-to-resolve-violation'
    );
    expect(within(baselineDaysWrapper).getByRole('textbox')).toHaveValue('150');

    const dailyRiskWrapper = screen.getByTestId(
      'edit-roi-configuration-page__input__daily-risk-cost-of-unfixed-violation'
    );
    expect(within(dailyRiskWrapper).getByRole('textbox')).toHaveValue('1234.56');

    const malwareWrapper = screen.getByTestId('edit-roi-configuration-page__input__malware-attacks-prevented');
    expect(within(malwareWrapper).getByRole('textbox')).toHaveValue('1111.11');

    const namespaceWrapper = screen.getByTestId('edit-roi-configuration-page__input__namespace-attacks-prevented');
    expect(within(namespaceWrapper).getByRole('textbox')).toHaveValue('2222.22');

    const safeComponentsWrapper = screen.getByTestId(
      'edit-roi-configuration-page__input__safe-components-auto-selected'
    );
    expect(within(safeComponentsWrapper).getByRole('textbox')).toHaveValue('3333.33');
  });

  it('renders validation error and hides update button', async () => {
    axiosMock.onPut(getPermissionContextTestUrl('global', 'global')).reply(200, ['CONFIGURE_SYSTEM']);
    axiosMock.onGet(getRoiConfigurationUrl('usd')).reply(200, mockGetPayload);
    const user = userEvent.setup();
    renderComponent();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const updateButton = screen.getByRole('button', { name: /Update/i });
    expect(updateButton).toBeVisible();

    const malwareWrapper = screen.getByTestId('edit-roi-configuration-page__input__malware-attacks-prevented');
    const malwareInput = within(malwareWrapper).getByRole('textbox');
    expect(malwareInput).toHaveValue('1111.11');

    await user.clear(malwareInput);
    await user.type(malwareInput, '10');

    expect(malwareInput.value).toBe('10');

    expect(screen.queryByText(/Must be greater than or equal to/i)).toBeVisible();

    const validationError = screen.getByTestId('edit-roi-configuration-page__alert__validation-error');
    expect(validationError).toBeVisible();

    expect(updateButton).not.toBeVisible();
  });

  describe('Restore Defaults Modal', () => {
    it('opens and closes', async () => {
      axiosMock.onPut(getPermissionContextTestUrl('global', 'global')).reply(200, ['CONFIGURE_SYSTEM']);
      axiosMock.onGet(getRoiConfigurationUrl('usd')).reply(200, mockGetPayload);
      const user = userEvent.setup();
      renderComponent();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const openModalButton = screen.getByRole('button', { name: /Restore Default Values/i });
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

      openModalButton.click();

      await waitFor(() => {
        expect(screen.queryByRole('dialog')).toBeInTheDocument();
      });

      expect(screen.getByRole('heading', { name: /Restore Default Values/i })).toBeVisible();

      const cancelButton = screen.getByRole('button', { name: /Cancel/i });

      user.click(cancelButton);
      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });
  });

  describe('license rendering', () => {
    it('should not render when both Lifecycle and Repository Firewal licenses are not present', async () => {
      axiosMock.onPut(getPermissionContextTestUrl('global', 'global')).reply(200, ['CONFIGURE_SYSTEM']);
      axiosMock.onGet(getRoiConfigurationUrl('usd')).reply(200, mockGetPayload);

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
      axiosMock.onGet(getRoiConfigurationUrl('usd')).reply(200, mockGetPayload);

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
      axiosMock.onGet(getRoiConfigurationUrl('usd')).reply(200, mockGetPayload);

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
      axiosMock.onGet(getRoiConfigurationUrl('usd')).reply(200, mockGetPayload);

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
