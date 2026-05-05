/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import TierTag from 'MainRoot/react/shared/TierTag';

describe('TierTag', () => {
  it('renders children text', () => {
    render(<TierTag>Enterprise Feature</TierTag>);
    expect(screen.getByText('Enterprise Feature')).toBeInTheDocument();
  });

  it('has iq-tier-tag class', () => {
    render(<TierTag>Pro</TierTag>);
    expect(screen.getByText('Pro')).toHaveClass('iq-tier-tag');
  });

  it('accepts additional className', () => {
    render(<TierTag className="custom-class">Test</TierTag>);
    const element = screen.getByText('Test');
    expect(element).toHaveClass('iq-tier-tag');
    expect(element).toHaveClass('custom-class');
  });
});
