/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen } from '@testing-library/react';
import { ButtonLink } from 'MainRoot/nosc/components/ButtonLink';

function renderButtonLink(props: Partial<React.ComponentProps<typeof ButtonLink>> = {}) {
  return render(
    <Theme>
      <ButtonLink href="/assets/#/applications/apple-java1" data-testid="test-button-link" {...props}>
        Open application
      </ButtonLink>
    </Theme>,
  );
}

describe('ButtonLink', () => {
  it('renders an anchor with the requested href and visible label', () => {
    renderButtonLink();
    const link = screen.getByTestId('test-button-link');
    expect(link).toHaveAttribute('href', '/assets/#/applications/apple-java1');
    expect(link).toHaveTextContent('Open application');
  });

  it('opens in a new tab when newTab is true', () => {
    renderButtonLink({ newTab: true });
    const link = screen.getByTestId('test-button-link');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('does not set target or rel for same-tab navigation by default', () => {
    renderButtonLink();
    const link = screen.getByTestId('test-button-link');
    expect(link).not.toHaveAttribute('target');
    expect(link).not.toHaveAttribute('rel');
  });

  it('forwards aria-label when provided', () => {
    renderButtonLink({ 'aria-label': 'Continue in Classic UI' });
    expect(screen.getByLabelText('Continue in Classic UI')).toBeInTheDocument();
  });
});
