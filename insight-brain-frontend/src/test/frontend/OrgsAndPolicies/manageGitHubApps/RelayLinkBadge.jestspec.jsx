/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import RelayLinkBadge, {
  RELAY_LINK_STATES,
} from 'MainRoot/OrgsAndPolicies/manageGitHubApps/RelayLinkBadge';

describe('RelayLinkBadge', () => {
  it('renders nothing when state is null (older server response)', () => {
    const { container } = render(<RelayLinkBadge state={null} attempts={0} />);
    expect(container.firstChild).toBeNull();
  });

  it('renders an OK badge for OK state', () => {
    render(<RelayLinkBadge state={RELAY_LINK_STATES.OK} attempts={0} />);
    expect(screen.getByTestId('relay-link-badge-ok')).toBeInTheDocument();
  });

  it('renders a Pending registration badge for UNREGISTERED state', () => {
    render(<RelayLinkBadge state={RELAY_LINK_STATES.UNREGISTERED} attempts={0} />);
    const badge = screen.getByTestId('relay-link-badge-unregistered');
    expect(badge).toBeInTheDocument();
    expect(badge.textContent).toMatch(/Pending registration/i);
  });

  it('renders Retrying with the attempt counter for ERROR state', () => {
    render(<RelayLinkBadge state={RELAY_LINK_STATES.ERROR} attempts={3} />);
    const badge = screen.getByTestId('relay-link-badge-error');
    expect(badge).toBeInTheDocument();
    // Counter format mirrors the backend's MAX_ATTEMPTS so admins can see remaining budget.
    expect(badge.textContent).toMatch(/Retrying \(3\/10\)/);
  });

  it('defaults attempts to 0 when not supplied for ERROR state', () => {
    render(<RelayLinkBadge state={RELAY_LINK_STATES.ERROR} />);
    expect(screen.getByTestId('relay-link-badge-error').textContent).toMatch(/Retrying \(0\/10\)/);
  });

  it('renders Failed with re-register hint for FAILED state', () => {
    render(<RelayLinkBadge state={RELAY_LINK_STATES.FAILED} attempts={10} />);
    const badge = screen.getByTestId('relay-link-badge-failed');
    expect(badge).toBeInTheDocument();
    expect(badge.textContent).toMatch(/Failed/i);
    expect(badge.textContent).toMatch(/re-register/i);
  });

  it('renders nothing for unknown future state', () => {
    const { container } = render(<RelayLinkBadge state="QUANTUM_SUPERPOSITION" attempts={0} />);
    // No badge -- a misleading "OK"/"FAILED" badge for an unrecognized state would be worse
    // than rendering nothing while the frontend catches up to a server upgrade.
    expect(container.firstChild).toBeNull();
  });
});
