/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

/**
 * Optional shell wrapper for {@link HostedRepositoryComponentReportRoot}.
 * <p>
 * Classic leaves this as identity. The Nexus One bundle installs
 * {@link ClassicComponentMount} so {@code #/hostedRepositoryComponentReport/...}
 * sits clear of LeftNav/TopNav instead of underrunning the shell.
 */
type RootWrapper = (node: React.ReactElement) => React.ReactElement;

let rootWrapper: RootWrapper = (node) => node;

export function setHostedRepositoryComponentReportRootWrapper(wrapper: RootWrapper): void {
  rootWrapper = wrapper;
}

export function wrapHostedRepositoryComponentReportRoot(node: React.ReactElement): React.ReactElement {
  return rootWrapper(node);
}
