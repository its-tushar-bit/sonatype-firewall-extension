/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';

import AddOrRequestWaiverButton from 'MainRoot/waivers/AddOrRequestWaiverButton';

describe('AddOrRequestWaiverButton', () => {
  let renderComponent;
  let redirectSpy;
  let initialProps;

  beforeEach(() => {
    redirectSpy = jest.fn();

    initialProps = {
      variant: 'primary',
      hasPermissionForAppWaivers: true,
      onClick: redirectSpy,
    };

    renderComponent = (props = {}) => render(<AddOrRequestWaiverButton {...initialProps} {...props} />);
  });

  describe('application and organization', () => {
    describe('add waiver button', () => {
      it('render enabled button primary variant', () => {
        renderComponent();
        const addWaiverButton = screen.getByRole('button', { name: 'Add Waiver' });

        expect(addWaiverButton).toBeEnabled();
        expect(addWaiverButton).toHaveClass('nx-btn--primary');
      });

      it('call on click action', () => {
        renderComponent();
        const addWaiverButton = screen.getByRole('button', { name: 'Add Waiver' });

        expect(addWaiverButton).toBeEnabled();
        addWaiverButton.click();

        expect(redirectSpy).toHaveBeenCalledTimes(1);
      });

      it('change button variant to secondary', () => {
        renderComponent({ variant: 'secondary' });

        const addWaiverButton = screen.getByRole('button', { name: 'Add Waiver' });

        expect(addWaiverButton).toBeEnabled();
        expect(addWaiverButton).toHaveClass('nx-btn--secondary');
      });
    });

    describe('request waiver button', () => {
      it('render enabled button primary variant when no waiver edit permission', () => {
        renderComponent({ hasPermissionForAppWaivers: false });
        const requestWaiverButton = screen.getByRole('button', { name: 'Request Waiver' });

        expect(requestWaiverButton).toBeEnabled();
        expect(requestWaiverButton).toHaveClass('nx-btn--primary');
      });

      it('call on click action', () => {
        renderComponent({ hasPermissionForAppWaivers: false });
        const requestWaiverButton = screen.getByRole('button', { name: 'Request Waiver' });

        expect(requestWaiverButton).toBeEnabled();
        requestWaiverButton.click();

        expect(redirectSpy).toHaveBeenCalledTimes(1);
      });

      it('change button variant to secondary', () => {
        renderComponent({ variant: 'secondary', hasPermissionForAppWaivers: false });

        const requestWaiverButton = screen.getByRole('button', { name: 'Request Waiver' });

        expect(requestWaiverButton).toBeEnabled();
        expect(requestWaiverButton).toHaveClass('nx-btn--secondary');
      });
    });
  });

  describe('firewall and repository', () => {
    it('hide add waiver button on firewall and no waive permission', () => {
      renderComponent({ isFirewallOrRepository: true, hasPermissionForAppWaivers: false });
      const addWaiverButton = screen.queryByRole('button', { name: 'Add Waiver' });

      expect(addWaiverButton).toBe(null);
    });
  });
});
