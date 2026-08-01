/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import { mountOrgsAndPoliciesChrome } from 'MainRoot/nexus-one/OrgsAndPoliciesEmbedMount';

// The real ClassicComponentMount pulls in theme/offset hooks that need shell providers; this mount's
// own contract (the #iq-sidebar-container host, the host-before-page gate, the viewport-sized class,
// page layout) is independent of that wrapper's Theme chrome, so stub it to a passthrough that still
// honors layout="page" (no scroll footer wrapper).
jest.mock('MainRoot/nexus-one/ClassicComponentMount', () => ({
  ClassicComponentMount: ({
    children,
    layout = 'scroll',
  }: {
    children: React.ReactNode;
    layout?: 'scroll' | 'page';
  }) => (
    <div data-testid="nexus-one-classic-component-mount" data-layout={layout} className="nosc-classic-mount nx-page">
      {layout === 'page' ? (
        children
      ) : (
        <div className="nx-global-footer-2-container nx-viewport-sized">{children}</div>
      )}
    </div>
  ),
}));

jest.mock('MainRoot/react/Footer/Footer', () => ({
  __esModule: true,
  default: function MockFooter() {
    return <div data-testid="orgs-embed-footer" />;
  },
}));

describe('mountOrgsAndPoliciesChrome', () => {
  let hostPresentAtPageRender: HTMLElement | null;

  function StubPage() {
    hostPresentAtPageRender = document.getElementById('iq-sidebar-container');
    return <div data-testid="stub-page" />;
  }

  const renderMount = (viewportSized?: boolean) => {
    const Mounted = mountOrgsAndPoliciesChrome(StubPage);
    return render(<Mounted />, {
      preloadedState: { router: { currentState: { data: viewportSized ? { viewportSized: true } : {} } } },
    });
  };

  beforeEach(() => {
    hostPresentAtPageRender = null;
  });

  it('renders the #iq-sidebar-container portal host the owner sidebars need', () => {
    renderMount();

    expect(document.getElementById('iq-sidebar-container')).toBeInTheDocument();
  });

  it('commits the portal host before the page renders, so its createPortal target is never null', () => {
    renderMount();

    expect(screen.getByTestId('stub-page')).toBeInTheDocument();
    expect(hostPresentAtPageRender).not.toBeNull();
  });

  it('adds nx-viewport-sized to #iq-footer-container when the current state is viewport-sized', () => {
    renderMount(true);

    expect(document.getElementById('iq-footer-container')).toHaveClass('nx-viewport-sized');
  });

  it('leaves nx-viewport-sized off #iq-footer-container when the current state is not viewport-sized', () => {
    renderMount(false);

    expect(document.getElementById('iq-footer-container')).not.toHaveClass('nx-viewport-sized');
  });

  it('uses page layout so #iq-content is a direct child of .nx-page (owner sidebar grid)', () => {
    renderMount();

    const mount = screen.getByTestId('nexus-one-classic-component-mount');
    expect(mount).toHaveAttribute('data-layout', 'page');
    expect(mount.querySelector(':scope > #iq-content')).not.toBeNull();
    expect(mount.querySelector(':scope > .nx-global-footer-2-container')).toBeNull();
  });

  it('places Footer inside #iq-footer-container like Classic App.jsx', () => {
    renderMount();

    const footerContainer = document.getElementById('iq-footer-container');
    expect(footerContainer).not.toBeNull();
    expect(footerContainer?.querySelector('[data-testid="orgs-embed-footer"]')).not.toBeNull();
  });
});
