/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import ActionsFooter from 'MainRoot/firewallOnboarding/ActionsFooter';
import { steps } from '../../../main/frontend/firewallOnboarding/firewallOnboardingUtils';

describe('ActionsFooter', function () {
  let renderComponent;
  let onNextSpy;
  let onPreviousSpy;
  let onLaunchSpy;

  beforeEach(() => {
    onNextSpy = jasmine.createSpy('onNext');
    onPreviousSpy = jasmine.createSpy('onPrevious');
    onLaunchSpy = jasmine.createSpy('onLaunch');
    renderComponent = (currentStep) =>
      render(
        <ActionsFooter currentStep={currentStep} onPrevious={onPreviousSpy} onNext={onNextSpy} onLaunch={onLaunchSpy} />
      );
  });

  describe('when the current step is the first step', () => {
    it('renders continue button', () => {
      renderComponent(steps[0]);
      const continueButton = screen.getByRole('button', { name: /continue/i });

      expect(continueButton).toBeVisible();
      fireEvent.click(continueButton);
      expect(onNextSpy).toHaveBeenCalled();
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
      expect(onPreviousSpy).toHaveBeenCalled();
    });

    it('renders "launch firewall" button', () => {
      renderComponent(steps[1]);
      const launchButton = screen.getByRole('button', { name: /launch firewall/i });

      expect(launchButton).toBeVisible();
      fireEvent.click(launchButton);
      expect(onLaunchSpy).toHaveBeenCalled();
    });
  });
});
