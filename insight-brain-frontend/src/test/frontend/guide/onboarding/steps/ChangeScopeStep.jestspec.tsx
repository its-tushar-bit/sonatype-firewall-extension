/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { render, screen } from '../../test-utils';
import { ChangeScopeStep } from 'GuideRoot/onboarding/steps/ChangeScopeStep';

describe('ChangeScopeStep', () => {
  it('points at the real Policy context bar and drops coming-soon/stage copy', () => {
    render(<ChangeScopeStep />);
    expect(screen.getByText(/top of your Components and search pages/i)).toBeInTheDocument();
    expect(screen.queryByText(/coming soon/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/once the picker ships/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Release/)).not.toBeInTheDocument();
    // "Policy context" appears in both the copy and the mock's trigger label.
    expect(screen.getAllByText('Policy context').length).toBeGreaterThanOrEqual(2);
    // The mock renders the real trigger's current-owner label + ORG type badge.
    expect(screen.getByText('Root Organization')).toBeInTheDocument();
    expect(screen.getByText('ORG')).toBeInTheDocument();
  });
});
