/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import FirewallBulkWaiveConfirmationPage from 'MainRoot/firewall/bulkWaive/bulkWaiveConfirmationPage/FirewallBulkWaiveConfirmationPage';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';
import * as firewallBulkWaiverActions from 'MainRoot/firewall/bulkWaive/firewallBulkWaiverActions';

describe('FirewallBulkWaiveConfirmationPage', () => {
  let mockRouterState, preloadedState, loadAllFilteredViolationsSpy;

  beforeEach(() => {
    mockRouterState = {
      href: jest.fn().mockImplementation((stateName) => {
        switch (stateName) {
          case 'firewall.bulkWaive':
            return '#/firewall/bulkWaive';
          case 'firewall.repository-report':
            return '#/firewall/repository/report';
          default:
            return '#/mocked-default-href';
        }
      }),
      get: jest.fn(),
      includes: jest.fn(),
    };
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue(mockRouterState);

    loadAllFilteredViolationsSpy = jest
      .spyOn(firewallBulkWaiverActions, 'loadAllFilteredViolations')
      .mockReturnValue({ type: 'LOAD_ALL_VIOLATIONS' });

    preloadedState = getDefaultPreloadedState();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('page rendering', () => {
    it('should render the confirmation page component', () => {
      const { container } = renderComponent();

      expect(container.querySelector('.fw-bulk-waiver-confirmation-page')).toBeInTheDocument();
    });
  });

  describe('waiver expiration display', () => {
    it('should display "Never" when expiryTime is "never"', () => {
      const stateWithNeverExpiry = {
        firewallBulkWaiver: {
          ...preloadedState.firewallBulkWaiver,
          waiverConfiguration: {
            ...preloadedState.firewallBulkWaiver.waiverConfiguration,
            expiryTime: 'never',
          },
        },
      };

      renderComponent(stateWithNeverExpiry);

      expect(screen.getByText('Never')).toBeInTheDocument();
    });
  });

  describe('filter handling', () => {
    it('should load all violations when selectAllMode is true and no active filters', () => {
      const stateWithSelectAll = {
        firewallBulkWaiver: {
          ...preloadedState.firewallBulkWaiver,
          selectAllMode: true,
          sourceContext: {
            source: 'repository-report',
          },
        },
        repositoryResultsSummaryPage: {
          ...preloadedState.repositoryResultsSummaryPage,
          componentsRequestBody: {
            ...preloadedState.repositoryResultsSummaryPage.componentsRequestBody,
            searchFilters: [],
            matchStateFilters: [],
            violationStateFilters: [],
            threatLevelFilters: [0, 10],
          },
        },
        router: preloadedState.router,
      };

      renderComponent(stateWithSelectAll);

      expect(loadAllFilteredViolationsSpy).toHaveBeenCalledWith(
        'test-repo-id',
        stateWithSelectAll.repositoryResultsSummaryPage.componentsRequestBody
      );
    });

    it('should load all filtered violations when filters are active', () => {
      const stateWithFilters = {
        firewallBulkWaiver: {
          ...preloadedState.firewallBulkWaiver,
          selectAllMode: true,
          sourceContext: {
            source: 'repository-report',
          },
        },
        repositoryResultsSummaryPage: {
          ...preloadedState.repositoryResultsSummaryPage,
          componentsRequestBody: {
            ...preloadedState.repositoryResultsSummaryPage.componentsRequestBody,
            searchFilters: [{ field: 'POLICY_NAME', value: 'test' }],
            matchStateFilters: [],
            violationStateFilters: [],
            threatLevelFilters: [0, 10],
          },
        },
        router: preloadedState.router,
      };

      renderComponent(stateWithFilters);

      expect(loadAllFilteredViolationsSpy).toHaveBeenCalledWith(
        'test-repo-id',
        stateWithFilters.repositoryResultsSummaryPage.componentsRequestBody
      );
    });

    it('should NOT load all violations when source is component-details', () => {
      const stateWithComponentDetails = {
        firewallBulkWaiver: {
          ...preloadedState.firewallBulkWaiver,
          selectAllMode: true,
          sourceContext: {
            source: 'component-details',
            repositoryId: 'test-repo-id',
            componentIdentifier: 'test-component',
          },
        },
        repositoryResultsSummaryPage: {
          ...preloadedState.repositoryResultsSummaryPage,
          componentsRequestBody: {
            ...preloadedState.repositoryResultsSummaryPage.componentsRequestBody,
            searchFilters: [],
            matchStateFilters: [],
            violationStateFilters: [],
            threatLevelFilters: [0, 10],
          },
        },
        router: preloadedState.router,
      };

      renderComponent(stateWithComponentDetails);

      expect(loadAllFilteredViolationsSpy).not.toHaveBeenCalled();
    });

    it('should load all filtered violations when threat level filters are non-default', () => {
      const stateWithThreatFilters = {
        firewallBulkWaiver: {
          ...preloadedState.firewallBulkWaiver,
          selectAllMode: true,
          sourceContext: {
            source: 'repository-report',
          },
        },
        repositoryResultsSummaryPage: {
          ...preloadedState.repositoryResultsSummaryPage,
          componentsRequestBody: {
            ...preloadedState.repositoryResultsSummaryPage.componentsRequestBody,
            searchFilters: [],
            matchStateFilters: [],
            violationStateFilters: [],
            threatLevelFilters: [3, 10],
          },
        },
        router: preloadedState.router,
      };

      renderComponent(stateWithThreatFilters);

      expect(loadAllFilteredViolationsSpy).toHaveBeenCalledWith(
        'test-repo-id',
        stateWithThreatFilters.repositoryResultsSummaryPage.componentsRequestBody
      );
    });

    it('should display correct violations count with filters active', () => {
      const stateWithFilters = {
        firewallBulkWaiver: {
          ...preloadedState.firewallBulkWaiver,
          selectAllMode: true,
          selectedCount: 10,
        },
        repositoryResultsSummaryPage: {
          ...preloadedState.repositoryResultsSummaryPage,
          componentsRequestBody: {
            ...preloadedState.repositoryResultsSummaryPage.componentsRequestBody,
            searchFilters: [{ field: 'POLICY_NAME', value: 'test' }],
            matchStateFilters: [],
            violationStateFilters: [],
            threatLevelFilters: [0, 10],
          },
        },
        router: preloadedState.router,
      };

      renderComponent(stateWithFilters);

      // Should show the count from selectedCount when filters are active
      expect(screen.getByText(/10 total violations?/i)).toBeInTheDocument();
    });

    it('should exclude explicitly unchecked violations from select-all filtered results', () => {
      const stateWithUncheckedFilteredViolation = {
        firewallBulkWaiver: {
          ...preloadedState.firewallBulkWaiver,
          selectAllMode: true,
          selectedCount: 2,
          checkboxState: {
            v2: false,
          },
          allFilteredViolations: [
            { policyViolationId: 'v1', policyName: 'Policy 1', threatLevel: 10, componentDisplayText: 'Component 1' },
            { policyViolationId: 'v2', policyName: 'Policy 2', threatLevel: 8, componentDisplayText: 'Component 2' },
            { policyViolationId: 'v3', policyName: 'Policy 3', threatLevel: 7, componentDisplayText: 'Component 3' },
          ],
        },
        repositoryResultsSummaryPage: {
          ...preloadedState.repositoryResultsSummaryPage,
        },
        router: preloadedState.router,
      };

      renderComponent(stateWithUncheckedFilteredViolation);

      expect(screen.getByText(/2 total violations/i)).toBeInTheDocument();
      expect(screen.getByText('Critical')).toBeInTheDocument();
      expect(screen.getByText('Severe')).toBeInTheDocument();
    });
  });

  function renderComponent(additionalState = {}) {
    const finalState = { ...preloadedState, ...additionalState };
    return render(<FirewallBulkWaiveConfirmationPage />, { preloadedState: finalState });
  }

  function getDefaultPreloadedState() {
    return {
      firewallBulkWaiver: {
        selectedViolations: [
          { policyViolationId: 'v1', policyName: 'Policy 1', threatLevel: 10 },
          { policyViolationId: 'v2', policyName: 'Policy 2', threatLevel: 8 },
          { policyViolationId: 'v3', policyName: 'Policy 3', threatLevel: 7 },
        ],
        selectedCount: 3,
        selectAllMode: false,
        checkboxState: {},
        waiverConfiguration: {
          selectedWaiverScope: {
            id: 'repo-1',
            name: 'Test Repository',
            label: 'Repository',
          },
          componentMatcherStrategy: 'ALL_VERSIONS',
          expiryTime: '30',
          comments: 'Test comments for bulk waiver',
        },
        waiverReasons: null,
        submitting: false,
        submitSuccess: false,
        submitError: null,
        sourceContext: null,
        originalAggregateState: null,
        allFilteredViolations: [],
        loadingAllViolations: false,
        allViolationsError: null,
      },
      repositoryResultsSummaryPage: {
        repositoryInfo: {
          repositoryId: 'test-repo-id',
          repositoryName: 'Test Repository',
        },
        componentsRequestBody: {
          aggregate: false,
          searchFilters: [],
          matchStateFilters: [],
          violationStateFilters: [],
          threatLevelFilters: [0, 10],
        },
      },
      router: {
        currentState: {
          name: 'firewall.bulkWaiveConfirmation',
        },
        currentParams: {
          repositoryId: 'test-repo-id',
        },
      },
    };
  }
});
