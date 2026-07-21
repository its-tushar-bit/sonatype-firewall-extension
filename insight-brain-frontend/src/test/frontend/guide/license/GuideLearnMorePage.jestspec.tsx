/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { GuideLearnMorePage } from 'GuideRoot/license/GuideLearnMorePage';

describe('GuideLearnMorePage', () => {
  it('renders the not-enabled message', () => {
    render(
      <Theme>
        <GuideLearnMorePage />
      </Theme>
    );

    expect(
      screen.getByText(/Sonatype AI Developer is not currently enabled/i)
    ).toBeInTheDocument();
  });

  it('renders a learn more link', () => {
    render(
      <Theme>
        <GuideLearnMorePage />
      </Theme>
    );

    const link = screen.getByRole('link', { name: /learn more/i });
    expect(link).toHaveAttribute('href', expect.stringContaining('sonatype.com'));
    expect(link).toHaveAttribute('target', '_blank');
  });
});
