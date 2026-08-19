/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { render, screen } from '@testing-library/react';
import { ClassicComponentMount, mountClassicComponent } from 'MainRoot/nexus-one/ClassicComponentMount';

jest.mock('MainRoot/nosc/theme/useNoscTheme', () => ({
  useNoscTheme: () => ({ effectiveTheme: 'light', themeMode: 'light' }),
}));

jest.mock('MainRoot/nosc/shell/previewShellLayout', () => ({
  usePreviewShellOffsets: () => ({ top: 56, left: 72 }),
}));

jest.mock('MainRoot/react/Footer/Footer', () => ({
  __esModule: true,
  default: function MockFooter() {
    return null;
  },
}));

function TestPage(): JSX.Element {
  return <div>Classic page content</div>;
}

describe('ClassicComponentMount', () => {
  it('renders children inside the classic mount wrapper', () => {
    render(
      <ClassicComponentMount>
        <TestPage />
      </ClassicComponentMount>,
    );
    expect(screen.getByTestId('nexus-one-classic-component-mount')).toBeInTheDocument();
    expect(screen.getByText('Classic page content')).toBeInTheDocument();
  });

  it('mountClassicComponent wraps a page for UI Router', () => {
    const Mounted = mountClassicComponent(TestPage);
    render(<Mounted />);
    expect(screen.getByTestId('nexus-one-classic-component-mount')).toBeInTheDocument();
    expect(screen.getByText('Classic page content')).toBeInTheDocument();
  });

  it('carries the nx-page class so PortalDrawer-based Classic content (e.g. filter sidebars) has a portal target', () => {
    render(
      <ClassicComponentMount>
        <TestPage />
      </ClassicComponentMount>,
    );
    expect(screen.getByTestId('nexus-one-classic-component-mount')).toHaveClass('nx-page');
  });

  it('carries the nosc-classic-mount class so nexus-one.css scoped overrides (e.g. the React2Shell table width fix) still apply', () => {
    render(
      <ClassicComponentMount>
        <TestPage />
      </ClassicComponentMount>,
    );
    expect(screen.getByTestId('nexus-one-classic-component-mount')).toHaveClass('nosc-classic-mount');
  });

  it('overrides nx-page width/min-width so the embed does not overflow past the viewport', () => {
    // Regression guard: .nx-page's own stylesheet rule (RSC's _nx-page-layout.scss) sets
    // `width: 100%` and `min-width: var(--nx-width-page-min)` (1366px), sized for Classic's
    // full-page layout where this element IS the viewport's content box. For a `position: fixed`
    // element, `width: 100%` resolves against the viewport itself, so combined with the `left`
    // offset below it over-constrains the box — per the CSS spec, `right: 0` gets silently
    // dropped and the element renders `left + 100vw` wide, overflowing off the right edge by
    // exactly the shell nav's width. Confirmed empirically in a real browser: at a 1440px
    // viewport with the nav open (left: 256), the mount measured right: 1696 (256px past the
    // viewport) before this override, and right: 1440 (flush with the viewport) after it.
    render(
      <ClassicComponentMount>
        <TestPage />
      </ClassicComponentMount>,
    );
    const mount = screen.getByTestId('nexus-one-classic-component-mount');
    expect(mount).toHaveStyle({ width: 'auto', minWidth: 0 });
  });

  it('default scroll layout wraps children in nx-global-footer-2-container', () => {
    render(
      <ClassicComponentMount>
        <TestPage />
      </ClassicComponentMount>,
    );
    const mount = screen.getByTestId('nexus-one-classic-component-mount');
    expect(mount).toHaveAttribute('data-layout', 'scroll');
    expect(mount.querySelector(':scope > .nx-global-footer-2-container.nx-viewport-sized')).not.toBeNull();
  });

  it('page layout mounts children directly under .nx-page for Orgs & Policies grid participation', () => {
    render(
      <ClassicComponentMount layout="page">
        <div id="iq-content" className="nx-page-content nx-page-content--full-width">
          <TestPage />
        </div>
      </ClassicComponentMount>,
    );
    const mount = screen.getByTestId('nexus-one-classic-component-mount');
    expect(mount).toHaveAttribute('data-layout', 'page');
    expect(mount.querySelector(':scope > #iq-content')).not.toBeNull();
    expect(mount.querySelector(':scope > .nx-global-footer-2-container')).toBeNull();
  });
});
