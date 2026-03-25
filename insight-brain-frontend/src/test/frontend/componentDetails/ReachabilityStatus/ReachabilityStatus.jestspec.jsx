/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from '@testing-library/react';
import ReachabilityStatus from 'MainRoot/componentDetails/ReachabilityStatus/ReachabilityStatus';

describe('ReachabilityStatus', () => {
  const renderComponent = (props) => {
    render(<ReachabilityStatus {...props} />);
  };

  it('renders "Not reachable" when reachabilityStatus is "NON_REACHABLE"', () => {
    renderComponent({ reachabilityStatus: 'NON_REACHABLE' });
    expect(screen.getByText('Not reachable')).toBeInTheDocument();
    expect(screen.queryByText('Reachable')).not.toBeInTheDocument();
  });

  it('renders "Reachable" when reachabilityStatus is "REACHABLE"', () => {
    renderComponent({ reachabilityStatus: 'REACHABLE' });
    expect(screen.getByText('Reachable')).toBeInTheDocument();
    expect(screen.queryByText('Not reachable')).not.toBeInTheDocument();
  });

  it('applies the correct class when reachabilityStatus is "REACHABLE"', () => {
    renderComponent({ reachabilityStatus: 'REACHABLE' });
    expect(screen.getByText('Reachable').closest('div')).toHaveClass(
      'iq-policy-violation-row__reachability--reachable'
    );
  });

  it('applies the correct icon class when reachabilityStatus is "REACHABLE"', () => {
    renderComponent({ reachabilityStatus: 'REACHABLE' });
    expect(screen.getByRole('img', { hidden: true })).toHaveClass(
      'iq-policy-violation-row__reachability-icon--reachable'
    );
  });

  it('applies the correct icon class when reachabilityStatus is not "REACHABLE"', () => {
    renderComponent({ reachabilityStatus: 'NON_REACHABLE' });
    expect(screen.getByRole('img', { hidden: true })).toHaveClass(
      'iq-policy-violation-row__reachability-icon--non-reachable'
    );
  });
});
