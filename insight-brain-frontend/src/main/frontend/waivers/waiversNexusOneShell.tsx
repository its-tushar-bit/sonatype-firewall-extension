/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

/**
 * Optional shell wrapper for the standalone Classic waiver pages.
 * <p>
 * Classic leaves this as identity. The Nexus One bundle installs
 * {@code ClassicComponentMount} so {@code #/addWaiver/...} and its siblings render
 * inside the shell content area instead of underrunning LeftNav — same pattern as
 * {@code applicationReportNexusOneShell}.
 * <p>
 * The wrapper is applied at render time rather than at registration, so the Nexus One
 * bundle does not have to install it before {@code waivers/route} registers its states
 * at import time.
 */
type PageWrapper = (node: React.ReactElement) => React.ReactElement;

let pageWrapper: PageWrapper = (node) => node;

export function setClassicWaiverPageWrapper(wrapper: PageWrapper): void {
  pageWrapper = wrapper;
}

export function shellWrappedWaiverPage<P extends object>(
  Component: React.ComponentType<P>
): React.ComponentType<P> {
  function ShellWrappedWaiverPage(props: P) {
    return pageWrapper(<Component {...props} />);
  }
  ShellWrappedWaiverPage.displayName = `ShellWrappedWaiverPage(${
    Component.displayName || Component.name || 'Component'
  })`;
  return ShellWrappedWaiverPage;
}
