/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { screen, fireEvent, within } from '@testing-library/react';

import { render } from 'TestRoot/SpecUtil';
import FirewallTabs from 'MainRoot/firewall/FirewallTabs';

describe('FirewallTabs', () => {
  const roiTabEnabled = { currentParams: { roiEnabled: 'test' } };
  const roiTabDisabled = { currentParams: { roiEnabled: undefined } };

  const props = {
    loadQuarantineList: jest.fn(),
    loadQuarantineGridError: '',
    setQuarantineGridPage: jest.fn(),
    setQuarantineGridSorting: jest.fn(),
    setQuarantineGridPolicyFilter: jest.fn(),
    setQuarantineGridComponentNameFilter: jest.fn(),
    setQuarantineGridRepositoryPublicIdFilter: jest.fn(),
    setQuarantineGridQuarantineTimeFilter: jest.fn(),
    loadedQuarantineList: true,
    quarantineList: [],
    quarantinePageCount: 10,
    policies: [],
    pageSize: 10,
    currentPage: 1,
    sortDir: '',
    sortField: '',
    filterPolicies: [],
    filterComponentName: '',
    filterRepositoryPublicId: '',
    filterQuarantineTime: 7,
    lastUpdated: {},
    goToRepositoryComponentDetailsPage: jest.fn(),
    stateGo: jest.fn(),
    router: roiTabEnabled,
  };

  const propsRoiTabDisabled = { ...props, router: roiTabDisabled };

  it('renders the FirewallTabs component correctly', () => {
    render(<FirewallTabs {...props} />);

    const tabList = screen.getByRole('tablist');
    const tabs = within(tabList).getAllByRole('tab');
    const tabPanels = screen.getAllByRole('tabpanel');

    expect(tabList).toBeInTheDocument();
    expect(tabs).toHaveLength(3);
    expect(tabs[0]).toHaveTextContent('Quarantine');
    expect(tabs[1]).toHaveTextContent('Waivers');
    expect(tabs[2]).toHaveTextContent('Return on InvestmentReturn on Investment');
    expect(tabPanels).toHaveLength(1);
    expect(tabPanels[0]).toHaveAttribute('id', 'firewall-quarantine-tab-panel');
  });

  it('does not render the ROI tab if roiEnabled param is undefined', () => {
    render(<FirewallTabs {...propsRoiTabDisabled} />);

    const tabList = screen.getByRole('tablist');
    const tabs = within(tabList).getAllByRole('tab');
    const tabPanels = screen.getAllByRole('tabpanel');

    expect(tabs).toHaveLength(2);
    expect(tabs[0]).toHaveTextContent('Quarantine');
    expect(tabs[1]).toHaveTextContent('Waivers');
    expect(tabPanels).toHaveLength(1);
    expect(tabPanels[0]).toHaveAttribute('id', 'firewall-quarantine-tab-panel');
  });

  it('renders quarantine table when clicking on corresponding tab', () => {
    render(<FirewallTabs {...props} />);

    const tabList = screen.getByRole('tablist');
    const tabs = within(tabList).getAllByRole('tab');

    fireEvent.click(tabs[0]);
    const tabPanels = screen.getAllByRole('tabpanel');

    expect(tabPanels).toHaveLength(1);
    expect(tabPanels[0]).toHaveAttribute('id', 'firewall-quarantine-tab-panel');
  });

  it('renders waivers table when clicking on corresponding tab', () => {
    const waiverProps = { ...props, router: { ...roiTabEnabled, currentState: { data: { activeTab: 'waivers' } } } };
    render(<FirewallTabs {...waiverProps} />);

    const tabPanels = screen.getAllByRole('tabpanel');

    expect(tabPanels).toHaveLength(1);
    expect(tabPanels[0]).toHaveAttribute('id', 'firewall-waivers-tab-panel');
  });

  it('renders ROI content when clicking on corresponding tab', () => {
    const roiProps = { ...props, router: { ...roiTabEnabled, currentState: { data: { activeTab: 'roi' } } } };
    render(<FirewallTabs {...roiProps} />);

    const tabPanels = screen.getAllByRole('tabpanel');

    expect(tabPanels).toHaveLength(1);
    expect(tabPanels[0]).toHaveAttribute('id', 'firewall-roi-tab-panel');
  });
});
