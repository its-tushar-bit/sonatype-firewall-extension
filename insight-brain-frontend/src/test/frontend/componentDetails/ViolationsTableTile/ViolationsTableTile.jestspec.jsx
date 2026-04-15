/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { axiosMockAdapter, fireEvent, render, screen } from 'TestRoot/SpecUtil';
import ViolationsTableTile, {
  ViewAllComponentWaiversButton,
  ViewTransitiveViolationsButton,
} from 'MainRoot/componentDetails/ViolationsTableTile/ViolationsTableTile';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import { getApplicationSummaryUrl, getPermissionContextTestUrl } from 'MainRoot/util/CLMLocation';

// Mock components that require additional setup (to hide irrelevant error logs)
jest.mock('MainRoot/componentDetails/ViolationsTableTile/PolicyViolationsTable', () => {
  const PolicyViolationsTable = (props) => {
    return (
      <div data-testid="policy-violations-table">
        <span>PolicyViolationsTable - Component: {props.componentName}</span>
        {props.violations && <span data-testid="violations-count">{props.violations.length} violations</span>}
      </div>
    );
  };

  PolicyViolationsTable.propTypes = {
    componentName: require('prop-types').string,
    violations: require('prop-types').array,
  };

  return PolicyViolationsTable;
});

describe('ViolationsTableTile component', () => {
  let stateGoSpy, preloadedState, axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');

    preloadedState = getDefaultPreloadedState();
  });

  it('renders the tile header with title', () => {
    renderComponent({ title: 'Policy Violations' });

    const title = screen.getByRole('heading', { name: 'Policy Violations' });
    expect(title).toBeVisible();
  });

  it('renders PolicyViolationsTable when not loading', () => {
    renderComponent({
      componentName: 'test-component',
      isLoadingComponentDetails: false,
    });

    const table = screen.getByTestId('policy-violations-table');
    expect(table).toBeVisible();
    expect(screen.getByText('PolicyViolationsTable - Component: test-component')).toBeVisible();
  });

  it('shows loading state when isLoadingComponentDetails is true', () => {
    renderComponent({
      isLoadingComponentDetails: true,
      componentName: '',
    });

    // The NxLoadWrapper should show loading state
    const loadWrapper = screen.getByRole('status');
    expect(loadWrapper).toBeVisible();
  });

  it('shows loading state when componentName is not provided', () => {
    renderComponent({
      isLoadingComponentDetails: false,
      componentName: '',
    });

    // The NxLoadWrapper should show loading state
    const loadWrapper = screen.getByRole('status');
    expect(loadWrapper).toBeVisible();
  });

  it('shows error state when componentDetailsLoadError is provided', () => {
    const errorMessage = 'Failed to load component details';
    renderComponent({
      componentDetailsLoadError: errorMessage,
      isLoadingComponentDetails: false,
      componentName: 'test-component',
    });

    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent(errorMessage);
  });

  it('calls loadComponentDetails when retry button is clicked', () => {
    const loadComponentDetailsSpy = jest.fn();
    const errorMessage = 'Failed to load component details';

    renderComponent({
      componentDetailsLoadError: errorMessage,
      isLoadingComponentDetails: false,
      componentName: 'test-component',
      loadComponentDetails: loadComponentDetailsSpy,
    });

    const retryButton = screen.getByRole('button', { name: 'Retry' });
    fireEvent.click(retryButton);

    expect(loadComponentDetailsSpy).toHaveBeenCalledTimes(1);
  });

  describe('Bulk Waive button', () => {
    beforeEach(() => {
      // Mock permission check API calls made by BulkWaiveButton
      axiosMock.onGet(getApplicationSummaryUrl('test-app-id')).reply(200, { id: 'internal-id-123' });
      axiosMock
        .onPut(getPermissionContextTestUrl('application', 'internal-id-123'))
        .reply(200, ['WAIVE_POLICY_VIOLATIONS']);
    });

    it('renders when showViewAllComponents is true', async () => {
      renderComponent({
        showViewAllComponents: true,
        violations: [
          { id: '1', waived: false, legacyViolation: false, waivedWithAutoWaiver: false },
          { id: '2', derivedViolationState: 'open' },
        ],
      });

      const bulkWaiveButton = await screen.findByRole('button', { name: 'Bulk Waive' });
      expect(bulkWaiveButton).toBeVisible();
      expect(bulkWaiveButton).not.toBeDisabled();
    });

    it('renders when showViewTransitiveViolations is true', async () => {
      renderComponent({
        showViewTransitiveViolations: true,
        violations: [
          { id: '1', waived: false, legacyViolation: false, waivedWithAutoWaiver: false },
          { id: '2', derivedViolationState: 'open' },
        ],
      });

      const bulkWaiveButton = await screen.findByRole('button', { name: 'Bulk Waive' });
      expect(bulkWaiveButton).toBeVisible();
      expect(bulkWaiveButton).not.toBeDisabled();
    });

    it('is disabled when violations array is empty', async () => {
      renderComponent({
        showViewAllComponents: true,
        violations: [],
      });

      const bulkWaiveButton = await screen.findByRole('button', { name: 'Bulk Waive' });
      expect(bulkWaiveButton).toBeDisabled();
    });

    it('is disabled when violations is null', async () => {
      renderComponent({
        showViewAllComponents: true,
        violations: null,
      });

      const bulkWaiveButton = await screen.findByRole('button', { name: 'Bulk Waive' });
      expect(bulkWaiveButton).toBeDisabled();
    });

    it('is disabled when violations are not open', async () => {
      renderComponent({
        showViewAllComponents: true,
        violations: [
          { id: '1', waived: true, legacyViolation: false, waivedWithAutoWaiver: false },
          { id: '2', legacyViolation: true, waived: false, waivedWithAutoWaiver: false },
        ],
      });
      const bulkWaiveButton = await screen.findByRole('button', { name: 'Bulk Waive' });
      expect(bulkWaiveButton).toBeDisabled();
    });

    it('navigates to component details bulk waive page when clicked', async () => {
      const stateWithHash = {
        ...preloadedState,
        router: {
          ...preloadedState.router,
          currentParams: {
            organizationId: 'test-org',
            publicId: 'test-app',
            scanId: 'test-scan',
            hash: 'test-hash',
          },
        },
      };

      renderComponent(
        {
          showViewAllComponents: true,
          violations: [
            { id: '1', waived: false, legacyViolation: false, waivedWithAutoWaiver: false },
            { id: '2', derivedViolationState: 'open' },
          ],
        },
        stateWithHash
      );

      const bulkWaiveButton = await screen.findByRole('button', { name: 'Bulk Waive' });
      fireEvent.click(bulkWaiveButton);

      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.cdpBulkWaive', {
        publicId: 'test-app',
        scanId: 'test-scan',
        hash: 'test-hash',
      });
    });

    it('navigates to priorities page container bulk waive when clicked - priorities container scenario', async () => {
      const stateWithPriorities = {
        ...preloadedState,
        router: {
          ...preloadedState.router,
          currentParams: {
            organizationId: 'test-org',
            publicId: 'test-app',
            scanId: 'test-scan',
            hash: 'test-hash',
          },
          currentState: {
            ...preloadedState.router.currentState,
            name: 'componentDetailsPageWithinPrioritiesPageContainerFromReports.violations',
          },
        },
      };

      renderComponent(
        {
          showViewAllComponents: true,
          violations: [
            { id: '1', waived: false, legacyViolation: false, waivedWithAutoWaiver: false },
            { id: '2', derivedViolationState: 'open' },
          ],
        },
        stateWithPriorities
      );

      const bulkWaiveButton = await screen.findByRole('button', { name: 'Bulk Waive' });
      fireEvent.click(bulkWaiveButton);

      expect(stateGoSpy).toHaveBeenCalledWith(
        'componentDetailsPageWithinPrioritiesPageContainerFromReports.bulkWaive',
        {
          publicId: 'test-app',
          scanId: 'test-scan',
          hash: 'test-hash',
        }
      );
    });

    it('does not render when both showViewAllComponents and showViewTransitiveViolations are false', () => {
      renderComponent({
        showViewAllComponents: false,
        showViewTransitiveViolations: false,
        violations: [
          { id: '1', waived: false, legacyViolation: false, waivedWithAutoWaiver: false },
          { id: '2', derivedViolationState: 'open' },
        ],
      });

      expect(screen.queryByRole('button', { name: 'Bulk Waive' })).not.toBeInTheDocument();
    });

    it('does not render in firewall container component details', () => {
      renderComponent({
        showViewAllComponents: true,
        isFirewall: true,
        violations: [
          { id: '1', waived: false, legacyViolation: false, waivedWithAutoWaiver: false },
          { id: '2', derivedViolationState: 'open' },
        ],
      });

      expect(screen.queryByRole('button', { name: 'Bulk Waive' })).not.toBeInTheDocument();
    });
  });

  describe('ViewTransitiveViolationsButton', () => {
    it('renders when showViewTransitiveViolations is true', () => {
      renderComponent({
        showViewTransitiveViolations: true,
      });

      const button = screen.getByRole('button', { name: 'View Transitive Violations' });
      expect(button).toBeVisible();
    });

    it('calls stateGo with correct parameters when clicked', () => {
      renderComponent({
        showViewTransitiveViolations: true,
        ownerType: 'application',
        ownerId: 'app-123',
        scanId: 'scan-456',
        hash: 'hash-789',
      });

      const button = screen.getByRole('button', { name: 'View Transitive Violations' });
      fireEvent.click(button);

      expect(stateGoSpy).toHaveBeenCalledWith('transitiveViolations', {
        ownerType: 'application',
        ownerId: 'app-123',
        scanId: 'scan-456',
        hash: 'hash-789',
      });
    });

    it('does not render when showViewTransitiveViolations is false', () => {
      renderComponent({
        showViewTransitiveViolations: false,
      });

      expect(screen.queryByRole('button', { name: 'View Transitive Violations' })).not.toBeInTheDocument();
    });
  });

  describe('ViewAllComponentWaiversButton', () => {
    it('renders when showViewAllComponents is true and not loading', () => {
      renderComponent({
        showViewAllComponents: true,
        isLoadingComponentDetails: false,
        componentName: 'test-component',
      });

      const button = screen.getByRole('button', { name: 'View Existing Waivers' });
      expect(button).toBeVisible();
    });

    it('calls toggleComponentWaiversPopover when clicked', () => {
      const toggleComponentWaiversPopoverSpy = jest.fn();

      renderComponent({
        showViewAllComponents: true,
        isLoadingComponentDetails: false,
        componentName: 'test-component',
        toggleComponentWaiversPopover: toggleComponentWaiversPopoverSpy,
      });

      const button = screen.getByRole('button', { name: 'View Existing Waivers' });
      fireEvent.click(button);

      expect(toggleComponentWaiversPopoverSpy).toHaveBeenCalledTimes(1);
    });

    it('does not render when showViewAllComponents is false', () => {
      renderComponent({
        showViewAllComponents: false,
        isLoadingComponentDetails: false,
        componentName: 'test-component',
      });

      expect(screen.queryByRole('button', { name: 'View Existing Waivers' })).not.toBeInTheDocument();
    });

    it('does not render when loading', () => {
      renderComponent({
        showViewAllComponents: true,
        isLoadingComponentDetails: true,
        componentName: '',
      });

      expect(screen.queryByRole('button', { name: 'View Existing Waivers' })).not.toBeInTheDocument();
    });
  });

  describe('setViolationType effect', () => {
    it('calls setViolationType with violationType prop', () => {
      const setViolationTypeSpy = jest.fn();

      renderComponent({
        violationType: 'security',
        setViolationType: setViolationTypeSpy,
      });

      expect(setViolationTypeSpy).toHaveBeenCalledWith('security');
    });
  });

  function renderComponent(additionalProps = {}, customPreloadedState = null) {
    const defaultProps = getDefaultProps();
    const finalProps = { ...defaultProps, ...additionalProps };
    const stateToUse = customPreloadedState || preloadedState;
    return render(<ViolationsTableTile {...finalProps} />, { preloadedState: stateToUse });
  }

  function getDefaultProps() {
    return {
      isLoadingComponentDetails: false,
      componentDetailsLoadError: null,
      loadComponentDetails: jest.fn(),
      violationType: 'all',
      setViolationType: jest.fn(),
      title: 'Violations',
      showViewAllComponents: false,
      showViewTransitiveViolations: true,
      stateGo: stateGoSpy,
      ownerType: 'application',
      ownerId: 'test-app-id',
      scanId: 'test-scan-id',
      hash: 'test-hash',
      // Props that get passed to PolicyViolationsTable
      componentName: 'test-component',
      violations: [
        { id: '1', policyViolationId: 'violation-1' },
        { id: '2', policyViolationId: 'violation-2' },
      ],
      toggleComponentWaiversPopover: jest.fn(),
      toggleShowViolationsDetailPopover: jest.fn(),
      setSelectedPolicyViolationId: jest.fn(),
      showComponentWaiversPopover: false,
      componentNameWithoutVersion: 'test-component',
      waivers: [],
      isAutoWaiverEnabled: false,
      setViolationsDetailRowClicked: jest.fn(),
      waiverToDelete: null,
      setWaiverToDelete: jest.fn(),
      isLegalTab: false,
      loadPolicyViolationsInformation: jest.fn(),
      error: null,
      loading: false,
    };
  }

  function getDefaultPreloadedState() {
    return {
      waivers: {
        bulkWaive: {
          checkboxState: {},
          selectAllChecked: false,
          selectedViolations: [],
        },
        permissions: {
          loading: {},
          error: {},
          byApplicationId: {},
        },
      },
      componentDetailsPolicyViolations: {
        violations: null,
        selectedPolicyViolationId: null,
        showViolationsDetailPopover: false,
      },
      router: {
        currentState: {
          name: 'applicationReport.componentDetails.violations',
          data: {},
        },
        currentParams: {
          organizationId: 'test-org',
          publicId: 'test-app',
          scanId: 'test-scan',
          hash: 'test-hash',
        },
      },
    };
  }
});

