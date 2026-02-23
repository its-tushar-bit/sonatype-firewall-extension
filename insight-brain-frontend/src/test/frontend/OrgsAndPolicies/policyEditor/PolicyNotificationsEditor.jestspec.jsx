/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, fireEvent, axiosMockAdapter, waitFor, within } from 'TestRoot/SpecUtil';
import { actions as policyActions, initialState } from 'MainRoot/OrgsAndPolicies/policySlice';
import PolicyNotificationsEditor from 'MainRoot/OrgsAndPolicies/policyEditor/policyNotificationsEditor';
import {
  getNotificationWebhooksUrl,
  getRoleMappingForCurrentOwnerUrl,
  getIsJiraEnabledUrl,
  getJiraProjectsUrl,
} from 'MainRoot/util/CLMLocation';
import { compose, last } from 'ramda';
import { pathSet } from 'MainRoot/util/jsUtil';

import 'TestRoot/SpecUtil';

import * as productLicenseSelectors from 'MainRoot/productFeatures/productLicenseSelectors';

const actionStages = [
  { stageTypeId: 'proxy', shortName: 'PROXY' },
  { stageTypeId: 'develop', shortName: 'DEVELOP' },
  { stageTypeId: 'source', shortName: 'SOURCE' },
  { stageTypeId: 'build', shortName: 'BUILD' },
  { stageTypeId: 'stage', shortName: 'STAGE' },
  { stageTypeId: 'release', shortName: 'RELEASE' },
  { stageTypeId: 'operate', shortName: 'OPERATE' },
];

