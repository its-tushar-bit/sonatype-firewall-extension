/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import Reachability from 'MainRoot/components/reachability/Reachability';

describe('Reachability', () => {
  const renderReachability = (props = {}) => {
    render(<Reachability {...props} />);
  };

  it('renders "Reachable" given true', async () => {
    renderReachability({
      reachable: true,
    });

    const component = screen.getByText('Reachable');
    expect(component).toBeVisible();
    expect(component).toHaveClass('iq-reachability__reachable');
  });

  it('renders "Not Reachable" given false', async () => {
    renderReachability({
      reachable: false,
    });

    const component = screen.getByText('Not Reachable');
    expect(component).toBeVisible();
    expect(component).not.toHaveClass('iq-reachability__reachable');
  });

  it('renders "-" given no properties', async () => {
    renderReachability();

    const component = screen.getByText('-');
    expect(component).toBeVisible();
    expect(component).not.toHaveClass('iq-reachability__reachable');
  });

  it('renders "-" given undefined', async () => {
    renderReachability({
      reachable: undefined,
    });

    const component = screen.getByText('-');
    expect(component).toBeVisible();
    expect(component).not.toHaveClass('iq-reachability__reachable');
  });

  it('renders "-" given null', async () => {
    renderReachability({
      reachable: null,
    });

    const component = screen.getByText('-');
    expect(component).toBeVisible();
    expect(component).not.toHaveClass('iq-reachability__reachable');
  });
});
