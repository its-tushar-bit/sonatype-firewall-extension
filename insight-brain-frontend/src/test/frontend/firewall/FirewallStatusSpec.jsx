/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen } from 'TestRoot/SpecUtil';
import FirewallStatus from 'MainRoot/firewall/FirewallStatus';

describe('FirewallStatus', () => {
  const props = {
    totalComponentCount: 99999,
    repositoryCount: 10000,
    quarantineEnabledRepositoryCount: 9001,
  };

  it('renders the correct components monitored text', () => {
    render(<FirewallStatus {...props} />);

    expect(screen.getByText((_, element) => element.textContent === '99,999 components monitored')).toBeVisible();
  });

  it('renders the correct status text', () => {
    render(<FirewallStatus {...props} />);
    const status = screen.getByRole('status');
    expect(status).toBeInTheDocument();
    expect(status).toHaveTextContent('9,001 of 10,000 repositories protected');
  });
});
