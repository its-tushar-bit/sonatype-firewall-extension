/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import FirewallWaiverRequestStatusBadge from 'MainRoot/firewall/waiverRequests/FirewallWaiverRequestStatusBadge';

describe('FirewallWaiverRequestStatusBadge', () => {
  it('shows Requested badge with expected accessible text', () => {
    render(<FirewallWaiverRequestStatusBadge status="REQUESTED" />);
    expect(screen.getByText('Requested')).toBeInTheDocument();
  });

  it('shows Approved badge with expected accessible text', () => {
    render(<FirewallWaiverRequestStatusBadge status="APPROVED" />);
    expect(screen.getByText('Approved')).toBeInTheDocument();
  });

  it('shows Rejected badge with expected accessible text', () => {
    render(<FirewallWaiverRequestStatusBadge status="REJECTED" />);
    expect(screen.getByText('Rejected')).toBeInTheDocument();
  });

  it('applies the correct color for REQUESTED status', () => {
    const { container } = render(<FirewallWaiverRequestStatusBadge status="REQUESTED" />);
    expect(container.querySelector('[data-accent-color="blue"]')).toBeInTheDocument();
  });

  it('applies the correct color for APPROVED status', () => {
    const { container } = render(<FirewallWaiverRequestStatusBadge status="APPROVED" />);
    expect(container.querySelector('[data-accent-color="green"]')).toBeInTheDocument();
  });

  it('applies the correct color for REJECTED status', () => {
    const { container } = render(<FirewallWaiverRequestStatusBadge status="REJECTED" />);
    expect(container.querySelector('[data-accent-color="tomato"]')).toBeInTheDocument();
  });
});
