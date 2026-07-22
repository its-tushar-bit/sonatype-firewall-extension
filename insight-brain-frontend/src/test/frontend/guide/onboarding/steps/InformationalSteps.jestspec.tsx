/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { render, screen } from '../../test-utils';
import { WelcomeStep } from 'GuideRoot/onboarding/steps/WelcomeStep';
import { PolicyStep } from 'GuideRoot/onboarding/steps/PolicyStep';

describe('WelcomeStep', () => {
  it('introduces the product as AI Developer, not Guide', () => {
    render(<WelcomeStep />);
    expect(screen.getByText(/Sonatype AI Developer/)).toBeInTheDocument();
    expect(screen.queryByText(/Sonatype Guide/)).not.toBeInTheDocument();
    expect(screen.getByText('Component intelligence')).toBeInTheDocument();
  });
});

describe('PolicyStep', () => {
  it('describes AI Developer policy compliance and renders both mocks', () => {
    render(<PolicyStep />);
    expect(screen.getByText(/AI Developer surfaces policy compliance/)).toBeInTheDocument();
    expect(screen.queryByText(/^Guide surfaces/)).not.toBeInTheDocument();
    expect(screen.getByText('On the Components list')).toBeInTheDocument();
    expect(screen.getByText('On a Component detail page')).toBeInTheDocument();
  });
});
