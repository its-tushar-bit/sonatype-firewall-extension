/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, fireEvent, axiosMockAdapter } from 'TestRoot/SpecUtil';
import WaiverConfigurationPage from 'MainRoot/waivers/WaiverConfigurationPage';
import { getPolicyWaiverReasonsUrl } from 'MainRoot/util/CLMLocation';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import { actions as waiverActions } from 'MainRoot/waivers/waiverSlice';
import { nxDateInputStateHelpers } from '@sonatype/react-shared-components';
import moment from 'moment';
import * as waiverSelectors from 'MainRoot/waivers/requestWaiverSelectors';

describe('WaiverConfigurationPage component', () => {
  let preloadedState, axiosMock, stateGoSpy;

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
    preloadedState = getDefaultPreloadedState();
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');

    // Mock waiver reasons API call
    axiosMock.onGet(getPolicyWaiverReasonsUrl()).reply(200, [
      { id: 'reason-1', reasonText: 'False Positive', type: 'system' },
      { id: 'reason-2', reasonText: 'Risk Accepted', type: 'system' },
    ]);

    // Ideally we refrain from mocking selectors, but in this case, it takes too much effort
    // to correctly set up all the requests, hence mocking selectors for simplicity,
    jest.spyOn(waiverSelectors, 'selectAddWaiverDataLoading').mockReturnValue(false);
    jest.spyOn(waiverSelectors, 'selectAddWaiverDataError').mockReturnValue(null);
  });

  describe('it renders basic structure', () => {
    const preloadedState = getDefaultPreloadedState();
    const stateWithViolations = {
      ...preloadedState,
      waivers: {
        ...preloadedState.waivers,
        bulkWaive: {
          ...preloadedState.waivers.bulkWaive,
          selectedViolations: [
            { policyViolationId: 'violation-1' },
            { policyViolationId: 'violation-2' },
            { policyViolationId: 'violation-3' },
          ],
        },
      },
    };

    it('renders the page title', () => {
      renderComponent(stateWithViolations);

      const pageTitle = screen.getByRole('heading', { name: 'Bulk Waiver' });
      expect(pageTitle).toBeVisible();
    });

    it('renders the basic tile structure', () => {
      renderComponent(stateWithViolations);

      // Check that the main tile container is present
      const tile = screen.getByRole('heading', { name: 'Bulk Waiver' }).closest('main');
      expect(tile).toBeInTheDocument();
    });
  });

  it('renders the section title with violation count', () => {
    const stateWithViolations = {
      ...preloadedState,
      waivers: {
        ...preloadedState.waivers,
        bulkWaive: {
          ...preloadedState.waivers.bulkWaive,
          selectedViolations: [
            { policyViolationId: 'violation-1' },
            { policyViolationId: 'violation-2' },
            { policyViolationId: 'violation-3' },
          ],
        },
      },
    };

    renderComponent(stateWithViolations);

    const sectionTitle = screen.getByRole('heading', { name: 'Waiver configuration for 3 selected violations' });
    expect(sectionTitle).toBeVisible();
  });

  describe('with selected violations', () => {
    let stateWithViolations;

    beforeEach(() => {
      stateWithViolations = {
        ...preloadedState,
        waivers: {
          ...preloadedState.waivers,
          bulkWaive: {
            ...preloadedState.waivers.bulkWaive,
            selectedViolations: [
              {
                policyViolationId: 'violation-1',
                policyName: 'Test Policy 1',
                derivedComponentName: 'test-component:1.0.0',
              },
              {
                policyViolationId: 'violation-2',
                policyName: 'Test Policy 2',
                derivedComponentName: 'test-component:2.0.0',
              },
            ],
          },
        },
      };
    });

    it('displays correct count of selected violations', () => {
      renderComponent(stateWithViolations);

      const sectionTitle = screen.getByRole('heading', { name: 'Waiver configuration for 2 selected violations' });
      expect(sectionTitle).toBeVisible();
    });
  });

  describe('Button navigation', () => {
    let stateWithViolationsAndData;

    beforeEach(() => {
      stateWithViolationsAndData = {
        ...preloadedState,
        waivers: {
          ...preloadedState.waivers,
          bulkWaive: {
            ...preloadedState.waivers.bulkWaive,
            selectedViolations: [
              {
                policyViolationId: 'violation-1',
                policyName: 'Test Policy 1',
                derivedComponentName: 'test-component:1.0.0',
                matchState: 'exact',
              },
            ],
          },
          waiverReasons: {
            loading: false,
            loadError: null,
            data: [
              { id: 'reason-1', reasonText: 'False Positive', type: 'system' },
              { id: 'reason-2', reasonText: 'Risk Accepted', type: 'system' },
            ],
          },
        },
        addWaiver: {
          loading: false,
          loadError: null,
          availableWaiverScopes: [
            { id: 'scope-1', name: 'Test Organization', label: 'Organization' },
            { id: 'scope-2', name: 'Test Application', label: 'Application' },
          ],
          selectedWaiverScope: { id: 'scope-1', name: 'Test Organization', label: 'Organization' },
        },
        requestWaiver: {
          loading: false,
          loadError: null,
          selectedWaiverScope: { id: 'scope-1', name: 'Test Organization', label: 'Organization' },
        },
      };
    });

    it('renders Next button as disabled initially when no expiration is selected', () => {
      renderComponent(stateWithViolationsAndData);

      const nextButton = screen.getByRole('button', { name: 'Next' });
      expect(nextButton).toBeVisible();
      expect(nextButton).toBeDisabled();
    });

    it('enables Next button when Waiver Expiration is selected', () => {
      renderComponent(stateWithViolationsAndData);

      // Initially disabled
      const nextButton = screen.getByRole('button', { name: 'Next' });
      expect(nextButton).toBeDisabled();

      // Find the Waiver Expiration dropdown by finding the option "30 Days" and getting its parent select
      const thirtyDaysOption = screen.getByRole('option', { name: '30 Days' });
      const expirationSelect = thirtyDaysOption.closest('select');
      expect(expirationSelect).toBeVisible();

      // Select an expiration option (simulate selecting "30 days")
      fireEvent.change(expirationSelect, { target: { value: '30' } });

      // Next button should now be enabled
      expect(nextButton).toBeVisible();
      expect(nextButton).not.toBeDisabled();
    });

    it('enables Next button when Never expiration is selected', () => {
      renderComponent(stateWithViolationsAndData);

      // Initially disabled
      const nextButton = screen.getByRole('button', { name: 'Next' });
      expect(nextButton).toBeDisabled();

      // Find the Waiver Expiration dropdown by finding the option "Never" and getting its parent select
      const neverOption = screen.getByRole('option', { name: 'Never' });
      const expirationSelect = neverOption.closest('select');
      expect(expirationSelect).toBeVisible();

      // Select "Never" expiration option
      fireEvent.change(expirationSelect, { target: { value: 'never' } });

      // Next button should now be enabled
      expect(nextButton).toBeVisible();
      expect(nextButton).not.toBeDisabled();
    });

    it('keeps Next button disabled when Custom expiration is selected without a valid date', () => {
      const stateWithParams = {
        ...stateWithViolationsAndData,
        requestWaiver: {
          ...stateWithViolationsAndData.requestWaiver,
          customExpiryTime: nxDateInputStateHelpers.initialState(''), // No custom date set
        },
      };

      renderComponent(stateWithParams);

      // Initially disabled
      const nextButton = screen.getByRole('button', { name: 'Next' });
      expect(nextButton).toBeDisabled();

      // Find the Waiver Expiration dropdown by finding the option "Custom" and getting its parent select
      const customOption = screen.getByRole('option', { name: 'Custom' });
      const expirationSelect = customOption.closest('select');
      expect(expirationSelect).toBeVisible();

      // Select "Custom" expiration option
      fireEvent.change(expirationSelect, { target: { value: 'custom' } });

      // Next button should remain disabled because no valid custom date is set
      expect(nextButton).toBeVisible();
      expect(nextButton).toBeDisabled();
    });

    it('enables Next button when Custom expiration is selected with a valid future date', () => {
      const futureDate = moment().add(30, 'days').format('YYYY-MM-DD');

      // Start with state that already has Custom selected and a valid future date
      const stateWithValidCustomDate = {
        ...stateWithViolationsAndData,
        requestWaiver: {
          ...stateWithViolationsAndData.requestWaiver,
          customExpiryTime: nxDateInputStateHelpers.userInput(() => null, futureDate), // Valid future date set
        },
      };

      renderComponent(stateWithValidCustomDate);

      // Initially disabled because no expiration selected yet
      const nextButton = screen.getByRole('button', { name: 'Next' });
      expect(nextButton).toBeDisabled();

      // Select "Custom" expiration option
      const customOption = screen.getByRole('option', { name: 'Custom' });
      const expirationSelect = customOption.closest('select');
      fireEvent.change(expirationSelect, { target: { value: 'custom' } });

      // Next button should now be enabled because Custom is selected with a valid future date
      expect(nextButton).toBeVisible();
      expect(nextButton).not.toBeDisabled();
    });

    it('navigates to waiver confirmation page when Next button is clicked', () => {
      const stateWithParams = {
        ...stateWithViolationsAndData,
        router: {
          ...stateWithViolationsAndData.router,
          currentParams: {
            publicId: 'test-app',
            scanId: 'test-scan',
          },
        },
      };

      renderComponent(stateWithParams);

      // Enable the Next button by selecting an expiration
      const thirtyDaysOption = screen.getByRole('option', { name: '30 Days' });
      const expirationSelect = thirtyDaysOption.closest('select');
      fireEvent.change(expirationSelect, { target: { value: '30' } });

      // Click the Next button
      const nextButton = screen.getByRole('button', { name: 'Next' });
      fireEvent.click(nextButton);

      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.waiverConfirmation', {
        publicId: 'test-app',
        scanId: 'test-scan',
      });
    });

    it('navigates to policy page when Cancel button is clicked and no hash param', () => {
      const stateWithParams = {
        ...stateWithViolationsAndData,
        router: {
          ...stateWithViolationsAndData.router,
          currentParams: {
            publicId: 'test-app',
            scanId: 'test-scan',
          },
        },
      };

      renderComponent(stateWithParams);

      // Click the Cancel button
      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      fireEvent.click(cancelButton);

      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.policy', {
        publicId: 'test-app',
        scanId: 'test-scan',
      });
    });

    it('navigates to component details violations when Cancel button is clicked with hash param and not in priorities page container', () => {
      const stateWithParams = {
        ...stateWithViolationsAndData,
        router: {
          ...stateWithViolationsAndData.router,
          currentParams: {
            publicId: 'test-app',
            scanId: 'test-scan',
            hash: 'test-hash',
          },
        },
      };

      renderComponent(stateWithParams);

      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      fireEvent.click(cancelButton);

      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.componentDetails.violations', {
        publicId: 'test-app',
        scanId: 'test-scan',
        hash: 'test-hash',
      });
    });

    it('navigates to priorities page component details violations when Cancel button is clicked with hash param and in priorities page container', () => {
      const stateWithParams = {
        ...stateWithViolationsAndData,
        router: {
          ...stateWithViolationsAndData.router,
          currentState: {
            ...stateWithViolationsAndData.router.currentState,
            name: 'componentDetailsPageWithinPrioritiesPageContainerFromDashboard.waiverConfiguration', // This makes it a priorities page container
          },
          currentParams: {
            publicId: 'test-app',
            scanId: 'test-scan',
            hash: 'test-hash',
          },
        },
      };

      renderComponent(stateWithParams);

      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      fireEvent.click(cancelButton);

      expect(stateGoSpy).toHaveBeenCalledWith(
        'componentDetailsPageWithinPrioritiesPageContainerFromDashboard.componentDetails.violations',
        {
          publicId: 'test-app',
          scanId: 'test-scan',
          hash: 'test-hash',
        }
      );
    });

    it('navigates to bulk waive page when Back button is clicked and no hash param', () => {
      const stateWithParams = {
        ...stateWithViolationsAndData,
        router: {
          ...stateWithViolationsAndData.router,
          currentParams: {
            publicId: 'test-app',
            scanId: 'test-scan',
          },
        },
      };

      renderComponent(stateWithParams);

      const backButton = screen.getByRole('button', { name: 'Back' });
      fireEvent.click(backButton);

      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.bulkWaive', {
        publicId: 'test-app',
        scanId: 'test-scan',
      });
    });

    it('navigates to CDP bulk waive page when Back button is clicked with hash param and not in priorities page container', () => {
      const stateWithParams = {
        ...stateWithViolationsAndData,
        router: {
          ...stateWithViolationsAndData.router,
          currentParams: {
            publicId: 'test-app',
            scanId: 'test-scan',
            hash: 'test-hash',
          },
        },
      };

      renderComponent(stateWithParams);

      const backButton = screen.getByRole('button', { name: 'Back' });
      fireEvent.click(backButton);

      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.cdpBulkWaive', {
        publicId: 'test-app',
        scanId: 'test-scan',
        hash: 'test-hash',
      });
    });

    it('navigates to priorities page bulk waive when Back button is clicked with hash param and in priorities page container', () => {
      const stateWithParams = {
        ...stateWithViolationsAndData,
        router: {
          ...stateWithViolationsAndData.router,
          currentState: {
            ...stateWithViolationsAndData.router.currentState,
            name: 'componentDetailsPageWithinPrioritiesPageContainerFromReports.waiverConfiguration', // This makes it a priorities page container
          },
          currentParams: {
            publicId: 'test-app',
            scanId: 'test-scan',
            hash: 'test-hash',
          },
        },
      };

      renderComponent(stateWithParams);

      const backButton = screen.getByRole('button', { name: 'Back' });
      fireEvent.click(backButton);

      expect(stateGoSpy).toHaveBeenCalledWith(
        'componentDetailsPageWithinPrioritiesPageContainerFromReports.bulkWaive',
        {
          publicId: 'test-app',
          scanId: 'test-scan',
          hash: 'test-hash',
        }
      );
    });

    it('saves waiver configuration to redux state when Next button is clicked', () => {
      const setWaiverConfigurationSpy = jest.spyOn(waiverActions, 'setWaiverConfiguration');

      const stateWithParams = {
        ...stateWithViolationsAndData,
        router: {
          ...stateWithViolationsAndData.router,
          currentParams: {
            publicId: 'test-app',
            scanId: 'test-scan',
          },
        },
      };

      renderComponent(stateWithParams);

      // Change scope selection (tests scope dropdown interaction)
      const scopeDropdown = screen.getByLabelText('select scope');
      fireEvent.change(scopeDropdown, { target: { value: 'scope-2' } });

      // Select waiver expiration
      const thirtyDaysOption = screen.getByRole('option', { name: '30 Days' });
      const expirationSelect = thirtyDaysOption.closest('select');
      fireEvent.change(expirationSelect, { target: { value: '30' } });

      // Select a reason
      const reasonSelect = screen.getByDisplayValue('Select');
      fireEvent.change(reasonSelect, { target: { value: 'reason-1' } });

      // Add comments
      const commentsTextarea = screen.getByRole('textbox');
      fireEvent.change(commentsTextarea, { target: { value: 'Test comment' } });

      // Test Components radio button selection
      const exactComponentRadio = screen.getByRole('radio', { name: 'Exact' });
      const allVersionsRadio = screen.getByRole('radio', { name: 'All Versions' });

      // Verify default selection (All Versions should be checked)
      expect(allVersionsRadio).toBeChecked();
      expect(exactComponentRadio).not.toBeChecked();

      // Change to Exact Component
      fireEvent.click(exactComponentRadio);

      // Verify radio button state changed
      expect(exactComponentRadio).toBeChecked();
      expect(allVersionsRadio).not.toBeChecked();

      // Click the Next button
      const nextButton = screen.getByRole('button', { name: 'Next' });
      fireEvent.click(nextButton);

      // Verify final configuration was saved (uses updated scope and component strategy)
      expect(setWaiverConfigurationSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          waiverReasonId: 'reason-1',
          expiryTime: '30',
          customExpiryTime: null, // Not custom expiry, so should be null
          comments: 'Test comment',
          componentMatcherStrategy: 'EXACT_COMPONENT', // Changed from default ALL_VERSIONS to EXACT_COMPONENT
          selectedWaiverScope: { id: 'scope-2', name: 'Test Application', label: 'Application' },
        })
      );
    });

    it('shows custom date picker when Custom expiration is selected', () => {
      const stateWithParams = {
        ...stateWithViolationsAndData,
        router: {
          ...stateWithViolationsAndData.router,
          currentParams: {
            publicId: 'test-app',
            scanId: 'test-scan',
          },
        },
        requestWaiver: {
          ...stateWithViolationsAndData.requestWaiver,
          customExpiryTime: nxDateInputStateHelpers.initialState(''), // Start with no custom date set
        },
      };

      renderComponent(stateWithParams);

      // Initially date picker should not be visible
      expect(screen.queryByLabelText('set custom expiration date')).not.toBeInTheDocument();

      // Select Custom option
      const customOption = screen.getByRole('option', { name: 'Custom' });
      const expirationSelect = customOption.closest('select');
      fireEvent.change(expirationSelect, { target: { value: 'custom' } });

      // Now date picker should be visible
      expect(screen.getByLabelText('set custom expiration date')).toBeVisible();
    });

    it('saves custom expiry time to configuration when custom date is already set and Next is clicked', () => {
      const customDate = moment().add(6, 'months').format('YYYY-MM-DD');

      const setWaiverConfigurationSpy = jest.spyOn(waiverActions, 'setWaiverConfiguration');

      // Start with a custom date already set in state (simulating user has already picked a date)
      const stateWithCustomDate = {
        ...stateWithViolationsAndData,
        router: {
          ...stateWithViolationsAndData.router,
          currentParams: {
            publicId: 'test-app',
            scanId: 'test-scan',
          },
        },
        requestWaiver: {
          ...stateWithViolationsAndData.requestWaiver,
          customExpiryTime: nxDateInputStateHelpers.userInput(() => null, customDate), // Custom date already set
        },
      };

      renderComponent(stateWithCustomDate);

      // Select Custom expiration option
      const customOption = screen.getByRole('option', { name: 'Custom' });
      const expirationSelect = customOption.closest('select');
      fireEvent.change(expirationSelect, { target: { value: 'custom' } });

      // Verify date picker is visible and shows the custom date
      expect(screen.getByLabelText('set custom expiration date')).toBeVisible();

      // Click Next button
      const nextButton = screen.getByRole('button', { name: 'Next' });
      fireEvent.click(nextButton);

      // Verify configuration includes the custom date
      expect(setWaiverConfigurationSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          expiryTime: 'custom',
          customExpiryTime: expect.objectContaining({
            value: customDate,
            isPristine: false,
            validationErrors: null,
          }),
          waiverReasonId: null,
          comments: '',
          componentMatcherStrategy: 'ALL_VERSIONS',
          selectedWaiverScope: { id: 'scope-1', name: 'Test Organization', label: 'Organization' },
        })
      );
    });
  });

  describe('Unknown component handling', () => {
    it('shows info alert when mixed violations (unknown + identified) are selected with All Versions strategy', () => {
      const stateWithMixedViolations = {
        ...preloadedState,
        waivers: {
          ...preloadedState.waivers,
          bulkWaive: {
            ...preloadedState.waivers.bulkWaive,
            selectedViolations: [
              { policyViolationId: 'violation-1', matchState: 'unknown' },
              { policyViolationId: 'violation-2', matchState: 'identified' },
            ],
          },
        },
      };

      renderComponent(stateWithMixedViolations);

      const allVersionsRadio = screen.getByRole('radio', { name: 'All Versions' });
      fireEvent.click(allVersionsRadio);

      expect(screen.getByText(/unknown\/unclaimed components/i)).toBeVisible();
      expect(screen.getByText(/only apply to identified components/i)).toBeVisible();
    });

    it('hides info alert when mixed violations are selected with Exact strategy', () => {
      const stateWithMixedViolations = {
        ...preloadedState,
        waivers: {
          ...preloadedState.waivers,
          bulkWaive: {
            ...preloadedState.waivers.bulkWaive,
            selectedViolations: [
              { policyViolationId: 'violation-1', matchState: 'unknown' },
              { policyViolationId: 'violation-2', matchState: 'identified' },
            ],
          },
        },
      };

      renderComponent(stateWithMixedViolations);

      const exactRadio = screen.getByRole('radio', { name: 'Exact' });
      fireEvent.click(exactRadio);

      expect(screen.queryByText(/unknown\/unclaimed components/i)).not.toBeInTheDocument();
    });

    it('disables All Versions radio when only unknown violations are selected', () => {
      const stateWithOnlyUnknown = {
        ...preloadedState,
        waivers: {
          ...preloadedState.waivers,
          bulkWaive: {
            ...preloadedState.waivers.bulkWaive,
            selectedViolations: [
              { policyViolationId: 'violation-1', matchState: 'unknown' },
              { policyViolationId: 'violation-2', matchState: 'unknown' },
            ],
          },
        },
      };

      renderComponent(stateWithOnlyUnknown);

      const allVersionsRadio = screen.getByRole('radio', { name: 'All Versions' });
      expect(allVersionsRadio).toBeDisabled();
      expect(screen.getByRole('radio', { name: 'Exact' })).toBeChecked();
    });

    it('shows tooltip on disabled All Versions radio when only unknown violations are selected', async () => {
      const stateWithOnlyUnknown = {
        ...preloadedState,
        waivers: {
          ...preloadedState.waivers,
          bulkWaive: {
            ...preloadedState.waivers.bulkWaive,
            selectedViolations: [{ policyViolationId: 'violation-1', matchState: 'unknown' }],
          },
        },
      };

      renderComponent(stateWithOnlyUnknown);

      const allVersionsRadio = screen.getByRole('radio', { name: 'All Versions' });
      expect(allVersionsRadio).toBeDisabled();

      fireEvent.mouseOver(allVersionsRadio);
      const tooltip = await screen.findByRole('tooltip');
      expect(tooltip).toHaveTextContent('Claim these components to apply all versions waiver');
    });

    it('enables All Versions radio when only identified violations are selected', () => {
      const stateWithOnlyIdentified = {
        ...preloadedState,
        waivers: {
          ...preloadedState.waivers,
          bulkWaive: {
            ...preloadedState.waivers.bulkWaive,
            selectedViolations: [
              { policyViolationId: 'violation-1', matchState: 'identified' },
              { policyViolationId: 'violation-2', matchState: 'identified' },
            ],
          },
        },
      };

      renderComponent(stateWithOnlyIdentified);

      const allVersionsRadio = screen.getByRole('radio', { name: 'All Versions' });
      expect(allVersionsRadio).not.toBeDisabled();
      expect(allVersionsRadio).toBeChecked();
    });
  });

  describe('Pro Tier Gating', () => {
    beforeEach(() => {
      const base = getDefaultPreloadedState();
      preloadedState = {
        ...base,
        productFeatures: { productFeatures: {} }, productLicense: { license: { products: ['Sonatype Lifecycle Pro'] } },
        addWaiver: { loading: false, loadError: null, availableWaiverScopes: ['APPLICATION'] },
        waivers: {
          ...base.waivers,
          bulkWaive: {
            ...base.waivers.bulkWaive,
            selectedViolations: [
              { policyViolationId: 'v1', componentIdentifier: { coordinates: {} }, threatLevel: 5 },
            ],
          },
        },
      };
    });

    it('shows enterprise banner when bulk-waivers feature is absent', () => {
      renderComponent();
      expect(screen.getByText(/Efficiently manage multiple policy violations/)).toBeVisible();
    });

    it('renders page with enterprise banner visible', () => {
      renderComponent();
      expect(screen.getByText(/Efficiently manage multiple policy violations/)).toBeVisible();
    });
  });

  function renderComponent(additionalState = {}) {
    const finalState = { ...preloadedState, ...additionalState };
    return render(<WaiverConfigurationPage />, { preloadedState: finalState });
  }

  function getDefaultPreloadedState() {
    return {
      productFeatures: {
        productFeatures: { 'bulk-waivers': true },
      },
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
      router: {
        currentState: {
          name: 'applicationReport.waiverConfiguration',
          url: '/organizations/{organizationId}/applications/{publicId}/reports/{scanId}/waiverConfiguration',
          data: {},
        },
        currentParams: {},
        prevState: {},
        prevParams: {},
      },
    };
  }
});
