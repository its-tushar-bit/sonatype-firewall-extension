/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import WaivedComponentUpgrades from 'MainRoot/OrgsAndPolicies/waivedComponentUpgrades/WaivedComponentUpgrades';
import ownerConstant from 'MainRoot/utility/services/owner.constant';
import { getWaivedComponentUpgradeConfigUrl } from 'MainRoot/util/CLMLocation';
import { render, screen, fireEvent, axiosMockAdapter } from 'TestRoot/SpecUtil';
import { actions as upgradeActions } from 'MainRoot/OrgsAndPolicies/waivedComponentUpgradesSlice';

describe('Waived Component Upgrades Component', () => {
  let axiosMock,
    renderComponent,
    preloadedState,
    loadUpgradeStageSpy,
    configuredStage,
    stageTypes,
    saveUpgradeStageSpy,
    setConfiguredStageSpy;

  beforeAll(function () {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    stageTypes = [
      {
        stageTypeId: 'develop',
        stageName: 'Develop',
      },
      {
        stageTypeId: 'source',
        stageName: 'Source',
      },
      {
        stageTypeId: 'build',
        stageName: 'Build',
      },
      {
        stageTypeId: 'stage-release',
        stageName: 'Stage Release',
      },
      {
        stageTypeId: 'release',
        stageName: 'Release',
      },
      {
        stageTypeId: 'operate',
        stageName: 'Operate',
      },
    ];
    configuredStage = { stage: null };
    preloadedState = {
      router: {
        currentParams: {
          organizationId: ownerConstant.ROOT_ORGANIZATION_ID,
        },
      },
      waivedComponentUpgrades: {
        loading: false,
        loadError: null,
        isDirty: false,
        submitMaskState: null,
        submitError: null,
        configuredStage: null,
      },
      stages: {
        cli: { stageTypes },
      },
    };

    loadUpgradeStageSpy = spyOn(upgradeActions, 'loadUpgradeStage').and.callThrough();
    saveUpgradeStageSpy = spyOn(upgradeActions, 'saveUpgradeStage').and.callThrough();
    setConfiguredStageSpy = spyOn(upgradeActions, 'setConfiguredStage').and.callThrough();

    axiosMock.onGet(getWaivedComponentUpgradeConfigUrl()).reply(200, configuredStage);

    renderComponent = (preloadedState) => render(<WaivedComponentUpgrades />, { preloadedState });
  });

  it('calls loadUpgradeStage once', () => {
    renderComponent(preloadedState);

    expect(loadUpgradeStageSpy).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders a form with radio buttons', async () => {
    renderComponent(preloadedState);

    expect(await screen.findByRole('heading', { name: 'Waived Component Upgrades' })).toBeVisible();

    expect(await screen.findByRole('radio', { name: 'None' })).toBeVisible();
    expect(await screen.findByRole('radio', { name: 'Develop' })).toBeVisible();
    expect(await screen.findByRole('radio', { name: 'Source' })).toBeVisible();
    expect(await screen.findByRole('radio', { name: 'Build' })).toBeVisible();
    expect(await screen.findByRole('radio', { name: 'Stage Release' })).toBeVisible();
    expect(await screen.findByRole('radio', { name: 'Release' })).toBeVisible();
    expect(await screen.findByRole('radio', { name: 'Operate' })).toBeVisible();
  });

  it('renders disabled radio buttons if not at root org level', async () => {
    preloadedState.router.currentParams.organizationId = '13245';
    renderComponent(preloadedState);

    expect(await screen.findByRole('radio', { name: 'None' })).toBeDisabled();
    expect(await screen.findByRole('radio', { name: 'Develop' })).toBeDisabled();
    expect(await screen.findByRole('radio', { name: 'Source' })).toBeDisabled();
    expect(await screen.findByRole('radio', { name: 'Build' })).toBeDisabled();
    expect(await screen.findByRole('radio', { name: 'Stage Release' })).toBeDisabled();
    expect(await screen.findByRole('radio', { name: 'Release' })).toBeDisabled();
    expect(await screen.findByRole('radio', { name: 'Operate' })).toBeDisabled();
  });

  it('handles stage option selection and updates state', async () => {
    renderComponent(preloadedState);
    expect(await screen.findByRole('radio', { name: 'None' })).toBeChecked();

    const developStageRadio = screen.getByRole('radio', { name: 'Develop' });
    fireEvent.click(developStageRadio);

    expect(setConfiguredStageSpy).toHaveBeenCalledTimes(1);
    expect(developStageRadio.parentElement).toHaveClass('tm-checked');
  });

  it('calls the submit action', async () => {
    axiosMock.onPut(getWaivedComponentUpgradeConfigUrl(), { stage: 'develop' }).reply(200, {});
    renderComponent(preloadedState);

    const developStageRadio = await screen.findByRole('radio', { name: 'Develop' });
    fireEvent.click(developStageRadio);

    const submitButton = screen.getByRole('button', { name: 'Update' });
    expect(submitButton).toBeVisible();
    fireEvent.click(submitButton);

    expect(saveUpgradeStageSpy).toHaveBeenCalledTimes(1);
  });

  it('shows error when retrieving upgrade config', async () => {
    axiosMock.onGet(getWaivedComponentUpgradeConfigUrl()).reply(500, 'Error');
    renderComponent(preloadedState);

    expect(screen.getByText('Loading…')).toBeVisible();

    const error = await screen.findByRole('alert');
    expect(error).toBeVisible();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
  });
});
