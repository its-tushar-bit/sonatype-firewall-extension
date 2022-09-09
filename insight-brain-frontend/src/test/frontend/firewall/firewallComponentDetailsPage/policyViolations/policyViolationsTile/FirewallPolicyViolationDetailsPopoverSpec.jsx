/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import PolicyViolationDetailsPopover from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationDetailsPopover';

describe('PolicyViolationDetailsPopover', () => {
  let minimalProps, renderComponent, showPopoverSpy;

  beforeEach(() => {
    showPopoverSpy = jasmine.createSpy('showPopover').and.callThrough();

    minimalProps = {
      selectPolicyId: '',
      showViolationsDetailPopover: false,
      onClose: showPopoverSpy,
    };

    renderComponent = (additionalProps = {}) =>
      render(<PolicyViolationDetailsPopover {...minimalProps} {...additionalProps} />);
  });

  it('renders the title in component', () => {
    renderComponent(minimalProps);
    expect(screen.getByText('Violation Details')).toBeVisible();
  });

  it('click close button', () => {
    renderComponent({ showViolationsDetailPopover: true });
    const button = screen.getByRole('button');
    expect(button).toBeVisible();
    fireEvent.click(button);
    expect(showPopoverSpy).toHaveBeenCalled();
  });
});
