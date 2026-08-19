/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { screen, within } from '@testing-library/react';

import { render } from 'TestRoot/SpecUtil';
import FirewallContainerTabs from 'MainRoot/firewall/FirewallContainerTabs';

describe('FirewallContainerTabs', () => {
  const props = {
    loadContainerQuarantineList: jest.fn(),
    loadContainerQuarantineGridError: '',
    setContainerQuarantineGridPage: jest.fn(),
    loadedContainerQuarantineList: true,
    containerQuarantineList: [],
    containerQuarantinePageCount: 10,
    containerPageSize: 10,
    containerCurrentPage: 1,
    containerLastUpdated: {},
    stateGo: jest.fn(),
    router: {},
  };

  it('renders only the Quarantine tab (no Waivers tab)', () => {
    render(<FirewallContainerTabs {...props} />);

    const tabList = screen.getByRole('tablist');
    const tabs = within(tabList).getAllByRole('tab');

    expect(tabList).toBeInTheDocument();
    expect(tabs).toHaveLength(1);
    expect(tabs[0]).toHaveTextContent('Quarantine');
    expect(screen.queryByRole('tab', { name: /waivers/i })).not.toBeInTheDocument();
  });

  it('renders quarantine tab panel by default', () => {
    render(<FirewallContainerTabs {...props} />);

    const tabPanels = screen.getAllByRole('tabpanel');
    expect(tabPanels).toHaveLength(1);
    expect(tabPanels[0]).toHaveAttribute('id', 'firewall-container-quarantine-tab-panel');
  });
});
