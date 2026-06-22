/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { render, screen } from '../../test-utils';
import { PolicyBadgeV2 } from 'GuideRoot/components/detail/PolicyBadgeV2';

describe('PolicyBadgeV2', () => {
  it('renders a green "Compliant" badge for PASS', () => {
    render(<PolicyBadgeV2 complianceLevel="PASS" />);
    const badge = screen.getByText('Compliant');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveAttribute('data-accent-color', 'green');
  });

  it('renders an amber "Compliant" badge for WARN', () => {
    render(<PolicyBadgeV2 complianceLevel="WARN" />);
    const badge = screen.getByText('Compliant');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveAttribute('data-accent-color', 'amber');
  });

  it('renders a red "Non-Compliant" badge for FAIL', () => {
    render(<PolicyBadgeV2 complianceLevel="FAIL" />);
    const badge = screen.getByText('Non-Compliant');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveAttribute('data-accent-color', 'red');
  });

  it('hides the label when onlyIcon is set', () => {
    const { container } = render(<PolicyBadgeV2 complianceLevel="PASS" onlyIcon />);
    expect(screen.queryByText('Compliant')).not.toBeInTheDocument();
    // icon still renders
    expect(container.querySelector('svg')).toBeInTheDocument();
  });
});
