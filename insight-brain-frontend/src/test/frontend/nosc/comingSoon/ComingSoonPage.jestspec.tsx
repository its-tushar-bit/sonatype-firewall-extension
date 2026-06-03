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
 * P1-F15 / CLM-39545: <ComingSoonPage> is rendered for every stub LeftNav
 * entry. Most of its behavior is presentational, but the Classic-IQ escape
 * hatch is the load-bearing piece: clicking that button is how users do
 * real work while the native Nexus One module is being built. These tests
 * lock that contract.
 *
 * NOTE: this project does not depend on jest-axe; accessibility correctness
 * is enforced by the explicit role / aria-label assertions below plus the
 * `nosc-leftnav-*` test-ids consumed by Selenium.
 */

function renderPage(props?: Partial<React.ComponentProps<typeof ComingSoonPage>>) {
  return render(
    <Theme>
      <ComingSoonPage
        moduleName="Reports"
        description="View, schedule, and download policy and SBOM scan reports."
        classicHref="/assets/#/reports/views"
        {...props}
      />
    </Theme>,
  );
}

describe('<ComingSoonPage>', () => {
  it('renders the module description in the body', () => {
    const desc = 'View, schedule, and download policy and SBOM scan reports.';
    renderPage({ description: desc });
    expect(screen.getByText(desc)).toBeInTheDocument();
  });

  it('renders a primary "Open in Classic (new tab)" button targeting target=_blank', () => {
    renderPage({ classicHref: '/assets/#/reports/views' });
    const button = screen.getByTestId('nosc-coming-soon-classic-newtab-button');
    const anchor = button.querySelector('a') ?? button.closest('a');
    expect(anchor).not.toBeNull();
    expect(anchor).toHaveAttribute('href', '/assets/#/reports/views');
    expect(anchor).toHaveAttribute('target', '_blank');
    // CLM-39545 / P1-F15: rel='noopener noreferrer' is a security requirement
    // for any anchor that opens in a new tab — without it the opened page can
    // hijack window.opener.
    expect(anchor).toHaveAttribute('rel', expect.stringContaining('noopener'));
    expect(anchor).toHaveAttribute('rel', expect.stringContaining('noreferrer'));
  });

  it('renders a secondary "Continue in Classic" button that navigates the current tab', () => {
    renderPage({ classicHref: '/assets/#/reports/views' });
    const button = screen.getByTestId('nosc-coming-soon-classic-samewindow-button');
    const anchor = button.querySelector('a') ?? button.closest('a');
    expect(anchor).not.toBeNull();
    expect(anchor).toHaveAttribute('href', '/assets/#/reports/views');
    // No target attr — defaults to self-tab navigation.
    expect(anchor).not.toHaveAttribute('target');
  });

  it('provides accessible names on both Classic-IQ links that include the module name', () => {
    renderPage({ moduleName: 'Reports' });
    expect(
      screen.getByLabelText(/open reports in classic iq in a new tab/i),
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText(/continue to reports in classic iq in this tab/i),
    ).toBeInTheDocument();
  });

  it('renders an eyebrow Badge with the module name above the hero', () => {
    renderPage({ moduleName: 'Policies' });
    expect(screen.getByTestId('nosc-coming-soon-eyebrow')).toHaveTextContent('Policies');
  });

  it('uses "Coming Soon" as the page heading (not the module name)', () => {
    renderPage({ moduleName: 'Policies' });
    // h1 is "Coming Soon" — module name is in the eyebrow Badge above.
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
