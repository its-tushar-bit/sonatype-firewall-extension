/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
jest.mock('MainRoot/dashboard/results/dashboardResultsActions', () => ({
  ...jest.requireActual('MainRoot/dashboard/results/dashboardResultsActions'),
  loadWaiverResults: () => () => {},
}));

import React from 'react';
import { axiosMockAdapter, render, screen, within } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import FirewallWaiversPage from 'MainRoot/firewall/waiverRequests/FirewallWaiversPage';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import {
  getContainerImageAllRepositoriesWaiversUrl,
  getListPolicyWaiverRequestsUrl,
} from 'MainRoot/util/CLMLocation';

const mockComponentRequests = [
  { policyWaiverRequestId: 'req-1', scopeOwnerType: 'repository', scopeOwnerId: 'npm-central', status: 'REQUESTED' },
  { policyWaiverRequestId: 'req-2', scopeOwnerType: 'repository', scopeOwnerId: 'maven-central', status: 'REQUESTED' },
  { policyWaiverRequestId: 'req-3', scopeOwnerType: 'repository', scopeOwnerId: 'nuget-proxy', status: 'REQUESTED' },
];

const mockContainerRequests = [
  { policyWaiverRequestId: 'req-4', scopeOwnerType: 'all_repositories', scopeOwnerId: 'REPOSITORY_CONTAINER_ID', status: 'REQUESTED' },
  { policyWaiverRequestId: 'req-5', scopeOwnerType: 'all_repositories', scopeOwnerId: 'REPOSITORY_CONTAINER_ID', status: 'REQUESTED' },
];

const mockContainerWaivers = [
  { policyWaiverId: 'w-1', threatLevel: 8, policyName: 'Security-Critical', scopeOwnerId: 'app-1' },
  { policyWaiverId: 'w-2', threatLevel: 5, policyName: 'Security-High', scopeOwnerId: 'app-2' },
  { policyWaiverId: 'w-3', threatLevel: 3, policyName: 'License-Weak', scopeOwnerId: 'app-3' },
  { policyWaiverId: 'w-4', threatLevel: 2, policyName: 'Quality-Low', scopeOwnerId: 'app-4' },
];

const mockExistingComponentWaivers = [
  { ownerType: 'application', policyName: 'Security-Critical', threatLevel: 10 },
  { ownerType: 'application', policyName: 'Security-High', threatLevel: 7 },
  { ownerType: 'repository', policyName: 'License-Weak', threatLevel: 3 },
  { ownerType: 'application', policyName: 'Quality-Low', threatLevel: 2 },
  { ownerType: 'application', policyName: 'Security-Medium', threatLevel: 5 },
];

const componentSubTabState = {
  router: {
    currentState: { name: 'firewall.waivers.components.requested', data: { activeTab: 'components', activeSubTab: 'requested' } },
    currentParams: {},
  },
  firewallWaiverRequests: {
    loading: false,
    error: null,
    waiverRequests: [...mockComponentRequests, ...mockContainerRequests],
    requestModal: { isOpen: false, violationContext: null, isSubmitting: false, submitError: null },
    reviewPage: { loading: false, error: null, waiverRequest: null, isSubmitting: false, submitError: null, rejectionReason: '' },
    isSubmitting: false,
    submitError: null,
  },
  dashboard: {
    waivers: {
      results: mockExistingComponentWaivers,
      hasNextPage: false,
      error: null,
      sortFields: ['expiryTime'],
      hasMultiplePages: false,
      page: null,
    },
  },
};

const containerExistingTabState = {
  ...componentSubTabState,
  router: {
    currentState: { name: 'firewall.waivers.containers', data: { activeTab: 'containers', activeSubTab: 'existing' } },
    currentParams: {},
  },
  containerImageWaivers: {
    loading: false,
    error: null,
    waivers: mockContainerWaivers,
  },
};

const containerRequestedTabState = {
  ...componentSubTabState,
  router: {
    currentState: { name: 'firewall.waivers.containers.requested', data: { activeTab: 'containers', activeSubTab: 'requested' } },
    currentParams: {},
  },
  containerImageWaivers: {
    loading: false,
    error: null,
    waivers: mockContainerWaivers,
  },
};

