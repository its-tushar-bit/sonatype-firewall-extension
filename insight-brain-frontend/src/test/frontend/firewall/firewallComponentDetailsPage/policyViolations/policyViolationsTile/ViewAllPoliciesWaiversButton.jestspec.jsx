/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import ViewAllPoliciesWaiversButton from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/policyViolationsTile/ViewAllPoliciesWaiversButton';

import 'TestRoot/SpecUtil';

describe('ViewAllPoliciesWaiversButton', () => {
  let minimalProps, renderComponent, setShowComponentWaiversPopoverSpy;

  beforeEach(() => {
    setShowComponentWaiversPopoverSpy = jest.fn().mockName('setShowComponentWaiversPopover');

    minimalProps = {
      showViolationsDetailPopover: false,
      setShowComponentWaiversPopover: setShowComponentWaiversPopoverSpy,
    };

    renderComponent = (additionalProps = {}) =>
      render(<ViewAllPoliciesWaiversButton {...minimalProps} {...additionalProps} />);
  });

  it('renders a ViewAllPoliciesWaiversButton component', () => {
    renderComponent(minimalProps);

    const button = screen.getByRole('button', { name: 'View Existing Waivers' });
    expect(button).toBeVisible();
    fireEvent.click(button);
    expect(setShowComponentWaiversPopoverSpy).toHaveBeenCalledWith(true);
  });
});
