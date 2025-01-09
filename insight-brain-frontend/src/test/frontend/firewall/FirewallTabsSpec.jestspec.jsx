/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import '@testing-library/jest-dom/extend-expect';
import { screen, fireEvent } from '@testing-library/react';

import { render } from 'TestRoot/SpecUtil';
import FirewallTabs from 'MainRoot/firewall/FirewallTabs';
import { QUARANTINE, WAIVERS } from 'MainRoot/firewall/firewallConstants';

describe('FirewallTabs', () => {
  const loadQuarantineList = jest.fn();
  const loadQuarantineGridError = '';
  const setQuarantineGridPage = jest.fn();
  const setQuarantineGridSorting = jest.fn();
  const setQuarantineGridPolicyFilter = jest.fn();
  const setQuarantineGridComponentNameFilter = jest.fn();
  const setQuarantineGridRepositoryPublicIdFilter = jest.fn();
  const loadedQuarantineList = true;
  const quarantineList = [];
  const quarantinePageCount = 10;
  const policies = [];
  const pageSize = 10;
  const currentPage = 1;
  const sortDir = '';
  const sortField = '';
  const filterPolicies = [];
  const filterComponentName = '';
  const filterRepositoryPublicId = '';
  const lastUpdated = {};
  const goToRepositoryComponentDetailsPage = jest.fn();
  const stateGo = jest.fn();

  const props = {
    loadQuarantineList,
    loadQuarantineGridError,
    setQuarantineGridPage,
    setQuarantineGridSorting,
    setQuarantineGridPolicyFilter,
    setQuarantineGridComponentNameFilter,
    setQuarantineGridRepositoryPublicIdFilter,
    loadedQuarantineList,
    quarantineList,
    quarantinePageCount,
    policies,
    pageSize,
    currentPage,
    sortDir,
    sortField,
    filterPolicies,
    filterComponentName,
    filterRepositoryPublicId,
    lastUpdated,
    goToRepositoryComponentDetailsPage,
    stateGo,
  };

  const quarantineTabTestId = `firewall-${QUARANTINE}-tab`;
  const quarantineTabPanelTestId = `firewall-${QUARANTINE}-tab-panel`;

  const waiversTabTestId = `firewall-${WAIVERS}-tab`;
  const waiversTabPanelTestId = `firewall-${WAIVERS}-tab-panel`;

  it('renders the FirewallTabs component correctly', () => {
    render(<FirewallTabs {...props} />);

    expect(screen.getByTestId(quarantineTabTestId)).toBeInTheDocument();
    expect(screen.getByTestId(waiversTabTestId)).toBeInTheDocument();
  });

  it('renders quarantine table when clicking on corresponding tab', () => {
    render(<FirewallTabs {...props} />);
    fireEvent.click(screen.getByTestId(quarantineTabTestId));

    expect(screen.getByTestId(quarantineTabPanelTestId)).toBeInTheDocument();
  });

  it('renders waivers table when clicking on corresponding tab', () => {
    render(<FirewallTabs {...props} />);
    fireEvent.click(screen.getByTestId(waiversTabTestId));

    expect(screen.getByTestId(waiversTabPanelTestId)).toBeInTheDocument();
  });
});
