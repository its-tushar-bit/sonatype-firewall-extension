/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, fireEvent, axiosMockAdapter, waitFor, within } from 'TestRoot/SpecUtil';
import { initialState } from 'MainRoot/OrgsAndPolicies/policySlice';
import PolicyNotificationsEditor from 'MainRoot/OrgsAndPolicies/policyEditor/policyNotificationsEditor';
import {
  getNotificationWebhooksUrl,
  getRoleMappingForCurrentOwnerUrl,
  getIsJiraEnabledUrl,
  getJiraProjectsUrl,
} from 'MainRoot/util/CLMLocation';

const actionStages = [
  { stageTypeId: 'proxy', shortName: 'PROXY' },
  { stageTypeId: 'develop', shortName: 'DEVELOP' },
  { stageTypeId: 'source', shortName: 'SOURCE' },
  { stageTypeId: 'stage', shortName: 'STAGE' },
  { stageTypeId: 'release', shortName: 'RELEASE' },
  { stageTypeId: 'operate', shortName: 'OPERATE' },
];
const notifications = {
  userNotifications: [{ emailAddress: 'user@email.com', stageIds: ['proxy', 'develop'] }],
  roleNotifications: [{ roleId: '1', stageIds: [] }],
};
const roles = [{ roleId: '1', roleName: 'developer' }];
const notificationWebhooks = [
  {
    description: 'webhook1name',
    eventTypes: null,
    id: 'webhook1',
    secretKey: null,
    url: 'http://sdf.com',
  },
];

