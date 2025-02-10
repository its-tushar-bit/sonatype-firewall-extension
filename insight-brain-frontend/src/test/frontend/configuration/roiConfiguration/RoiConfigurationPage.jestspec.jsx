/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as R from 'ramda';
import { axiosMockAdapter, render, waitFor, screen } from 'TestRoot/SpecUtil';
import RoiConfigurationPage from 'MainRoot/configuration/roiConfiguration/RoiConfigurationPage';

import { getPermissionContextTestUrl } from 'MainRoot/utilAngular/CLMContextLocation';

describe('roiConfigurationPage', () => {
  let axiosMock;

  const sampleConfiguration = Object.freeze({
    developerHourlyRate: 1000.11,
    fixRate: 5,
    securityViolation: {
      criticalEnabled: true,
      critical: 2000.22,
      highEnabled: true,
      high: 3000.33,
      mediumEnabled: true,
      medium: 4000.44,
      lowEnabled: false,
      low: 5000.55,
    },
    supplyChainAttacksBlocked: 6000.66,
    namespaceAttacksBlocked: 7000.77,
    safeComponentsAutoSelected: 8000.88,
    waivedViolations: true,
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

  const renderComponent = (preloadedState = bootstrapState()) => render(<RoiConfigurationPage />, { preloadedState });

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
  });

  it('renders the correct content', async () => {
    axiosMock.onPut(getPermissionContextTestUrl('global', 'global')).reply(200, ['CONFIGURE_SYSTEM']);

    renderComponent();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByRole('heading', { level: 1, name: /Return on Investment/i })).toBeVisible();

    const numericValueClass = (value) => `roi-configuration-page__numeric-value__${value}`;

    expect(screen.getByTestId(numericValueClass('developer-hourly-rate'))).toHaveTextContent('$1,000.11');
    expect(screen.getByTestId(numericValueClass('fix-rate'))).toHaveTextContent('5');
    expect(screen.getByTestId(numericValueClass('security-violation-critical'))).toHaveTextContent('$2,000.22');
    expect(screen.getByTestId(numericValueClass('security-violation-high'))).toHaveTextContent('$3,000.33');
    expect(screen.getByTestId(numericValueClass('security-violation-medium'))).toHaveTextContent('$4,000.44');
    expect(screen.getByTestId('roi-configuration-security-violation-types__content__low')).toHaveTextContent(
      'Not Included'
    );
    expect(screen.getByTestId(numericValueClass('supply-chain-attacks-blocked'))).toHaveTextContent('$6,000.66');
    expect(screen.getByTestId(numericValueClass('namespace-attacks-blocked'))).toHaveTextContent('$7,000.77');
    expect(screen.getByTestId(numericValueClass('safe-components-auto-selected'))).toHaveTextContent('$8,000.88');

    expect(screen.getByTestId('roi-configuration-page__checkbox__waived-violations')).toBeChecked();
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
