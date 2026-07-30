/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import RequestWaiverReviewPage from 'MainRoot/waivers/RequestWaiverReviewPage';
import { axiosMockAdapter, fireEvent, render, screen, waitFor } from 'TestRoot/SpecUtil';
import { waiverRequestStatus } from 'MainRoot/util/waiverUtils';
import router from 'MainRoot/router/routerInstance';
import {
  getApplicationSummaryUrl,
  getOwnerContextHierarchyUrl,
  getPermissionContextTestUrl,
  getViewOrUpdatePolicyWaiverRequestUrl,
  getReviewPolicyWaiverRequestUrl,
} from 'MainRoot/util/CLMLocation';
import { fetchCrossStageViolation } from 'MainRoot/violation/violationActions';
import { clone } from 'ramda';
import userEvent from '@testing-library/user-event';
import { within } from '@testing-library/react';

describe('RequestWaiverReviewPage', function () {
  let renderComponent;
  let mock;
  const ownerType = 'application';
  const ownerId = 'someApplicationPublicId';
  const violationId = 'someViolationId';
  const rootOrganizationId = 'ROOT_ORGANIZATION_ID';
  const organizationId = 'someOrgId';
  const organizationName = 'someOrgName';
  const applicationPublicId = 'someApplicationPublicId';
  const internalApplicationId = 'someInternalApplicationId';
  const applicationName = 'someApplicationName';
  const policyId = 'policyId';
  const violationDetails = {
    constraintViolations: [
      {
        constraintId: 'constraintId',
        constraintName: 'constraintName',
        reasons: [
          {
            reason: 'reason',
            reference: {
              value: 'vulnerabilityId',
            },
          },
        ],
      },
    ],
    filename: 'componentName',
    policyViolationId: violationId,
    policyName: 'policyName',
    policyId,
    applicationPublicId,
    threatLevel: 9,
  };

  const defaultPreloadedState = {
    addWaiver: {
      availableWaiverScopes: [
        {
          type: 'organization',
          id: rootOrganizationId,
          name: 'Root Organization',
        },
        {
          type: 'organization',
          id: organizationId,
          name: organizationName,
        },
        {
          type: 'application',
          id: applicationPublicId,
          name: applicationName,
        },
      ],
      selectedWaiverScope: {
        type: 'application',
        id: applicationPublicId,
      },
    },
    requestWaiver: {
      submitError: null,
      selectedWaiverScope: {
        type: 'application',
        id: applicationPublicId,
      },
      comments: {
        isPristine: false,
        value: 'some comment',
      },
    },
    requestWaiverDetails: {
      loading: false,
      loadError: null,
      waiverRequestDetails: {
        policyViolationId: violationId,
        policyWaiverReasonId: '9b704ef5bc064fc29d7fe08a251ee9a6',
        scopeOwnerType: 'application',
        scopeOwnerId: internalApplicationId,
        scopeOwnerName: applicationName,
        matcherStrategy: 'EXACT_COMPONENT',
        expiryTime: null,
        comment: 'some comment',
        noteToReviewer: 'some note to reviewer',
        requestTime: '2025-05-09T04:00:00Z',
        requesterName: 'someUserName',
        status: waiverRequestStatus.REQUESTED,
      },
    },
    router: {
      currentParams: {
        '#': null,
        ownerId: 'applicationId',
        ownerType: 'application',
        policyWaiverRequestId: 'policyWaiverRequestId',
      },
      prevParams: {
        '#': null,
      },
      prevState: { name: 'dashboard.overview.waiverRequests' },
    },
    violation: {
      selectedViolationId: violationId,
      violationDetails,
      loading: false,
      violationDetailsError: null,
    },
    waivers: {
      waiverReasons: {
        loading: false,
        loadError: null,
        data: [
          {
            id: '9b704ef5bc064fc29d7fe08a251ee9a6',
            type: 'system',
            reasonText: 'Acknowledged violation',
          },
          {
            id: '42069f58114f4df8b435a40a415d2835',
            type: 'system',
            reasonText: 'Mitigated externally',
          },
          {
            id: '39984de3d6e64f508df82b4cbfd72f70',
            type: 'system',
            reasonText: 'No upgrade path',
          },
        ],
      },
    },
  };

  beforeEach(() => {
    mock = axiosMockAdapter({ delayResponse: 200 }); // delay necessary for loading test

    jest.spyOn(router.stateService, 'href').mockReturnValue('#');
    jest.spyOn(router.stateService, 'includes').mockReturnValue(false);

    mock
      .onGet(getViewOrUpdatePolicyWaiverRequestUrl('application', 'applicationId', 'policyWaiverRequestId'))
      .reply(200, defaultPreloadedState.requestWaiverDetails.waiverRequestDetails);

    mock.onGet(getApplicationSummaryUrl(applicationPublicId)).reply(200, {
      contact: null,
      hasPendingSourceControlPolicyEvaluation: false,
      id: internalApplicationId,
      name: 'someAppName',
      organizationId: 'someOrgId',
      organizationName: 'someOrgName',
      policyEvaluations: {},
      policyEvaluationsResults: {},
      publicId: applicationPublicId,
    });

    mock.onGet(fetchCrossStageViolation(violationId)).reply(200, violationDetails);

    mock
      .onPut(getPermissionContextTestUrl('application', internalApplicationId))
      .reply(200, ['WAIVE_POLICY_VIOLATIONS']);

    mock.onGet(getOwnerContextHierarchyUrl(ownerType, ownerId, policyId)).reply(200, {
      id: 'ROOT_ORGANIZATION_ID',
      name: 'Root Organization',
      type: 'organization',
      children: [
        {
          id: 'someOrgId',
          name: 'Some Org',
          type: 'organization',
          children: [{ id: applicationPublicId, name: 'Some App', type: 'application', children: null }],
        },
      ],
    });

    renderComponent = (preloadedState) =>
      render(<RequestWaiverReviewPage />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  const assertElementsDisabled = () => {
    // Get comments textarea by finding the input in the fieldset with label "Comments"
    const commentsFieldset = screen.getByText('Comments').closest('fieldset');
    const commentsTextarea = commentsFieldset.querySelector('textarea');

    const elementsToCheck = [
      screen.getByRole('combobox', { name: /select scope/i }),
      ...screen.getAllByRole('radio'),
      screen.getByRole('combobox', { name: /waiver expiration/i }),
      screen.getByRole('combobox', { name: /reason/i }),
      commentsTextarea,
    ];

    elementsToCheck.forEach((element) => {
      expect(element).toBeDisabled();
    });

    const approveButton = screen.getByRole('button', { name: 'Approve' });
    expect(approveButton).toHaveClass('disabled');

    const rejectButton = screen.getByRole('button', { name: 'Reject Waiver Request' });
    expect(rejectButton).toBeDisabled();
  };

  describe('renders the waiver request review page', () => {
    it('and all the fields prefilled with the waiver request details', async () => {
      renderComponent(defaultPreloadedState);
      //We need a delay to make sure the component is loaded
      await waitFor(() => expect(screen.queryByText('Loading…')).not.toBeInTheDocument());

      const dropdownFields = screen.getAllByRole('combobox');
      const radioFields = screen.getAllByRole('radio');
      const textboxFields = screen.getAllByRole('textbox');

      const requestedWaiverInfoTitle = screen.getByText('Requested Waiver Information');
      expect(requestedWaiverInfoTitle).toBeVisible();

      const requestedByTitle = screen.getByText('Requested By');
      const requestedByValue = screen.getByText('someUserName');
      expect(requestedByTitle).toBeVisible();
      expect(requestedByValue).toBeVisible();

      const dateRequestedTitle = screen.getByText('Date Requested');
      const dateRequestedValue = screen.getByText('2025-05-09');
      expect(dateRequestedTitle).toBeVisible();
      expect(dateRequestedValue).toBeVisible();

      const noteToReviewerTitle = screen.getByText('Note to Reviewer');
      const noteToReviewerValue = screen.getByText('some note to reviewer');
      expect(noteToReviewerTitle).toBeVisible();
      expect(noteToReviewerValue).toBeVisible();

      const requestedWaiverConfigTitle = screen.getByText('Waiver Configuration');
      expect(requestedWaiverConfigTitle).toBeVisible();

      const componentFields = screen.getAllByText('componentName');
      expect(componentFields[0]).toBeVisible();
      expect(componentFields[1]).toBeVisible();

      const policyTitle = screen.getByText('Policy');
      const policyValue = screen.getByText('policyName');
      expect(policyTitle).toBeVisible();
      expect(policyValue).toBeVisible();

      const constraintTitle = screen.getByText('Constraint Name');
      const constraintValue = screen.getByText('constraintName');
      expect(constraintTitle).toBeVisible();
      expect(constraintValue).toBeVisible();

      const conditionsTitle = screen.getByText('Conditions');
      const conditionsValue = screen.getByText('reason');
      expect(conditionsTitle).toBeVisible();
      expect(conditionsValue).toBeVisible();

      const scopeTitle = screen.getByText('Scope');
      const scopeValues = Array.from(dropdownFields[0].options).map((option) => option.value);
      const selectedScopeValue = Array.from(dropdownFields[0].options).find((option) => option.selected);
      expect(scopeTitle).toBeVisible();
      expect(scopeValues).toEqual([applicationPublicId, organizationId, rootOrganizationId]);
      expect(selectedScopeValue.textContent).toContain('Application - Some App');

      const componentsTitle = screen.getByText('Components');
      const componentsValues = Array.from(radioFields).map((radio) => radio.parentElement.textContent.trim());
      const selectedComponentValue = Array.from(radioFields).find((radio) => radio.checked);
      expect(componentsTitle).toBeVisible();
      expect(componentsValues).toEqual(['componentName', 'componentName (all versions)', 'All Components']);
      expect(selectedComponentValue.parentElement.textContent.trim()).toBe('componentName');

      const expirationTitle = screen.getByText('Waiver Expiration');
      const expirationValues = Array.from(dropdownFields[1].options).map((option) => option.value);
      const selectedExpirationValue = Array.from(dropdownFields[1].options).find((option) => option.selected);
      expect(expirationTitle).toBeVisible();
      expect(expirationValues).toEqual(['never', '7', '14', '30', '60', '90', '120', 'custom']);
      expect(selectedExpirationValue.textContent).toBe('Never');

      const reasonTitle = screen.getByText('Reason');
      const reasonValues = Array.from(dropdownFields[2].options).map((option) => option.value);
      const selectedReasonValue = Array.from(dropdownFields[2].options).find((option) => option.selected);
      expect(reasonTitle).toBeVisible();
      expect(reasonValues).toEqual([
        '',
        '9b704ef5bc064fc29d7fe08a251ee9a6',
        '42069f58114f4df8b435a40a415d2835',
        '39984de3d6e64f508df82b4cbfd72f70',
      ]);
      expect(selectedReasonValue.textContent).toBe('Acknowledged violation');

      const commentsTitle = screen.getByText('Comments');
      const commentsValue = textboxFields[0];
      expect(commentsTitle).toBeVisible();
      expect(commentsValue.value).toBe('some comment');

      const cancelButton = screen.getByRole('button', { name: /Cancel/ });
      expect(cancelButton).toBeVisible();

      const approveButton = screen.getByRole('button', { name: /Approve/ });
      expect(approveButton).toBeVisible();
    });

    it('and the Rejection button and dialog', async () => {
      renderComponent(defaultPreloadedState);
      //We need a delay to make sure the component is loaded
      await waitFor(() => expect(screen.queryByText('Loading…')).not.toBeInTheDocument());

      const rejectionButton = screen.getByRole('button', { name: /Reject Waiver Request/ });
      expect(rejectionButton).toBeVisible();

      await userEvent.click(rejectionButton);
      const dialog = await screen.findByRole('dialog');

      const rejectionDialogTitle = within(dialog).getByText('Reject Waiver Request');
      expect(rejectionDialogTitle).toBeVisible();

      const rejectionDialogText = within(dialog).getByRole('textbox');
      expect(rejectionDialogText).toBeVisible();
      fireEvent.change(rejectionDialogText, { target: { value: 'new rejection reason' } });
      expect(rejectionDialogText.value).toBe('new rejection reason');

      const cancelButton = within(dialog).getByRole('button', { name: /Cancel/ });
      expect(cancelButton).toBeVisible();

      const sendButton = within(dialog).getByRole('button', { name: /Send/ });
      expect(sendButton).toBeVisible();
    });
  });

  describe('renders error when the loading requests fail', () => {
    it('get waiver requests fail', async () => {
      mock
        .onGet(getViewOrUpdatePolicyWaiverRequestUrl('application', 'applicationId', 'policyWaiverRequestId'))
        .reply(500, 'waiver requests failed');
      renderComponent(defaultPreloadedState);
      const errorAlert = await screen.findByRole('alert');
      expect(errorAlert).toBeVisible();
      expect(errorAlert).toHaveTextContent('An error occurred loading data. waiver requests failed');
    });
  });

  describe('renders error when the states fail', () => {
    it('violationDetailsError', async () => {
      const preloadedState = clone(defaultPreloadedState);
      preloadedState.violation.violationDetails = null;
      renderComponent(preloadedState);
      const errorAlert = await screen.findByRole('alert');
      expect(errorAlert).toBeVisible();
      expect(errorAlert).toHaveTextContent('An error occurred loading data. Error');
    });

    it('addWaiverDataError', async () => {
      const preloadedState = clone(defaultPreloadedState);
      preloadedState.addWaiver.loadError = 'add waiver data failed';
      renderComponent(preloadedState);
      const errorAlert = await screen.findByRole('alert');
      expect(errorAlert).toBeVisible();
      expect(errorAlert).toHaveTextContent('add waiver data failed');
    });
  });

  // CLM-41118: when the underlying CVE data changes after a waiver is requested, the violation can
  // still resolve by id but with an empty constraintViolations array. The page must not crash.
  it('renders without crashing when the violation has no constraint violations', async () => {
    const preloadedState = clone(defaultPreloadedState);
    preloadedState.violation.violationDetails = {
      ...preloadedState.violation.violationDetails,
      constraintViolations: [],
    };
    mock.onGet(fetchCrossStageViolation(violationId)).reply(200, preloadedState.violation.violationDetails);

    renderComponent(preloadedState);

    await waitFor(() => expect(screen.queryByText('Loading…')).not.toBeInTheDocument());

    // The whole page was previously unresponsive; the approver must still be able to Cancel/Approve.
    expect(screen.getByRole('button', { name: /Cancel/ })).toBeVisible();
    expect(screen.getByRole('button', { name: /Approve/ })).toBeVisible();
  });

  it('renders in read-only mode when the waiver request is already approved', async () => {
    const preloadedState = clone(defaultPreloadedState);
    preloadedState.requestWaiverDetails.waiverRequestDetails.status = waiverRequestStatus.APPROVED;
    mock
      .onGet(getViewOrUpdatePolicyWaiverRequestUrl('application', 'applicationId', 'policyWaiverRequestId'))
      .reply(200, preloadedState.requestWaiverDetails.waiverRequestDetails);
    renderComponent(preloadedState);

    await waitFor(() => expect(screen.queryByText('Loading…')).not.toBeInTheDocument());

    assertElementsDisabled();
  });

  it('renders in read-only mode when permissions are not proper', async () => {
    mock.onPut(getPermissionContextTestUrl('application', internalApplicationId)).reply(200, []);
    renderComponent(defaultPreloadedState);

    await waitFor(() => expect(screen.queryByText('Loading…')).not.toBeInTheDocument());

    assertElementsDisabled();
  });

  describe('waiver expiration with expireWhenRemediationAvailable flag', () => {
    it('preserves expireWhenRemediationAvailable=true on approve when the loaded request has the flag set', async () => {
      const user = userEvent.setup();
      const remediationState = clone(defaultPreloadedState);
      remediationState.requestWaiverDetails.waiverRequestDetails.expireWhenRemediationAvailable = true;
      remediationState.requestWaiverDetails.waiverRequestDetails.expiryTime = null;

      mock
        .onGet(getViewOrUpdatePolicyWaiverRequestUrl('application', 'applicationId', 'policyWaiverRequestId'))
        .reply(200, remediationState.requestWaiverDetails.waiverRequestDetails);

      const reviewUrl = getReviewPolicyWaiverRequestUrl('application', internalApplicationId, 'policyWaiverRequestId');
      mock.onPost(reviewUrl).reply(200, {});

      renderComponent(remediationState);

      await waitFor(() => expect(screen.queryByText('Loading…')).not.toBeInTheDocument());

      const approveButton = screen.getByRole('button', { name: 'Approve' });
      expect(approveButton).toBeVisible();
      await user.click(approveButton);

      await waitFor(() => {
        expect(mock.history.post.length).toBe(1);
        const body = JSON.parse(mock.history.post[0].data);
        expect(body.status).toBe('APPROVED');
        expect(body.expireWhenRemediationAvailable).toBe(true);
        expect(body.expiryTime).toBeNull();
      });
    });

    it('preserves expiryTime=null on approve when the loaded request was submitted as Never', async () => {
      const user = userEvent.setup();
      const neverState = clone(defaultPreloadedState);
      neverState.requestWaiverDetails.waiverRequestDetails.expireWhenRemediationAvailable = false;
      neverState.requestWaiverDetails.waiverRequestDetails.expiryTime = null;

      mock
        .onGet(getViewOrUpdatePolicyWaiverRequestUrl('application', 'applicationId', 'policyWaiverRequestId'))
        .reply(200, neverState.requestWaiverDetails.waiverRequestDetails);

      const reviewUrl = getReviewPolicyWaiverRequestUrl('application', internalApplicationId, 'policyWaiverRequestId');
      mock.onPost(reviewUrl).reply(200, {});

      renderComponent(neverState);

      await waitFor(() => expect(screen.queryByText('Loading…')).not.toBeInTheDocument());

      await user.click(screen.getByRole('button', { name: 'Approve' }));

      await waitFor(() => {
        expect(mock.history.post.length).toBe(1);
        const body = JSON.parse(mock.history.post[0].data);
        expect(body.status).toBe('APPROVED');
        expect(body.expiryTime).toBeNull();
        expect(body.expireWhenRemediationAvailable).toBe(false);
      });
    });
  });
});
