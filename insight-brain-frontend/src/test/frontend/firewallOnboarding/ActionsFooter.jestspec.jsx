/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { last } from 'ramda';

import { render, screen, fireEvent, axiosMockAdapter } from 'TestRoot/SpecUtil';
import ActionsFooter from 'MainRoot/firewallOnboarding/ActionsFooter';
import { actions, initialState, REDUCER_NAME } from 'MainRoot/firewallOnboarding/firewallOnboardingSlice';
import { steps } from 'MainRoot/firewallOnboarding/firewallOnboardingUtils';
import * as RouterActions from '../../../main/frontend/reduxUiRouter/routerActions';
import userEvent from '@testing-library/user-event';
import { getConfigureFirewallOnboardingUrl, getConfigureRepositoriesUrl } from 'MainRoot/util/CLMLocation';

describe('ActionsFooter', function () {
  const renderComponent = (currentStep, preloadedState) =>
    render(<ActionsFooter currentStep={currentStep} />, { preloadedState });
  const HELP_URL = 'http://links.sonatype.com/products/nxiq/doc/firewall-onboarding';

  let axiosMock;

  beforeEach(() => {
    jest.spyOn(actions, 'continueToNextStep');
    jest.spyOn(actions, 'goBackToPreviousStep');
    jest.spyOn(actions, 'launchFirewall');
    jest.spyOn(RouterActions, 'stateGo');

    axiosMock = axiosMockAdapter();
  });

  describe('when the current step is the first step', () => {
    it('renders a help button', () => {
      renderComponent(steps[0]);
      const helpButton = screen.getByRole('link', { name: /help/i });

      expect(helpButton).toBeVisible();
      expect(helpButton).toHaveAttribute('href', HELP_URL);
    });

    it('renders cancel button', () => {
      renderComponent(steps[0]);
      const cancelButton = screen.getByRole('button', { name: /cancel/i });

      expect(cancelButton).toBeVisible();
      fireEvent.click(cancelButton);
      expect(RouterActions.stateGo).toHaveBeenCalled();
    });

    it('renders continue button', () => {
      renderComponent(steps[0]);
      const continueButton = screen.getByRole('button', { name: /continue/i });

      expect(continueButton).toBeVisible();
      fireEvent.click(continueButton);
      expect(actions.continueToNextStep).toHaveBeenCalled();
    });

    it('does not render previous button', () => {
      renderComponent(steps[0]);
      const previousButton = screen.queryByRole('button', { name: /previous/i });

      expect(previousButton).toBeNull();
    });

    it('does not render "launch firewall" button', () => {
      renderComponent(steps[0]);
      const launchButton = screen.queryByRole('button', { name: /launch firewall/i });

      expect(launchButton).toBeNull();
    });
  });

  describe('when the current step is the second step', () => {
    it('renders a help button', () => {
      renderComponent(steps[1]);
      const helpButton = screen.getByRole('link', { name: /help/i });

      expect(helpButton).toBeVisible();
      expect(helpButton).toHaveAttribute('href', HELP_URL);
    });

    it('renders cancel button', () => {
      renderComponent(steps[1]);
      const cancelButton = screen.getByRole('button', { name: /cancel/i });

      expect(cancelButton).toBeVisible();
      fireEvent.click(cancelButton);
      expect(RouterActions.stateGo).toHaveBeenCalled();
    });

    it('renders continue button', () => {
      renderComponent(steps[1]);
      const continueButton = screen.getByRole('button', { name: /continue/i });

      expect(continueButton).toBeVisible();
      fireEvent.click(continueButton);
      expect(actions.continueToNextStep).toHaveBeenCalled();
    });

    it('renders previous button', () => {
      renderComponent(steps[1]);
      const previousButton = screen.queryByRole('button', { name: /previous/i });

      expect(previousButton).toBeVisible();
      fireEvent.click(previousButton);
      expect(actions.goBackToPreviousStep).toHaveBeenCalled();
    });

    it('does not render "launch firewall" button', () => {
      renderComponent(steps[1]);
      const launchButton = screen.queryByRole('button', { name: /launch firewall/i });

      expect(launchButton).toBeNull();
    });
  });

  describe('when the current step is the last step', () => {
    it('renders a help button', () => {
      renderComponent(steps[0]);
      const helpButton = screen.getByRole('link', { name: /help/i });

      expect(helpButton).toBeVisible();
      expect(helpButton).toHaveAttribute('href', HELP_URL);
    });

    it('renders cancel button', () => {
      renderComponent(last(steps));
      const cancelButton = screen.getByRole('button', { name: /cancel/i });

      expect(cancelButton).toBeVisible();
      fireEvent.click(cancelButton);
      expect(RouterActions.stateGo).toHaveBeenCalled();
    });

    it('does not render continue button', () => {
      renderComponent(last(steps));
      const continueButton = screen.queryByRole('button', { name: /continue/i });

      expect(continueButton).toBeNull();
    });

    it('renders previous button', () => {
      renderComponent(last(steps));
      const previousButton = screen.queryByRole('button', { name: /previous/i });

      expect(previousButton).toBeVisible();
      fireEvent.click(previousButton);
      expect(actions.goBackToPreviousStep).toHaveBeenCalled();
    });

    it('renders "launch firewall" button', async () => {
      const unconfiguredRepoId = '915a860b-ac30-49a9-be40-34755107dac0';
      const state = {
        [REDUCER_NAME]: {
          ...initialState,
          unconfiguredRepoManagers: {
            repoManagers: [getRepository(unconfiguredRepoId)],
            loading: false,
            loadError: null,
          },
        },
      };

      mockAxiosCallsLaunchFirewallAction(unconfiguredRepoId);

      renderComponent(last(steps), state);
      const launchButton = screen.getByRole('button', { name: /launch firewall/i });

      expect(launchButton).toBeVisible();

      await userEvent.click(launchButton);

      expect(actions.launchFirewall).toHaveBeenCalled();
      expect(RouterActions.stateGo).toHaveBeenCalled();
    });
  });

  function getRepository(id, configured = false) {
    return {
      id,
      instanceId: `some-instance-id-${id}`,
      productName: `some-product-name-${id}`,
      productVersion: `some-product-version-${id}`,
      userAgent: `some-user-agent-${id}`,
      configured,
      configureTime: new Date(),
    };
  }

  function mockAxiosCallsLaunchFirewallAction(unconfiguredRepoId) {
    axiosMock.onPut(getConfigureFirewallOnboardingUrl()).reply(200, { protectionRules: {} });
    axiosMock.onPut(getConfigureRepositoriesUrl(unconfiguredRepoId)).reply(200);
  }
});
