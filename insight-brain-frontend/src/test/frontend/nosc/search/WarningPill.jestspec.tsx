/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen } from '@testing-library/react';
import { WarningPill } from 'MainRoot/nosc/search/WarningPill';

function renderPill(warnings: readonly string[]): void {
  render(
    <Theme>
      <WarningPill warnings={warnings} />
    </Theme>,
  );
}

describe('WarningPill', () => {
  it('renders nothing when there are no warnings', () => {
    renderPill([]);
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('announces a single warning politely without stealing focus', () => {
    renderPill(["Unknown filter 'foo' ignored"]);
    const status = screen.getByRole('status');
    expect(status).toHaveAttribute('aria-live', 'polite');
    expect(status).toHaveTextContent("Unknown filter 'foo' ignored");
    expect(document.activeElement).toBe(document.body);
  });

  it('summarizes multiple warnings into one announcement', () => {
    renderPill(["Unknown filter 'foo' ignored", 'Unclosed quote']);
    expect(screen.getByRole('status')).toHaveTextContent(
      "2 warnings: Unknown filter 'foo' ignored • Unclosed quote",
    );
  });
});
