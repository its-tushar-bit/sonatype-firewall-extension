/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { screen, fireEvent, within } from '@testing-library/react';

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
    loadContainerWaiverGridError: null,
    loadingContainerWaiverList: false,
    containerWaiverList: [],
    containerWaiverPageCount: 2,
    containerWaiverPageSize: 10,
    containerWaiverCurrentPage: 1,
    containerWaiverLastUpdated: null,
    loadContainerWaiverList: jest.fn(),
    setContainerWaiverGridPage: jest.fn(),
    stateGo: jest.fn(),
    router: {},
  };

  it('renders the FirewallContainerTabs component correctly', () => {
    render(<FirewallContainerTabs {...props} />);

    const tabList = screen.getByRole('tablist');
    const tabs = within(tabList).getAllByRole('tab');
    const tabPanels = screen.getAllByRole('tabpanel');

    expect(tabList).toBeInTheDocument();
    expect(tabs).toHaveLength(2);
    expect(tabs[0]).toHaveTextContent('Quarantine');
    expect(tabs[1]).toHaveTextContent('Waivers');
    expect(tabPanels).toHaveLength(1);
    expect(tabPanels[0]).toHaveAttribute('id', 'firewall-container-quarantine-tab-panel');
  });

  it('renders quarantine table when clicking on corresponding tab', () => {
    render(<FirewallContainerTabs {...props} />);

    const tabList = screen.getByRole('tablist');
    const tabs = within(tabList).getAllByRole('tab');

    fireEvent.click(tabs[0]);
    const tabPanels = screen.getAllByRole('tabpanel');

    expect(tabPanels).toHaveLength(1);
    expect(tabPanels[0]).toHaveAttribute('id', 'firewall-container-quarantine-tab-panel');
  });

  it('renders waivers table when clicking on corresponding tab', () => {
    const waiverProps = { ...props, router: { currentState: { data: { activeTab: 'waivers' } } } };
    render(<FirewallContainerTabs {...waiverProps} />);

    const tabPanels = screen.getAllByRole('tabpanel');

    expect(tabPanels).toHaveLength(1);
    expect(tabPanels[0]).toHaveAttribute('id', 'firewall-container-waivers-tab-panel');
  });
});
