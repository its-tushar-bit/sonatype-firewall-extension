/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import AutoWaiverScopeDropdownSelector from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiverScopeDropdownSelector';
import userEvent from '@testing-library/user-event';

describe('AutoWaiverScopeDropdownSelector', () => {
  const mockOnSelectScope = jest.fn();

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('renders the dropdown with the correct default scope', () => {
    render(<AutoWaiverScopeDropdownSelector scope="any" onSelectScope={mockOnSelectScope} />);

    const dropdownButton = screen.getByRole('button', { name: 'any' });
    expect(dropdownButton).toBeInTheDocument();
  });

  it('disables the dropdown when the disabled prop is true', () => {
    render(<AutoWaiverScopeDropdownSelector scope="any" disabled={true} />);

    const dropdownButton = screen.getByRole('button', { name: 'any/all' });
    expect(dropdownButton).toBeInTheDocument();
    expect(dropdownButton).toHaveClass('disabled');
  });

  it('shows tooltip when disabled', async () => {
    const user = userEvent.setup();

    render(<AutoWaiverScopeDropdownSelector scope="any" disabled={true} />);

    const dropdownButton = screen.getByRole('button', { name: 'any/all' });
    expect(dropdownButton).toBeInTheDocument();
    expect(dropdownButton).toHaveClass('disabled');

    await user.hover(dropdownButton);

    const tooltip = await screen.findByRole('tooltip');
    expect(tooltip).toBeInTheDocument();
    expect(tooltip).toHaveTextContent('Select both conditions below to enable this option');
  });

  it('calls onSelectScope with the correct scope when an option is clicked', async () => {
    const user = userEvent.setup();

    render(<AutoWaiverScopeDropdownSelector scope="any" onSelectScope={mockOnSelectScope} />);

    await user.click(screen.getByRole('button', { name: 'any' }));
    await user.click(screen.getByRole('button', { name: 'all' }));
    expect(mockOnSelectScope).toHaveBeenCalledWith('all');
  });

  it('toggles the dropdown open and close state', async () => {
    const user = userEvent.setup();

    render(<AutoWaiverScopeDropdownSelector scope="any" onSelectScope={mockOnSelectScope} />);

    const dropdownButton = screen.getByRole('button', { name: 'any' });

    await user.click(dropdownButton);
    expect(screen.getByText('all')).toBeInTheDocument();

    await user.click(dropdownButton);
    expect(screen.queryByText('all')).not.toBeInTheDocument();
  });

  it('applies custom className when provided', () => {
    const customClass = 'custom-class';
    render(<AutoWaiverScopeDropdownSelector scope="any" className={customClass} />);

    const dropdownButton = screen.getByRole('button', { name: 'any' });
    expect(dropdownButton).toBeInTheDocument();
    expect(dropdownButton.parentElement).toHaveClass('custom-class');
  });
});