describe('ViewAllComponentWaiversButton component', () => {
  it('renders with correct text and calls toggle function when clicked', () => {
    const toggleComponentWaiversPopoverSpy = jest.fn();

    render(<ViewAllComponentWaiversButton toggleComponentWaiversPopover={toggleComponentWaiversPopoverSpy} />);

    const button = screen.getByRole('button', { name: 'View Existing Waivers' });
    expect(button).toBeVisible();
    expect(button).toHaveAttribute('id', 'component-details-view-waivers');

    fireEvent.click(button);
    expect(toggleComponentWaiversPopoverSpy).toHaveBeenCalledTimes(1);
  });
});

describe('ViewTransitiveViolationsButton component', () => {
  it('renders with correct text and calls stateGo when clicked', () => {
    const stateGoSpy = jest.fn();
    const props = {
      stateGo: stateGoSpy,
      ownerType: 'application',
      ownerId: 'app-123',
      scanId: 'scan-456',
      hash: 'hash-789',
    };

    render(<ViewTransitiveViolationsButton {...props} />);

    const button = screen.getByRole('button', { name: 'View Transitive Violations' });
    expect(button).toBeVisible();
    expect(button).toHaveAttribute('id', 'component-details-view-transitive-violations');

    fireEvent.click(button);
    expect(stateGoSpy).toHaveBeenCalledWith('transitiveViolations', {
      ownerType: 'application',
      ownerId: 'app-123',
      scanId: 'scan-456',
      hash: 'hash-789',
    });
  });
});
