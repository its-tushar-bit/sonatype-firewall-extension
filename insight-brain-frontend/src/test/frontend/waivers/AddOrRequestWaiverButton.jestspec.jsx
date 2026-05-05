/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { fireEvent, render, screen } from 'TestRoot/SpecUtil';

import AddOrRequestWaiverButton from 'MainRoot/waivers/AddOrRequestWaiverButton';

describe('AddOrRequestWaiverButton', () => {
  let renderComponent;
  let addRedirectSpy;
  let requestRedirectSpy;
  let initialProps;

  beforeEach(() => {
    addRedirectSpy = jest.fn();
    requestRedirectSpy = jest.fn();

    initialProps = {
      variant: 'primary',
      hasPermissionForAppWaivers: true,
      onClickAddWaiver: addRedirectSpy,
      onClickRequestWaiver: requestRedirectSpy,
    };

    renderComponent = (props = {}) => render(<AddOrRequestWaiverButton {...initialProps} {...props} />);
  });

  describe('application and organization', () => {
    describe('add waiver segmented button', () => {
      it('renders enabled button primary variant', () => {
        renderComponent();
        const addWaiverButton = screen.getByRole('button', { name: 'Add Waiver' });
        const dropdownButton = screen.getByLabelText('more options');

        expect(addWaiverButton).toBeEnabled();
        expect(dropdownButton).toBeEnabled();
        expect(addWaiverButton).toHaveClass('nx-btn--primary');
      });

      it('renders request waiver button on dropdown click', () => {
        renderComponent();
        let requestWaiverButton = screen.queryByRole('button', { name: 'Request Waiver' });
        const dropdownButton = screen.getByLabelText('more options');
        expect(requestWaiverButton).toBeNull();

        fireEvent.click(dropdownButton);

        requestWaiverButton = screen.getByRole('button', { name: 'Request Waiver' });
        expect(requestWaiverButton).toBeVisible();
      });

      it('call on click action', () => {
        renderComponent();
        const addWaiverButton = screen.getByRole('button', { name: 'Add Waiver' });

        expect(addWaiverButton).toBeEnabled();
        addWaiverButton.click();

        expect(addRedirectSpy).toHaveBeenCalledTimes(1);

        const dropdownButton = screen.getByLabelText('more options');

        fireEvent.click(dropdownButton);

        const requestWaiverButton = screen.getByRole('button', { name: 'Request Waiver' });
        requestWaiverButton.click();
        expect(requestRedirectSpy).toHaveBeenCalledTimes(1);
      });

      it('change button variant to secondary', () => {
        renderComponent({ variant: 'secondary' });

        const addWaiverButton = screen.getByRole('button', { name: 'Add Waiver' });

        expect(addWaiverButton).toBeEnabled();
        expect(addWaiverButton).toHaveClass('nx-btn--secondary');
      });
    });

    describe('request waiver button', () => {
      it('render enabled button primary variant when no waiver edit permission and does not render add waiver button', () => {
        renderComponent({ hasPermissionForAppWaivers: false });
        const requestWaiverButton = screen.getByRole('button', { name: 'Request Waiver' });
        const addWaiverButton = screen.queryByRole('button', { name: 'Add Waiver' });

        expect(requestWaiverButton).toBeEnabled();
        expect(requestWaiverButton).toHaveClass('nx-btn--primary');
        expect(addWaiverButton).toBe(null);
      });

      it('call on click action', () => {
        renderComponent({ hasPermissionForAppWaivers: false });
        const requestWaiverButton = screen.getByRole('button', { name: 'Request Waiver' });

        expect(requestWaiverButton).toBeEnabled();
        requestWaiverButton.click();

        expect(requestRedirectSpy).toHaveBeenCalledTimes(1);
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
    it('shows add waiver button on firewall and no segmented button', () => {
      renderComponent({ isFirewallOrRepository: true });
      const addWaiverButton = screen.getByRole('button', { name: 'Add Waiver' });
      const dropdownButton = screen.queryByLabelText('more options');

      expect(addWaiverButton).toBeVisible();
      expect(dropdownButton).toBe(null);
    });

    it('hide add waiver and request waiver buttons on firewall and no waive permission', () => {
      renderComponent({ isFirewallOrRepository: true, hasPermissionForAppWaivers: false });
      const addWaiverButton = screen.queryByRole('button', { name: 'Add Waiver' });
      const requestWaiverButton = screen.queryByRole('button', { name: 'Request Waiver' });

      expect(addWaiverButton).toBe(null);
      expect(requestWaiverButton).toBe(null);
    });
  });

  describe('Pro Tier Gating', () => {
    it('shows lock icon on Request Waiver button when isRequestWaiverGated is true', () => {
      renderComponent({ hasPermissionForAppWaivers: false, isRequestWaiverGated: true });
      const requestButton = screen.getByRole('button', { name: /Request Waiver/ });
      expect(requestButton).toBeVisible();
    });

    it('shows lock icon on Add Waiver button when isRequestWaiverGated is true', () => {
      renderComponent({ isRequestWaiverGated: true });
      const addButton = screen.getByRole('button', { name: /Add Waiver/ });
      expect(addButton).toBeVisible();
    });
  });
});