describe('PolicyNotificationsEditor', () => {
  let renderComponent, mockAxiosCalls, state, jiraProjects;
  const webhooksUrl = getNotificationWebhooksUrl('organization', 'organizationId');
  const rolesUrl = getRoleMappingForCurrentOwnerUrl('organization', 'organizationId');
  const isJiraEnabledUrl = getIsJiraEnabledUrl();
  const jiraProjectsUrl = getJiraProjectsUrl();

  beforeAll(() => {
    mockAxiosCalls = axiosMockAdapter();
  });

  beforeEach(() => {
    state = {
      orgsAndPolicies: {
        policy: {
          ...initialState,
          isInherited: false,
          currentPolicy: { notifications },
          notificationsEditor: {
            roles,
            notificationWebhooks,
          },
        },
        stages: { action: { stageTypes: actionStages, loading: false, error: null } },
      },
      router: {
        currentParams: { organizationId: 'organizationId' },
        currentState: { name: 'organization' },
      },
      productFeatures: {
        productFeatures: {
          firewall: true,
          notifications: true,
          'policy-monitoring': true,
          'webhooks-for-repositories': true,
          'webhooks-for-applications': true,
        },
      },
    };
    jiraProjects = [
      {
        key: 'key1',
        name: 'Project One',
        issueTypes: [
          {
            id: 1,
            name: 'Bug',
          },
          {
            id: 2,
            name: 'Task',
          },
        ],
      },
      {
        key: 'key2',
        name: 'Project Two',
        issueTypes: [
          {
            id: 1,
            name: 'Bug',
          },
          {
            id: 3,
            name: 'Issue',
          },
        ],
      },
    ];
    mockAxiosCalls.onGet(webhooksUrl).reply(200, notificationWebhooks);
    mockAxiosCalls.onGet(rolesUrl).reply(200, { membersByRole: roles });
    mockAxiosCalls.onGet(isJiraEnabledUrl).reply(200, true);
    mockAxiosCalls.onGet(jiraProjectsUrl).reply(200, jiraProjects);
    renderComponent = (preloadedState = state) => render(<PolicyNotificationsEditor />, { preloadedState });
  });

  it('renders loading indicator and handles error', async () => {
    mockAxiosCalls.reset();

    // roles request error
    mockAxiosCalls.onGet(rolesUrl).replyOnce(404).onGet(rolesUrl).reply(200, roles);
    renderComponent();
    expect(screen.getByText('Loading…')).toBeVisible();
    expect(await screen.findByRole('alert', /An error occurred loading data. Error 404/i)).toBeVisible();

    // webhooks request error
    mockAxiosCalls.onGet(webhooksUrl).replyOnce(500).onGet(webhooksUrl).reply(200, []);
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByRole('alert', /An error occurred loading data. Error 500/i)).toBeVisible();

    // isJiraEnabled request error
    mockAxiosCalls.onGet(isJiraEnabledUrl).networkErrorOnce().onGet(isJiraEnabledUrl).reply(200, true);
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByRole('alert', /An error occurred loading data. Error/i)).toBeVisible();

    // jiraProjects request error
    mockAxiosCalls.onGet(jiraProjectsUrl).timeoutOnce().onGet(jiraProjectsUrl).reply(200, jiraProjects);
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByRole('alert', /An error occurred loading data. Error/i)).toBeVisible();

    // no errors
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByText('Notifications')).toBeVisible();
  });

  it('renders table headers', () => {
    renderComponent();

    actionStages.forEach(async (stage) => {
      expect(await screen.findByRole('columnheader', { name: stage.shortName })).toBeVisible();
    });
  });

  it('renders recipients', async () => {
    renderComponent();

    expect(await screen.findByRole('cell', { name: 'user@email.com' })).toBeVisible();
    expect(screen.getByRole('cell', { name: 'developer' })).toBeVisible();
  });

  it('renders checked checkboxes and enabled remove buttons', async () => {
    renderComponent();

    expect(await screen.findByRole('checkbox', { name: 'notify user@email.com for proxy' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'notify user@email.com for develop' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'notify user@email.com for source' })).not.toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'notify user@email.com for stage' })).not.toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'notify user@email.com for release' })).not.toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'notify user@email.com for operate' })).not.toBeChecked();
    expect(screen.getAllByLabelText('Remove recipient')[0]).not.toBeDisabled();
  });

  describe('when recipient email address is invalid', () => {
    it('displays invalid email format error message', async () => {
      renderComponent();

      fireEvent.change(await screen.findByRole('textbox', { name: 'Email' }), { target: { value: 'invalid format' } });
      expect(screen.getByText('Use valid format: abc@xyz.com')).toBeVisible();
      expect(screen.getByRole('button', { name: /Add/i })).toBeDisabled();
    });

    it('displays email already exists error message', async () => {
      renderComponent();

      fireEvent.change(await screen.findByRole('textbox', { name: 'Email' }), { target: { value: 'user@email.com' } });
      fireEvent.click(screen.getByRole('button', { name: /Add/i }));
      fireEvent.change(screen.getByRole('textbox', { name: 'Email' }), { target: { value: 'user@email.com' } });

      expect(screen.getByText('Email already exists')).toBeVisible();
      expect(screen.getByRole('button', { name: /Add/i })).toBeDisabled();
    });

    it('prevents email from being added if error exists', async () => {
      renderComponent();
      await waitFor(() => screen.getByRole('table'));

      const emailInput = screen.getByRole('textbox', { name: 'Email' });

      fireEvent.change(emailInput, { target: { value: 'invalid format' } });
      expect(screen.getByRole('alert', { description: 'Use valid format: abc@xyz.com' })).toBeVisible();
      expect(screen.getByRole('button', { name: /Add/i })).toBeDisabled();

      fireEvent.keyDown(emailInput, { key: 'Enter', charCode: 13 });
      expect(emailInput).toHaveValue('invalid format');
    });
  });

  [
    ['Email', 'Email'],
    ['Role', 'Role'],
    ['Webhook', 'Select Webhook'],
    ['JIRA', 'Project'],
  ].forEach(([recipientType, inputLabelText]) => {
    it(`displays ${recipientType} field when recipient type is ${recipientType}`, async () => {
      renderComponent();

      fireEvent.change(await screen.findByLabelText('Recipient Type'), { target: { value: recipientType } });

      expect(screen.getByLabelText(inputLabelText)).toBeVisible();
    });
  });

  describe('renders disabled form state', () => {
    it('when policy is inherited', async () => {
      state.orgsAndPolicies.policy.isInherited = true;

      renderComponent();

      expect(await screen.findByLabelText('Recipient Type')).toBeDisabled();
      expect(screen.getByLabelText('Email')).toBeDisabled();
      expect(screen.getByRole('button', { name: /Add/i })).toBeDisabled();
      expect(screen.getAllByLabelText('Remove recipient')[0]).toBeDisabled();
    });

    it('when notifications and firewall feature are not supported', async () => {
      state.productFeatures.productFeatures.notifications = false;
      state.productFeatures.productFeatures.firewall = false;

      renderComponent();

      expect(await screen.findByLabelText('Recipient Type')).toBeDisabled();
      expect(screen.getByLabelText('Email')).toBeDisabled();
      expect(screen.getByRole('button', { name: /Add/i })).toBeDisabled();
      expect(screen.getAllByLabelText('Remove recipient')[0]).toBeDisabled();
    });
  });

  describe('Jira form fields', () => {
    it('renders empty Jira field', async () => {
      mockAxiosCalls.onGet(jiraProjectsUrl).reply(200);

      renderComponent();

      fireEvent.change(await screen.findByLabelText('Recipient Type'), { target: { value: 'JIRA' } });

      expect(screen.getByText('No applicable projects available.')).toBeVisible();
      expect(screen.getByText('-- Select JIRA Project --')).toBeVisible();
      expect(screen.getByLabelText('Issue Type')).toBeDisabled();
    });

    it('renders No applicable issue type.', async () => {
      jiraProjects[0].issueTypes = [];
      mockAxiosCalls.onGet(jiraProjectsUrl).reply(200, jiraProjects);

      renderComponent();
      const recipientTypeSelector = await screen.findByLabelText('Recipient Type');
      fireEvent.change(recipientTypeSelector, { target: { value: 'JIRA' } });
      fireEvent.change(screen.getByLabelText('Project'), { target: { value: 'key1' } });

      expect(screen.getByText('-- Select Project --')).toBeVisible();
      expect(screen.getByText('No applicable issue type.')).toBeVisible();
    });

    it('renders and updates fields', async () => {
      renderComponent();

      // updating recipient type
      const addRecipientButton = await screen.findByRole('button', { name: /Add/i });
      fireEvent.change(screen.getByLabelText('Recipient Type'), { target: { value: 'JIRA' } });
      expect(screen.getByText('-- Select Project --')).toBeVisible();
      expect(screen.getByText('-- Select JIRA Project --')).toBeVisible();
      expect(addRecipientButton).toBeDisabled();

      // updating Jira project
      fireEvent.change(screen.getByLabelText('Project'), { target: { value: 'key1' } });
      expect(screen.getByText('Project One')).toBeVisible();
      expect(screen.getByText('-- Select Issue Type --')).toBeVisible();
      expect(addRecipientButton).toBeDisabled();

      // updating jira issue type
      fireEvent.change(screen.getByLabelText('Issue Type'), { target: { value: '1' } });
      expect(screen.getByText('Project One')).toBeVisible();
      expect(screen.getByText('Bug')).toBeVisible();
      expect(addRecipientButton).toBeEnabled();
    });
  });

  it('renders webhook displayName', async () => {
    renderComponent();
    await waitFor(() => screen.getByRole('table'));

    fireEvent.change(screen.getByLabelText('Recipient Type'), { target: { value: 'Webhook' } });
    fireEvent.change(screen.getByLabelText('Select Webhook'), {
      target: { value: 'webhook1' },
    });

    expect(screen.getByText('webhook1name')).toBeVisible();
  });

  it('renders webhook url if no displayName was provided', async () => {
    mockAxiosCalls.onGet(webhooksUrl).reply(200, [
      {
        description: '',
        eventTypes: null,
        id: 'webhook1',
        secretKey: null,
        url: 'http://sdf.com',
      },
    ]);
    renderComponent();
    await waitFor(() => screen.getByRole('table'));

    fireEvent.change(screen.getByLabelText('Recipient Type'), { target: { value: 'Webhook' } });
    fireEvent.change(screen.getByLabelText('Select Webhook'), {
      target: { value: 'webhook1' },
    });

    expect(screen.getByText('http://sdf.com')).toBeVisible();
  });

  it('renders disabled proxy stage for webhook notifications', async () => {
    state.orgsAndPolicies.policy.currentPolicy.notifications = {
      ...notifications,
      webhookNotifications: [{ webhookId: 'webhook1', stageIds: [] }],
    };
    renderComponent();
    await waitFor(() => screen.getByRole('table'));

    expect(screen.getByRole('checkbox', { name: 'notify Webhook: webhook1name for develop' })).not.toBeDisabled();
    expect(screen.getByRole('checkbox', { name: 'notify Webhook: webhook1name for source' })).not.toBeDisabled();
    expect(screen.getByRole('checkbox', { name: 'notify Webhook: webhook1name for stage' })).not.toBeDisabled();
    expect(screen.getByRole('checkbox', { name: 'notify Webhook: webhook1name for release' })).not.toBeDisabled();
    expect(screen.getByRole('checkbox', { name: 'notify Webhook: webhook1name for operate' })).not.toBeDisabled();
    expect(screen.getByRole('checkbox', { name: 'notify Webhook: webhook1name for proxy' })).toBeDisabled();
  });

  it('renders disabled proxy stage tooltip message for webhook notifications', async () => {
    state.orgsAndPolicies.policy.currentPolicy.notifications = {
      ...notifications,
      webhookNotifications: [{ webhookId: 'webhook1', stageIds: [] }],
    };
    SpecUtil.requestIdleCallbackInvokeImmediate();
    renderComponent();
    await waitFor(() => screen.getByRole('table'));

    const checkbox = screen.getByRole('checkbox', { name: 'notify Webhook: webhook1name for proxy' });
    expect(checkbox).toBeDisabled();
    fireEvent.mouseOver(checkbox);
    const tooltip = await screen.findByRole('tooltip');

    expect(
      within(tooltip).getByText('Webhooks are not available for policy violations at Proxy stage.', {
        exact: false,
      })
    ).toBeInTheDocument();
  });

  it('renders disabled proxy stage for jira notifications', async () => {
    state.orgsAndPolicies.policy.currentPolicy.notifications = {
      ...notifications,
      jiraNotifications: [{ projectKey: 'key1', issueTypeId: 'Task', stageIds: [] }],
    };
    renderComponent();
    await waitFor(() => screen.getByRole('table'));

    expect(screen.getByRole('checkbox', { name: 'notify key1 (Issue Type ID: Task) for develop' })).not.toBeDisabled();
    expect(screen.getByRole('checkbox', { name: 'notify key1 (Issue Type ID: Task) for source' })).not.toBeDisabled();
    expect(screen.getByRole('checkbox', { name: 'notify key1 (Issue Type ID: Task) for stage' })).not.toBeDisabled();
    expect(screen.getByRole('checkbox', { name: 'notify key1 (Issue Type ID: Task) for release' })).not.toBeDisabled();
    expect(screen.getByRole('checkbox', { name: 'notify key1 (Issue Type ID: Task) for operate' })).not.toBeDisabled();
    expect(screen.getByRole('checkbox', { name: 'notify key1 (Issue Type ID: Task) for proxy' })).toBeDisabled();
  });

  it('renders disabled proxy stage tooltip message for jira notifications', async () => {
    state.orgsAndPolicies.policy.currentPolicy.notifications = {
      ...notifications,
      jiraNotifications: [{ projectKey: 'key1', issueTypeId: 'Task', stageIds: [] }],
    };
    SpecUtil.requestIdleCallbackInvokeImmediate();
    renderComponent();
    await waitFor(() => screen.getByRole('table'));

    const checkbox = screen.getByRole('checkbox', { name: 'notify key1 (Issue Type ID: Task) for proxy' });
    expect(checkbox).toBeDisabled();
    fireEvent.mouseOver(checkbox);
    const tooltip = await screen.findByRole('tooltip');

    expect(
      within(tooltip).getByText('Jira notifications are not available for policy violations at Proxy stage.', {
        exact: false,
      })
    ).toBeInTheDocument();
  });

  it('renders "notifications are not supported" tooltip message when notifications are not supported', async () => {
    state.productFeatures.productFeatures.notifications = false;
    SpecUtil.requestIdleCallbackInvokeImmediate();

    renderComponent();
    await waitFor(() => screen.getByRole('table'));

    const checkbox = screen.getByRole('checkbox', { name: 'notify user@email.com for develop' });
    fireEvent.mouseOver(checkbox);

    const tooltip = await screen.findByRole('tooltip');
    expect(
      within(tooltip).getByText('Notifications are not supported by your license.', {
        exact: false,
      })
    ).toBeInTheDocument();
  });
});
