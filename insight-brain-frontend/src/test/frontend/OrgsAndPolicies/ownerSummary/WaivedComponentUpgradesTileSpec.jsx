/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, axiosMockAdapter } from 'TestRoot/SpecUtil';
import { actions as upgradeActions } from 'MainRoot/OrgsAndPolicies/waivedComponentUpgradesSlice';
import { getWaivedComponentUpgradeConfigUrl } from 'MainRoot/util/CLMLocation';
import WaivedComponentUpgradesTile from 'MainRoot/OrgsAndPolicies/ownerSummary/WaivedComponentUpgradesTile';

import ownerConstant from 'MainRoot/utility/services/owner.constant';
import * as waivedComponentUpgradesSelectors from 'MainRoot/OrgsAndPolicies/waivedComponentUpgradesSelectors';
import * as ownerSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

describe('Waived Component Upgrades Tile', () => {
  let axiosMock, renderComponent, loadUpgradeStageSpy, selectSelectedOwnerSpy;

  beforeAll(function () {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    loadUpgradeStageSpy = spyOn(upgradeActions, 'loadUpgradeStage').and.callThrough();
    selectSelectedOwnerSpy = spyOn(ownerSelectors, 'selectSelectedOwner').and.returnValue({
      id: ownerConstant.ROOT_ORGANIZATION_ID,
    });
    spyOn(waivedComponentUpgradesSelectors, 'selectUpgradeMonitoringLinkParams').and.returnValue({
      to: 'management.edit.organization.monitor-component-upgrades',
      params: {
        organizationId: ownerConstant.ROOT_ORGANIZATION_ID,
      },
    });

    axiosMock.onGet(getWaivedComponentUpgradeConfigUrl()).reply(200, { stage: null });

    renderComponent = () => render(<WaivedComponentUpgradesTile />);
  });

  it('calls loadUpgradeStage once', () => {
    axiosMock.onGet(getWaivedComponentUpgradeConfigUrl()).reply(200, { loading: true });
    renderComponent();

    expect(loadUpgradeStageSpy).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('shows error when it fails to retrieve upgrade config', async () => {
    axiosMock.onGet(getWaivedComponentUpgradeConfigUrl()).reply(500, 'Error');
    renderComponent();

    const error = await screen.findByRole('alert');
    expect(error).toBeVisible();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
  });

  describe('Owner is root organization', () => {
    it('renders expected text when monitoring is NOT configured', async () => {
      renderComponent();

      expect(await screen.findByText(/upgrade monitoring is not configured/i)).toBeVisible();
      expect(await screen.queryByText(/inherited by all organizations and applications/i)).not.toBeInTheDocument();
    });
    it('renders expected text when monitoring is configured', async () => {
      axiosMock
        .onGet(getWaivedComponentUpgradeConfigUrl())
        .reply(200, { loading: false, loadError: null, stage: 'develop' });
      renderComponent();

      expect(await screen.findByText(/upgrade monitoring is configured/i)).toBeVisible();
      expect(await screen.findByText(/inherited by all organizations and applications/i)).toBeVisible();
      expect(await screen.findByText(/(develop)/i)).toBeVisible();
    });
  });
  describe('Owner is organization', () => {
    beforeEach(() => {
      selectSelectedOwnerSpy.and.returnValue({
        id: ownerConstant.ORGANIZATION_TYPE,
      });
    });
    it('renders expected text when monitoring is NOT configured', async () => {
      renderComponent();

      expect(await screen.findByText(/upgrade monitoring can only be configured at the root org level/i)).toBeVisible();
      expect(await screen.findByText(/inheriting from root organization/i)).toBeVisible();
      expect(await screen.findByText(/(not configured)/i)).toBeVisible();
    });
    it('renders expected text when monitoring is configured', async () => {
      axiosMock.onGet(getWaivedComponentUpgradeConfigUrl()).reply(200, { stage: 'develop' });
      renderComponent();

      expect(await screen.findByText(/upgrade monitoring can only be configured at the root org level/i)).toBeVisible();
      expect(await screen.findByText(/inheriting from root organization/i)).toBeVisible();
      expect(await screen.findByText(/(develop)/i)).toBeVisible();
    });
  });
  describe('Owner is application', () => {
    beforeEach(() => {
      selectSelectedOwnerSpy.and.returnValue({
        id: ownerConstant.APPLICATION_TYPE,
      });
    });
    it('renders expected text when monitoring is NOT configured', async () => {
      renderComponent();

      expect(await screen.findByText(/upgrade monitoring can only be configured at the root org level/i)).toBeVisible();
      expect(await screen.findByText(/inheriting from root organization/i)).toBeVisible();
      expect(await screen.findByText(/(not configured)/i)).toBeVisible();
    });
    it('renders expected text when monitoring is configured', async () => {
      axiosMock.onGet(getWaivedComponentUpgradeConfigUrl()).reply(200, { stage: 'develop' });
      renderComponent();

      expect(await screen.findByText(/upgrade monitoring can only be configured at the root org level/i)).toBeVisible();
      expect(await screen.findByText(/inheriting from root organization/i)).toBeVisible();
      expect(await screen.findByText(/(develop)/i)).toBeVisible();
    });
  });
});
