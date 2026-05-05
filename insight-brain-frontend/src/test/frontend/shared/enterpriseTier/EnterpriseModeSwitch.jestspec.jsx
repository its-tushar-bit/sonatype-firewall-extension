/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EnterpriseModeSwitch from 'MainRoot/shared/enterpriseTier/EnterpriseModeSwitch';

describe('EnterpriseModeSwitch', () => {
  const mockToggle = jest.fn();

  beforeEach(() => {
    mockToggle.mockClear();
  });

  it('renders Default and Custom buttons', () => {
    render(<EnterpriseModeSwitch isCustomMode={false} onToggleMode={mockToggle} />);
    expect(screen.getByText('Default')).toBeInTheDocument();
    expect(screen.getByText(/Custom/)).toBeInTheDocument();
  });

  it('calls onToggleMode when Custom clicked in default mode', async () => {
    render(<EnterpriseModeSwitch isCustomMode={false} onToggleMode={mockToggle} />);
    await userEvent.click(screen.getByText(/Custom/));
    expect(mockToggle).toHaveBeenCalledTimes(1);
  });

  it('calls onToggleMode when Default clicked in custom mode', async () => {
    render(<EnterpriseModeSwitch isCustomMode={true} onToggleMode={mockToggle} />);
    await userEvent.click(screen.getByText('Default'));
    expect(mockToggle).toHaveBeenCalledTimes(1);
  });

  it('does not call onToggleMode when already-active button clicked', async () => {
    render(<EnterpriseModeSwitch isCustomMode={false} onToggleMode={mockToggle} />);
    await userEvent.click(screen.getByText('Default'));
    expect(mockToggle).not.toHaveBeenCalled();
  });
});
