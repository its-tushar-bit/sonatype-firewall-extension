/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, waitFor, axiosMockAdapter } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import FirewallBulkWaiveConfigurationPage from 'MainRoot/firewall/bulkWaive/bulkWaiveConfigurationPage/FirewallBulkWaiveConfigurationPage';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import { actions as repositoryActions } from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSlice';
import { getPolicyWaiverReasonsUrl, getRepositoryInfoUrl } from 'MainRoot/util/CLMLocation';

// Mock FirewallScopeDropdown component
jest.mock(
  'MainRoot/firewall/bulkWaive/bulkWaiveConfigurationPage/firewallIqScopeDropdown/FirewallScopeDropdown',
  () => {
    return function MockScopeDropdown() {
      return <div data-testid="firewall-scope-dropdown">FirewallScopeDropdown</div>;
    };
  }
);

// Mock BulkWaiveTitle component
jest.mock('MainRoot/firewall/bulkWaive/bulkWaiveTitle/BulkWaiveTitle', () => {
  return function MockBulkWaiveTitle() {
    return <div data-testid="bulk-waive-title">Bulk Waive Title</div>;
  };
});

describe('FirewallBulkWaiveConfigurationPage', () => {
  let mockRouterState, preloadedState, stateGoSpy, toggleAggregateSpy, axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    // Mock all HTTP endpoints that the component calls
    axiosMock.onGet(getPolicyWaiverReasonsUrl()).reply(200, [
      { id: 'reason-1', name: 'False Positive' },
      { id: 'reason-2', name: 'Risk Accepted' },
    ]);

    axiosMock.onGet(getRepositoryInfoUrl('test-repo-id')).reply(200, {
      repositoryId: 'test-repo-id',
      repositoryName: 'Test Repository',
    });

    // Mock the owner context hierarchy endpoint - use regex to match any context ID
    axiosMock.onGet(/\/api\/v2\/repositories\/.*\/contexts\/.*\/hierarchy/).reply(200, {
      type: 'repository',
      id: 'repo-1',
      name: 'Test Repository',
      children: [],
    });

    mockRouterState = {
      href: jest.fn().mockImplementation((stateName) => {
        switch (stateName) {
          case 'firewall.bulkWaiveConfirmation':
            return '#/firewall/bulkWaive/confirmation';
          case 'firewall.repository-report':
            return '#/firewall/repository/report';
          case 'firewall.bulkWaive':
            return '#/firewall/bulkWaive';
          case 'firewall.componentDetailsPage.violations':
            return '#/firewall/component-details/violations';
          default:
            return '#/mocked-default-href';
        }
      }),
      get: jest.fn(),
      includes: jest.fn(),
    };
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue(mockRouterState);
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');
    toggleAggregateSpy = jest.spyOn(repositoryActions, 'toggleAggregate');

    preloadedState = getDefaultPreloadedState();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('page rendering', () => {
    it('should render the configuration page component', async () => {
      const { container } = await renderComponent();

      expect(container.querySelector('.fw-bulk-waiver-configuration-page')).toBeInTheDocument();
    });

    it('should display the count of selected violations in the header', async () => {
      await renderComponent();

      expect(screen.getByText(/Waiver configuration for 3 selected violations/i)).toBeInTheDocument();
    });

    it('should render bulk waive title', async () => {
      await renderComponent();

      expect(screen.getByTestId('bulk-waive-title')).toBeInTheDocument();
    });

    it('should render scope dropdown', async () => {
      await renderComponent();

      expect(screen.getByTestId('firewall-scope-dropdown')).toBeInTheDocument();
    });

    it('should render component matcher strategy radio buttons', async () => {
      await renderComponent();

      expect(screen.getByLabelText('Exact')).toBeInTheDocument();
      expect(screen.getByLabelText('All Versions')).toBeInTheDocument();
    });

    it('should render Cancel, Back, and Next buttons', async () => {
      await renderComponent();

      expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Back' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Next' })).toBeInTheDocument();
    });
  });

  describe('form validation - Next button state', () => {
    it('should disable Next button when no expiry time is selected', async () => {
      await renderComponent();

      const nextButton = screen.getByRole('button', { name: 'Next' });
      expect(nextButton).toBeDisabled();
    });
  });

  describe('component matcher strategy', () => {
    it('should have Exact selected by default', async () => {
      await renderComponent();

      const exactRadio = screen.getByLabelText('Exact');
      expect(exactRadio).toBeChecked();
    });

    it('should disable All Versions when onlyUnknownViolations is true', async () => {
      const stateWithUnknownViolations = {
        ...preloadedState,
        firewallBulkWaiver: {
          ...preloadedState.firewallBulkWaiver,
          onlyUnknownViolations: true,
        },
      };

      await renderComponent(stateWithUnknownViolations);

      const allVersionsRadio = screen.getByLabelText('All Versions');
      expect(allVersionsRadio).toBeDisabled();
    });
  });

  describe('cancel button navigation', () => {
    it('should navigate to repository-report when source is not component-details', async () => {
      const user = userEvent.setup();
      const stateWithAggregateChange = {
        ...preloadedState,
        firewallBulkWaiver: {
          ...preloadedState.firewallBulkWaiver,
          originalAggregateState: true, // Original was true
        },
        repositoryResultsSummaryPage: {
          ...preloadedState.repositoryResultsSummaryPage,
          componentsRequestBody: {
            ...preloadedState.repositoryResultsSummaryPage.componentsRequestBody,
            aggregate: false, // Current is false, so needs to toggle
          },
        },
      };

      await renderComponent(stateWithAggregateChange);

      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      await user.click(cancelButton);

      expect(toggleAggregateSpy).toHaveBeenCalled();
      expect(stateGoSpy).toHaveBeenCalledWith('firewall.repository-report', {
        repositoryId: 'test-repo-id',
      });
    });

    it('should clear violations state when cancel is clicked', async () => {
      const user = userEvent.setup();
      const { store } = await renderComponent();

      // Verify initial state has violations
      expect(store.getState().firewallBulkWaiver.selectedViolations.length).toBe(3);
      expect(store.getState().firewallBulkWaiver.selectedCount).toBe(3);

      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      await user.click(cancelButton);

      // Verify state was cleared
      const state = store.getState();
      expect(state.firewallBulkWaiver.selectedViolations).toEqual([]);
      expect(state.firewallBulkWaiver.selectedCount).toBe(0);
      expect(state.firewallBulkWaiver.selectAllMode).toBe(false);
    });
  });

  describe('back button navigation', () => {
    it('should navigate to bulk waive page', async () => {
      const user = userEvent.setup();
      await renderComponent();

      const backButton = screen.getByRole('button', { name: 'Back' });
      await user.click(backButton);

      expect(stateGoSpy).toHaveBeenCalledWith('firewall.bulkWaive', {
        repositoryId: 'test-repo-id',
      });
    });

    it('should toggle aggregate when needed to restore original state', async () => {
      const user = userEvent.setup();
      const stateWithAggregateChange = {
        ...preloadedState,
        firewallBulkWaiver: {
          ...preloadedState.firewallBulkWaiver,
          originalAggregateState: true, // Original was true
        },
        repositoryResultsSummaryPage: {
          ...preloadedState.repositoryResultsSummaryPage,
          componentsRequestBody: {
            ...preloadedState.repositoryResultsSummaryPage.componentsRequestBody,
            aggregate: false, // Current is false, so needs to toggle
          },
        },
      };

      await renderComponent(stateWithAggregateChange);

      const backButton = screen.getByRole('button', { name: 'Back' });
      await user.click(backButton);

      expect(toggleAggregateSpy).toHaveBeenCalled();
    });

    it('should NOT toggle aggregate when already in correct state', async () => {
      const user = userEvent.setup();
      const stateWithSameAggregate = {
        ...preloadedState,
        firewallBulkWaiver: {
          ...preloadedState.firewallBulkWaiver,
          originalAggregateState: false, // Original was false
        },
        repositoryResultsSummaryPage: {
          ...preloadedState.repositoryResultsSummaryPage,
          componentsRequestBody: {
            ...preloadedState.repositoryResultsSummaryPage.componentsRequestBody,
            aggregate: false, // Current is also false, no toggle needed
          },
        },
      };

      await renderComponent(stateWithSameAggregate);

      const backButton = screen.getByRole('button', { name: 'Back' });
      await user.click(backButton);

      expect(toggleAggregateSpy).not.toHaveBeenCalled();
    });

    it('should NOT toggle aggregate when source is component-details', async () => {
      const user = userEvent.setup();
      const stateWithComponentDetails = {
        ...preloadedState,
        firewallBulkWaiver: {
          ...preloadedState.firewallBulkWaiver,
          originalAggregateState: true,
          sourceContext: {
            source: 'component-details',
            repositoryId: 'test-repo-id',
          },
        },
      };

      await renderComponent(stateWithComponentDetails);

      const backButton = screen.getByRole('button', { name: 'Back' });
      await user.click(backButton);

      expect(toggleAggregateSpy).not.toHaveBeenCalled();
    });
  });

  describe('Cancel button navigation', () => {
    it('should toggle aggregate in Cancel when needed to restore original state', async () => {
      const user = userEvent.setup();
      const stateWithAggregateChange = {
        ...preloadedState,
        firewallBulkWaiver: {
          ...preloadedState.firewallBulkWaiver,
          originalAggregateState: true, // Original was true
        },
        repositoryResultsSummaryPage: {
          ...preloadedState.repositoryResultsSummaryPage,
          componentsRequestBody: {
            ...preloadedState.repositoryResultsSummaryPage.componentsRequestBody,
            aggregate: false, // Current is false, so needs to toggle
          },
        },
      };

      await renderComponent(stateWithAggregateChange);

      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      await user.click(cancelButton);

      expect(toggleAggregateSpy).toHaveBeenCalled();
    });

    it('should NOT toggle aggregate in Cancel when already in correct state', async () => {
      const user = userEvent.setup();
      const stateWithSameAggregate = {
        ...preloadedState,
        firewallBulkWaiver: {
          ...preloadedState.firewallBulkWaiver,
          originalAggregateState: false, // Original was false
        },
        repositoryResultsSummaryPage: {
          ...preloadedState.repositoryResultsSummaryPage,
          componentsRequestBody: {
            ...preloadedState.repositoryResultsSummaryPage.componentsRequestBody,
            aggregate: false, // Current is also false, no toggle needed
          },
        },
      };

      await renderComponent(stateWithSameAggregate);

      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      await user.click(cancelButton);

      expect(toggleAggregateSpy).not.toHaveBeenCalled();
    });
  });

  describe('mixed violations alert', () => {
    it('should not show alert when hasMixedViolations is false', async () => {
      await renderComponent();

      expect(
        screen.queryByText(/Only exact component waivers can be created for unknown violations/i)
      ).not.toBeInTheDocument();
    });
  });

  // Helper functions
  async function renderComponent(additionalState = {}) {
    const finalState = { ...preloadedState, ...additionalState };
    const result = render(<FirewallBulkWaiveConfigurationPage />, { preloadedState: finalState });

    // Wait for the component to finish loading by checking for form elements to appear
    await waitFor(
      () => {
        // Wait for the exact component radio button to appear (always rendered)
        expect(screen.getByLabelText('Exact')).toBeInTheDocument();
        // Ensure loading spinner is gone
        expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
      },
      { timeout: 5000 }
    );

    return result;
  }

  function getDefaultPreloadedState() {
    return {
      firewallBulkWaiver: {
        selectedViolations: [
          { policyViolationId: 'v1', policyName: 'Policy 1' },
          { policyViolationId: 'v2', policyName: 'Policy 2' },
          { policyViolationId: 'v3', policyName: 'Policy 3' },
        ],
        selectedCount: 3,
        checkboxState: {
          v1: true,
          v2: true,
          v3: true,
        },
        selectAllMode: false,
        waiverConfiguration: null,
        waiverReasons: [
          { id: 'reason-1', name: 'False Positive' },
          { id: 'reason-2', name: 'Risk Accepted' },
        ],
        loadingWaiverReasons: false,
        waiverReasonsError: null,
        selectedWaiverScope: {
          id: 'repo-1',
          label: 'Repository',
          name: 'Test Repository',
        },
        availableWaiverScopes: [
          {
            id: 'repo-1',
            label: 'Repository',
            name: 'Test Repository',
          },
        ],
        onlyUnknownViolations: false,
        hasMixedViolations: false,
        source: null,
        sourceContext: null,
        originalAggregateState: null,
      },
      repositoryResultsSummaryPage: {
        repositoryInfo: {
          repositoryId: 'test-repo-id',
          repositoryName: 'Test Repository',
        },
        componentsRequestBody: {
          page: 1,
          pageSize: 12,
          searchFilters: [],
          sortFields: [],
          aggregate: true,
          matchStateFilters: [],
          violationStateFilters: [],
          threatLevelFilters: [0, 10],
        },
        repositoryComponents: [],
        loadingRepositoryComponents: false,
        errorComponentsTable: null,
        hasMoreResults: false,
      },
      router: {
        currentState: {
          name: 'firewall.bulkWaiveConfiguration',
        },
        currentParams: {
          repositoryId: 'test-repo-id',
        },
      },
    };
  }
});
