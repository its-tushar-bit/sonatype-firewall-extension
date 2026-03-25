/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, fireEvent, render, screen, waitFor } from 'TestRoot/SpecUtil';
import BulkWaivePage from 'MainRoot/waivers/BulkWaivePage';
import { getPolicyWaiverReasonsUrl } from 'MainRoot/util/CLMLocation';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';

// Mock components that require additional setup (to hide irrelevant error logs)
jest.mock('MainRoot/componentDetails/ViolationsTableTile/PolicyViolationDetailsPopover', () => {
  const PolicyViolationDetailsPopover = () => {
    return <div>PolicyViolationDetailsPopover</div>;
  };

  return PolicyViolationDetailsPopover;
});

jest.mock('MainRoot/applicationReport/ReportFilterPopover', () => {
  const ReportFilterPopover = () => {
    return <div>ReportFilterPopover</div>;
  };

  return ReportFilterPopover;
});

describe('BulkWaivePage component', () => {
  let axiosMock, allEntries, preloadedState, routerContextMock, stateGoSpy;

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
    allEntries = getDefaultAllEntriesDataForTest();
    preloadedState = getDefaultPreloadedState(allEntries);
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');

    // Mock waiver reasons API call
    axiosMock.onGet(getPolicyWaiverReasonsUrl()).reply(200, []);

    routerContextMock = {
      href: jest.fn().mockImplementation(() => '#/dashboard/violations'),
      get: jest.fn().mockImplementation((state) => state),
      includes: jest.fn(),
    };

    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue(routerContextMock);
  });

  it('renders the page title', () => {
    renderComponent();

    const pageTitle = screen.getByRole('heading', { name: 'Bulk Waiver' });
    expect(pageTitle).toBeVisible();
  });

  it('renders the section title', () => {
    renderComponent();

    const sectionTitle = screen.getByRole('heading', { name: 'Choose violations to Waive' });
    expect(sectionTitle).toBeVisible();
  });

  it('renders a Filter button', () => {
    renderComponent();

    const filterButton = screen.getByRole('button', { name: 'Filter' });
    expect(filterButton).toBeVisible();
  });

  it('renders table with correct headers when in non-CDP mode', () => {
    renderComponent();

    expect(screen.getByRole('table')).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Threat' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Policy' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Component' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Condition' })).toBeVisible();
  });

  it('renders table with correct headers when in CDP mode', () => {
    const stateWithCdpBulkWaive = {
      ...preloadedState,
      router: {
        ...preloadedState.router,
        currentParams: {
          scanId: 'test-scan-id',
          hash: 'test-hash',
        },
        currentState: {
          ...preloadedState.router.currentState,
          name: 'applicationReport.cdpBulkWaive',
        },
      },
    };
    renderComponent(stateWithCdpBulkWaive);

    expect(screen.getByRole('table')).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Threat' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Policy' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Constraint' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Condition' })).toBeVisible();
  });

  it('renders sortable column headers as clickable buttons', () => {
    renderComponent();

    // Check that sortable columns have clickable sort buttons
    // Note: Without router initialization, all columns start as "unsorted"
    const threatSortButton = screen.getByRole('button', { name: 'Threat unsorted' });
    const policySortButton = screen.getByRole('button', { name: 'Policy unsorted' });
    const componentSortButton = screen.getByRole('button', { name: 'Component unsorted' });

    expect(threatSortButton).toBeVisible();
    expect(policySortButton).toBeVisible();
    expect(componentSortButton).toBeVisible();
  });

  it('renders table structure', () => {
    renderComponent();

    const rows = screen.getAllByRole('row');
    // Should have at least header row and filter row
    expect(rows.length).toBeGreaterThanOrEqual(2);

    // Table should be present and functional
    expect(screen.getByRole('table')).toBeVisible();
  });

  it('displays "No Results" when there are no displayed entries', () => {
    // Create state with empty displayedEntries
    const stateWithNoEntries = {
      ...preloadedState,
      applicationReport: {
        ...preloadedState.applicationReport,
        selectedReport: {
          allEntries: [],
          displayedEntries: [],
        },
      },
    };

    renderComponent(stateWithNoEntries);

    // Should display "No Results" text
    expect(screen.getByText('No Results')).toBeVisible();

    // Should not have any data rows
    const rows = screen.getAllByRole('row');
    // Should only have header row, filter row, and "No Results" row
    expect(rows.length).toBe(3);
  });

  it('displays "No Results" when all entries are filtered out', () => {
    const allEntries = getDefaultAllEntriesDataForTest();

    // Create state where all entries exist but none are displayed (filtered out)
    const stateWithFilteredOutEntries = {
      ...preloadedState,
      applicationReport: {
        ...preloadedState.applicationReport,
        selectedReport: {
          allEntries: allEntries,
          displayedEntries: [], // All entries filtered out
        },
      },
    };

    renderComponent(stateWithFilteredOutEntries);

    // Should display "No Results" text
    expect(screen.getByText('No Results')).toBeVisible();

    // Should not have any data rows
    const rows = screen.getAllByRole('row');
    // Should only have header row, filter row, and "No Results" row
    expect(rows.length).toBe(3);
  });

  it('renders filter inputs in the table', () => {
    renderComponent();

    const policyNameFilter = screen.getByPlaceholderText('policy name');
    const componentNameFilter = screen.getByPlaceholderText('component name');

    expect(policyNameFilter).toBeVisible();
    expect(componentNameFilter).toBeVisible();
  });

  it('renders Cancel and Next buttons', () => {
    renderComponent();

    const cancelButton = screen.getByRole('button', { name: 'Cancel' });
    const nextButton = screen.getByRole('button', { name: 'Next' });

    expect(cancelButton).toBeVisible();
    expect(nextButton).toBeVisible();
  });

  it('disables Next button when no violations are selected', () => {
    renderComponent();

    const nextButton = screen.getByRole('button', { name: 'Next' });
    expect(nextButton).toBeDisabled();
  });

  it('renders all row checkboxes unchecked', () => {
    renderComponent();

    const checkboxes = screen.getAllByRole('checkbox');
    // Should have at least one checkbox (header checkbox)
    expect(checkboxes.length).toBeGreaterThanOrEqual(1);

    // All checkboxes should start unchecked
    // Note: checkboxes[0] is the header checkbox, checkboxes[1-3] are row checkboxes
    expect(checkboxes[0]).not.toBeChecked();
    expect(checkboxes[1]).not.toBeChecked();
    expect(checkboxes[2]).not.toBeChecked();
    expect(checkboxes[3]).not.toBeChecked();
  });

  it('enables Next button when violations are selected', () => {
    // Start with some violations selected
    const stateWithSelections = {
      waivers: {
        ...preloadedState.waivers,
        bulkWaive: {
          ...preloadedState.waivers.bulkWaive,
          checkboxState: {
            'violation-id-1': true,
            'violation-id-2': true,
          },
        },
      },
    };

    renderComponent(stateWithSelections);

    const nextButton = screen.getByRole('button', { name: 'Next' });
    expect(nextButton).toBeVisible();
    expect(nextButton).not.toBeDisabled();
  });

  describe('selection count messages', () => {
    it('displays correct singular selection count message', () => {
      const stateWithOneSelection = {
        waivers: {
          ...preloadedState.waivers,
          bulkWaive: {
            ...preloadedState.waivers.bulkWaive,
            checkboxState: {
              'violation-id-1': true,
            },
          },
        },
      };

      renderComponent(stateWithOneSelection);

      expect(screen.getByText('1 violation selected')).toBeVisible();
    });

    it('displays correct plural selection count message', () => {
      const stateWithMultipleSelections = {
        waivers: {
          ...preloadedState.waivers,
          bulkWaive: {
            ...preloadedState.waivers.bulkWaive,
            checkboxState: {
              'violation-id-1': true,
              'violation-id-2': true,
              'violation-id-3': true,
            },
          },
        },
      };

      renderComponent(stateWithMultipleSelections);

      expect(screen.getByText('3 violations selected')).toBeVisible();
    });

    it('displays hidden count when selected violations are filtered out', () => {
      // Create a state where some violations are selected but filtered out
      const filteredEntries = [
        allEntries[0], // only show first violation
      ];

      const stateWithHiddenSelections = {
        waivers: {
          ...preloadedState.waivers,
          bulkWaive: {
            ...preloadedState.waivers.bulkWaive,
            checkboxState: {
              'violation-id-1': true, // This one is visible
              'violation-id-2': true, // This one is hidden by filter
              'violation-id-3': true, // This one is hidden by filter
            },
          },
        },
        applicationReport: {
          ...preloadedState.applicationReport,
          selectedReport: {
            allEntries: allEntries,
            displayedEntries: filteredEntries, // Only show first entry
          },
        },
      };

      renderComponent(stateWithHiddenSelections);

      expect(screen.getByText('3 violations selected (2 hidden)')).toBeVisible();
    });

    it('does not display hidden count when no violations are hidden', () => {
      const stateWithVisibleSelections = {
        waivers: {
          ...preloadedState.waivers,
          bulkWaive: {
            ...preloadedState.waivers.bulkWaive,
            checkboxState: {
              'violation-id-1': true,
              'violation-id-2': true,
            },
          },
        },
      };

      renderComponent(stateWithVisibleSelections);

      expect(screen.getByText('2 violations selected')).toBeVisible();
      expect(screen.queryByText(/hidden/)).not.toBeInTheDocument();
    });
  });

  describe('select all toggle functionality', () => {
    it('header checkbox selects individual row checkboxes when clicked', async () => {
      const user = userEvent.setup();
      renderComponent();

      // Get all checkboxes
      const allCheckboxes = screen.getAllByRole('checkbox');

      // Identify header checkbox (first one) and row checkboxes (rest)
      const headerCheckbox = allCheckboxes[0];
      const rowCheckboxes = allCheckboxes.slice(1);

      // Ensure we actually have row checkboxes to test
      expect(rowCheckboxes.length).toBe(3);

      // Initially all checkboxes should be unchecked
      expect(headerCheckbox).not.toBeChecked();
      expect(rowCheckboxes[0]).not.toBeChecked();
      expect(rowCheckboxes[1]).not.toBeChecked();
      expect(rowCheckboxes[2]).not.toBeChecked();

      // Click the header checkbox to select all
      await user.click(headerCheckbox.parentElement);

      // Now header and all row checkboxes should be checked
      await waitFor(() => {
        expect(headerCheckbox).toBeChecked();
      });
      expect(rowCheckboxes[0]).toBeChecked();
      expect(rowCheckboxes[1]).toBeChecked();
      expect(rowCheckboxes[2]).toBeChecked();
    });

    it('header checkbox can unselect all violations', async () => {
      const user = userEvent.setup();
      renderComponent();

      // Get all checkboxes
      const allCheckboxes = screen.getAllByRole('checkbox');

      // Identify header checkbox (first one) and row checkboxes (rest)
      const headerCheckbox = allCheckboxes[0];
      const rowCheckboxes = allCheckboxes.slice(1);

      // Ensure we actually have row checkboxes to test
      expect(rowCheckboxes.length).toBe(3);

      // Initially all checkboxes should be unchecked
      expect(headerCheckbox).not.toBeChecked();
      expect(rowCheckboxes[0]).not.toBeChecked();
      expect(rowCheckboxes[1]).not.toBeChecked();
      expect(rowCheckboxes[2]).not.toBeChecked();

      // Step 1: Click header checkbox to select all
      await user.click(headerCheckbox.parentElement);

      // All checkboxes should now be checked
      await waitFor(() => {
        expect(headerCheckbox).toBeChecked();
      });
      expect(rowCheckboxes[0]).toBeChecked();
      expect(rowCheckboxes[1]).toBeChecked();
      expect(rowCheckboxes[2]).toBeChecked();

      // Uncheck one individual row to create mixed state
      await user.click(rowCheckboxes[1].parentElement); // Uncheck second row

      // Verify mixed state: header unchecked, while one row is now unchecked
      await waitFor(() => {
        expect(headerCheckbox).not.toBeChecked();
      });
      expect(rowCheckboxes[0]).toBeChecked(); // First row still checked
      expect(rowCheckboxes[1]).not.toBeChecked(); // Second row now unchecked
      expect(rowCheckboxes[2]).toBeChecked(); // Third row still checked

      // Step 2: Click header checkbox again to unselect all
      // This should unselect everything regardless of individual row states
      await user.click(headerCheckbox.parentElement);

      // All checkboxes should now be checked
      await waitFor(() => {
        expect(headerCheckbox).toBeChecked();
      });
      expect(rowCheckboxes[0]).toBeChecked();
      expect(rowCheckboxes[1]).toBeChecked();
      expect(rowCheckboxes[2]).toBeChecked();

      // Clicking the header checkbox again should unselect all checkboxes
      await user.click(headerCheckbox.parentElement);
      await waitFor(() => {
        expect(headerCheckbox).not.toBeChecked();
      });
      expect(rowCheckboxes[0]).not.toBeChecked();
      expect(rowCheckboxes[1]).not.toBeChecked();
      expect(rowCheckboxes[2]).not.toBeChecked();
    });

    it('header checkbox is checked only when all displayed entries are selected', () => {
      renderComponent();

      const allCheckboxes = screen.getAllByRole('checkbox');
      const headerCheckbox = allCheckboxes[0];
      const rowCheckboxes = allCheckboxes.slice(1);

      // Initially, header checkbox should be unchecked
      expect(headerCheckbox).not.toBeChecked();

      // Select some but not all individual rows
      fireEvent.click(rowCheckboxes[0]);
      fireEvent.click(rowCheckboxes[1]);

      // Header checkbox should still be unchecked (not all are selected)
      expect(headerCheckbox).not.toBeChecked();

      // Select the last row
      fireEvent.click(rowCheckboxes[2]);

      // Now header checkbox should be checked (all are selected)
      expect(headerCheckbox).toBeChecked();
    });

    it('header checkbox toggles all displayed entries while preserving other selections', () => {
      // Create state with some entries filtered out and one hidden selection
      const filteredEntries = [
        allEntries[0], // show first violation
        allEntries[1], // show second violation
        // allEntries[2] is hidden by filter but selected
      ];

      const stateWithMixedSelections = {
        waivers: {
          ...preloadedState.waivers,
          bulkWaive: {
            ...preloadedState.waivers.bulkWaive,
            checkboxState: {
              'violation-id-3': true, // This one is hidden by filter but selected
            },
          },
        },
        applicationReport: {
          ...preloadedState.applicationReport,
          selectedReport: {
            allEntries: allEntries,
            displayedEntries: filteredEntries,
          },
        },
      };

      renderComponent(stateWithMixedSelections);

      const allCheckboxes = screen.getAllByRole('checkbox');
      const headerCheckbox = allCheckboxes[0];
      const rowCheckboxes = allCheckboxes.slice(1);

      // Should show 1 violation selected (1 hidden) initially
      expect(screen.getByText('1 violation selected (1 hidden)')).toBeVisible();

      // Click header checkbox to select all displayed entries
      fireEvent.click(headerCheckbox);

      // Should now show 3 violations selected (1 hidden)
      expect(screen.getByText('3 violations selected (1 hidden)')).toBeVisible();

      // All visible row checkboxes should be checked
      expect(rowCheckboxes[0]).toBeChecked();
      expect(rowCheckboxes[1]).toBeChecked();

      // Click header checkbox again to unselect all displayed entries
      fireEvent.click(headerCheckbox);

      // Should now show 1 violation selected (1 hidden) - preserving the hidden selection
      expect(screen.getByText('1 violation selected (1 hidden)')).toBeVisible();

      // All visible row checkboxes should be unchecked
      expect(rowCheckboxes[0]).not.toBeChecked();
      expect(rowCheckboxes[1]).not.toBeChecked();
    });

    it('header checkbox behavior when some displayed entries are already selected', async () => {
      const user = userEvent.setup();
      renderComponent();

      const allCheckboxes = screen.getAllByRole('checkbox');
      const headerCheckbox = allCheckboxes[0];
      const rowCheckboxes = allCheckboxes.slice(1);

      // Select first two rows individually
      await user.click(rowCheckboxes[0].parentElement);
      await user.click(rowCheckboxes[1].parentElement);

      // Header checkbox should not be checked yet (not all are selected)
      await waitFor(() => {
        expect(headerCheckbox).not.toBeChecked();
      });
      expect(screen.getByText('2 violations selected')).toBeVisible();

      // Click header checkbox - should select all displayed entries (including the unselected third one)
      await user.click(headerCheckbox.parentElement);

      // All should now be selected
      await waitFor(() => {
        expect(headerCheckbox).toBeChecked();
      });
      expect(screen.getByText('3 violations selected')).toBeVisible();
      expect(rowCheckboxes[0]).toBeChecked();
      expect(rowCheckboxes[1]).toBeChecked();
      expect(rowCheckboxes[2]).toBeChecked();

      // Click header checkbox again - should unselect all displayed entries
      await user.click(headerCheckbox.parentElement);

      // All should now be unselected
      await waitFor(() => {
        expect(headerCheckbox).not.toBeChecked();
      });
      expect(screen.getByText('0 violations selected')).toBeVisible();
      expect(rowCheckboxes[0]).not.toBeChecked();
      expect(rowCheckboxes[1]).not.toBeChecked();
      expect(rowCheckboxes[2]).not.toBeChecked();
    });
  });

  describe('cancel button routing behavior', () => {
    it('routes to applicationReport.policy when hash is not present', () => {
      const stateWithoutHash = {
        ...preloadedState,
        router: {
          ...preloadedState.router,
          currentParams: {
            scanId: 'test-scan-id',
          },
        },
      };

      renderComponent(stateWithoutHash);

      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      fireEvent.click(cancelButton);

      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.policy', {
        scanId: 'test-scan-id',
      });
    });

    it('routes to applicationReport.componentDetails.violations when hash is present and not in priorities page container', () => {
      const stateWithHash = {
        ...preloadedState,
        router: {
          ...preloadedState.router,
          currentParams: {
            scanId: 'test-scan-id',
            hash: 'test-hash',
          },
        },
      };

      renderComponent(stateWithHash);

      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      fireEvent.click(cancelButton);

      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.componentDetails.violations', {
        scanId: 'test-scan-id',
        hash: 'test-hash',
      });
    });

    it('routes to priorities page container componentDetails.violations when hash is present and in priorities page container', () => {
      const stateWithPrioritiesContainer = {
        ...preloadedState,
        router: {
          ...preloadedState.router,
          currentParams: {
            scanId: 'test-scan-id',
            hash: 'test-hash',
          },
          currentState: {
            ...preloadedState.router.currentState,
            name: 'componentDetailsPageWithinPrioritiesPageContainerFromReports.bulkWaive',
          },
        },
      };

      renderComponent(stateWithPrioritiesContainer);

      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      fireEvent.click(cancelButton);

      expect(stateGoSpy).toHaveBeenCalledWith(
        'componentDetailsPageWithinPrioritiesPageContainerFromReports.componentDetails.violations',
        {
          scanId: 'test-scan-id',
          hash: 'test-hash',
        }
      );
    });
  });

  describe('displayedEntries filtering logic', () => {
    it('filters entries to show only open violations in non-CDP mode', () => {
      const entriesWithMixedStates = [
        {
          derivedComponentName: 'componentA : 1.0.0',
          derivedDependencyType: 'direct',
          policyViolationId: 'violation-id-1',
          policyName: 'Security-Critical',
          policyThreatLevel: 10,
          policyThreatCategory: 'SECURITY',
          derivedViolationState: 'open', // Should be displayed
          proprietary: false,
          derivedInnerSource: false,
          matchState: 'exact',
          hash: 'hash-1',
          constraints: [
            {
              conditions: [
                {
                  conditionReason: 'Critical security vulnerability',
                },
              ],
            },
          ],
        },
        {
          derivedComponentName: 'componentB : 1.0.0',
          derivedDependencyType: 'direct',
          policyViolationId: 'violation-id-2',
          policyName: 'Security-Medium',
          policyThreatLevel: 7,
          policyThreatCategory: 'SECURITY',
          derivedViolationState: 'waived', // Should be filtered out
          proprietary: false,
          derivedInnerSource: false,
          matchState: 'exact',
          hash: 'hash-2',
          constraints: [
            {
              conditions: [
                {
                  conditionReason: 'Medium security vulnerability',
                },
              ],
            },
          ],
        },
        {
          derivedComponentName: 'componentC : 1.0.0',
          derivedDependencyType: 'direct',
          policyViolationId: 'violation-id-3',
          policyName: 'License-Issue',
          policyThreatLevel: 5,
          policyThreatCategory: 'LICENSE',
          derivedViolationState: 'legacy', // Should be filtered out
          proprietary: false,
          derivedInnerSource: false,
          matchState: 'exact',
          hash: 'hash-3',
          constraints: [
            {
              conditions: [
                {
                  conditionReason: 'License issue detected',
                },
              ],
            },
          ],
        },
        {
          derivedComponentName: 'componentD : 1.0.0',
          derivedDependencyType: 'transitive',
          policyViolationId: 'violation-id-4',
          policyName: 'Component-Unknown',
          policyThreatLevel: 2,
          policyThreatCategory: 'OTHER',
          derivedViolationState: 'open', // Should be displayed
          proprietary: false,
          derivedInnerSource: true,
          matchState: 'similar',
          hash: 'hash-4',
          constraints: [
            {
              conditions: [
                {
                  conditionReason: 'Unknown component',
                },
              ],
            },
          ],
        },
      ];

      const stateWithMixedViolationStates = {
        ...preloadedState,
        applicationReport: {
          ...preloadedState.applicationReport,
          selectedReport: {
            allEntries: entriesWithMixedStates,
            displayedEntries: entriesWithMixedStates, // Component will filter these
          },
        },
        router: {
          ...preloadedState.router,
          currentParams: {
            scanId: 'test-scan-id',
            // No hash - non-CDP mode
          },
        },
      };

      renderComponent(stateWithMixedViolationStates);

      // Verify only 2 data rows (open violations)are visible (plus header and filter rows)
      const rows = screen.getAllByRole('row');
      expect(rows).toHaveLength(4); // header + filter + 2 data rows
    });

    it('filters entries by both hash and open violation state in CDP mode', () => {
      const entriesWithMixedStatesAndHashes = [
        {
          derivedComponentName: 'componentA : 1.0.0',
          derivedDependencyType: 'direct',
          policyViolationId: 'violation-id-1',
          policyName: 'Security-Critical',
          policyThreatLevel: 10,
          policyThreatCategory: 'SECURITY',
          derivedViolationState: 'open', // Matches violation state filter
          proprietary: false,
          derivedInnerSource: false,
          matchState: 'exact',
          hash: 'target-hash', // Matches CDP hash filter
          constraints: [
            {
              conditions: [
                {
                  conditionReason: 'Critical security vulnerability',
                },
              ],
            },
          ],
        },
        {
          derivedComponentName: 'componentB : 1.0.0',
          derivedDependencyType: 'direct',
          policyViolationId: 'violation-id-2',
          policyName: 'Security-Medium',
          policyThreatLevel: 7,
          policyThreatCategory: 'SECURITY',
          derivedViolationState: 'waived', // Does NOT match violation state filter
          proprietary: false,
          derivedInnerSource: false,
          matchState: 'exact',
          hash: 'target-hash', // Matches CDP hash filter but violation state is wrong
          constraints: [
            {
              conditions: [
                {
                  conditionReason: 'Medium security vulnerability',
                },
              ],
            },
          ],
        },
        {
          derivedComponentName: 'componentC : 1.0.0',
          derivedDependencyType: 'transitive',
          policyViolationId: 'violation-id-3',
          policyName: 'Component-Unknown',
          policyThreatLevel: 2,
          policyThreatCategory: 'OTHER',
          derivedViolationState: 'open', // Matches violation state filter
          proprietary: false,
          derivedInnerSource: true,
          matchState: 'similar',
          hash: 'other-hash', // Does NOT match CDP hash filter
          constraints: [
            {
              conditions: [
                {
                  conditionReason: 'Unknown component',
                },
              ],
            },
          ],
        },
        {
          derivedComponentName: 'componentD : 1.0.0',
          derivedDependencyType: 'direct',
          policyViolationId: 'violation-id-4',
          policyName: 'License-Issue',
          policyThreatLevel: 5,
          policyThreatCategory: 'LICENSE',
          derivedViolationState: 'open', // Matches violation state filter
          proprietary: false,
          derivedInnerSource: false,
          matchState: 'exact',
          hash: 'target-hash', // Matches CDP hash filter
          constraints: [
            {
              conditions: [
                {
                  conditionReason: 'License issue detected',
                },
              ],
            },
          ],
        },
      ];

      const cdpStateWithMixedViolationStates = {
        ...preloadedState,
        applicationReport: {
          ...preloadedState.applicationReport,
          selectedReport: {
            allEntries: entriesWithMixedStatesAndHashes,
            displayedEntries: entriesWithMixedStatesAndHashes, // Component will filter these
          },
        },
        router: {
          ...preloadedState.router,
          currentParams: {
            scanId: 'test-scan-id',
            hash: 'target-hash', // Enable CDP filtering
          },
        },
      };

      renderComponent(cdpStateWithMixedViolationStates);

      // Verify only 2 data rows are visible (plus header and filter rows)
      const rows = screen.getAllByRole('row');
      expect(rows).toHaveLength(4); // header + filter + 2 data rows
    });

    it('shows no results when CDP filter matches but no violations are open', () => {
      const entriesWithClosedViolations = [
        {
          derivedComponentName: 'componentA : 1.0.0',
          derivedDependencyType: 'direct',
          policyViolationId: 'violation-id-1',
          policyName: 'Security-Critical',
          policyThreatLevel: 10,
          policyThreatCategory: 'SECURITY',
          derivedViolationState: 'waived', // Not open
          proprietary: false,
          derivedInnerSource: false,
          matchState: 'exact',
          hash: 'target-hash', // Matches CDP filter
          constraints: [
            {
              conditions: [
                {
                  conditionReason: 'Critical security vulnerability',
                },
              ],
            },
          ],
        },
        {
          derivedComponentName: 'componentB : 1.0.0',
          derivedDependencyType: 'direct',
          policyViolationId: 'violation-id-2',
          policyName: 'Security-Medium',
          policyThreatLevel: 7,
          policyThreatCategory: 'SECURITY',
          derivedViolationState: 'dismissed', // Not open
          proprietary: false,
          derivedInnerSource: false,
          matchState: 'exact',
          hash: 'target-hash', // Matches CDP filter
          constraints: [
            {
              conditions: [
                {
                  conditionReason: 'Medium security vulnerability',
                },
              ],
            },
          ],
        },
      ];

      const cdpStateWithNoOpenViolations = {
        ...preloadedState,
        applicationReport: {
          ...preloadedState.applicationReport,
          selectedReport: {
            allEntries: entriesWithClosedViolations,
            displayedEntries: entriesWithClosedViolations, // Component will filter these to empty
          },
        },
        router: {
          ...preloadedState.router,
          currentParams: {
            scanId: 'test-scan-id',
            hash: 'target-hash', // Enable CDP filtering
          },
        },
      };

      renderComponent(cdpStateWithNoOpenViolations);

      // Should display "No Results" since all violations are filtered out
      expect(screen.getByText('No Results')).toBeVisible();

      // Should not display any violation components
      expect(screen.queryByText('componentA : 1.0.0')).not.toBeInTheDocument();
      expect(screen.queryByText('componentB : 1.0.0')).not.toBeInTheDocument();

      // Should have only 1 checkbox (header checkbox)
      const checkboxes = screen.getAllByRole('checkbox');
      expect(checkboxes).toHaveLength(1);

      // Should have 3 rows: header + filter + "No Results" row
      const rows = screen.getAllByRole('row');
      expect(rows).toHaveLength(3);
    });
  });

  function renderComponent(additionalState = {}) {
    const finalState = { ...preloadedState, ...additionalState };
    return render(<BulkWaivePage />, { preloadedState: finalState });
  }

  function getDefaultPreloadedState(entries = []) {
    return {
      waivers: {
        waiverReasons: {
          loading: false,
          loadError: null,
          data: [],
        },
        bulkWaive: {
          checkboxState: {},
          selectAllChecked: false,
          selectedViolations: [],
        },
      },
      violation: {
        activeWaivers: [],
        expiredWaivers: [],
        violationDetails: null,
        loading: false,
      },
      componentDetailsPolicyViolations: {
        violations: null,
        selectedPolicyViolationId: null,
        showViolationsDetailPopover: false,
      },
      applicationReport: {
        selectedReport: {
          allEntries: entries,
          displayedEntries: entries,
        },
        metadata: null,
        exactValueFilters: {
          policyThreatLevel: [],
          policyThreatCategory: [],
          derivedViolationState: new Set(),
          proprietary: [],
          derivedInnerSource: [],
          matchState: new Set(),
          derivedDependencyType: new Set(),
        },
        substringFilters: {
          policyName: '',
          derivedComponentName: '',
        },
        sortConfiguration: null,
        loadError: null,
        loading: false,
        pendingLoads: new Set(),
        isNotFiltered: true,
        showFilterPopover: false,
      },
      router: {
        currentState: {
          name: 'applicationReport.bulkWaive',
          url: '/organizations/{organizationId}/applications/{publicId}/reports/{scanId}/bulkWaive',
          data: {},
        },
        currentParams: {
          // Remove publicId and scanId to prevent useEffect from clearing selectedReport
        },
        prevState: {},
        prevParams: {},
      },
    };
  }

  function getDefaultAllEntriesDataForTest() {
    return [
      {
        derivedComponentName: 'componentA : 1.0.0',
        derivedDependencyType: 'direct',
        policyViolationId: 'violation-id-1',
        policyName: 'Security-Critical',
        policyThreatLevel: 10,
        policyThreatCategory: 'SECURITY',
        derivedViolationState: 'open',
        proprietary: false,
        derivedInnerSource: false,
        matchState: 'exact',
        hash: 'hash-1',
        constraints: [
          {
            conditions: [
              {
                conditionReason: 'Critical security vulnerability',
              },
            ],
          },
        ],
      },
      {
        derivedComponentName: 'componentB : 1.0.0',
        derivedDependencyType: 'direct',
        policyViolationId: 'violation-id-2',
        policyName: 'Security-Medium',
        policyThreatLevel: 7,
        policyThreatCategory: 'SECURITY',
        derivedViolationState: 'open',
        proprietary: false,
        derivedInnerSource: false,
        matchState: 'exact',
        hash: 'hash-2',
        constraints: [
          {
            conditions: [
              {
                conditionReason: 'Medium security vulnerability',
              },
            ],
          },
        ],
      },
      {
        derivedComponentName: 'componentC : 1.0.0',
        derivedDependencyType: 'transitive',
        policyViolationId: 'violation-id-3',
        policyName: 'Component-Unknown',
        policyThreatLevel: 2,
        policyThreatCategory: 'OTHER',
        derivedViolationState: 'open',
        proprietary: false,
        derivedInnerSource: true,
        matchState: 'similar',
        hash: 'hash-3',
        constraints: [
          {
            conditions: [
              {
                conditionReason: 'Unknown component',
              },
            ],
          },
        ],
      },
    ];
  }
});