const sbomStages = [{ stageTypeId: 'compliance', shortName: 'COMPLIANCE' }];

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
        root: {
          policiesByOwner: [{ ownerId: 'ownerId' }, { ownerId: 'ROOT_ORGANIZATION_ID' }],
          selectedOwner: {
            id: 'ownerId',
          },
        },
        policy: {
          ...initialState,
          originalOverrideNotificationsFlag: false,
          overrideNotificationsFlag: false,
          hasEditIqPermission: true,
          isInherited: false,
          currentPolicy: { notifications, constraints: [] },
          notificationsEditor: {
            roles,
            notificationWebhooks,
          },
          notificationWebhooks: [],
        },
        stages: {
          action: { stageTypes: actionStages, loading: false, error: null },
          sbom: { stageTypes: sbomStages, loading: false, error: null },
        },
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

  it('renders correct table headers when not SBOM Manager', async () => {
    renderComponent();

    expect(await screen.queryByRole('columnheader', { name: 'COMPLIANCE' })).not.toBeInTheDocument();
    const assertion = async (stage) =>
      expect(await screen.findByRole('columnheader', { name: stage.shortName })).toBeVisible();
    await Promise.all(actionStages.map(assertion));
  });

  it('renders correct table headers when SBOM Manager', async () => {
    const sbomState = {
      ...state,
      router: {
        currentParams: { organizationId: 'organizationId' },
        currentState: { name: 'sbomManager.organization' },
      },
    };
    renderComponent(sbomState);

    expect(await screen.findByRole('columnheader', { name: 'COMPLIANCE' })).toBeVisible();
    const assertionNotInDocument = async (stage) =>
      expect(await screen.queryByRole('columnheader', { name: stage.shortName })).not.toBeInTheDocument();
    await Promise.all(actionStages.map(assertionNotInDocument));
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
    SpecUtil.requestIdleCallbackInvokeImmediateJest();
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
    SpecUtil.requestIdleCallbackInvokeImmediateJest();
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
    SpecUtil.requestIdleCallbackInvokeImmediateJest();

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

  describe('when policy is inherited', () => {
    it('dispatches setOverrideParentNotifications and setNotificationsOverride actions', async () => {
      const spSetOverrideParentNotifications = jest.spyOn(policyActions, 'setOverrideParentNotifications');
      const spySetNotificationsOverride = jest.spyOn(policyActions, 'setNotificationsOverride');
      const preloadedState = compose(
        pathSet(['productFeatures', 'productFeatures', 'firewall'], true),
        pathSet(['productFeatures', 'productFeatures', 'enforcement'], true),
        pathSet(['productFeatures', 'productFeatures', 'policy-monitoring'], true),
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'overrideNotificationsFlag'], false),
        pathSet(['orgsAndPolicies', 'policy', 'originalOverrideNotificationsFlag'], false),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyNotificationsOverrideAllowed'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'notifications'], {
          roleNotifications: [
            {
              roleId: 'roleId',
              stageIds: ['proxy', 'develop', 'source', 'build', 'stage', 'release', 'operate', 'continuous-monitoring'],
            },
          ],
          userNotifications: [
            {
              emailAddress: 'email@email.com',
              stageIds: ['proxy', 'develop', 'source', 'build', 'stage', 'release', 'operate', 'continuous-monitoring'],
            },
          ],
          webhookNotifications: [
            {
              webhookId: 'webhookId',
              stageIds: ['develop', 'source', 'build', 'stage', 'release', 'operate', 'continuous-monitoring'],
            },
          ],
          jiraNotifications: [
            {
              projectKey: 'key1',
              issueTypeId: 1,
              stageIds: ['develop', 'source', 'build', 'stage', 'release', 'operate', 'continuous-monitoring'],
            },
          ],
        }),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyNotificationsOverrides'], null)
      )(state);
      renderComponent(preloadedState);
      await waitFor(() => screen.getByRole('table'));
      const overrideParentNotificationsRadio = screen.getByLabelText(/Override parent notifications/i);
      expect(overrideParentNotificationsRadio).not.toBeChecked();
      const table = screen.getByRole('table', { name: 'Edit policy notifications table' });
      const checkboxes = within(table).getAllByRole('checkbox');
      expect(checkboxes.length).toBe(32);
      checkboxes.forEach((checkbox) => expect(checkbox).not.toBeEnabled());

      fireEvent.click(overrideParentNotificationsRadio);

      expect(overrideParentNotificationsRadio).toBeChecked();
      expect(checkboxes.length).toBe(32);
      checkboxes.forEach((checkbox, index) => {
        // Webhook and JIRA don't allow proxy stage
        if (index === 8 || index === 24) {
          expect(checkbox).not.toBeEnabled();
          expect(checkbox).not.toBeChecked();
        } else {
          expect(checkbox).toBeEnabled();
          expect(checkbox).toBeChecked();
        }
      });
      expect(spSetOverrideParentNotifications).toHaveBeenCalled();
      expect(spySetNotificationsOverride).toHaveBeenCalledWith({
        ownerId: preloadedState.orgsAndPolicies.root.selectedOwner.id,
        notificationsOverride: preloadedState.orgsAndPolicies.policy.currentPolicy.notifications,
      });
    });

    it('dispatches unSetOverrideParentNotifications action', async () => {
      const spy = jest.spyOn(policyActions, 'unSetOverrideParentNotifications');
      const preloadedState = compose(
        pathSet(['productFeatures', 'productFeatures', 'firewall'], true),
        pathSet(['productFeatures', 'productFeatures', 'enforcement'], true),
        pathSet(['productFeatures', 'productFeatures', 'policy-monitoring'], true),
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'overrideNotificationsFlag'], true),
        pathSet(['orgsAndPolicies', 'policy', 'originalOverrideNotificationsFlag'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyNotificationsOverrideAllowed'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'notifications'], {
          userNotifications: [
            {
              emailAddress: 'email@email.com',
              stageIds: [],
            },
          ],
        }),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyNotificationsOverrides'], {
          ownerId: {
            userNotifications: [
              {
                emailAddress: 'email2@email.com',
                stageIds: [
                  'proxy',
                  'develop',
                  'source',
                  'build',
                  'stage',
                  'release',
                  'operate',
                  'continuous-monitoring',
                ],
              },
            ],
          },
        })
      )(state);
      renderComponent(preloadedState);
      await waitFor(() => screen.getByRole('table'));
      const table = screen.getByRole('table', { name: 'Edit policy notifications table' });
      let checkboxes = within(table).getAllByRole('checkbox');
      expect(checkboxes.length).toBe(8);
      checkboxes.forEach((checkbox) => expect(checkbox).toBeEnabled());
      checkboxes.forEach((checkbox) => expect(checkbox).toBeChecked());
      const inheritParentNotificationsRadio = screen.getByLabelText(/Inherit parent notifications/i);
      expect(inheritParentNotificationsRadio).not.toBeChecked();

      fireEvent.click(inheritParentNotificationsRadio);

      expect(spy).toHaveBeenCalledWith(preloadedState.orgsAndPolicies.root.selectedOwner.id);
      expect(inheritParentNotificationsRadio).toBeChecked();
      checkboxes = within(table).getAllByRole('checkbox');
      expect(checkboxes.length).toBe(8);
      checkboxes.forEach((checkbox) => expect(checkbox).not.toBeEnabled());
      checkboxes.forEach((checkbox) => expect(checkbox).not.toBeChecked());
    });

    it('renders notifications overrides disabled message when notification overrides are disabled', async () => {
      const preloadedState = compose(
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyNotificationsOverrideAllowed'], false)
      )(state);
      renderComponent(preloadedState);
      await waitFor(() => screen.getByRole('table'));

      const notificationsOverridesDisabledMessage = screen.getByText(
        /Notification overrides have been disabled for this policy./i
      );

      expect(notificationsOverridesDisabledMessage).toBeVisible();
    });

    it('renders notifications overrides enabled message when notification overrides are enabled', async () => {
      const preloadedState = compose(
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyNotificationsOverrideAllowed'], true)
      )(state);
      renderComponent(preloadedState);
      await waitFor(() => screen.getByRole('table'));

      const notificationsOverridesEnabledMessage = screen.getByText(
        /Notification overrides have been enabled for this policy. Modifying notifications will only affect this level./i
      );

      expect(notificationsOverridesEnabledMessage).toBeVisible();
    });

    it('renders enabled radios when notification overrides are enabled', async () => {
      const preloadedState = compose(
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyNotificationsOverrideAllowed'], true)
      )(state);
      renderComponent(preloadedState);
      await waitFor(() => screen.getByRole('table'));

      const inheritParentNotificationsRadio = screen.getByLabelText(/Inherit parent notifications/i);
      const overrideParentNotificationsRadio = screen.getByLabelText(/Override parent notifications/i);

      expect(inheritParentNotificationsRadio).toBeVisible();
      expect(inheritParentNotificationsRadio).toBeEnabled();
      expect(overrideParentNotificationsRadio).toBeVisible();
      expect(overrideParentNotificationsRadio).toBeEnabled();
    });

    it('renders disabled radios when notification overrides are not enabled', async () => {
      const preloadedState = compose(
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyNotificationsOverrideAllowed'], false)
      )(state);
      renderComponent(preloadedState);
      await waitFor(() => screen.getByRole('table'));

      const inheritParentNotificationsRadio = screen.getByLabelText(/Inherit parent notifications/i);
      const overrideParentNotificationsRadio = screen.getByLabelText(/Override parent notifications/i);

      expect(inheritParentNotificationsRadio).toBeVisible();
      expect(inheritParentNotificationsRadio).toBeDisabled();
      expect(overrideParentNotificationsRadio).toBeVisible();
      expect(overrideParentNotificationsRadio).toBeDisabled();
    });

    it('renders disabled radios when there is no permission', async () => {
      const preloadedState = compose(
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'hasEditIqPermission'], false)
      )(state);
      renderComponent(preloadedState);
      await waitFor(() => screen.getByRole('table'));

      const inheritParentNotificationsRadio = screen.getByLabelText(/Inherit parent notifications/i);
      const overrideParentNotificationsRadio = screen.getByLabelText(/Override parent notifications/i);

      expect(inheritParentNotificationsRadio).toBeVisible();
      expect(inheritParentNotificationsRadio).toBeDisabled();
      expect(overrideParentNotificationsRadio).toBeVisible();
      expect(overrideParentNotificationsRadio).toBeDisabled();
    });

    it('renders table with disabled radios when policy notifications overriding is not enabled', async () => {
      const preloadedState = compose(
        pathSet(['productFeatures', 'productFeatures', 'firewall'], true),
        pathSet(['productFeatures', 'productFeatures', 'enforcement'], true),
        pathSet(['productFeatures', 'productFeatures', 'policy-monitoring'], true),
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'overrideNotificationsFlag'], true),
        pathSet(['orgsAndPolicies', 'policy', 'originalOverrideNotificationsFlag'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyNotificationsOverrideAllowed'], false),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'notifications'], {
          userNotifications: [
            {
              emailAddress: 'email@email.com',
              stageIds: [],
            },
          ],
        }),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyNotificationsOverrides'], {
          ownerId: {
            userNotifications: [
              {
                emailAddress: 'email2@email.com',
                stageIds: [
                  'proxy',
                  'develop',
                  'source',
                  'build',
                  'stage',
                  'release',
                  'operate',
                  'continuous-monitoring',
                ],
              },
            ],
          },
        })
      )(state);
      renderComponent(preloadedState);
      await waitFor(() => screen.getByRole('table'));

      const table = screen.getByRole('table', { name: 'Edit policy notifications table' });
      const checkboxes = within(table).getAllByRole('checkbox');

      expect(checkboxes.length).toBe(8);
      checkboxes.forEach((checkbox) => expect(checkbox).toBeDisabled());
    });

    it('dispatches toggleNotificationRecipientStage action when notifications overriding is enabled and a checkbox is clicked', async () => {
      const spy = jest.spyOn(policyActions, 'toggleNotificationRecipientStage');
      const preloadedState = compose(
        pathSet(['productFeatures', 'productFeatures', 'firewall'], true),
        pathSet(['productFeatures', 'productFeatures', 'enforcement'], true),
        pathSet(['productFeatures', 'productFeatures', 'policy-monitoring'], true),
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'overrideNotificationsFlag'], true),
        pathSet(['orgsAndPolicies', 'policy', 'originalOverrideNotificationsFlag'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyNotificationsOverrideAllowed'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'notifications'], {
          userNotifications: [
            {
              emailAddress: 'email@email.com',
              stageIds: [],
            },
          ],
        }),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyNotificationsOverrides'], {
          ownerId: {
            userNotifications: [
              {
                emailAddress: 'email2@email.com',
                stageIds: [
                  'proxy',
                  'develop',
                  'source',
                  'build',
                  'stage',
                  'release',
                  'operate',
                  'continuous-monitoring',
                ],
              },
            ],
          },
        })
      )(state);
      renderComponent(preloadedState);
      await waitFor(() => screen.getByRole('table'));
      const table = screen.getByRole('table', { name: 'Edit policy notifications table' });
      const checkboxes = within(table).getAllByRole('checkbox');
      checkboxes.forEach((checkbox) => expect(checkbox).toBeEnabled());
      expect(last(checkboxes)).toBeEnabled();
      expect(last(checkboxes)).toBeChecked();

      fireEvent.click(last(checkboxes));

      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy).toHaveBeenCalledWith({
        ownerId: 'ownerId',
        recipient: {
          emailAddress: 'email2@email.com',
          stageIds: ['proxy', 'develop', 'source', 'build', 'stage', 'release', 'operate', 'continuous-monitoring'],
          displayName: 'email2@email.com',
        },
        stageId: 'continuous-monitoring',
      });
      expect(last(checkboxes)).toBeEnabled();
      expect(last(checkboxes)).not.toBeChecked();
    });

    describe('uses correct stageId based on isSbomManager flag', () => {
      it('uses sbom-continuous-monitoring when isSbomManager is true', async () => {
        const spy = jest.spyOn(policyActions, 'toggleNotificationRecipientStage');
        const sbomState = {
          ...state,
          router: {
            currentParams: { organizationId: 'organizationId' },
            currentState: { name: 'sbomManager.organization' },
          },
        };
        renderComponent(sbomState);
        await waitFor(() => screen.getByRole('table'));
        const table = screen.getByRole('table', { name: 'Edit policy notifications table' });
        const checkboxes = within(table).getAllByRole('checkbox');
        expect(last(checkboxes)).toBeEnabled();
        expect(last(checkboxes)).not.toBeChecked();
        expect(screen.getByLabelText('notify user@email.com for continuous-monitoring')).toBeInTheDocument();

        fireEvent.click(last(checkboxes));
        expect(last(checkboxes)).toBeEnabled();
        expect(last(checkboxes)).toBeChecked();
        expect(spy).toHaveBeenCalledTimes(1);
        expect(spy).toHaveBeenCalledWith({
          ownerId: 'ownerId',
          recipient: {
            emailAddress: 'user@email.com',
            stageIds: ['proxy', 'develop'],
            displayName: 'user@email.com',
          },
          stageId: 'sbom-continuous-monitoring',
        });
      });

      it('uses continuous-monitoring when isSbomManager is false', async () => {
        const spy = jest.spyOn(policyActions, 'toggleNotificationRecipientStage');
        renderComponent(state);

        await waitFor(() => screen.getByRole('table'));
        const table = screen.getByRole('table', { name: 'Edit policy notifications table' });
        const checkboxes = within(table).getAllByRole('checkbox');
        expect(last(checkboxes)).toBeEnabled();
        expect(last(checkboxes)).not.toBeChecked();
        expect(screen.getByLabelText('notify user@email.com for continuous-monitoring')).toBeInTheDocument();

        fireEvent.click(last(checkboxes));
        expect(last(checkboxes)).toBeEnabled();
        expect(last(checkboxes)).toBeChecked();
        expect(spy).toHaveBeenCalledTimes(1);
        expect(spy).toHaveBeenCalledWith({
          ownerId: 'ownerId',
          recipient: {
            emailAddress: 'user@email.com',
            stageIds: ['proxy', 'develop'],
            displayName: 'user@email.com',
          },
          stageId: 'continuous-monitoring',
        });
      });
    });

    describe('when enforcement is not supported', () => {
      it('renders notifications not supported message when firewall is not supported', async () => {
        const preloadedState = compose(
          pathSet(['productFeatures', 'productFeatures', 'firewall'], false),
          pathSet(['productFeatures', 'productFeatures', 'notifications'], false),
          pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
          pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyNotificationsOverrideAllowed'], false)
        )(state);
        renderComponent(preloadedState);
        await waitFor(() => screen.getByRole('table'));

        const alert = screen.getByText('Notifications are not supported by your product license.');

        expect(alert).toBeVisible();
      });

      it('renders only proxy notifications are supported message when firewall is supported', async () => {
        const preloadedState = compose(
          pathSet(['productFeatures', 'productFeatures', 'firewall'], true),
          pathSet(['productFeatures', 'productFeatures', 'notifications'], false),
          pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
          pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyNotificationsOverrideAllowed'], false)
        )(state);
        renderComponent(preloadedState);
        await waitFor(() => screen.getByRole('table'));

        const alert = screen.getByText('Only Proxy Notifications are supported with your Firewall product license.');

        expect(alert).toBeVisible();
      });
    });
  });

  describe('when isFirewallOnlyLicense changes', () => {
    beforeEach(() => {
      jest.restoreAllMocks();
    });

    it('renders only proxy notifications enabled when isFirewallOnlyLicense is true', async () => {
      jest.spyOn(productLicenseSelectors, 'selectIsFirewallOnlyLicense').mockReturnValue(true);
      state.orgsAndPolicies.policy.currentPolicy.notifications = {
        userNotifications: [{ emailAddress: 'user@email.com', stageIds: ['proxy'] }],
      };

      renderComponent(state);
      await waitFor(() => screen.getByRole('table'));

      actionStages.forEach((stage) => {
        const checkbox = screen.getByRole('checkbox', { name: `notify user@email.com for ${stage.stageTypeId}` });
        if (stage.stageTypeId === 'proxy') {
          expect(checkbox).toBeEnabled();
        } else {
          expect(checkbox).toBeDisabled();
        }
      });
    });

    it('renders all notification stages enabled when isFirewallOnlyLicense is false', async () => {
      jest.spyOn(productLicenseSelectors, 'selectIsFirewallOnlyLicense').mockReturnValue(false);
      state.orgsAndPolicies.policy.currentPolicy.notifications = {
        userNotifications: [{ emailAddress: 'user@email.com', stageIds: actionStages.map((s) => s.stageTypeId) }],
      };

      renderComponent(state);
      await waitFor(() => screen.getByRole('table'));

      actionStages.forEach((stage) => {
        const checkbox = screen.getByRole('checkbox', { name: `notify user@email.com for ${stage.stageTypeId}` });
        expect(checkbox).toBeEnabled();
      });
    });
  });
});
