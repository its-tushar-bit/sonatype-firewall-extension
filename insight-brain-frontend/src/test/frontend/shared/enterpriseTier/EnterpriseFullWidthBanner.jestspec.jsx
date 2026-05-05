/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import EnterpriseFullWidthBanner from 'MainRoot/shared/enterpriseTier/EnterpriseFullWidthBanner';

describe('EnterpriseFullWidthBanner', () => {
  it('renders title and description when title provided', () => {
    render(<EnterpriseFullWidthBanner title="Custom Policies" description="Define policies." />);
    expect(screen.getByText('Custom Policies')).toBeInTheDocument();
    expect(screen.getByText(/Define policies/)).toBeInTheDocument();
  });

  it('renders description without title header when no title', () => {
    render(<EnterpriseFullWidthBanner description="Some description." />);
    expect(screen.getByText(/Some description/)).toBeInTheDocument();
    expect(screen.queryByRole('heading', { level: 3 })).toBeNull();
  });

  it('renders Request Demo link', () => {
    render(<EnterpriseFullWidthBanner description="Test" />);
    expect(screen.getByText('Request Demo')).toBeInTheDocument();
  });

  it('renders Enterprise Feature tag when title provided', () => {
    render(<EnterpriseFullWidthBanner title="Test" description="Test" />);
    expect(screen.getByText('Enterprise Feature')).toBeInTheDocument();
  });
});
