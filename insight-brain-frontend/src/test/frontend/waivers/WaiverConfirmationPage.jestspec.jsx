/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen, axiosMockAdapter, waitFor } from 'TestRoot/SpecUtil';
import WaiverConfirmationPage from 'MainRoot/waivers/WaiverConfirmationPage';
import { getBulkWaiverUrl, getPolicyWaiverReasonsUrl } from 'MainRoot/util/CLMLocation';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import { actions as waiverActions } from 'MainRoot/waivers/waiverSlice';
import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';
import { getExpiryTime } from 'MainRoot/util/waiverUtils';
import { nxDateInputStateHelpers } from '@sonatype/react-shared-components';
import moment from 'moment';

describe('WaiverConfirmationPage component', () => {
  let preloadedState, axiosMock, stateGoSpy;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    preloadedState = getDefaultPreloadedState();
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');

    // Mock waiver reasons API call
    axiosMock.onGet(getPolicyWaiverReasonsUrl()).reply(200, [
      { id: 'reason-1', reasonText: 'False Positive', type: 'system' },
      { id: 'reason-2', reasonText: 'Risk Accepted', type: 'system' },
    ]);
  });

  it('renders the page title', () => {
    renderComponent();

    const pageTitle = screen.getByRole('heading', { name: 'Bulk Waiver' });
    expect(pageTitle).toBeVisible();
  });

  it('renders the confirmation section title', () => {
    renderComponent();

    const sectionTitle = screen.getByRole('heading', { name: 'Confirmation' });
    expect(sectionTitle).toBeVisible();
  });

  it('redirects to bulk waive page if no violations are selected', () => {
    const stateWithoutViolations = {
      ...preloadedState,
      waivers: {
        ...preloadedState.waivers,
        bulkWaive: {
          ...preloadedState.waivers.bulkWaive,
          selectedViolations: [],
        },
      },
      router: {
        ...preloadedState.router,
        currentParams: {
          publicId: 'test-app',
          scanId: 'test-scan',
        },
      },
    };

    renderComponent(stateWithoutViolations);

    expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.bulkWaive', {
      publicId: 'test-app',
      scanId: 'test-scan',
    });
  });

  describe('with selected violations and configuration', () => {
    let stateWithViolationsAndConfiguration;

    beforeEach(() => {
      stateWithViolationsAndConfiguration = {
        ...preloadedState,
        waivers: {
          ...preloadedState.waivers,
          waiverReasons: {
            loading: false,
            loadError: null,
            data: [
              { id: 'reason-1', reasonText: 'False Positive', type: 'system' },
              { id: 'reason-2', reasonText: 'Risk Accepted', type: 'system' },
            ],
          },
          bulkWaive: {
            ...preloadedState.waivers.bulkWaive,
            selectedViolations: [
              {
                policyViolationId: 'violation-1',
                policyName: 'Test Policy 1',
                derivedComponentName: 'test-component:1.0.0',
                policyThreatLevel: 10, // Critical
              },
              {
                policyViolationId: 'violation-2',
                policyName: 'Test Policy 2',
                derivedComponentName: 'test-component:2.0.0',
                policyThreatLevel: 7, // Severe
              },
              {
                policyViolationId: 'violation-3',
                policyName: 'Test Policy 3',
                derivedComponentName: 'test-component:1.0.0',
                policyThreatLevel: 3, // Moderate
              },
              {
                policyViolationId: 'violation-4',
                policyName: 'Test Policy 4',
                derivedComponentName: 'test-component:1.5.0',
                policyThreatLevel: 10, // Critical
              },
            ],
            waiverConfiguration: {
              waiverReasonId: 'reason-1',
              expiryTime: '30',
              customExpiryTime: null,
              comments: 'Test waiver comment',
              componentMatcherStrategy: 'ALL_VERSIONS',
              selectedWaiverScope: {
                id: 'scope-1',
                name: 'Test Organization',
                label: 'Organization',
                type: 'organization',
              },
            },
            submitMaskState: null,
            submitError: null,
          },
        },
        router: {
          ...preloadedState.router,
          currentParams: {
            publicId: 'test-app',
            scanId: 'test-scan',
          },
        },
      };
    });

    it('displays violations being waived with correct count and components', () => {
      renderComponent(stateWithViolationsAndConfiguration);

      expect(screen.getByText('4 total violations across 3 components')).toBeVisible();
    });

    it('displays singular violation and component correctly', () => {
      const stateWithSingularViolation = {
        ...stateWithViolationsAndConfiguration,
        waivers: {
          ...stateWithViolationsAndConfiguration.waivers,
          bulkWaive: {
            ...stateWithViolationsAndConfiguration.waivers.bulkWaive,
            selectedViolations: [
              {
                policyViolationId: 'violation-1',
                policyName: 'Test Policy 1',
                derivedComponentName: 'test-component:1.0.0',
                policyThreatLevel: 10,
              },
            ],
          },
        },
      };

      renderComponent(stateWithSingularViolation);

      expect(screen.getByText('1 total violation across 1 component')).toBeVisible();
    });

    it('displays plural violations with singular component correctly', () => {
      const stateWithMultipleViolationsSameComponent = {
        ...stateWithViolationsAndConfiguration,
        waivers: {
          ...stateWithViolationsAndConfiguration.waivers,
          bulkWaive: {
            ...stateWithViolationsAndConfiguration.waivers.bulkWaive,
            selectedViolations: [
              {
                policyViolationId: 'violation-1',
                policyName: 'Test Policy 1',
                derivedComponentName: 'test-component:1.0.0',
                policyThreatLevel: 10,
              },
              {
                policyViolationId: 'violation-2',
                policyName: 'Test Policy 2',
                derivedComponentName: 'test-component:1.0.0',
                policyThreatLevel: 7,
              },
            ],
          },
        },
      };

      renderComponent(stateWithMultipleViolationsSameComponent);

      expect(screen.getByText('2 total violations across 1 component')).toBeVisible();
    });

    it('displays policy threat level counts correctly', () => {
      renderComponent(stateWithViolationsAndConfiguration);

      // Critical: 2, Severe: 1, Moderate: 1
      const threatCounter = screen.getByText('Policy Violations being waived').closest('fieldset');
      expect(threatCounter).toBeInTheDocument();
      expect(threatCounter).toHaveTextContent('Critical2Severe1Moderate1');
    });

    it('displays scope information correctly for organization', () => {
      renderComponent(stateWithViolationsAndConfiguration);

      expect(screen.getByText('Organization - Test Organization')).toBeVisible();
    });

    it('displays scope information correctly for repository container', () => {
      const stateWithRepoScope = {
        ...stateWithViolationsAndConfiguration,
        waivers: {
          ...stateWithViolationsAndConfiguration.waivers,
          bulkWaive: {
            ...stateWithViolationsAndConfiguration.waivers.bulkWaive,
            waiverConfiguration: {
              ...stateWithViolationsAndConfiguration.waivers.bulkWaive.waiverConfiguration,
              selectedWaiverScope: {
                id: 'repo-1',
                name: 'Test Repository',
                label: 'Repository_container',
                type: 'repository',
              },
            },
          },
        },
      };

      renderComponent(stateWithRepoScope);

      expect(screen.getByText('Test Repository')).toBeVisible();
    });

    it('displays component matcher strategy', () => {
      renderComponent(stateWithViolationsAndConfiguration);

      expect(screen.getByText('All Versions')).toBeVisible();
    });

    it('displays expiration time for numeric days', () => {
      renderComponent(stateWithViolationsAndConfiguration);

      const daysToAdd = 30;
      expect(screen.getByText(`${daysToAdd} days`)).toBeVisible();
    });

    it('displays never expiration correctly', () => {
      const stateWithNeverExpiry = {
        ...stateWithViolationsAndConfiguration,
        waivers: {
          ...stateWithViolationsAndConfiguration.waivers,
          bulkWaive: {
            ...stateWithViolationsAndConfiguration.waivers.bulkWaive,
            waiverConfiguration: {
              ...stateWithViolationsAndConfiguration.waivers.bulkWaive.waiverConfiguration,
              expiryTime: null,
            },
          },
        },
      };

      renderComponent(stateWithNeverExpiry);

      expect(screen.getByText('Never')).toBeVisible();
    });

    it('displays custom expiration with days calculation', () => {
      const daysToAdd = 45;
      const futureDate = moment().startOf('day').add(daysToAdd, 'days').format('YYYY-MM-DD');
      const stateWithCustomExpiry = {
        ...stateWithViolationsAndConfiguration,
        waivers: {
          ...stateWithViolationsAndConfiguration.waivers,
          bulkWaive: {
            ...stateWithViolationsAndConfiguration.waivers.bulkWaive,
            waiverConfiguration: {
              ...stateWithViolationsAndConfiguration.waivers.bulkWaive.waiverConfiguration,
              expiryTime: 'custom',
              customExpiryTime: nxDateInputStateHelpers.userInput(() => null, futureDate),
            },
          },
        },
      };

      renderComponent(stateWithCustomExpiry);

      expect(screen.getByText(`${daysToAdd} days`)).toBeVisible();
    });

    it('displays waiver reason text', () => {
      renderComponent(stateWithViolationsAndConfiguration);

      expect(screen.getByText('False Positive')).toBeVisible();
    });

    it('displays comments', () => {
      renderComponent(stateWithViolationsAndConfiguration);

      expect(screen.getByText('Test waiver comment')).toBeVisible();
    });

    it('displays "--" for missing data fields', () => {
      const stateWithMissingData = {
        ...stateWithViolationsAndConfiguration,
        waivers: {
          ...stateWithViolationsAndConfiguration.waivers,
          bulkWaive: {
            ...stateWithViolationsAndConfiguration.waivers.bulkWaive,
            waiverConfiguration: {
              ...stateWithViolationsAndConfiguration.waivers.bulkWaive.waiverConfiguration,
              selectedWaiverScope: null,
              comments: '',
              waiverReasonId: '',
            },
          },
        },
      };

      renderComponent(stateWithMissingData);

      const dashElements = screen.getAllByText('--');
      expect(dashElements).toHaveLength(3); // scope, reason, and comments
    });
  });

  describe('Form submission', () => {
    let stateWithFullConfiguration;

    beforeEach(() => {
      stateWithFullConfiguration = {
        ...preloadedState,
        waivers: {
          ...preloadedState.waivers,
          waiverReasons: {
            loading: false,
            loadError: null,
            data: [{ id: 'reason-1', reasonText: 'False Positive', type: 'system' }],
          },
          bulkWaive: {
            ...preloadedState.waivers.bulkWaive,
            selectedViolations: [
              {
                policyViolationId: 'violation-1',
                policyName: 'Test Policy 1',
                derivedComponentName: 'test-component:1.0.0',
                policyThreatLevel: 8,
                matchState: 'exact',
              },
            ],
            waiverConfiguration: {
              waiverReasonId: 'reason-1',
              expiryTime: '30',
              customExpiryTime: null,
              comments: 'Test comment',
              componentMatcherStrategy: 'ALL_VERSIONS',
              selectedWaiverScope: {
                id: 'org-1',
                name: 'Test Org',
                label: 'Organization',
                type: 'organization',
              },
            },
            submitMaskState: null,
            submitError: null,
          },
        },
        router: {
          ...preloadedState.router,
          currentParams: {
            publicId: 'test-app',
            scanId: 'test-scan',
          },
        },
      };
    });

    it('submits bulk waiver with correct API call', async () => {
      const user = userEvent.setup();

      // Mock successful API response
      axiosMock.onPost(getBulkWaiverUrl('organization', 'org-1')).reply(200, {});

      renderComponent(stateWithFullConfiguration);

      const submitButton = screen.getByRole('button', { name: 'Submit' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(axiosMock.history.post).toHaveLength(1);
      });

      const postRequest = axiosMock.history.post[0];
      expect(postRequest.url).toBe(getBulkWaiverUrl('organization', 'org-1'));

      const requestBody = JSON.parse(postRequest.data);
      expect(requestBody).toEqual({
        violationIds: ['violation-1'],
        apiWaiverOptionsDTO: {
          comment: 'Test comment',
          matcherStrategy: 'ALL_VERSIONS',
          expiryTime: getExpiryTime(30),
          waiverReasonId: 'reason-1',
        },
      });
    });

    it('handles API error correctly', async () => {
      const user = userEvent.setup();

      // Mock API error
      axiosMock.onPost(getBulkWaiverUrl('organization', 'org-1')).reply(500, {
        message: 'Internal Server Error',
      });

      renderComponent(stateWithFullConfiguration);

      const submitButton = screen.getByRole('button', { name: 'Submit' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/error/i)).toBeVisible();
      });
    });

    it('submits bulk waiver with custom expiry time in request body', async () => {
      const user = userEvent.setup();
      const daysToAdd = 45;
      const futureDate = moment().startOf('day').add(daysToAdd, 'days').format('YYYY-MM-DD');

      const stateWithCustomExpiry = {
        ...stateWithFullConfiguration,
        waivers: {
          ...stateWithFullConfiguration.waivers,
          bulkWaive: {
            ...stateWithFullConfiguration.waivers.bulkWaive,
            waiverConfiguration: {
              ...stateWithFullConfiguration.waivers.bulkWaive.waiverConfiguration,
              expiryTime: 'custom',
              customExpiryTime: nxDateInputStateHelpers.userInput(() => null, futureDate),
            },
          },
        },
      };

      axiosMock.onPost(getBulkWaiverUrl('organization', 'org-1')).reply(200, {});

      renderComponent(stateWithCustomExpiry);

      const submitButton = screen.getByRole('button', { name: 'Submit' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(axiosMock.history.post).toHaveLength(1);
      });

      const requestBody = JSON.parse(axiosMock.history.post[0].data);
      // custom date is sent correctly as end of day of the selected date
      const expectedExpiryTime = moment(futureDate).endOf('day').format('YYYY-MM-DDTHH:mm:ss.SSSZZ');
      expect(requestBody.apiWaiverOptionsDTO.expiryTime).toBe(expectedExpiryTime);
      expect(requestBody.apiWaiverOptionsDTO.matcherStrategy).toBe('ALL_VERSIONS');
    });

    it('sends correct custom date (not today) when expiryTime is "custom"', async () => {
      const user = userEvent.setup();
      const daysToAdd = 5;
      const futureDate = moment().startOf('day').add(daysToAdd, 'days').format('YYYY-MM-DD');

      const stateWithCustomExpiry = {
        ...stateWithFullConfiguration,
        waivers: {
          ...stateWithFullConfiguration.waivers,
          bulkWaive: {
            ...stateWithFullConfiguration.waivers.bulkWaive,
            waiverConfiguration: {
              ...stateWithFullConfiguration.waivers.bulkWaive.waiverConfiguration,
              expiryTime: 'custom',
              customExpiryTime: nxDateInputStateHelpers.userInput(() => null, futureDate),
            },
          },
        },
      };

      // Mock successful API response since the date should now be correct
      axiosMock.onPost(getBulkWaiverUrl('organization', 'org-1')).reply(200, {});

      renderComponent(stateWithCustomExpiry);

      const submitButton = screen.getByRole('button', { name: 'Submit' });
      await user.click(submitButton);

      // Wait for the request to complete
      await waitFor(() => {
        expect(axiosMock.history.post).toHaveLength(1);
      });

      // The sent expiryTime should now correctly be 5 days in the future based on futureDate
      const requestBody = JSON.parse(axiosMock.history.post[0].data);
      const sentDate = moment(requestBody.apiWaiverOptionsDTO.expiryTime, 'YYYY-MM-DDTHH:mm:ss.SSSZZ');
      const expectedDate = moment(futureDate, 'YYYY-MM-DD').endOf('day');
      expect(sentDate.format('YYYY-MM-DD')).toBe(expectedDate.format('YYYY-MM-DD'));
    });

    it('clears previous submission error when re-entering confirmation page', () => {
      // Simulate state where user navigated back from confirmation page and forward again
      // After fix, the error should be cleared when navigating forward
      const stateAfterBackAndForward = {
        ...stateWithFullConfiguration,
        waivers: {
          ...stateWithFullConfiguration.waivers,
          bulkWaive: {
            ...stateWithFullConfiguration.waivers.bulkWaive,
            waiverConfiguration: {
              ...stateWithFullConfiguration.waivers.bulkWaive.waiverConfiguration,
              expiryTime: 'custom',
              customExpiryTime: nxDateInputStateHelpers.userInput(
                () => null,
                moment().startOf('day').add(2, 'days').format('YYYY-MM-DD')
              ),
            },
            submitMaskState: null,
            submitError: null, // Error should be cleared when nextClick dispatches resetBulkWaiverSubmitState
          },
        },
      };

      renderComponent(stateAfterBackAndForward);

      // Error should NOT be visible on page load
      expect(screen.queryByText(/expiration date must be in the future/i)).not.toBeInTheDocument();

      // Submit button should be present and enabled
      expect(screen.getByRole('button', { name: 'Submit' })).toBeVisible();

      // No API call should have been made yet
      expect(axiosMock.history.post).toHaveLength(0);
    });

    it('submits bulk waiver with never expiry (null) in request body', async () => {
      const user = userEvent.setup();

      const stateWithNeverExpiry = {
        ...stateWithFullConfiguration,
        waivers: {
          ...stateWithFullConfiguration.waivers,
          bulkWaive: {
            ...stateWithFullConfiguration.waivers.bulkWaive,
            waiverConfiguration: {
              ...stateWithFullConfiguration.waivers.bulkWaive.waiverConfiguration,
              expiryTime: null,
            },
          },
        },
      };

      axiosMock.onPost(getBulkWaiverUrl('organization', 'org-1')).reply(200, {});

      renderComponent(stateWithNeverExpiry);

      const submitButton = screen.getByRole('button', { name: 'Submit' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(axiosMock.history.post).toHaveLength(1);
      });

      const requestBody = JSON.parse(axiosMock.history.post[0].data);
      expect(requestBody.apiWaiverOptionsDTO.expiryTime).toBeNull();
    });

    it('submits bulk waiver with multiple violations in request body', async () => {
      const user = userEvent.setup();

      const stateWithMultipleViolations = {
        ...stateWithFullConfiguration,
        waivers: {
          ...stateWithFullConfiguration.waivers,
          bulkWaive: {
            ...stateWithFullConfiguration.waivers.bulkWaive,
            selectedViolations: [
              {
                policyViolationId: 'violation-1',
                policyName: 'Test Policy 1',
                derivedComponentName: 'test-component:1.0.0',
                policyThreatLevel: 8,
                matchState: 'exact',
              },
              {
                policyViolationId: 'violation-2',
                policyName: 'Test Policy 2',
                derivedComponentName: 'test-component:2.0.0',
                policyThreatLevel: 7,
                matchState: 'exact',
              },
              {
                policyViolationId: 'violation-3',
                policyName: 'Test Policy 3',
                derivedComponentName: 'test-component:3.0.0',
                policyThreatLevel: 5,
                matchState: 'exact',
              },
            ],
          },
        },
      };

      axiosMock.onPost(getBulkWaiverUrl('organization', 'org-1')).reply(200, {});

      renderComponent(stateWithMultipleViolations);

      const submitButton = screen.getByRole('button', { name: 'Submit' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(axiosMock.history.post).toHaveLength(1);
      });

      const requestBody = JSON.parse(axiosMock.history.post[0].data);
      expect(requestBody.violationIds).toEqual(['violation-1', 'violation-2', 'violation-3']);
      expect(requestBody.apiWaiverOptionsDTO.matcherStrategy).toBe('ALL_VERSIONS');
    });

    it('submits bulk waiver with EXACT_COMPONENT strategy in request body', async () => {
      const user = userEvent.setup();

      const stateWithExactStrategy = {
        ...stateWithFullConfiguration,
        waivers: {
          ...stateWithFullConfiguration.waivers,
          bulkWaive: {
            ...stateWithFullConfiguration.waivers.bulkWaive,
            waiverConfiguration: {
              ...stateWithFullConfiguration.waivers.bulkWaive.waiverConfiguration,
              componentMatcherStrategy: 'EXACT_COMPONENT',
            },
          },
        },
      };

      axiosMock.onPost(getBulkWaiverUrl('organization', 'org-1')).reply(200, {});

      renderComponent(stateWithExactStrategy);

      const submitButton = screen.getByRole('button', { name: 'Submit' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(axiosMock.history.post).toHaveLength(1);
      });

      const requestBody = JSON.parse(axiosMock.history.post[0].data);
      expect(requestBody.apiWaiverOptionsDTO.matcherStrategy).toBe('EXACT_COMPONENT');
    });

    it('filters out unknown components when using ALL_VERSIONS strategy', async () => {
      const user = userEvent.setup();

      const stateWithMixedViolations = {
        ...stateWithFullConfiguration,
        waivers: {
          ...stateWithFullConfiguration.waivers,
          bulkWaive: {
            ...stateWithFullConfiguration.waivers.bulkWaive,
            selectedViolations: [
              {
                policyViolationId: 'violation-1',
                policyName: 'Test Policy 1',
                derivedComponentName: 'test-component:1.0.0',
                policyThreatLevel: 8,
                matchState: 'exact',
              },
              {
                policyViolationId: 'violation-2',
                policyName: 'Test Policy 2',
                derivedComponentName: 'unknown-component',
                policyThreatLevel: 7,
                matchState: 'unknown',
              },
              {
                policyViolationId: 'violation-3',
                policyName: 'Test Policy 3',
                derivedComponentName: 'test-component:2.0.0',
                policyThreatLevel: 5,
                matchState: 'identified',
              },
            ],
            waiverConfiguration: {
              ...stateWithFullConfiguration.waivers.bulkWaive.waiverConfiguration,
              componentMatcherStrategy: 'ALL_VERSIONS',
            },
          },
        },
      };

      axiosMock.onPost(getBulkWaiverUrl('organization', 'org-1')).reply(200, {});

      renderComponent(stateWithMixedViolations);

      const submitButton = screen.getByRole('button', { name: 'Submit' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(axiosMock.history.post).toHaveLength(1);
      });

      const requestBody = JSON.parse(axiosMock.history.post[0].data);
      // Should only include violation-1 and violation-3, filtering out unknown violation-2
      expect(requestBody.violationIds).toEqual(['violation-1', 'violation-3']);
      expect(requestBody.apiWaiverOptionsDTO.matcherStrategy).toBe('ALL_VERSIONS');
    });

    it('includes unknown components when using EXACT_COMPONENT strategy', async () => {
      const user = userEvent.setup();

      const stateWithMixedViolations = {
        ...stateWithFullConfiguration,
        waivers: {
          ...stateWithFullConfiguration.waivers,
          bulkWaive: {
            ...stateWithFullConfiguration.waivers.bulkWaive,
            selectedViolations: [
              {
                policyViolationId: 'violation-1',
                policyName: 'Test Policy 1',
                derivedComponentName: 'test-component:1.0.0',
                policyThreatLevel: 8,
                matchState: 'exact',
              },
              {
                policyViolationId: 'violation-2',
                policyName: 'Test Policy 2',
                derivedComponentName: 'unknown-component',
                policyThreatLevel: 7,
                matchState: 'unknown',
              },
            ],
            waiverConfiguration: {
              ...stateWithFullConfiguration.waivers.bulkWaive.waiverConfiguration,
              componentMatcherStrategy: 'EXACT_COMPONENT',
            },
          },
        },
      };

      axiosMock.onPost(getBulkWaiverUrl('organization', 'org-1')).reply(200, {});

      renderComponent(stateWithMixedViolations);

      const submitButton = screen.getByRole('button', { name: 'Submit' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(axiosMock.history.post).toHaveLength(1);
      });

      const requestBody = JSON.parse(axiosMock.history.post[0].data);
      // Should include both violations when using EXACT_COMPONENT strategy
      expect(requestBody.violationIds).toEqual(['violation-1', 'violation-2']);
      expect(requestBody.apiWaiverOptionsDTO.matcherStrategy).toBe('EXACT_COMPONENT');
    });

    it('sends null for empty comments in request body', async () => {
      const user = userEvent.setup();

      const stateWithEmptyComments = {
        ...stateWithFullConfiguration,
        waivers: {
          ...stateWithFullConfiguration.waivers,
          bulkWaive: {
            ...stateWithFullConfiguration.waivers.bulkWaive,
            waiverConfiguration: {
              ...stateWithFullConfiguration.waivers.bulkWaive.waiverConfiguration,
              comments: '',
            },
          },
        },
      };

      axiosMock.onPost(getBulkWaiverUrl('organization', 'org-1')).reply(200, {});

      renderComponent(stateWithEmptyComments);

      const submitButton = screen.getByRole('button', { name: 'Submit' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(axiosMock.history.post).toHaveLength(1);
      });

      const requestBody = JSON.parse(axiosMock.history.post[0].data);
      expect(requestBody.apiWaiverOptionsDTO.comment).toBeNull();
    });

    it('submits bulk waiver with repository scope correctly', async () => {
      const user = userEvent.setup();

      const stateWithRepoScope = {
        ...stateWithFullConfiguration,
        waivers: {
          ...stateWithFullConfiguration.waivers,
          bulkWaive: {
            ...stateWithFullConfiguration.waivers.bulkWaive,
            waiverConfiguration: {
              ...stateWithFullConfiguration.waivers.bulkWaive.waiverConfiguration,
              selectedWaiverScope: {
                id: 'repo-1',
                name: 'Test Repository',
                label: 'Repository_container',
                type: 'repository',
              },
            },
          },
        },
      };

      axiosMock.onPost(getBulkWaiverUrl('repository', 'repo-1')).reply(200, {});

      renderComponent(stateWithRepoScope);

      const submitButton = screen.getByRole('button', { name: 'Submit' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(axiosMock.history.post).toHaveLength(1);
      });

      const postRequest = axiosMock.history.post[0];
      expect(postRequest.url).toBe(getBulkWaiverUrl('repository', 'repo-1'));

      const requestBody = JSON.parse(postRequest.data);
      expect(requestBody).toEqual({
        violationIds: ['violation-1'],
        apiWaiverOptionsDTO: {
          comment: 'Test comment',
          matcherStrategy: 'ALL_VERSIONS',
          expiryTime: getExpiryTime(30),
          waiverReasonId: 'reason-1',
        },
      });
    });
  });

  describe('Navigation', () => {
    let stateWithParams;

    beforeEach(() => {
      stateWithParams = {
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
                policyThreatLevel: 8,
              },
            ],
            waiverConfiguration: {
              waiverReasonId: 'reason-1',
              expiryTime: '30',
              customExpiryTime: null,
              comments: '',
              componentMatcherStrategy: 'ALL_VERSIONS',
              selectedWaiverScope: {
                id: 'org-1',
                name: 'Test Org',
                label: 'Organization',
                type: 'organization',
              },
            },
          },
        },
        router: {
          ...preloadedState.router,
          currentParams: {
            publicId: 'test-app',
            scanId: 'test-scan',
          },
        },
      };
    });

    it('navigates to waiver configuration page when Back button is clicked', async () => {
      const user = userEvent.setup();
      renderComponent(stateWithParams);

      const backButton = screen.getByRole('button', { name: 'Back' });
      await user.click(backButton);

      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.waiverConfiguration', {
        publicId: 'test-app',
        scanId: 'test-scan',
      });
    });

    it('navigates to policy page when Cancel button is clicked', async () => {
      const user = userEvent.setup();
      renderComponent(stateWithParams);

      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      await user.click(cancelButton);

      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.policy', {
        publicId: 'test-app',
        scanId: 'test-scan',
      });
    });
  });

  describe('Success handling', () => {
    let stateWithSuccessfulSubmission;
    let clearBulkWaiveCheckboxesSpy, resetWaiverConfigurationSpy, addToastSpy;

    beforeEach(() => {
      clearBulkWaiveCheckboxesSpy = jest.spyOn(waiverActions, 'clearBulkWaiveCheckboxes');
      resetWaiverConfigurationSpy = jest.spyOn(waiverActions, 'resetWaiverConfiguration');
      addToastSpy = jest.spyOn(toastActions, 'addToast');

      stateWithSuccessfulSubmission = {
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
                policyThreatLevel: 8,
              },
            ],
            submitMaskState: true, // Success state
            submitError: null,
          },
        },
        router: {
          ...preloadedState.router,
          currentParams: {
            publicId: 'test-app',
            scanId: 'test-scan',
          },
        },
      };
    });

    it('redirects to policy page and shows success toast on successful submission', () => {
      renderComponent(stateWithSuccessfulSubmission);

      expect(stateGoSpy).toHaveBeenCalledWith('applicationReport.policy', {
        publicId: 'test-app',
        scanId: 'test-scan',
      });

      expect(clearBulkWaiveCheckboxesSpy).toHaveBeenCalled();
      expect(resetWaiverConfigurationSpy).toHaveBeenCalled();
      expect(addToastSpy).toHaveBeenCalledWith({
        type: 'success',
        message: 'Bulk Waivers will apply when report is re-evaluated.',
      });
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
              { policyViolationId: 'violation-1', matchState: 'unknown', policyThreatLevel: 8 },
              { policyViolationId: 'violation-2', matchState: 'identified', policyThreatLevel: 7 },
            ],
            waiverConfiguration: {
              waiverReasonId: 'reason-1',
              expiryTime: '30',
              customExpiryTime: null,
              comments: '',
              componentMatcherStrategy: 'ALL_VERSIONS',
              selectedWaiverScope: {
                id: 'org-1',
                name: 'Test Org',
                label: 'Organization',
                type: 'organization',
              },
            },
          },
        },
      };

      renderComponent(stateWithMixedViolations);

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
              { policyViolationId: 'violation-1', matchState: 'unknown', policyThreatLevel: 8 },
              { policyViolationId: 'violation-2', matchState: 'identified', policyThreatLevel: 7 },
            ],
            waiverConfiguration: {
              waiverReasonId: 'reason-1',
              expiryTime: '30',
              customExpiryTime: null,
              comments: '',
              componentMatcherStrategy: 'EXACT_COMPONENT',
              selectedWaiverScope: {
                id: 'org-1',
                name: 'Test Org',
                label: 'Organization',
                type: 'organization',
              },
            },
          },
        },
      };

      renderComponent(stateWithMixedViolations);

      expect(screen.queryByText(/unknown\/unclaimed components/i)).not.toBeInTheDocument();
    });

    it('hides info alert when only identified violations are selected', () => {
      const stateWithOnlyIdentified = {
        ...preloadedState,
        waivers: {
          ...preloadedState.waivers,
          bulkWaive: {
            ...preloadedState.waivers.bulkWaive,
            selectedViolations: [
              { policyViolationId: 'violation-1', matchState: 'identified', policyThreatLevel: 8 },
              { policyViolationId: 'violation-2', matchState: 'identified', policyThreatLevel: 7 },
            ],
            waiverConfiguration: {
              waiverReasonId: 'reason-1',
              expiryTime: '30',
              customExpiryTime: null,
              comments: '',
              componentMatcherStrategy: 'ALL_VERSIONS',
              selectedWaiverScope: {
                id: 'org-1',
                name: 'Test Org',
                label: 'Organization',
                type: 'organization',
              },
            },
          },
        },
      };

      renderComponent(stateWithOnlyIdentified);

      expect(screen.queryByText(/unknown\/unclaimed components/i)).not.toBeInTheDocument();
    });

    it('hides info alert when only unknown violations are selected', () => {
      const stateWithOnlyUnknown = {
        ...preloadedState,
        waivers: {
          ...preloadedState.waivers,
          bulkWaive: {
            ...preloadedState.waivers.bulkWaive,
            selectedViolations: [
              { policyViolationId: 'violation-1', matchState: 'unknown', policyThreatLevel: 8 },
              { policyViolationId: 'violation-2', matchState: 'unknown', policyThreatLevel: 7 },
            ],
            waiverConfiguration: {
              waiverReasonId: 'reason-1',
              expiryTime: '30',
              customExpiryTime: null,
              comments: '',
              componentMatcherStrategy: 'EXACT_COMPONENT',
              selectedWaiverScope: {
                id: 'org-1',
                name: 'Test Org',
                label: 'Organization',
                type: 'organization',
              },
            },
          },
        },
      };

      renderComponent(stateWithOnlyUnknown);

      expect(screen.queryByText(/unknown\/unclaimed components/i)).not.toBeInTheDocument();
    });
  });

  function renderComponent(additionalState = {}) {
    const finalState = { ...preloadedState, ...additionalState };
    return render(<WaiverConfirmationPage />, { preloadedState: finalState });
  }

  function getDefaultPreloadedState() {
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
          selectedViolations: [
            {
              policyViolationId: 'violation-1',
              policyName: 'Test Policy 1',
              derivedComponentName: 'test-component:1.0.0',
              policyThreatLevel: 8,
            },
          ],
          waiverConfiguration: {
            waiverReasonId: 'reason-1',
            expiryTime: '30',
            customExpiryTime: null,
            comments: 'Test comment',
            componentMatcherStrategy: 'ALL_VERSIONS',
            selectedWaiverScope: {
              id: 'org-1',
              name: 'Test Org',
              label: 'Organization',
              type: 'organization',
            },
          },
          submitMaskState: null,
          submitError: null,
        },
      },
      router: {
        currentState: {
          name: 'applicationReport.waiverConfirmation',
          url: '/organizations/{organizationId}/applications/{publicId}/reports/{scanId}/waiverConfirmation',
          data: {},
        },
        currentParams: {
          publicId: 'test-app',
          scanId: 'test-scan',
        },
        prevState: {},
        prevParams: {},
      },
    };
  }
});
