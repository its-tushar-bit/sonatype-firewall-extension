/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import RequestWaiverPage from 'MainRoot/waivers/RequestWaiverPage';
import { axiosMockAdapter, fireEvent, render, screen, waitFor } from 'TestRoot/SpecUtil';
import router from 'MainRoot/router/routerInstance';
import {
  getApplicableWaiversUrl,
  getSimilarWaiversUrl,
  getApplicationSummaryUrl,
  getPermissionContextTestUrl,
  getCreatePolicyWaiverRequestUrl,
  getOwnerContextHierarchyUrl,
} from 'MainRoot/util/CLMLocation';
import { clone } from 'ramda';
import { initialState } from 'MainRoot/waivers/requestWaiverSlice';
import { fetchCrossStageViolation } from 'MainRoot/violation/violationActions';

describe('RequestWaiverPage', function () {
  let renderComponent;
  let mock;
  const ownerType = 'application';
  const ownerId = 'someApplicationPublicId';
  const violationId = 'someViolationId';
  const rootOrganizationId = 'ROOT_ORGANIZATION_ID';
  const organizationId = 'someOrgId';
  const applicationPublicId = 'someApplicationPublicId';
  const internalApplicationId = 'someInternalApplicationId';
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
        },
        {
          type: 'organization',
          id: organizationId,
        },
        {
          type: 'application',
          id: applicationPublicId,
        },
      ],
      selectedWaiverScope: {
        type: 'application',
        id: applicationPublicId,
      },
      componentMatcherStrategy: 'EXACT_COMPONENT',
      loadError: null,
    },
    firewall: {
      componentDetailsPage: {
        showManageWaiverPage: false,
      },
    },
    requestWaiver: {
      loading: true,
      loadError: null,
      selectedWaiverScope: {
        type: 'application',
        id: applicationPublicId,
      },
      comments: {
        isPristine: true,
        value: '',
      },
      noteToReviewer: {
        isPristine: true,
        value: '',
      },
    },
    router: {
      currentParams: {
        violationId,
      },
      prevParams: {
        publicId: 'publicId',
        scanId: 'scanId',
        hash: 'hash',
      },
      prevState: { name: 'applicationReport.violationWaivers' },
    },
    violation: {
      selectedViolationId: violationId,
      violationDetails,
      loading: false,
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

    jest.spyOn(router.stateService, 'href').mockImplementation((url, params) => {
      if (url.includes('sidebarView.violation')) {
        const violationId = params.id;
        return `#/violation/${violationId}`;
      }
      if (url.includes('addWaiver')) {
        const violationId = params.violationId;
        return `#/addWaiver/${violationId}`;
      }
      return '#';
    });
    jest.spyOn(router.stateService, 'includes').mockReturnValue(false);

    mock.onGet(getApplicableWaiversUrl(violationId)).reply(200, { activeWaivers: [], expiredWaivers: [] });
    mock.onGet(getSimilarWaiversUrl(violationId)).reply(200, []);
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
    mock.onGet(fetchCrossStageViolation(violationId)).reply(200, {});
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
          children: [{ id: 'someApplicationPublicId', name: 'Some App', type: 'application', children: null }],
        },
      ],
    });

    renderComponent = (preloadedState) =>
      render(<RequestWaiverPage />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  describe('when neither policyWaiverRequestId or violationId is provided', () => {
    it('renders a LoadWrapper with an error message', async () => {
      const preloadedState = clone(defaultPreloadedState);
      preloadedState.router.currentParams.violationId = null;
      renderComponent(preloadedState);
      const error = await screen.findByText(
        'An error occurred loading data. No Violation ID or Waiver Request ID provided.'
      );
      expect(error).toBeVisible();
    });
  });

  it('renders a page title and loading', async () => {
    renderComponent(defaultPreloadedState);
    const loading = await screen.findByText('Loading…');
    expect(loading).toBeVisible();
    const title = screen.getByText('Request Waiver');
    expect(title).toBeVisible();
  });

  describe('renders error when the loading requests fail', () => {
    it('applicable waivers fail', async () => {
      mock.onGet(getApplicableWaiversUrl(violationId)).reply(500, 'waivers failed');
      renderComponent(defaultPreloadedState);
      const errorAlert = await screen.findByRole('alert');
      expect(errorAlert).toBeVisible();
      expect(errorAlert).toHaveTextContent('waivers failed');
    });

    it('application summary fail', async () => {
      mock.onGet(getApplicationSummaryUrl(applicationPublicId)).reply(500, 'application summary failed');
      renderComponent(defaultPreloadedState);
      const errorAlert = await screen.findByRole('alert');
      expect(errorAlert).toBeVisible();
      expect(errorAlert).toHaveTextContent('application summary failed');
    });

    it('permissions fail', async () => {
      mock.onPut(getPermissionContextTestUrl('application', internalApplicationId)).reply(500, 'permissions failed');
      renderComponent(defaultPreloadedState);
      const errorAlert = await screen.findByRole('alert');
      expect(errorAlert).toBeVisible();
      expect(errorAlert).toHaveTextContent('permissions failed');
    });
  });

  describe('renders error when the states fail', () => {
    it('addWaiverDataError occurs', async () => {
      const preloadedState = clone(defaultPreloadedState);
      preloadedState.addWaiver.loadError = 'add waiver data failed';
      renderComponent(preloadedState);
      const errorAlert = await screen.findByRole('alert');
      expect(errorAlert).toBeVisible();
      expect(errorAlert).toHaveTextContent('add waiver data failed');
    });
  });

  it('renders the list of fields to show', async () => {
    renderComponent(defaultPreloadedState);
    //We need a delay to make sure the component is loaded
    await waitFor(() => expect(screen.queryByText('Loading…')).not.toBeInTheDocument());

    const dropdownFields = screen.getAllByRole('combobox');
    const radioFields = screen.getAllByRole('radio');
    const textboxFields = screen.getAllByRole('textbox');

    const componentField = screen.getAllByText('componentName');
    const policyTitle = screen.getByText('Policy');
    const policyField = screen.getByText('policyName');
    const constraintTitle = screen.getByText('Constraint Name');
    const constraintField = screen.getByText('constraintName');
    const conditionsTitle = screen.getByText('Conditions');
    const conditionsField = screen.getByText('reason');
    const scopeTitle = screen.getByText('Scope');
    const scopeValues = Array.from(dropdownFields[0].options).map((option) => option.value);
    const componentsTitle = screen.getByText('Components');
    const componentsValues = Array.from(radioFields).map((radio) => radio.parentElement.textContent.trim());
    const expirationTitle = screen.getByText('Waiver Expiration');
    const expirationValues = Array.from(dropdownFields[1].options).map((option) => option.value);
    const reasonTitle = screen.getByText('Reason');
    const reasonValues = Array.from(dropdownFields[2].options).map((option) => option.value);
    const commentsTitle = screen.getByText('Comments');
    const commentsField = textboxFields[0];
    const noteToReviewerTitle = screen.getByText('Note to Reviewer');
    const noteToReviewerField = textboxFields[1];

    expect(componentField[0]).toBeVisible();
    expect(componentField[1]).toBeVisible();
    expect(policyTitle).toBeVisible();
    expect(policyField).toBeVisible();
    expect(constraintTitle).toBeVisible();
    expect(constraintField).toBeVisible();
    expect(conditionsTitle).toBeVisible();
    expect(conditionsField).toBeVisible();
    expect(scopeTitle).toBeVisible();
    expect(scopeValues).toEqual([applicationPublicId, organizationId, rootOrganizationId]);
    expect(componentsTitle).toBeVisible();
    expect(componentsValues).toEqual(['componentName', 'componentName (all versions)', 'All Components']);
    expect(expirationTitle).toBeVisible();
    expect(expirationValues).toEqual(['never', '7', '14', '30', '60', '90', '120', 'custom']);
    expect(reasonTitle).toBeVisible();
    expect(reasonValues).toEqual([
      '',
      '9b704ef5bc064fc29d7fe08a251ee9a6',
      '42069f58114f4df8b435a40a415d2835',
      '39984de3d6e64f508df82b4cbfd72f70',
    ]);
    expect(commentsTitle).toBeVisible();
    expect(commentsField.value).toBe('');
    expect(noteToReviewerTitle).toBeVisible();
    expect(noteToReviewerField.value).toBe('');
  });

  it('renders empty comments field on first load', async () => {
    defaultPreloadedState.requestWaiver = { ...initialState, comments: 'some preloaded comment' };
    renderComponent(defaultPreloadedState);
    const commentsTitle = await screen.findByText('Comments');
    const textboxFields = screen.getAllByRole('textbox');
    const commentsField = textboxFields[0];
    expect(commentsTitle).toBeVisible();
    expect(commentsField.value).toBe('');
  });

  it('renders empty note to reviewer field on first load', async () => {
    defaultPreloadedState.requestWaiver = { ...initialState, noteToReviewer: 'some preloaded note' };
    renderComponent(defaultPreloadedState);
    const noteToReviewerTitle = await screen.findByText('Note to Reviewer');
    const textboxFields = screen.getAllByRole('textbox');
    const noteToReviewerField = textboxFields[1];
    expect(noteToReviewerTitle).toBeVisible();
    expect(noteToReviewerField.value).toBe('');
  });

  it('submits the form successfully', async () => {
    mock
      .onPost(getCreatePolicyWaiverRequestUrl(ownerType, ownerId, violationId), {
        comment: 'new comment',
        noteToReviewer: 'new note to reviewer',
        matcherStrategy: 'EXACT_COMPONENT',
        expiryTime: null,
        waiverReasonId: null,
        expireWhenRemediationAvailable: false,
      })
      .reply(200, []);
    renderComponent(defaultPreloadedState);
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const textboxFields = screen.getAllByRole('textbox');
    const commentsTitle = screen.getByText('Comments');
    const commentsField = textboxFields[0];
    const noteToReviewerTitle = screen.getByText('Note to Reviewer');
    const noteToReviewerField = textboxFields[1];
    expect(commentsTitle).toBeVisible();
    expect(commentsField).toBeVisible();
    expect(noteToReviewerTitle).toBeVisible();
    expect(noteToReviewerField).toBeVisible();

    fireEvent.change(commentsField, { target: { value: 'new comment' } });
    fireEvent.change(noteToReviewerField, { target: { value: 'new note to reviewer' } });
    expect(commentsField.value).toBe('new comment');
    expect(noteToReviewerField.value).toBe('new note to reviewer');

    const submitBtn = screen.getByText('Submit');
    fireEvent.click(submitBtn);

    expect(mock.history.post[0].url).toBe(
      '/api/v2/policyWaiverRequests/application/someApplicationPublicId/policyViolation/someViolationId'
    );
    expect(mock.history.post[0].data).toBe(
      JSON.stringify({
        comment: 'new comment',
        noteToReviewer: 'new note to reviewer',
        matcherStrategy: 'EXACT_COMPONENT',
        expiryTime: null,
        waiverReasonId: null,
        expireWhenRemediationAvailable: false,
      })
    );

    const success = await screen.findByText('Success!');
    expect(success).toBeVisible();
  });

  it('submits the form and receives error', async () => {
    mock
      .onPost(getCreatePolicyWaiverRequestUrl(ownerType, ownerId, violationId), {
        comment: 'new comment',
        noteToReviewer: 'new note to reviewer',
        matcherStrategy: 'EXACT_COMPONENT',
        expiryTime: null,
        waiverReasonId: null,
        expireWhenRemediationAvailable: false,
      })
      .reply(400, []);
    renderComponent(defaultPreloadedState);
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const textboxFields = screen.getAllByRole('textbox');
    const commentsField = textboxFields[0];
    const noteToReviewerField = textboxFields[1];

    fireEvent.change(commentsField, { target: { value: 'new comment' } });
    fireEvent.change(noteToReviewerField, { target: { value: 'new note to reviewer' } });

    const submitBtn = screen.getByText('Submit');
    fireEvent.click(submitBtn);

    expect(mock.history.post[0].url).toBe(
      '/api/v2/policyWaiverRequests/application/someApplicationPublicId/policyViolation/someViolationId'
    );
    expect(mock.history.post[0].data).toBe(
      JSON.stringify({
        comment: 'new comment',
        noteToReviewer: 'new note to reviewer',
        matcherStrategy: 'EXACT_COMPONENT',
        expiryTime: null,
        waiverReasonId: null,
        expireWhenRemediationAvailable: false,
      })
    );

    const error = await screen.findByText('An error occurred saving data. Error');
    expect(error).toBeVisible();
  });
});
