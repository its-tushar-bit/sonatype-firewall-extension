/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { axiosMockAdapter, fireEvent, render, screen } from 'TestRoot/SpecUtil';
import WaivedComponentUpgradesConfiguration from 'MainRoot/configuration/waivedComponentUpgradesConfiguration/WaivedComponentUpgradesConfiguration';
import { getConfigurationUrl } from 'MainRoot/util/CLMLocation';
import { CONFIG_PROPERTIES_PARAMS } from 'MainRoot/configuration/waivedComponentUpgradesConfiguration/waivedComponentUpgradesConfigurationSlice';

describe('WaivedComponentUpgradesConfiguration', () => {
  let renderComponent;
  let state;
  let mock;

  beforeAll(() => {
    mock = axiosMockAdapter();
  });

  beforeEach(() => {
    renderComponent = (preloadedState = state) => render(<WaivedComponentUpgradesConfiguration />, { preloadedState });
  });
  describe('when loading is successful', () => {
    beforeEach(() => {
      mock
        .onGet(getConfigurationUrl().concat(CONFIG_PROPERTIES_PARAMS))
        .reply(200, { waivedComponentUpgradeMonitoringEnabled: true });
    });

    it('should render WaivedComponentUpgradesConfiguration with loading indicator', async () => {
      renderComponent();
      expect(screen.getByText('Loading…')).toBeVisible();
      expect(screen.queryByRole('switch')).toBeNull();
      expect(await screen.findByText('Component Upgrade Availability')).toBeVisible();
      expect(screen.queryByText('Loading…')).toBeNull();
      expect(screen.getByRole('switch')).toBeChecked();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
      expect(screen.getByRole('button', { name: 'Update' })).not.toBeDisabled();
    });

    it('should render no changes error', async () => {
      renderComponent();
      expect(await screen.findByText('Component Upgrade Availability')).toBeVisible();
      const updateBtn = screen.getByRole('button', { name: 'Update' });
      expect(screen.getByRole('switch')).toBeChecked();
      fireEvent.click(updateBtn);
      expect(screen.getByText('There were validation errors. There are no changes to update.')).toBeVisible();
    });

    describe('when updating is successful', () => {
      beforeEach(() => {
        mock.onPut(getConfigurationUrl()).reply(204);
      });

      it('should update WaivedComponentUpgradesConfiguration successfully', async () => {
        renderComponent();
        expect(await screen.findByText('Component Upgrade Availability')).toBeVisible();
        let updateBtn = screen.getByRole('button', { name: 'Update' });
        const switchInput = screen.getByRole('switch');

        expect(switchInput).toBeChecked();
        fireEvent.click(switchInput);
        expect(switchInput).not.toBeChecked();
        fireEvent.click(updateBtn);

        expect(screen.getByText('Saving…')).toBeVisible();
        expect(await screen.findByText('Success!')).toBeVisible();

        updateBtn = screen.getByRole('button', { name: 'Update' });

        expect(screen.getByRole('switch')).not.toBeChecked();
        expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
        expect(updateBtn).not.toBeDisabled();

        fireEvent.click(updateBtn);
        expect(screen.getByText('There were validation errors. There are no changes to update.')).toBeVisible();
      });
    });

    describe('when updating is not successful', () => {
      beforeEach(() => {
        mock.onPut(getConfigurationUrl()).reply(400, 'some saving error');
      });

      it('should update WaivedComponentUpgradesConfiguration successfully', async () => {
        renderComponent();
        expect(await screen.findByText('Component Upgrade Availability')).toBeVisible();
        const updateBtn = screen.getByRole('button', { name: 'Update' });
        const switchInput = screen.getByRole('switch');

        expect(switchInput).toBeChecked();
        fireEvent.click(switchInput);
        expect(switchInput).not.toBeChecked();
        fireEvent.click(updateBtn);

        expect(await screen.findByText('An error occurred saving data. some saving error')).toBeVisible();
      });
    });
  });

  describe('when loading is not successful', () => {
    beforeEach(() => {
      mock.onGet(getConfigurationUrl().concat(CONFIG_PROPERTIES_PARAMS)).reply(500, 'some loading error');
    });

    it('should render WaivedComponentUpgradesConfiguration with loading indicator', async () => {
      renderComponent();
      expect(screen.getByText('Loading…')).toBeVisible();
      expect(await screen.findByText('An error occurred loading data. some loading error')).toBeVisible();
      expect(screen.queryByText('Loading…')).toBeNull();
      expect(screen.queryByRole('switch')).toBeNull();
      expect(screen.queryByRole('button', { name: 'Cancel' })).toBeNull();
      expect(screen.queryByRole('button', { name: 'Update' })).toBeNull();
    });
  });
});
