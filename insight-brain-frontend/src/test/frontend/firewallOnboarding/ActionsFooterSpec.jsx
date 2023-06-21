/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import ActionsFooter from 'MainRoot/firewallOnboarding/ActionsFooter';
import { actions } from 'MainRoot/firewallOnboarding/firewallOnboardingSlice';
import { steps } from 'MainRoot/firewallOnboarding/firewallOnboardingUtils';

describe('ActionsFooter', function () {
  const renderComponent = (currentStep) => render(<ActionsFooter currentStep={currentStep} />);
  const HELP_URL = 'http://links.sonatype.com/products/nxiq/doc/firewall-onboarding';

  beforeAll(() => {
    spyOn(actions, 'saveRepositories').and.callThrough();
    spyOn(actions, 'continueToNextStep').and.callThrough();
    spyOn(actions, 'goBackToPreviousStep').and.callThrough();
    spyOn(actions, 'openIncompleteConfigurationModal').and.callThrough();
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
      expect(actions.openIncompleteConfigurationModal).toHaveBeenCalled();
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

  describe('when the current step is the last step', () => {
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
      expect(actions.openIncompleteConfigurationModal).toHaveBeenCalled();
    });

    it('does not render continue button', () => {
      renderComponent(steps[1]);
      const continueButton = screen.queryByRole('button', { name: /continue/i });

      expect(continueButton).toBeNull();
    });

    it('renders previous button', () => {
      renderComponent(steps[1]);
      const previousButton = screen.queryByRole('button', { name: /previous/i });

      expect(previousButton).toBeVisible();
      fireEvent.click(previousButton);
      expect(actions.goBackToPreviousStep).toHaveBeenCalled();
    });

    it('renders "launch firewall" button', () => {
      renderComponent(steps[1]);
      const launchButton = screen.getByRole('button', { name: /launch firewall/i });

      expect(launchButton).toBeVisible();
      fireEvent.click(launchButton);
      expect(actions.saveRepositories).toHaveBeenCalled();
    });
  });
});
