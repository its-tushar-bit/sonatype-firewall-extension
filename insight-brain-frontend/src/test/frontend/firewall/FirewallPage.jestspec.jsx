/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { fireEvent, screen, within } from '@testing-library/react';

import { render } from 'TestRoot/SpecUtil';
import FirewallPage from 'MainRoot/firewall/FirewallPage';
import * as productFeatures from 'MainRoot/productFeatures/productFeaturesSelectors';

describe('FirewallPage', () => {
  const props = {
    loadQuarantineList: jest.fn(),
    loadFirewallData: jest.fn(),
    loadQuarantineGridError: '',
    loadedReleaseQuarantineSummary: false,
    setQuarantineGridPage: jest.fn(),
    setQuarantineGridSorting: jest.fn(),
    setQuarantineGridPolicyFilter: jest.fn(),
    setQuarantineGridComponentNameFilter: jest.fn(),
    setQuarantineGridRepositoryPublicIdFilter: jest.fn(),
    setQuarantineGridQuarantineTimeFilter: jest.fn(),
    setQuarantineGridPolicyFilterWithProprietaryNameConflict: jest.fn(),
    setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCode: jest.fn(),
    initializeWelcomeModal: jest.fn(),
    closeWelcomeModal: jest.fn(),
    loadedConfiguration: false,
    loadedQuarantineSummary: false,
    loadedQuarantineList: true,
    showWelcomeModal: false,
    isShowConfigurationModal: false,
    quarantineList: [],
    quarantinePageCount: 10,
    componentsAutoReleased: 0,
    componentsQuarantined: 0,
    namespaceAttacksBlocked: 0,
    safeVersionsSelected: 0,
    supplyChainAttacksBlocked: 0,
    waivedComponents: 0,
    totalComponentCount: 0,
    repositoryCount: 0,
    quarantineEnabledRepositoryCount: 0,
    policies: [],
    pageSize: 10,
    currentPage: 1,
    sortDir: '',
    sortField: '',
    filterPolicies: [],
    autoReleaseQuarantineCountMTD: '',
    filterComponentName: '',
    filterRepositoryPublicId: '',
    filterQuarantineTime: 7,
    lastUpdated: {},
    goToRepositoryComponentDetailsPage: jest.fn(),
    stateGo: jest.fn(),
    router: { currentParams: { roiEnabled: 'test' } },
  };

  beforeEach(() => {
    jest.spyOn(productFeatures, 'selectIsContainerImagesEvaluationEnabled').mockReturnValue(true);
  });

  it('renders tabs and Components tab panel by default', () => {
    render(<FirewallPage {...props} />);

    const tabList = screen.getByRole('tablist');
    const tabs = within(tabList).getAllByRole('tab');
    const tabPanels = screen.getAllByRole('tabpanel');

    expect(tabList).toBeInTheDocument();
    expect(tabs).toHaveLength(2);
    expect(tabs[0]).toHaveTextContent('Components');
    expect(tabs[1]).toHaveTextContent('Containers');
    expect(tabPanels).toHaveLength(1);
    expect(tabPanels[0]).toHaveAttribute('id', 'firewall-components-tab-panel');
  });

  it('renders Components tab panel when clicking on the components tab', () => {
    render(<FirewallPage {...props} />);

    const tabList = screen.getByRole('tablist');
    const tabs = within(tabList).getAllByRole('tab');

    fireEvent.click(tabs[0]);
    const tabPanels = screen.getAllByRole('tabpanel');

    expect(tabPanels).toHaveLength(1);
    expect(tabPanels[0]).toHaveAttribute('id', 'firewall-components-tab-panel');
  });

  it('renders Containers tab panel when clicking on the container tab', () => {
    render(<FirewallPage {...props} />);
    const tabList = screen.getByRole('tablist');
    const tabs = within(tabList).getAllByRole('tab');

    fireEvent.click(tabs[1]);
    const tabPanels = screen.getAllByRole('tabpanel');

    expect(tabPanels).toHaveLength(1);
    expect(tabPanels[0]).toHaveAttribute('id', 'firewall-containers-tab-panel');
  });

  it('does not render dashboard tab view when container images evaluation is disabled', () => {
    jest.spyOn(productFeatures, 'selectIsContainerImagesEvaluationEnabled').mockReturnValue(false);

    render(<FirewallPage {...props} />);
    const tabList = screen.queryByRole('tablist');
    const tabPanels = screen.queryAllByRole('tabpanel');

    expect(tabList).not.toBeInTheDocument();
    expect(tabPanels).toHaveLength(0);
  });
});
