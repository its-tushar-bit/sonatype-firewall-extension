/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from 'TestRoot/SpecUtil';
import FirewallBulkWaivePage from 'MainRoot/firewall/bulkWaive/bulkWaivePage/FirewallBulkWaivePage';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import * as repositorySummaryActions from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSlice';
import * as firewallActions from 'MainRoot/firewall/firewallActions';

describe('FirewallBulkWaivePage', () => {
  let mockRouterState, stateGoSpy, getRepositoryComponentsSpy;

  const mockViolationsPage1 = [
    {
      policyViolationId: 'v1',
      policyName: 'Policy 1',
      threatLevel: 10,
      pathname: '/path/to/component1',
      componentDisplayText: 'Component 1',
    },
    {
      policyViolationId: 'v2',
      policyName: 'Policy 2',
      threatLevel: 8,
      pathname: '/path/to/component2',
      componentDisplayText: 'Component 2',
    },
  ];

  const mockViolationsPage2 = [
    {
      policyViolationId: 'v3',
      policyName: 'Policy 3',
      threatLevel: 7,
      pathname: '/path/to/component3',
      componentDisplayText: 'Component 3',
    },
  ];

  beforeEach(() => {
    mockRouterState = {
      href: jest.fn().mockImplementation((stateName) => {
        switch (stateName) {
          case 'firewall.bulkWaiveConfiguration':
            return '#/firewall/bulkWaiveConfiguration';
          case 'firewall.repository-report':
            return '#/firewall/repository-report';
          default:
            return '#/mocked-default-href';
        }
      }),
      get: jest.fn(),
      includes: jest.fn(),
    };
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue(mockRouterState);

    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');

    // Mock Redux actions to prevent actual API calls
    getRepositoryComponentsSpy = jest
      .spyOn(repositorySummaryActions.actions, 'getRepositoryComponentsForBulkWaive')
      .mockReturnValue({ type: 'GET_COMPONENTS' });
    jest.spyOn(repositorySummaryActions.actions, 'getRepositoryInformation').mockReturnValue({ type: 'GET_INFO' });
    jest.spyOn(repositorySummaryActions.actions, 'getRepositorySummary').mockReturnValue({ type: 'GET_SUMMARY' });
    jest.spyOn(repositorySummaryActions.actions, 'setPageSize').mockReturnValue({ type: 'SET_PAGE_SIZE' });
    jest.spyOn(repositorySummaryActions.actions, 'toggleAggregate').mockReturnValue({ type: 'TOGGLE_AGGREGATE' });
    jest.spyOn(repositorySummaryActions.actions, 'increasePage').mockReturnValue({ type: 'INCREASE_PAGE' });
    jest.spyOn(repositorySummaryActions.actions, 'decreasePage').mockReturnValue({ type: 'DECREASE_PAGE' });

    // Mock firewall actions for component-details source
    jest.spyOn(firewallActions, 'loadComponentPolicyViolations').mockReturnValue({ type: 'LOAD_COMPONENT_VIOLATIONS' });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('page rendering', () => {
    it('should render the bulk waive page component', () => {
      const { container } = renderComponent();

      expect(container.querySelector('.fw-bulk-waive-page')).toBeInTheDocument();
    });

    it('should display the correct header title', () => {
      renderComponent();

      expect(screen.getByText('Choose violations to Waive')).toBeInTheDocument();
    });

    it('should display selected count', () => {
      renderComponent();

      expect(screen.getByText('0 violations selected')).toBeInTheDocument();
    });
  });

  describe('checkbox selection', () => {
    it('should update selected count when checking a violation', async () => {
      const user = userEvent.setup();
      renderComponent();

      const firstCheckbox = screen.getAllByRole('checkbox')[1]; // Skip "select all" checkbox
      await user.click(firstCheckbox);

      expect(screen.getByText('1 violation selected')).toBeInTheDocument();
    });

    it('should update selected count when checking multiple violations', async () => {
      const user = userEvent.setup();
      renderComponent();

      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[1]); // First violation
      await user.click(checkboxes[2]); // Second violation

      expect(screen.getByText('2 violations selected')).toBeInTheDocument();
    });

    it('should decrease count when unchecking a violation', async () => {
      const user = userEvent.setup();
      renderComponent();

      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[1]); // Check
      await user.click(checkboxes[2]); // Check
      expect(screen.getByText('2 violations selected')).toBeInTheDocument();

      await user.click(checkboxes[1]); // Uncheck
      expect(screen.getByText('1 violation selected')).toBeInTheDocument();
    });
  });

  describe('select all functionality', () => {
    it('should select all violations when clicking select-all checkbox', async () => {
      const user = userEvent.setup();
      renderComponent();

      const selectAllCheckbox = screen.getAllByRole('checkbox')[0];
      await user.click(selectAllCheckbox);

      expect(screen.getByText('2 violations selected')).toBeInTheDocument();
    });

    it('should deselect all violations when clicking select-all checkbox twice', async () => {
      const user = userEvent.setup();
      renderComponent();

      const selectAllCheckbox = screen.getAllByRole('checkbox')[0];
      await user.click(selectAllCheckbox); // Select all
      await user.click(selectAllCheckbox); // Deselect all

      expect(screen.getByText('0 violations selected')).toBeInTheDocument();
    });

    it('should properly store all violations when clicking Next in selectAllMode', async () => {
      const user = userEvent.setup();
      const { store } = renderComponent({
        violations: mockViolationsPage1,
      });

      // Click select all
      const selectAllCheckbox = screen.getAllByRole('checkbox')[0];
      await user.click(selectAllCheckbox);
      expect(screen.getByText('2 violations selected')).toBeInTheDocument();

      // Click Next
      const nextButton = screen.getByRole('button', { name: 'Next' });
      await user.click(nextButton);

      // Verify all violations are stored in Redux
      const state = store.getState();
      expect(state.firewallBulkWaiver.selectedViolations).toHaveLength(2);
      expect(state.firewallBulkWaiver.selectedViolations.map((v) => v.policyViolationId)).toEqual(
        expect.arrayContaining(['v1', 'v2'])
      );
      expect(state.firewallBulkWaiver.selectAllMode).toBe(true);
    });

    it('should exclude explicitly unchecked violations in selectAllMode', async () => {
      const user = userEvent.setup();
      const { store } = renderComponent({
        violations: mockViolationsPage1,
      });

      // Click select all
      const selectAllCheckbox = screen.getAllByRole('checkbox')[0];
      await user.click(selectAllCheckbox);
      expect(screen.getByText('2 violations selected')).toBeInTheDocument();

      // Uncheck v2
      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[2]); // v2
      expect(screen.getByText('1 violation selected')).toBeInTheDocument();

      // Click Next
      const nextButton = screen.getByRole('button', { name: 'Next' });
      await user.click(nextButton);

      // Verify only v1 is stored (v2 was explicitly unchecked)
      const state = store.getState();
      expect(state.firewallBulkWaiver.selectedViolations).toHaveLength(1);
      expect(state.firewallBulkWaiver.selectedViolations[0].policyViolationId).toBe('v1');
    });

    it('should properly store violations from component-details source in selectAllMode', async () => {
      const user = userEvent.setup();
      const customState = getDefaultPreloadedState({
        violations: mockViolationsPage1,
      });
      customState.firewallBulkWaiver.sourceContext = {
        source: 'component-details',
        repositoryId: 'test-repo-id',
        componentIdentifier: 'test-component',
        componentHash: 'test-hash',
        matchState: 'identified',
        pathname: '/test/path',
        componentDisplayName: 'Test Component',
      };

      // For component-details source, violations come from firewall state
      customState.firewall = {
        componentDetailsPage: {
          policyViolations: mockViolationsPage1,
          isLoadingPolicyViolations: false,
          policyViolationsError: null,
        },
      };

      const { store } = render(<FirewallBulkWaivePage />, { preloadedState: customState });

      // Click select all
      const selectAllCheckbox = screen.getAllByRole('checkbox')[0];
      await user.click(selectAllCheckbox);
      expect(screen.getByText('2 violations selected')).toBeInTheDocument();

      // Click Next
      const nextButton = screen.getByRole('button', { name: 'Next' });
      await user.click(nextButton);

      // Verify all violations are stored in Redux (component-details should store actual violations)
      const state = store.getState();
      expect(state.firewallBulkWaiver.selectedViolations).toHaveLength(2);
      expect(state.firewallBulkWaiver.selectedViolations.map((v) => v.policyViolationId)).toEqual(
        expect.arrayContaining(['v1', 'v2'])
      );
    });
  });

  describe('pagination selection bug fix', () => {
    it('should maintain correct count when selecting across pages', async () => {
      const user = userEvent.setup();
      // Start on page 2 with stored selections from page 1
      renderComponent({
        violations: mockViolationsPage2,
        currentPage: 2,
        hasMoreResults: false,
        storedSelectedViolations: mockViolationsPage1, // v1 and v2 from page 1
        storedCheckboxState: { v1: true, v2: true },
      });

      // Should show 2 violations already selected from page 1
      expect(screen.getByText('2 violations selected')).toBeInTheDocument();

      // Select v3 on page 2
      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[1]); // v3

      // Now should show 3 total
      expect(screen.getByText('3 violations selected')).toBeInTheDocument();
    });

    it('should correctly remove deselected items when going back to previous page', async () => {
      const user = userEvent.setup();
      const { rerender, store } = renderComponent({
        violations: mockViolationsPage1,
        currentPage: 2,
        hasMoreResults: false,
        storedSelectedViolations: [...mockViolationsPage1, ...mockViolationsPage2], // All 3 selected
        storedCheckboxState: { v1: true, v2: true, v3: true },
      });

      // Initially should show 3 selected (from previous page navigation)
      expect(screen.getByText('3 violations selected')).toBeInTheDocument();

      // Deselect one item (v2) on the current page
      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[2]); // Deselect v2

      expect(screen.getByText('2 violations selected')).toBeInTheDocument();

      // Click "Next" to go to configuration page
      const nextButton = screen.getByRole('button', { name: 'Next' });
      await user.click(nextButton);

      // Verify that only 2 violations are stored in Redux (v1 and v3, not v2)
      const state = store.getState();
      expect(state.firewallBulkWaiver.selectedViolations).toHaveLength(2);
      expect(state.firewallBulkWaiver.selectedViolations.map((v) => v.policyViolationId)).toEqual(
        expect.arrayContaining(['v1', 'v3'])
      );
      expect(state.firewallBulkWaiver.selectedViolations.map((v) => v.policyViolationId)).not.toContain('v2');
    });

    it('should filter out deselected items from current page when clicking Next', async () => {
      const user = userEvent.setup();
      const { store } = renderComponent({
        violations: mockViolationsPage1,
        currentPage: 1,
        hasMoreResults: true,
        storedSelectedViolations: mockViolationsPage2, // v3 selected from page 2
        storedCheckboxState: { v3: true },
      });

      // Select v1 on page 1
      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[1]); // v1
      expect(screen.getByText('2 violations selected')).toBeInTheDocument(); // v1 + v3

      // Click "Next"
      const nextButton = screen.getByRole('button', { name: 'Next' });
      await user.click(nextButton);

      // Should have 2 violations: v1 and v3
      const state = store.getState();
      expect(state.firewallBulkWaiver.selectedViolations).toHaveLength(2);
      expect(state.firewallBulkWaiver.selectedViolations.map((v) => v.policyViolationId)).toEqual(
        expect.arrayContaining(['v1', 'v3'])
      );
    });

    it('should not include unchecked items from current page when clicking Next', async () => {
      const user = userEvent.setup();
      const { store } = renderComponent({
        violations: mockViolationsPage1,
        currentPage: 1,
        hasMoreResults: false,
      });

      // Select only v1, not v2
      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[1]); // v1 only
      expect(screen.getByText('1 violation selected')).toBeInTheDocument();

      // Click "Next"
      const nextButton = screen.getByRole('button', { name: 'Next' });
      await user.click(nextButton);

      // Should have only v1
      const state = store.getState();
      expect(state.firewallBulkWaiver.selectedViolations).toHaveLength(1);
      expect(state.firewallBulkWaiver.selectedViolations[0].policyViolationId).toBe('v1');
    });
  });

  describe('navigation', () => {
    it('should navigate to configuration page when Next is clicked with selections', async () => {
      const user = userEvent.setup();
      renderComponent();

      // Select a violation
      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[1]);

      const nextButton = screen.getByRole('button', { name: 'Next' });
      await user.click(nextButton);

      expect(stateGoSpy).toHaveBeenCalledWith('firewall.bulkWaiveConfiguration', {
        repositoryId: 'test-repo-id',
      });
    });

    it('should disable Next button when no violations are selected', () => {
      renderComponent();

      const nextButton = screen.getByRole('button', { name: 'Next' });
      expect(nextButton).toBeDisabled();
    });

    it('should enable Next button when violations are selected', async () => {
      const user = userEvent.setup();
      renderComponent();

      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[1]);

      const nextButton = screen.getByRole('button', { name: 'Next' });
      expect(nextButton).not.toBeDisabled();
    });

    it('should navigate to repository report when Cancel is clicked', async () => {
      const user = userEvent.setup();
      renderComponent();

      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      await user.click(cancelButton);

      expect(stateGoSpy).toHaveBeenCalledWith('firewall.repository-report', {
        repositoryId: 'test-repo-id',
      });
    });

    it('should navigate to component details when Cancel is clicked from component-details source', async () => {
      const user = userEvent.setup();
      const customState = getDefaultPreloadedState({
        violations: mockViolationsPage1,
      });
      customState.firewallBulkWaiver.sourceContext = {
        source: 'component-details',
        repositoryId: 'test-repo-id',
        componentIdentifier: 'test-component',
        componentHash: 'test-hash',
        matchState: 'identified',
        pathname: '/test/path',
      };

      // For component-details source, violations come from firewall state
      customState.firewall = {
        componentDetailsPage: {
          policyViolations: mockViolationsPage1,
          isLoadingPolicyViolations: false,
          policyViolationsError: null,
        },
      };

      render(<FirewallBulkWaivePage />, { preloadedState: customState });

      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      await user.click(cancelButton);

      expect(stateGoSpy).toHaveBeenCalledWith('firewall.componentDetailsPage.violations', {
        repositoryId: 'test-repo-id',
        componentIdentifier: 'test-component',
        componentHash: 'test-hash',
        matchState: 'identified',
        pathname: '/test/path',
      });
    });

    it('should not navigate when Cancel is clicked from component-details source without repositoryId', async () => {
      const user = userEvent.setup();
      const customState = getDefaultPreloadedState({
        violations: mockViolationsPage1,
      });
      customState.firewallBulkWaiver.sourceContext = {
        source: 'component-details',
        repositoryId: null, // Missing repositoryId
        componentIdentifier: 'test-component',
      };

      // For component-details source, violations come from firewall state
      customState.firewall = {
        componentDetailsPage: {
          policyViolations: mockViolationsPage1,
          isLoadingPolicyViolations: false,
          policyViolationsError: null,
        },
      };

      render(<FirewallBulkWaivePage />, { preloadedState: customState });

      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      await user.click(cancelButton);

      // Should navigate to repository-report instead (fallback)
      expect(stateGoSpy).toHaveBeenCalledWith('firewall.repository-report', {
        repositoryId: 'test-repo-id',
      });
    });
  });

  describe('pagination controls', () => {
    it('should not show pagination for component-details source', () => {
      const customState = getDefaultPreloadedState({
        violations: mockViolationsPage1,
        currentPage: 2,
        hasMoreResults: true,
      });

      // Override source to component-details
      customState.firewallBulkWaiver.sourceContext = {
        source: 'component-details',
        repositoryId: 'test-repo-id',
        pathname: '/test/path',
      };

      // For component-details source, violations come from firewall state
      customState.firewall = {
        componentDetailsPage: {
          policyViolations: mockViolationsPage1,
          isLoadingPolicyViolations: false,
          policyViolationsError: null,
        },
      };

      render(<FirewallBulkWaivePage />, { preloadedState: customState });

      // Pagination should not show for component-details
      expect(screen.queryByLabelText('Previous page')).not.toBeInTheDocument();
      expect(screen.queryByLabelText('Next page')).not.toBeInTheDocument();
    });

    it('should not show pagination on first page without more results', () => {
      renderComponent({
        currentPage: 1,
        hasMoreResults: false,
      });

      expect(screen.queryByLabelText('Previous page')).not.toBeInTheDocument();
      expect(screen.queryByLabelText('Next page')).not.toBeInTheDocument();
    });
  });

  function renderComponent(overrides = {}) {
    const defaultState = getDefaultPreloadedState(overrides);

    return render(<FirewallBulkWaivePage />, { preloadedState: defaultState });
  }

  function getDefaultPreloadedState(overrides = {}) {
    const {
      violations = mockViolationsPage1,
      currentPage = 1,
      hasMoreResults = false,
      storedSelectedViolations = [],
      storedCheckboxState = {},
    } = overrides;

    return {
      firewallBulkWaiver: {
        selectedViolations: storedSelectedViolations,
        selectedCount: storedSelectedViolations.length,
        selectAllMode: false,
        checkboxState: storedCheckboxState,
        waiverConfiguration: {
          waiverReasonId: '',
          expiryTime: '',
          comments: '',
          componentMatcherStrategy: null,
          selectedWaiverScope: null,
        },
        waiverReasons: [],
        loadingWaiverReasons: false,
        waiverReasonsError: null,
        availableWaiverScopes: [],
        loadingWaiverScopes: false,
        waiverScopesError: null,
        selectedWaiverScope: null,
        allFilteredViolations: [],
        loadingAllViolations: false,
        allViolationsError: null,
        totalFilteredCount: 0,
        submitting: false,
        submitSuccess: false,
        submitError: null,
        sourceContext: {
          source: 'repository-report',
          repositoryId: 'test-repo-id',
          componentIdentifier: null,
          componentHash: null,
          matchState: null,
          tabId: null,
          pathname: null,
          componentDisplayName: null,
        },
        originalAggregateState: null,
      },
      repositoryResultsSummaryPage: {
        repositoryInfo: {
          repositoryId: 'test-repo-id',
          repositoryName: 'Test Repository',
        },
        repositoryComponents: violations,
        loadingRepositoryComponents: false,
        errorComponentsTable: null,
        currentPage,
        hasMoreResults,
        totalComponentCount: violations.length,
        filteredTotalCount: violations.length,
        searchFiltersValues: {
          POLICY_NAME: '',
          COMPONENT_COORDINATES: '',
        },
        selectedThreatLevelFilters: [0, 10],
        selectedViolationStateFilters: [],
        selectedMatchStateFilters: [],
        showFilterPopover: false,
        componentsRequestBody: {
          aggregate: false,
          searchFilters: [],
          matchStateFilters: [],
          violationStateFilters: [],
          threatLevelFilters: [0, 10],
          sortFields: [],
          page: currentPage,
        },
      },
      router: {
        currentState: {
          name: 'firewall.bulkWaive',
        },
        currentParams: {
          repositoryId: 'test-repo-id',
        },
      },
    };
  }
});
