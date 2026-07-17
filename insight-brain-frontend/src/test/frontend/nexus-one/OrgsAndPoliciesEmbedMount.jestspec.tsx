/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import { mountOrgsAndPoliciesChrome } from 'MainRoot/nexus-one/OrgsAndPoliciesEmbedMount';

// The real ClassicComponentMount pulls in theme/offset hooks that need shell providers; this mount's
// own contract (the #iq-sidebar-container host, the host-before-page gate, the viewport-sized class)
// is independent of that wrapper, so stub it to a passthrough.
jest.mock('MainRoot/nexus-one/ClassicComponentMount', () => ({
  ClassicComponentMount: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
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
});
