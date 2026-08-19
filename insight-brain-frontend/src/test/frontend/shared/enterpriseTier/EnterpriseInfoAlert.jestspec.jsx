/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EnterpriseInfoAlert from 'MainRoot/shared/enterpriseTier/EnterpriseInfoAlert';

describe('EnterpriseInfoAlert', () => {
  it('renders enterprise feature message', () => {
    render(<EnterpriseInfoAlert />);
    expect(screen.getByText(/Enterprise feature/i)).toBeInTheDocument();
  });

  it('renders Return to Lifecycle Pro link when callback provided', () => {
    const mockCallback = jest.fn();
    render(<EnterpriseInfoAlert onGoBackToDefault={mockCallback} />);
    expect(screen.getByText('Return to Lifecycle Pro')).toBeInTheDocument();
  });

  it('does not render link when no callback', () => {
    render(<EnterpriseInfoAlert />);
    expect(screen.queryByText('Return to Lifecycle Pro')).toBeNull();
  });

  it('calls callback when link clicked', async () => {
    const mockCallback = jest.fn();
    render(<EnterpriseInfoAlert onGoBackToDefault={mockCallback} />);
    await userEvent.click(screen.getByText('Return to Lifecycle Pro'));
    expect(mockCallback).toHaveBeenCalledTimes(1);
  });
});
