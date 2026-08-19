/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen } from 'TestRoot/SpecUtil';
import PolicyActionTag from 'MainRoot/react/PolicyActionTag';

describe('PolicyActionTag', () => {
  it('renders a "Fail" tag', () => {
    render(<PolicyActionTag action="fail" />);
    expect(screen.getByText('fail')).toBeInTheDocument();
  });

  it('renders a "Warn" tag', () => {
    render(<PolicyActionTag action="warn" />);
    expect(screen.getByText('warn')).toBeInTheDocument();
  });

  it('does not render a tag if no action is provided', () => {
    const { container } = render(<PolicyActionTag />);
    expect(container).toBeEmptyDOMElement();
  });
});
