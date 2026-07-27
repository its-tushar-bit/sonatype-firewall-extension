/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  setApplicationReportRootWrapper,
  wrapApplicationReportRoot,
} from 'MainRoot/applicationReport/applicationReportNexusOneShell';

describe('applicationReportNexusOneShell', () => {
  afterEach(() => {
    // Restore identity wrap so other suites are not affected by a leftover wrapper.
    setApplicationReportRootWrapper((node) => node);
  });

  it('defaults to an identity wrap', () => {
    const node = React.createElement('div', { 'data-testid': 'report-root' }, 'report');
    expect(wrapApplicationReportRoot(node)).toBe(node);
  });

  it('applies an installed root wrapper from setApplicationReportRootWrapper', () => {
    setApplicationReportRootWrapper((node) =>
      React.createElement('section', { 'data-testid': 'n1-shell' }, node),
    );
    const node = React.createElement('div', { 'data-testid': 'report-root' }, 'report');
    const wrapped = wrapApplicationReportRoot(node);
    expect(React.isValidElement(wrapped)).toBe(true);
    expect(wrapped.type).toBe('section');
    expect(wrapped.props['data-testid']).toBe('n1-shell');
    expect(wrapped.props.children).toBe(node);
  });
});
