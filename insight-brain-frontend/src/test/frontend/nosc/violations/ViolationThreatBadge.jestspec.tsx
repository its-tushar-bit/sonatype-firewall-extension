/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { ViolationThreatBadge } from 'MainRoot/nosc/violations/ViolationThreatBadge';

function renderBadge(threat?: number) {
  return render(
    <Theme>
      <ViolationThreatBadge threat={threat} />
    </Theme>,
  );
}

describe('ViolationThreatBadge', () => {
  it('renders the numeric threat level and an accessible name', () => {
    renderBadge(10);
    const badge = screen.getByTestId('violation-threat-badge');
    expect(badge).toHaveTextContent('10');
    expect(badge).toHaveAttribute('aria-label', 'Threat level 10');
  });

  it('treats a zero threat as present (renders 0, not the em-dash)', () => {
    renderBadge(0);
    const badge = screen.getByTestId('violation-threat-badge');
    expect(badge).toHaveTextContent('0');
    expect(badge).toHaveAttribute('aria-label', 'Threat level 0');
  });

  it('falls back to an em-dash and "unknown" label when the threat is absent', () => {
    renderBadge(undefined);
    const badge = screen.getByTestId('violation-threat-badge');
    expect(badge).toHaveTextContent('—');
    expect(badge).toHaveAttribute('aria-label', 'Threat level unknown');
  });
});
