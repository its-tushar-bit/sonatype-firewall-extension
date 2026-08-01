/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { ComingSoonPage } from 'MainRoot/nosc/comingSoon/ComingSoonPage';

/**
 * Coming Soon stays in the NOUX shell — TopNav owns the only Classic toggle.
 */

function renderPage(props?: Partial<React.ComponentProps<typeof ComingSoonPage>>) {
  return render(
    <Theme>
      <ComingSoonPage
        moduleName="Enterprise Reporting"
        description="View, schedule, and download enterprise policy and SBOM reports."
        {...props}
      />
    </Theme>,
  );
}

describe('<ComingSoonPage>', () => {
  it('renders the module description in the body', () => {
    const desc = 'View, schedule, and download enterprise policy and SBOM reports.';
    renderPage({ description: desc });
    expect(screen.getByText(desc)).toBeInTheDocument();
  });

  it('does not render Classic escape hatches', () => {
    renderPage();
    expect(screen.queryByTestId('nosc-coming-soon-classic-newtab-button')).not.toBeInTheDocument();
    expect(screen.queryByTestId('nosc-coming-soon-classic-samewindow-button')).not.toBeInTheDocument();
    expect(screen.queryByText(/classic/i)).not.toBeInTheDocument();
  });

  it('renders an eyebrow Badge with the module name above the hero', () => {
    renderPage({ moduleName: 'Policies' });
    expect(screen.getByTestId('nosc-coming-soon-eyebrow')).toHaveTextContent('Policies');
  });

  it('uses "Coming Soon" as the page heading (not the module name)', () => {
    renderPage({ moduleName: 'Policies' });
    expect(
      screen.getByRole('heading', { name: /^coming soon$/i, level: 1 }),
    ).toBeInTheDocument();
  });

  it('renders a "Back to Home" link pointing at #/home', () => {
    renderPage();
    const backLink = screen.getByTestId('nosc-coming-soon-back-home-link');
    expect(backLink).toHaveAttribute('href', '#/home');
  });

  it('exposes the module name via data-module-name for downstream telemetry hooks', () => {
    renderPage({ moduleName: 'Policies' });
    expect(screen.getByTestId('nosc-coming-soon-page')).toHaveAttribute(
      'data-module-name',
      'Policies',
    );
  });
});
