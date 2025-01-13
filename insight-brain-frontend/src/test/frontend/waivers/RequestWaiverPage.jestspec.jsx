/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import RequestWaiverPage from 'MainRoot/waivers/RequestWaiverPage';
import { axiosMockAdapter, fireEvent, render, screen } from 'TestRoot/SpecUtil';
import RouterStateContext from 'MainRoot/react/RouterStateContext';
import {
  getApplicableWaiversUrl,
  getSimilarWaiversUrl,
  getApplicationSummaryUrl,
  getPermissionContextTestUrl,
  saveRequestWaiverUrl,
  getWaiverRequestWebhooksCountUrl,
} from 'MainRoot/util/CLMLocation';
import { clone } from 'ramda';
import { initialState } from 'MainRoot/waivers/requestWaiverSlice';
import { fetchCrossStageViolation } from 'MainRoot/violation/violationActions';

describe('RequestWaiverPage', function () {
  let renderComponent;
  let mock;
  const violationId = 'someViolationId';
  const applicationPublicId = 'someApplicationPublicId';
  const internalApplicationId = 'someInternalApplicationId';
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
    applicationPublicId,
  };

  const defaultPreloadedState = {
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
    firewall: {
      componentDetailsPage: {
        showManageWaiverPage: false,
      },
    },
    violation: {
      selectedViolationId: violationId,
      violationDetails,
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

    const routerContext = {
      href: jest.fn((url, params) => {
        if (url.includes('sidebarView.violation')) {
          const violationId = params.id;
          return `#/violation/${violationId}`;
        }
        if (url.includes('addWaiver')) {
          const violationId = params.violationId;
          return `#/addWaiver/${violationId}`;
        }
        return '#';
      }),
      includes: jest.fn(() => false),
    };

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
    mock.onGet(getWaiverRequestWebhooksCountUrl()).reply(200, 1);
    mock
      .onPut(getPermissionContextTestUrl('application', internalApplicationId))
      .reply(200, ['WAIVE_POLICY_VIOLATIONS']);

    renderComponent = (preloadedState, router = routerContext) =>
      render(
        <RouterStateContext.Provider value={router}>
          <RequestWaiverPage />
        </RouterStateContext.Provider>,
        { preloadedState: preloadedState || defaultPreloadedState }
      );
  });

  describe('when violationId is null', () => {
    it('renders a LoadWrapper with an error message', async () => {
      const preloadedState = clone(defaultPreloadedState);
      preloadedState.router.currentParams.violationId = null;
      renderComponent(preloadedState);
      const error = await screen.findByText('An error occurred loading data. No Violation ID provided.');
      expect(error).toBeVisible();
    });
  });

  it('renders a page title and loading', async () => {
    renderComponent(defaultPreloadedState);
    const loading = await screen.findByText('Loading…');
    expect(loading).toBeVisible();
    const title = screen.getByText('Request Waiver');
    const description = screen.getByText(
      'A waiver request will be sent to the designated approver upon submit, if a webhook event for waiver requests is configured. If you are unsure about the webhook configuration, share the policy violation ID and the curl command with the designated approver.'
    );
    expect(title).toBeVisible();
    expect(description).toBeVisible();
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

  it('renders the list of fields to show', async () => {
    renderComponent(defaultPreloadedState);
    const componentTitle = await screen.findByText('Component');
    const componentField = screen.getByText('componentName');
    const policyTitle = screen.getByText('Policy');
    const policyField = screen.getByText('policyName');
    const constraintTitle = screen.getByText('Constraint Name');
    const constraintField = screen.getByText('constraintName');
    const conditionsTitle = screen.getByText('Conditions');
    const conditionsField = screen.getByText('reason');
    const policyViolationTitle = screen.getByText('Policy Violation ID');
    const policyViolationField = screen.getByText('someViolationId');
    const policyViolationDetailsTitle = screen.getByText('Policy Violation Details Page');
    const policyViolationDetailsField = screen.getByText('/ui/links/policyViolation/someViolationId');
    const curlExampleTitle = screen.getByText('Curl Example');
    const curlExampleField = screen.getByText(
      `curl -X POST -u user:pass -H "Content-Type: text/plain; charset=UTF-8" /api/v2/policyWaiver/someViolationId/application --data-binary 'waiver comment (optional)'`
    );
    const reasonTitle = screen.getByText('Reason');
    const dropdownFields = screen.getAllByRole('combobox');
    const reasonField = dropdownFields[0];
    const commentsTitle = screen.getByText('Comments');
    const textboxFields = screen.getAllByRole('textbox');
    const commentsField = textboxFields[textboxFields.length - 1];
    expect(componentTitle).toBeVisible();
    expect(componentField).toBeVisible();
    expect(policyTitle).toBeVisible();
    expect(policyField).toBeVisible();
    expect(constraintTitle).toBeVisible();
    expect(constraintField).toBeVisible();
    expect(conditionsTitle).toBeVisible();
    expect(conditionsField).toBeVisible();
    expect(policyViolationTitle).toBeVisible();
    expect(policyViolationField).toBeVisible();
    expect(policyViolationDetailsTitle).toBeVisible();
    expect(policyViolationDetailsField).toBeVisible();
    expect(curlExampleTitle).toBeVisible();
    expect(curlExampleField).toBeVisible();
    expect(reasonTitle).toBeVisible();
    expect(reasonField.selectedIndex).toBe(0);
    expect(commentsTitle).toBeVisible();
    expect(commentsField.value).toBe('');
  });

  it('renders empty comments field on first load', async () => {
    defaultPreloadedState.requestWaiver = { ...initialState, comments: 'some preloaded comment' };
    renderComponent(defaultPreloadedState);
    const commentsTitle = await screen.findByText('Comments');
    const textboxFields = screen.getAllByRole('textbox');
    const commentsField = textboxFields[textboxFields.length - 1];
    expect(commentsTitle).toBeVisible();
    expect(commentsField.value).toBe('');
  });

  it('submits the form successfully', async () => {
    mock
      .onPost(saveRequestWaiverUrl(violationId), {
        policyViolationLink: '/ui/links/policyViolation/someViolationId',
        addWaiverLink: '/ui/links/addWaiver/someViolationId?comments=new%20comment',
        comment: 'new comment',
      })
      .reply(204);
    renderComponent(defaultPreloadedState);
    const commentsTitle = await screen.findByText('Comments');
    const textboxFields = screen.getAllByRole('textbox');
    const commentsField = textboxFields[textboxFields.length - 1];
    expect(commentsTitle).toBeVisible();
    expect(commentsField).toBeVisible();
    fireEvent.change(commentsField, { target: { value: 'new comment' } });
    expect(commentsField.value).toBe('new comment');
    const submitBtn = screen.getByText('Submit');
    fireEvent.click(submitBtn);
  });

  describe('waiver request webhook', () => {
    function mockWaiverRequestWebhook() {
      mock.onGet(getWaiverRequestWebhooksCountUrl()).reply(200, 1);
    }

    it('sumbit button should be disabled and error is displayed if endpoint fails', async () => {
      mock.onGet(getWaiverRequestWebhooksCountUrl()).reply(500, 'Error message');

      renderComponent(defaultPreloadedState);
      const waiverRequestWebhookError = await screen.findByRole('alert');
      const submitBtn = screen.getByText('Submit');
      expect(submitBtn).toHaveClass('disabled');
      expect(waiverRequestWebhookError).toHaveTextContent('An error occurred loading data. Error message');
    });

    it('sumbit button should be disabled and alert is displayed', async () => {
      mock.onGet(getWaiverRequestWebhooksCountUrl()).reply(200, 0);

      renderComponent(defaultPreloadedState);
      const waiverRequestWebhookAlert = await screen.findByText(
        'Webhook event for Automatic Waiver Request is not configured. Contact your admin or request the waiver manually.'
      );
      const submitBtn = screen.getByText('Submit');
      expect(submitBtn).toHaveClass('disabled');
      expect(waiverRequestWebhookAlert).toBeVisible();
    });

    it('submit button should not to be disabled and alert is not displayed', async () => {
      mockWaiverRequestWebhook();

      function findAlert() {
        return screen.getByText(
          'Webhook event for Automatic Waiver Request is not configured. Contact your admin or request the waiver manually.'
        );
      }

      renderComponent(defaultPreloadedState);

      const submitBtn = await screen.findByText('Submit');
      expect(findAlert).toThrowError(/Unable to find an element with the text: Webhook/g);
      expect(submitBtn).not.toHaveClass('disabled');
    });

    it('submits the form successfully with a comment and reason', async () => {
      mockWaiverRequestWebhook();
      mock
        .onPost(saveRequestWaiverUrl(violationId), {
          policyViolationLink: '/ui/links/policyViolation/someViolationId',
          addWaiverLink:
            '/ui/links/addWaiver/someViolationId?comments=new%20comment&reasonId=42069f58114f4df8b435a40a415d2835',
          comment: 'new comment',
          reasonId: '42069f58114f4df8b435a40a415d2835',
        })
        .reply(204);

      // If the comments are not trimmed fail the test
      mock.onAny().reply(400);

      renderComponent(defaultPreloadedState);
      const commentsTitle = await screen.findByText('Comments');
      const textboxFields = screen.getAllByRole('textbox');
      const commentsField = textboxFields[textboxFields.length - 1];
      const reasonTitle = screen.getByText('Reason');
      const dropdownFields = screen.getAllByRole('combobox');
      const reasonField = dropdownFields[0];
      expect(commentsTitle).toBeVisible();
      expect(commentsField).toBeVisible();
      expect(reasonTitle).toBeVisible();
      expect(reasonField).toBeVisible();

      //Comments are trimmed before sending them to backend
      fireEvent.change(commentsField, { target: { value: '     new comment         ' } });
      fireEvent.change(reasonField, { target: { value: '42069f58114f4df8b435a40a415d2835' } });
      expect(commentsField.value).toBe('     new comment         ');
      expect(reasonField.selectedIndex).toBe(2);
      const submitBtn = screen.getByText('Submit');
      fireEvent.click(submitBtn);

      const success = await screen.findByText('Success!');
      expect(success).toBeVisible();
    });

    it('submits the form successfully with only comment without reason', async () => {
      mockWaiverRequestWebhook();
      mock
        .onPost(saveRequestWaiverUrl(violationId), {
          policyViolationLink: '/ui/links/policyViolation/someViolationId',
          addWaiverLink: '/ui/links/addWaiver/someViolationId?comments=new%20comment',
          comment: 'new comment',
          reasonId: null,
        })
        .reply(204);

      // If the comments are not trimmed fail the test
      mock.onAny().reply(400);

      renderComponent(defaultPreloadedState);
      const commentsTitle = await screen.findByText('Comments');
      const textboxFields = screen.getAllByRole('textbox');
      const commentsField = textboxFields[textboxFields.length - 1];
      expect(commentsTitle).toBeVisible();
      expect(commentsField).toBeVisible();

      //Comments are trimmed before sending them to backend
      fireEvent.change(commentsField, { target: { value: '     new comment         ' } });
      expect(commentsField.value).toBe('     new comment         ');
      const submitBtn = screen.getByText('Submit');
      fireEvent.click(submitBtn);

      const success = await screen.findByText('Success!');
      expect(success).toBeVisible();
    });

    it('submits the form successfully with only reason without comment', async () => {
      mockWaiverRequestWebhook();
      mock
        .onPost(saveRequestWaiverUrl(violationId), {
          policyViolationLink: '/ui/links/policyViolation/someViolationId',
          addWaiverLink: '/ui/links/addWaiver/someViolationId?reasonId=42069f58114f4df8b435a40a415d2835',
          comment: '',
          reasonId: '42069f58114f4df8b435a40a415d2835',
        })
        .reply(204);

      renderComponent(defaultPreloadedState);
      const reasonTitle = await screen.findByText('Reason');
      const dropdownFields = screen.getAllByRole('combobox');
      const reasonField = dropdownFields[0];
      expect(reasonTitle).toBeVisible();
      expect(reasonField).toBeVisible();

      fireEvent.change(reasonField, { target: { value: '42069f58114f4df8b435a40a415d2835' } });
      expect(reasonField.selectedIndex).toBe(2);
      const submitBtn = screen.getByText('Submit');
      fireEvent.click(submitBtn);

      const success = await screen.findByText('Success!');
      expect(success).toBeVisible();
    });

    it('submits the form successfully with no comment or reason', async () => {
      mockWaiverRequestWebhook();
      mock
        .onPost(saveRequestWaiverUrl(violationId), {
          policyViolationLink: '/ui/links/policyViolation/someViolationId',
          addWaiverLink: '/ui/links/addWaiver/someViolationId',
          comment: '',
          reasonId: null,
        })
        .reply(204);

      renderComponent(defaultPreloadedState);
      const commentsTitle = await screen.findByText('Comments');
      const textboxFields = screen.getAllByRole('textbox');
      const commentsField = textboxFields[textboxFields.length - 1];
      expect(commentsTitle).toBeVisible();
      expect(commentsField).toBeVisible();
      expect(commentsField.value).toBe('');
      const submitBtn = screen.getByText('Submit');
      fireEvent.click(submitBtn);

      const success = await screen.findByText('Success!');
      expect(success).toBeVisible();
    });

    it('fails to submit the form', async () => {
      mockWaiverRequestWebhook();
      mock
        .onPost(saveRequestWaiverUrl(violationId), {
          policyViolationLink: '/ui/links/policyViolation/someViolationId',
          addWaiverLink:
            '/ui/links/addWaiver/someViolationId?comments=new%20comment%20%3C%3E&reasonId=42069f58114f4df8b435a40a415d2835',
          comment: 'new comment <>',
          reasonId: '42069f58114f4df8b435a40a415d2835',
        })
        .reply(500, 'some saving error');

      renderComponent(defaultPreloadedState);
      const commentsTitle = await screen.findByText('Comments');
      const textboxFields = screen.getAllByRole('textbox');
      const commentsField = textboxFields[textboxFields.length - 1];
      const reasonTitle = await screen.findByText('Reason');
      const dropdownFields = screen.getAllByRole('combobox');
      const reasonField = dropdownFields[0];
      expect(commentsTitle).toBeVisible();
      expect(commentsField).toBeVisible();
      expect(reasonTitle).toBeVisible();
      expect(reasonField).toBeVisible();
      fireEvent.change(commentsField, { target: { value: 'new comment <>' } });
      fireEvent.change(reasonField, { target: { value: '42069f58114f4df8b435a40a415d2835' } });
      expect(reasonField.selectedIndex).toBe(2);
      expect(commentsField.value).toBe('new comment <>');
      const submitBtn = screen.getByText('Submit');
      fireEvent.click(submitBtn);
      const errorAlert = await screen.findByRole('alert');
      expect(errorAlert).toHaveTextContent('An error occurred saving data. some saving error');
    });
  });
});