describe('FirewallWaiversPage', () => {
  let stateGoSpy;
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');
    axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(200, mockContainerWaivers);
    axiosMock.onGet(getListPolicyWaiverRequestsUrl('organization', 'ROOT_ORGANIZATION_ID'))
      .reply(200, [...mockComponentRequests, ...mockContainerRequests]);
  });

  it('renders the page title "Waivers"', () => {
    render(<FirewallWaiversPage />, { preloadedState: componentSubTabState });
    expect(screen.getByRole('heading', { name: 'Waivers' })).toBeInTheDocument();
  });

  it('renders Components and Containers top-level tabs', () => {
    render(<FirewallWaiversPage />, { preloadedState: componentSubTabState });
    const tabList = screen.getAllByRole('tablist')[0];
    const tabs = within(tabList).getAllByRole('tab');
    expect(tabs[0]).toHaveTextContent('Components');
    expect(tabs[1]).toHaveTextContent('Containers');
  });

  it('renders Existing Waivers and Requested Waivers sub-tabs with counts inside Components tab', () => {
    render(<FirewallWaiversPage />, { preloadedState: componentSubTabState });
    expect(screen.getByRole('tab', { name: /existing waivers \(5\)/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /requested waivers \(3\)/i })).toBeInTheDocument();
  });

  it('navigates to components requested sub-tab on click', async () => {
    const user = userEvent.setup();
    render(<FirewallWaiversPage />, { preloadedState: componentSubTabState });
    await user.click(screen.getByRole('tab', { name: /requested waivers \(3\)/i }));
    expect(stateGoSpy).toHaveBeenCalledWith('firewall.waivers.components.requested');
  });

  it('navigates to components approved sub-tab on click', async () => {
    const user = userEvent.setup();
    render(<FirewallWaiversPage />, { preloadedState: componentSubTabState });
    await user.click(screen.getByRole('tab', { name: /existing waivers \(5\)/i }));
    expect(stateGoSpy).toHaveBeenCalledWith('firewall.waivers.components.approved');
  });

  it('navigates to Containers top-level tab on click', async () => {
    const user = userEvent.setup();
    render(<FirewallWaiversPage />, { preloadedState: componentSubTabState });
    const topLevelTabs = within(screen.getAllByRole('tablist')[0]).getAllByRole('tab');
    await user.click(topLevelTabs[1]); // Containers
    expect(stateGoSpy).toHaveBeenCalledWith('firewall.waivers.containers');
  });

  it('navigates to Components top-level tab on click when on Containers tab', async () => {
    const user = userEvent.setup();
    render(<FirewallWaiversPage />, { preloadedState: containerExistingTabState });
    const topLevelTabs = within(screen.getAllByRole('tablist')[0]).getAllByRole('tab');
    await user.click(topLevelTabs[0]); // Components
    expect(stateGoSpy).toHaveBeenCalledWith('firewall.waivers.components');
  });

  describe('Containers tab', () => {
    it('renders Existing Waivers and Requested Waivers sub-tabs with counts inside Containers tab', () => {
      render(<FirewallWaiversPage />, { preloadedState: containerExistingTabState });
      const tabLists = screen.getAllByRole('tablist');
      const subTabs = within(tabLists[1]).getAllByRole('tab');
      expect(subTabs[0]).toHaveTextContent('Existing Waivers (4)');
      expect(subTabs[1]).toHaveTextContent('Requested Waivers (2)');
    });

    it('shows ContainerImageWaiversTable on Containers > Existing Waivers tab', async () => {
      render(<FirewallWaiversPage />, { preloadedState: containerExistingTabState });
      expect(await screen.findByRole('table')).toBeInTheDocument();
    });

    it('navigates to containers existing sub-tab on click', async () => {
      const user = userEvent.setup();
      render(<FirewallWaiversPage />, { preloadedState: containerRequestedTabState });
      const tabLists = screen.getAllByRole('tablist');
      const subTabs = within(tabLists[1]).getAllByRole('tab');
      await user.click(subTabs[0]); // Existing Waivers
      expect(stateGoSpy).toHaveBeenCalledWith('firewall.waivers.containers.approved');
    });

    it('navigates to containers requested sub-tab on click', async () => {
      const user = userEvent.setup();
      render(<FirewallWaiversPage />, { preloadedState: containerExistingTabState });
      const tabLists = screen.getAllByRole('tablist');
      const subTabs = within(tabLists[1]).getAllByRole('tab');
      await user.click(subTabs[1]); // Requested Waivers
      expect(stateGoSpy).toHaveBeenCalledWith('firewall.waivers.containers.requested');
    });

    it('highlights Containers tab as active when on a containers state', () => {
      render(<FirewallWaiversPage />, { preloadedState: containerExistingTabState });
      const topLevelTabs = within(screen.getAllByRole('tablist')[0]).getAllByRole('tab');
      expect(topLevelTabs[1]).toHaveAttribute('aria-selected', 'true');
    });

    it('highlights Components tab as active when on a components state', () => {
      render(<FirewallWaiversPage />, { preloadedState: componentSubTabState });
      const topLevelTabs = within(screen.getAllByRole('tablist')[0]).getAllByRole('tab');
      expect(topLevelTabs[0]).toHaveAttribute('aria-selected', 'true');
    });
  });

  describe('limited firewall access', () => {
    const limitedAccessState = {
      ...componentSubTabState,
      firewall: { showLimitedFirewallAccessAlert: true },
    };

    it('renders the LimitedFirewallAccessAlert banner under the title', () => {
      render(<FirewallWaiversPage />, { preloadedState: limitedAccessState });
      expect(
        screen.getByText(/You have limited access to Repository Firewall based on your current permissions/i)
      ).toBeInTheDocument();
    });

    it('shows the required-access error inside the Components tab and hides sub-tabs/tables', () => {
      render(<FirewallWaiversPage />, { preloadedState: limitedAccessState });
      expect(
        screen.getByText(/An error occurred loading data\. Don.t have required access to access waivers\./i)
      ).toBeInTheDocument();
      expect(screen.queryByRole('tab', { name: /existing waivers/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('tab', { name: /requested waivers/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('shows the required-access error inside the Containers tab and hides sub-tabs/tables', () => {
      render(<FirewallWaiversPage />, {
        preloadedState: { ...containerExistingTabState, firewall: { showLimitedFirewallAccessAlert: true } },
      });
      expect(
        screen.getByText(/An error occurred loading data\. Don.t have required access to access waivers\./i)
      ).toBeInTheDocument();
      expect(screen.queryByRole('tab', { name: /existing waivers/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('still shows top-level Components and Containers tabs', () => {
      render(<FirewallWaiversPage />, { preloadedState: limitedAccessState });
      const tabList = screen.getAllByRole('tablist')[0];
      const tabs = within(tabList).getAllByRole('tab');
      expect(tabs[0]).toHaveTextContent('Components');
      expect(tabs[1]).toHaveTextContent('Containers');
    });
  });
});
