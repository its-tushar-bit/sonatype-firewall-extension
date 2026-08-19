/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import IncompleteConfigurationModal from 'MainRoot/firewallOnboarding/IncompleteConfigurationModal';
import { render, screen } from 'TestRoot/SpecUtil';

describe('IncompleteConfigurationModal', () => {
  let renderComponent;

  beforeEach(() => {
    renderComponent = () => render(<IncompleteConfigurationModal />);
  });

  it('renders a dialog element with the correct content', () => {
    renderComponent();

    expect(screen.getByRole('dialog', { hidden: true })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('Repository Firewall has not been configured');
    expect(
      screen.getByText(
        'You have not completed the Repository Firewall configuration. Your environment will not be protected from malicious code or dependency confusion threats until Repository Firewall has been configured. You can restart and complete the configuration process at a later time by reloading Repository Firewall.'
      )
    ).toBeVisible();
    expect(screen.getByText('If you continue, any changes you have made will be discarded.')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Continue' })).toBeVisible();
  });
});
