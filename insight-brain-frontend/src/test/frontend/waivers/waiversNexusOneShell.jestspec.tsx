/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import {
  setClassicWaiverPageWrapper,
  shellWrappedWaiverPage,
} from 'MainRoot/waivers/waiversNexusOneShell';

function WaiverPage() {
  return <div data-testid="waiver-page">waiver</div>;
}

function shellWrapper(node: React.ReactElement) {
  return <section data-testid="n1-shell">{node}</section>;
}

describe('waiversNexusOneShell', () => {
  afterEach(() => {
    // The wrapper is module-level mutable state, so restore identity or a leftover wrapper
    // leaks into every suite that renders a waiver page after this one.
    setClassicWaiverPageWrapper((node) => node);
  });

  it('renders the page unchanged when no wrapper is installed', () => {
    const Wrapped = shellWrappedWaiverPage(WaiverPage);

    render(<Wrapped />);

    expect(screen.getByTestId('waiver-page')).toBeInTheDocument();
    expect(screen.queryByTestId('n1-shell')).not.toBeInTheDocument();
  });

  it('renders the page inside an installed wrapper', () => {
    setClassicWaiverPageWrapper(shellWrapper);
    const Wrapped = shellWrappedWaiverPage(WaiverPage);

    render(<Wrapped />);

    expect(screen.getByTestId('n1-shell')).toContainElement(screen.getByTestId('waiver-page'));
  });

  it('applies a wrapper installed after the page was already wrapped', () => {
    // waivers/route registers its states at import time, which happens before nexus-one/routes
    // installs the wrapper. Resolving at render rather than at registration is what keeps that
    // ordering from mattering, so a regression here would put Add Waiver back under LeftNav.
    const Wrapped = shellWrappedWaiverPage(WaiverPage);

    setClassicWaiverPageWrapper(shellWrapper);
    render(<Wrapped />);

    expect(screen.getByTestId('n1-shell')).toContainElement(screen.getByTestId('waiver-page'));
  });

  it('forwards props through to the wrapped page', () => {
    function ViolationPage({ violationId }: { readonly violationId: string }) {
      return <div data-testid="violation">{violationId}</div>;
    }
    const Wrapped = shellWrappedWaiverPage(ViolationPage);

    render(<Wrapped violationId="violation-1" />);

    expect(screen.getByTestId('violation')).toHaveTextContent('violation-1');
  });
});
