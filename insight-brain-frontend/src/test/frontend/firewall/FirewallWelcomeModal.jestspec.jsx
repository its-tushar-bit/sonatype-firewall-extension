/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import FirewallWelcomeModal from 'MainRoot/firewall/FirewallWelcomeModal';
import { fireEvent, render, screen } from 'TestRoot/SpecUtil';

import 'TestRoot/SpecUtil';

describe('FirewallWelcomeModal', () => {
  it('renders a dialog element with the correct content', () => {
    render(<FirewallWelcomeModal close={() => {}} />);

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('Welcome to Sonatype Repository Firewall!');
    expect(
      screen.getByText(
        'Firewall configuration will run in the background and populate data related to all your enabled repositories. The time taken to complete this process depends on the number of enabled repositories and size of individual repositories.'
      )
    ).toBeVisible();
    expect(screen.getByRole('button', { name: 'Close' })).toBeVisible();
  });

  it('close is called when close button is pressed', () => {
    const closeSpy = jest.fn().mockName('closeSpy');
    render(<FirewallWelcomeModal close={closeSpy} />);

    fireEvent.click(screen.getByRole('button'));
    expect(closeSpy).toHaveBeenCalled();
  });
});
