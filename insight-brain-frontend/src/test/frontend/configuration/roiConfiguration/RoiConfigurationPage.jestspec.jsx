/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as R from 'ramda';
import { axiosMockAdapter, render, waitFor, screen } from 'TestRoot/SpecUtil';
import RoiConfigurationPage from 'MainRoot/configuration/roiConfiguration/RoiConfigurationPage';

import { getPermissionContextTestUrl } from 'MainRoot/util/CLMContextLocation';
import { getRoiConfigurationUrl } from 'MainRoot/util/CLMLocation';

describe('roiConfigurationPage', () => {
  let axiosMock;

  const mockPayload = Object.freeze({
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
    baselineDaysToResolveViolation: 0,
    dailyRiskCostOfUnfixedViolation: 0,
    malwareAttacksPrevented: 0,
    namespaceAttacksPrevented: 0,
    safeComponentsAutoSelected: 0,
  });

  const bootstrapState = ({
    loading = true,
    error = null,
    configuration = {},
    withLifecycleLicense = true,
    withFirewallLicense = true,
  } = {}) =>
    Object.freeze({
      roiConfigurationPage: {
        loading,
        error,
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

  const renderComponent = (preloadedState = bootstrapState()) => render(<RoiConfigurationPage />, { preloadedState });

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
  });

  it('renders the correct content', async () => {
    axiosMock.onPut(getPermissionContextTestUrl('global', 'global')).reply(200, ['CONFIGURE_SYSTEM']);
    axiosMock.onGet(getRoiConfigurationUrl('usd')).reply(200, mockPayload);

    renderComponent();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByRole('heading', { level: 1, name: /Return on Investment/i })).toBeVisible();

    const numericValueClass = (value) => `roi-configuration-page__numeric-value__${value}`;
    expect(screen.getByTestId(numericValueClass('baseline-days-to-resolve-violation'))).toHaveTextContent('150 days');
    expect(screen.getByTestId(numericValueClass('daily-risk-cost-of-unfixed-violation'))).toHaveTextContent(
      '$1,234.56'
    );
    expect(screen.getByTestId(numericValueClass('safe-components-auto-selected'))).toHaveTextContent('$3,333.33');
    expect(screen.getByTestId(numericValueClass('malware-attacks-prevented'))).toHaveTextContent('$1,111.11');
    expect(screen.getByTestId(numericValueClass('namespace-attacks-prevented'))).toHaveTextContent('$2,222.22');
    expect(screen.getByTestId(numericValueClass('safe-components-auto-selected'))).toHaveTextContent('$3,333.33');
  });

  describe('license rendering', () => {
    it('should not render when both Lifecycle and Repository Firewal licenses are not present', async () => {
      axiosMock.onPut(getPermissionContextTestUrl('global', 'global')).reply(200, ['CONFIGURE_SYSTEM']);
      axiosMock.onGet(getRoiConfigurationUrl('usd')).reply(200, mockPayload);

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
      axiosMock.onGet(getRoiConfigurationUrl('usd')).reply(200, mockPayload);

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
      axiosMock.onGet(getRoiConfigurationUrl('usd')).reply(200, mockPayload);

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
      axiosMock.onGet(getRoiConfigurationUrl('usd')).reply(200, mockPayload);

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
