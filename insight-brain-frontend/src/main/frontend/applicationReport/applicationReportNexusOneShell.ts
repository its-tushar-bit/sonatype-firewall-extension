/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

/**
 * Optional shell wrapper for {@link ApplicationReportRoot}.
 * <p>
 * Classic leaves this as identity. The Nexus One bundle installs
 * {@link ClassicComponentMount} so {@code #/applicationReport/...} (including
 * Component Details Legal) sits clear of LeftNav/TopNav instead of underrunning
 * the shell — same pattern as other Classic embeds in {@code nexus-one/routes.tsx}.
 */
type RootWrapper = (node: React.ReactElement) => React.ReactElement;

let rootWrapper: RootWrapper = (node) => node;

export function setApplicationReportRootWrapper(wrapper: RootWrapper): void {
  rootWrapper = wrapper;
}

export function wrapApplicationReportRoot(node: React.ReactElement): React.ReactElement {
  return rootWrapper(node);
}
