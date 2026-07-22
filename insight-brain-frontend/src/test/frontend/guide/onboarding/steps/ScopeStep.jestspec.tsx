/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { render, screen } from '../../test-utils';
import { ScopeStep } from 'GuideRoot/onboarding/steps/ScopeStep';

describe('ScopeStep', () => {
  it('explains org-or-app scope and drops the coming-soon framing', () => {
    render(<ScopeStep />);
    // The two scope kinds are called out (exact <strong> text, not "Root Organization").
    expect(screen.getByText('organization')).toBeInTheDocument();
    expect(screen.getByText('application')).toBeInTheDocument();
    expect(screen.queryByText(/under construction/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/won’t change what you see yet/i)).not.toBeInTheDocument();
    // No fake picker control
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
  });
});
