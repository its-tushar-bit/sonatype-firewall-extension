/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import { waiverMatcherStrategy } from 'MainRoot/firewall/bulkWaive/firewallWaiverUtils';
import moment from 'moment';
import DashboardWaiversTableRow from 'MainRoot/dashboard/results/waivers/DashboardWaiversTableRow';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';

import 'TestRoot/SpecUtil';

describe('DashboardWaiversTableRow', function () {
  const createTime = 1661485533772;
  const expiryTime = 1662094799999;
  let renderComponent, stateGoSpy, minimalProps;
  function getUpgradeCell() {
    const row = screen.getByRole('row');
    return row.children[6];
  }

  beforeEach(function () {
    stateGoSpy = jest
      .fn()
      .mockName('stateGo')
      .mockReturnValue({
        type: 'STATE_GO',
        payload: {
          state: 'waiver.details',
          params: {
            waiverId: 'waiverId',
            ownerId: 'ownerId',
            ownerType: 'organization',
            type: 'waiver',
            sidebarReference: 'filter',
            page: 3,
          },
        },
      });
    minimalProps = {
      stateGo: stateGoSpy,
      page: 2,
      waiver: {
        id: 'waiverId',
        threatLevel: 8,
        createTime,
        expiryTime,
        policyName: 'policyName',
        ownerId: 'ownerId',
        ownerName: 'ownerName',
        ownerType: 'organization',
        scope: 'Organization - ownerName',
        associatedPackageUrl: 'a/package/url',
        matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
        displayName: {
          parts: [
            {
              field: 'Group',
              value: 'test-group',
            },
            {
              value: ':',
            },
            {
              field: 'Artifact',
              value: 'test-artifact',
            },
            {
              value: ':',
            },
            {
              field: 'Version',
              value: '1.2.3',
            },
          ],
        },
      },
    };
    renderComponent = (additionalWaiverProps = {}) => {
      const additionalProps = { waiver: { ...minimalProps.waiver, ...additionalWaiverProps } };
      return render(<DashboardWaiversTableRow {...minimalProps} {...additionalProps} isExpiringWaiversEnabled={true} />);
    };
  });

  it('renders all the waiver info', () => {
    renderComponent(minimalProps);

    expect(screen.getByLabelText('threat level critical')).toBeVisible();
    expect(screen.getByText('8')).toBeVisible();
    expect(screen.getByText(moment(createTime).format('YYYY-MM-DD'))).toBeVisible();
    expect(screen.getByText(moment(expiryTime).format('YYYY-MM-DD'))).toBeVisible();
    expect(screen.getByText('policyName')).toBeVisible();
    expect(screen.getByText('Organization - ownerName')).toBeVisible();
    expect(screen.getByText('test-group:test-artifact:1.2.3')).toBeVisible();
  });

  it('links on click to the waiver details state', async () => {
    const user = userEvent.setup();
    const { container } = await renderComponent(minimalProps);

    await user.click(container.children[0]);

    expect(stateGoSpy).toHaveBeenCalledWith('waiver.details', {
      waiverId: 'waiverId',
      ownerId: 'ownerId',
      ownerType: 'organization',
      type: 'waiver',
      sidebarReference: 'filter',
      page: 3,
    });
  });

  it('renders waiver scope', () => {
    renderComponent({ scope: 'my scope' });

    expect(screen.getByText('my scope')).toBeVisible();
  });

  it('renders a row with expiration time set to never', () => {
    renderComponent({ expiryTime: null });

    expect(screen.getByText('Never')).toBeVisible();
  });

  it('renders a row with threat level severe', () => {
    renderComponent({ threatLevel: 5 });

    expect(screen.getByLabelText('threat level severe')).toBeVisible();
    expect(screen.getByText('5')).toBeVisible();
  });

  it('renders a row with threat level moderate', () => {
    renderComponent({ threatLevel: 3 });

    expect(screen.getByLabelText('threat level moderate')).toBeVisible();
    expect(screen.getByText('3')).toBeVisible();
  });

  it('renders a row with threat level low', () => {
    renderComponent({ threatLevel: 1 });

    expect(screen.getByLabelText('threat level low')).toBeVisible();
    expect(screen.getByText('1')).toBeVisible();
  });

  it('renders a row with threat level none', () => {
    renderComponent({ threatLevel: 0 });

    expect(screen.getByLabelText('threat level none')).toBeVisible();
    expect(screen.getByText('0')).toBeVisible();
  });

  it('renders a row with upgrade available empty', () => {
    renderComponent();

    const upgradeCell = getUpgradeCell();

    expect(upgradeCell).toHaveTextContent('—');
  });

  it('renders a row with upgrade available tag', () => {
    renderComponent({ componentUpgradeAvailable: true });

    const upgradeCell = getUpgradeCell();

    expect(upgradeCell).toHaveTextContent('Available');
  });

  it('renders a row with upgrade available empty when the component is unknown', () => {
    renderComponent({
      componentUpgradeAvailable: true,
      displayName: null,
    });

    const upgradeCell = getUpgradeCell();

    expect(upgradeCell).toHaveTextContent('—');
  });

  it('renders a row with upgrade available empty when match strategy is different than exact', () => {
    const { unmount } = renderComponent({
      componentUpgradeAvailable: true,
      matcherStrategy: waiverMatcherStrategy.ALL_COMPONENTS,
    });

    let upgradeCell = getUpgradeCell();

    expect(upgradeCell).toHaveTextContent('—');
    unmount();

    renderComponent({
      componentUpgradeAvailable: true,
      matcherStrategy: waiverMatcherStrategy.ALL_VERSIONS,
    });

    upgradeCell = getUpgradeCell();

    expect(upgradeCell).toHaveTextContent('—');
  });

  describe('expiry status descriptor', () => {
    const firewallRouterState = {
      router: {
        currentState: { name: 'firewall.dashboard.overview.waivers' },
        currentParams: {},
        prevState: null,
        prevParams: null,
      },
    };

    it('shows "Expires in X days" in grey when expiry is more than 7 days away', () => {
      const futureExpiry = moment().add(60, 'days').endOf('day').valueOf();
      render(
        <DashboardWaiversTableRow {...minimalProps} waiver={{ ...minimalProps.waiver, expiryTime: futureExpiry }} />,
        { preloadedState: firewallRouterState }
      );

      const status = screen.getByText(/Expires in \d+ days/);
      expect(status).toBeVisible();
      expect(status).toHaveClass('iq-waiver-expiry-status--muted');
    });

    it('shows "Expires in X days" in red when expiry is within 7 days', () => {
      const futureExpiry = moment().add(3, 'days').endOf('day').valueOf();
      render(
        <DashboardWaiversTableRow {...minimalProps} waiver={{ ...minimalProps.waiver, expiryTime: futureExpiry }} />,
        { preloadedState: firewallRouterState }
      );

      const status = screen.getByText(/Expires in \d+ days?/);
      expect(status).toBeVisible();
      expect(status).toHaveClass('iq-waiver-expiry-status--critical');
    });

    it('shows "Expired" in red when expiry is in the past', () => {
      const pastExpiry = moment().subtract(2, 'days').valueOf();
      render(
        <DashboardWaiversTableRow {...minimalProps} waiver={{ ...minimalProps.waiver, expiryTime: pastExpiry }} />,
        { preloadedState: firewallRouterState }
      );

      const status = screen.getByText('Expired');
      expect(status).toBeVisible();
      expect(status).toHaveClass('iq-waiver-expiry-status--critical');
    });

    it('does not show status when expiryTime is null (never expires)', () => {
      renderComponent({ expiryTime: null });

      expect(screen.queryByText('Expired')).not.toBeInTheDocument();
      expect(screen.queryByText(/Expires in/)).not.toBeInTheDocument();
    });

    it('does not show status for auto waivers', () => {
      const futureExpiry = moment().add(3, 'days').valueOf();
      renderComponent({ expiryTime: futureExpiry, isAutoWaiver: true });

      expect(screen.queryByText(/Expires in/)).not.toBeInTheDocument();
      expect(screen.getByText('Auto')).toBeVisible();
    });

    it('does not show status for expire-when-remediation-available waivers', () => {
      renderComponent({ expiryTime: null, isExpireWhenRemediationAvailable: true });

      expect(screen.queryByText('Expired')).not.toBeInTheDocument();
      expect(screen.getByText('When Remediation Available')).toBeVisible();
    });
  });

  describe('Firewall inline actions', () => {
    const firewallRouterState = {
      router: {
        currentState: { name: 'firewall.dashboard.overview.waivers' },
        currentParams: {},
        prevState: null,
        prevParams: null,
      },
    };

    const firewallWithPermissionState = {
      ...firewallRouterState,
      firewallDashboardWaiver: { hasWaivePermission: true },
    };

    it('shows Renew and Delete buttons in Firewall context with waive permission', () => {
      const { container } = render(<DashboardWaiversTableRow {...minimalProps} isExpiringWaiversEnabled={true} />, {
        preloadedState: firewallWithPermissionState,
      });

      expect(container.querySelector('.iq-waiver-renew-btn')).toBeInTheDocument();
      expect(container.querySelector('.iq-waiver-delete-btn')).toBeInTheDocument();
    });

    it('does not show action buttons in non-Firewall context', () => {
      renderComponent();

      expect(screen.queryByTitle('Renew waiver')).not.toBeInTheDocument();
      expect(screen.queryByTitle('Delete waiver')).not.toBeInTheDocument();
    });

    it('does not show action buttons in Firewall context without waive permission', () => {
      render(<DashboardWaiversTableRow {...minimalProps} />, {
        preloadedState: {
          ...firewallRouterState,
          firewallDashboardWaiver: { hasWaivePermission: false },
        },
      });

      expect(screen.queryByTitle('Renew waiver')).not.toBeInTheDocument();
      expect(screen.queryByTitle('Delete waiver')).not.toBeInTheDocument();
    });

    it('clicking Renew navigates to firewall.renewWaiver state', async () => {
      const user = userEvent.setup();
      const stateGoSpy = jest.spyOn(RouterActions, 'stateGo').mockReturnValue({ type: 'STATE_GO' });

      const { container } = render(<DashboardWaiversTableRow {...minimalProps} isExpiringWaiversEnabled={true} />, {
        preloadedState: firewallWithPermissionState,
      });

      await user.click(container.querySelector('.iq-waiver-renew-btn'));

      expect(stateGoSpy).toHaveBeenCalledWith(
        'firewall.renewWaiver',
        expect.objectContaining({ waiverId: 'waiverId', ownerId: 'ownerId' })
      );
    });
  });
});
