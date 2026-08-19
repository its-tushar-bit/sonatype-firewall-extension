/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

import { fireEvent, render, screen } from 'TestRoot/SpecUtil';
import FirewallRulesSelector from 'MainRoot/firewallOnboarding/FirewallRulesSelector';

describe('FirewallRulesSelector', function () {
  const renderComponent = () => render(<FirewallRulesSelector />);

  it('renders all protection rule checkboxes', () => {
    renderComponent();

    expect(screen.getByRole('checkbox', { name: 'Supply chain attacks protection' })).not.toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Namespace confusion protection' })).not.toBeChecked();

    fireEvent.click(screen.getByRole('checkbox', { name: 'Supply chain attacks protection' }));

    expect(screen.getByRole('checkbox', { name: 'Supply chain attacks protection' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Namespace confusion protection' })).not.toBeChecked();

    fireEvent.click(screen.getByRole('checkbox', { name: 'Namespace confusion protection' }));

    expect(screen.getByRole('checkbox', { name: 'Supply chain attacks protection' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Namespace confusion protection' })).toBeChecked();
  });
});
