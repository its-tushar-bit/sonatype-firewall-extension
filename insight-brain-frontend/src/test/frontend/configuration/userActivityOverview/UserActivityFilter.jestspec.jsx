/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, setupPortalContainer } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import UserActivityFilter from 'MainRoot/configuration/userActivityOverview/UserActivityFilter';

describe('UserActivityFilter', () => {
  let defaultProps;

  beforeEach(() => {
    setupPortalContainer(); // Required for PortalDrawer

    defaultProps = {
      isOpen: true,
      onClose: jest.fn(),
      selectedAge: 30,
      onAgeChange: jest.fn(),
      onApply: jest.fn(),
      onReset: jest.fn(),
      filtersAreDirty: false,
    };
  });

  it('should not render when isOpen is false', () => {
    render(<UserActivityFilter {...defaultProps} isOpen={false} />);

    expect(document.querySelector('.nx-drawer')).not.toBeInTheDocument();
  });

  it('should render drawer with correct title when open', () => {
    render(<UserActivityFilter {...defaultProps} />);

    expect(document.querySelector('.nx-drawer')).toBeInTheDocument();
    expect(screen.getByText('Filters', { hidden: true })).toBeInTheDocument();
  });

  it('should render Time Frame filter options', async () => {
    const user = userEvent.setup();
    render(<UserActivityFilter {...defaultProps} />);

    expect(screen.getByText('Time Frame', { hidden: true })).toBeInTheDocument();

    // Expand the collapsible section first
    const expandButton = screen.getByRole('button', { name: /Time Frame/, hidden: true });
    await user.click(expandButton);

    // Check all age options are present after expansion
    expect(screen.getByRole('menuitemradio', { name: 'past 24 hours', hidden: true })).toBeInTheDocument();
    expect(screen.getByRole('menuitemradio', { name: 'past 7 days', hidden: true })).toBeInTheDocument();
    expect(screen.getByRole('menuitemradio', { name: 'past 30 days', hidden: true })).toBeInTheDocument();
  });

  it('should have correct age option selected', async () => {
    const user = userEvent.setup();
    render(<UserActivityFilter {...defaultProps} selectedAge={7} />);

    // Expand the collapsible section first
    const expandButton = screen.getByRole('button', { name: /Time Frame/, hidden: true });
    await user.click(expandButton);

    expect(screen.getByRole('menuitemradio', { name: 'past 7 days', hidden: true })).toBeChecked();
    expect(screen.getByRole('menuitemradio', { name: 'past 30 days', hidden: true })).not.toBeChecked();
  });

  it('should call onAgeChange when age option is selected', async () => {
    const user = userEvent.setup();
    render(<UserActivityFilter {...defaultProps} />);

    // Expand the collapsible section first
    const expandButton = screen.getByRole('button', { name: /Time Frame/, hidden: true });
    await user.click(expandButton);

    await user.click(screen.getByRole('menuitemradio', { name: 'past 7 days', hidden: true }));

    expect(defaultProps.onAgeChange).toHaveBeenCalledWith(7);
  });

  it('should disable Apply and Reset buttons when filters are not dirty', () => {
    render(<UserActivityFilter {...defaultProps} filtersAreDirty={false} />);

    expect(screen.getByRole('button', { name: 'Apply', hidden: true })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Reset', hidden: true })).toBeDisabled();
  });

  it('should enable Apply and Reset buttons when filters are dirty', () => {
    render(<UserActivityFilter {...defaultProps} filtersAreDirty={true} />);

    expect(screen.getByRole('button', { name: 'Apply', hidden: true })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Reset', hidden: true })).toBeEnabled();
  });

  it('should call onApply and onClose when Apply button is clicked', async () => {
    const user = userEvent.setup();
    render(<UserActivityFilter {...defaultProps} filtersAreDirty={true} />);

    await user.click(screen.getByRole('button', { name: 'Apply', hidden: true }));

    expect(defaultProps.onApply).toHaveBeenCalledTimes(1);
    expect(defaultProps.onClose).toHaveBeenCalledTimes(1);
  });

  it('should call onReset and onClose when Reset button is clicked', async () => {
    const user = userEvent.setup();
    render(<UserActivityFilter {...defaultProps} filtersAreDirty={true} />);

    await user.click(screen.getByRole('button', { name: 'Reset', hidden: true }));

    expect(defaultProps.onReset).toHaveBeenCalledTimes(1);
    expect(defaultProps.onClose).toHaveBeenCalledTimes(1);
  });

  it('should have correct button variants', () => {
    render(<UserActivityFilter {...defaultProps} filtersAreDirty={true} />);

    const resetButton = screen.getByRole('button', { name: 'Reset', hidden: true });
    const applyButton = screen.getByRole('button', { name: 'Apply', hidden: true });

    expect(resetButton).toHaveClass('nx-btn--tertiary');
    expect(applyButton).toHaveClass('nx-btn--primary');
  });

  it('should render with narrow drawer variant', () => {
    render(<UserActivityFilter {...defaultProps} />);

    const drawer = document.querySelector('.nx-drawer');
    expect(drawer).toHaveClass('nx-drawer--narrow');
  });

  it('should use proper ARIA labeling', () => {
    render(<UserActivityFilter {...defaultProps} />);

    const timeFrameGroup = document.querySelector('#user-activity-age-filter');
    expect(timeFrameGroup).toHaveAttribute('id', 'user-activity-age-filter');

    const drawer = document.querySelector('.nx-drawer');
    expect(drawer).toBeInTheDocument();
  });
});
