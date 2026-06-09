/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import RepositorySummaryView from 'MainRoot/OrgsAndPolicies/repositorySummaryView/RepositorySummaryView';
import {
  getAccessPageRolesUrl,
  getActionStageUrl,
  getApplicablePolicies,
  getRepositoryInfoUrl,
} from 'MainRoot/util/CLMLocation';
import { actionStagesPayload } from 'TestRoot/OrgsAndPolicies/ownerSummary/policiesTile/policiesTileTestData';
import { render, axiosMockAdapter, screen, within, fireEvent, mockInterceptionObserver } from 'TestRoot/SpecUtil';
import { getOwnersMap } from 'TestRoot/OrgsAndPolicies/ownerSideNav/nLevelMockData';

const ownerType = 'repository';
const ownerId = 'repository 1';
const ownersMap = getOwnersMap();
const inheritedFromRepoContainerPolicy = {
  id: '458eea63cba34b019a4bd99d589267de',
  name: 'repo-container-policy-1',
  ownerId: 'REPOSITORY_CONTAINER_ID',
  threatLevel: 5,
  legacyViolationAllowed: false,
  constraints: [
    {
      id: 'a843320012194a5699faa594959d9895',
      name: 'repo-container-policy-1',
      operator: 'OR',
      conditions: [
        {
          conditionTypeId: 'AgeInDays',
          operator: 'older than',
          value: '1460',
          conditionIndex: 0,
        },
      ],
    },
  ],
  actions: {},
  notifications: {
    userNotifications: [],
    roleNotifications: [],
    jiraNotifications: [],
    webhookNotifications: [],
  },
  policyActionsOverrideAllowed: false,
  policyActionsOverrides: null,
  policyNotificationsOverrideAllowed: false,
  policyNotificationsOverrides: null,
};
const inheritedFromRootOrganizationPolicy = {
  id: '458eea63cba34b019a4bd99d589267de',
  name: 'root-organization-policy-1',
  ownerId: 'ROOT_ORGANIZATION_ID',
  threatLevel: 5,
  legacyViolationAllowed: false,
  constraints: [
    {
      id: 'a843320012194a5699faa594959d9895',
      name: 'root-organization-policy-constraint-1',
      operator: 'OR',
      conditions: [
        {
          conditionTypeId: 'AgeInDays',
          operator: 'older than',
          value: '1460',
          conditionIndex: 0,
        },
      ],
    },
  ],
  actions: {},
  notifications: {
    userNotifications: [],
    roleNotifications: [],
    jiraNotifications: [],
    webhookNotifications: [],
  },
  policyActionsOverrideAllowed: false,
  policyActionsOverrides: null,
  policyNotificationsOverrideAllowed: false,
  policyNotificationsOverrides: null,
};
const policiesByOwner = [
  {
    ownerId,
    ownerName: 'repository 1',
    ownerType,
    policies: [],
    policyTags: [],
  },
  {
    ownerId: 'repository-manager',
    ownerName: 'repo-manager-name',
    ownerType: 'repocitory_manager',
    policies: [],
    policyTags: [],
  },
  {
    ownerId: 'REPOSITORY_CONTAINER_ID',
    ownerName: 'Repository Managers',
    ownerType: 'repository_container',
    policies: [inheritedFromRepoContainerPolicy],
    policyTags: [],
  },
  {
    ownerId: 'ROOT_ORGANIZATION_ID',
    ownerName: 'Root Organization',
    ownerType: 'organization',
    policies: [inheritedFromRootOrganizationPolicy],
    policyTags: [],
  },
];

describe('RepositorySummaryView', () => {
  let axiosMock, preloadedState;
  const renderComponent = (preloadedState) => render(<RepositorySummaryView />, { preloadedState });

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    mockInterceptionObserver();
  });

  beforeEach(() => {
    preloadedState = {
      router: {
        currentState: {
          name: 'management.view.repository',
          url: '/repository/{repositoryId}',
          data: {
            title: 'Repository Management',
            viewportSized: true,
          },
        },
        currentParams: {
          repositoryId: ownerId,
        },
      },
      orgsAndPolicies: {
        ownerSideNav: { ownersMap },
        sourceControl: {
          data: {
            repositoryUrl: null,
            provider: {
              value: null,
              parentValue: 'github',
            },
            token: {
              value: null,
            },
          },
        },
      },
    };

    axiosMock.onGet(getAccessPageRolesUrl(ownerType, ownerId)).reply(200, { membersByRole: [] });
    axiosMock.onGet(getRepositoryInfoUrl(ownerId)).reply(200, {
      managerInstanceId: '7EDE7A0F-41612922-1613498A-165AA9D9-D1031992',
      managerName: '7EDE7A0F-41612922-1613498A-165AA9D9-D1031992',
      oldestEvalTimestamp: '1681749017083',
      repository: {
        auditEnabled: true,
        format: 'maven2',
        id: ownerId,
        name: 'repository 1',
        lastManualConfigureTime: null,
        namespaceConfusionProtectionEnabled: false,
        policyCompliantComponentSelectionEnabled: false,
        publicId: 'repository 1',
        quarantineEnabled: true,
        repositoryManagerId: '655831bc0477421998b5600e64c05247',
        repositoryType: 'proxy',
      },
    });
  });

  it('renders a loading indicator', () => {
    renderComponent(preloadedState);
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders proper header with selected repository name and policy tile', async () => {
    axiosMock.onGet(getActionStageUrl()).reply(200, actionStagesPayload);
    axiosMock.onGet(getApplicablePolicies(ownerType, ownerId)).reply(200, { policiesByOwner });
    renderComponent(preloadedState);

    expect(await screen.findByText('repository 1')).toBeVisible();
    expect(await screen.findByText('(maven2 : proxy)')).toBeVisible();

    expect(await screen.findByRole('button', { name: 'Policies' })).toBeVisible();

    expect(await within(screen.getByTestId('policies-tile')).findByText('Local to repository 1')).toBeVisible();
    expect(await within(screen.getByTestId('policies-tile')).findByText('No local policies defined')).toBeVisible();

    expect(
      await within(screen.getByTestId('policies-tile')).findByText('Inherited from Repository Managers')
    ).toBeVisible();
    expect(await within(screen.getByTestId('policies-tile')).findByText('repo-container-policy-1')).toBeVisible();
  });

  it('renders an alert with retry if something goes wrong', async () => {
    axiosMock.onGet(getApplicablePolicies(ownerType, ownerId)).reply(200, { data: { policiesByOwner: {} } });
    axiosMock.onGet(getRepositoryInfoUrl(ownerId)).reply(() => Promise.reject('An error occurred loading data. Error'));

    renderComponent(preloadedState);

    let failureAlert = await screen.findByRole('alert');

    expect(failureAlert).toBeVisible();
    expect(failureAlert).toHaveTextContent('An error occurred loading data');

    let retryButton = await within(failureAlert).getByRole('button');

    expect(retryButton).toBeVisible();
    fireEvent.click(retryButton);

    expect(screen.getByText('Loading…')).toBeVisible();
    failureAlert = await screen.findByRole('alert');
    expect(failureAlert).toBeVisible();
    expect(failureAlert).toHaveTextContent('An error occurred loading data');
  });

  it('renders an alert if there is no matching owner', async () => {
    axiosMock.onGet(getApplicablePolicies(ownerType, ownerId)).reply(200, { policiesByOwner: [] });
    axiosMock.onGet(getRepositoryInfoUrl(ownerId)).reply('some error');

    renderComponent(preloadedState);

    let failureAlert = await screen.findByRole('alert');

    expect(failureAlert).toBeVisible();
    expect(failureAlert).toHaveTextContent('some error');
  });

  it('renders Access tile', async () => {
    renderComponent(preloadedState);
    const accessTile = await screen.findByTestId('repositories_access');

    expect(accessTile).toBeVisible();
  });

  it('does not render IQ Proxy form on individual repository page', async () => {
    renderComponent(preloadedState);

    await screen.findByTestId('repositories_access');

    expect(screen.queryByPlaceholderText('Repository name')).not.toBeInTheDocument();
    expect(screen.queryByPlaceholderText('Upstream repository URL')).not.toBeInTheDocument();
  });

  it('renders namespace confusion protection and access tile only for hosted repositories', async () => {
    axiosMock.onGet(getRepositoryInfoUrl(ownerId)).reply(200, {
      managerInstanceId: '7EDE7A0F-41612922-1613498A-165AA9D9-D1031992',
      managerName: '7EDE7A0F-41612922-1613498A-165AA9D9-D1031992',
      oldestEvalTimestamp: '1681749017083',
      repository: {
        auditEnabled: true,
        format: 'maven2',
        id: ownerId,
        name: 'repository 1',
        lastManualConfigureTime: null,
        namespaceConfusionProtectionEnabled: false,
        policyCompliantComponentSelectionEnabled: false,
        publicId: 'repository 1',
        quarantineEnabled: true,
        repositoryManagerId: '655831bc0477421998b5600e64c05247',
        repositoryType: 'hosted',
      },
    });
    renderComponent(preloadedState);

    expect(await screen.findByTestId('namespace-confusion-protection-pill-configuration')).toBeVisible();
    expect(await screen.findByTestId('repositories_access')).toBeVisible();
    expect(await screen.queryByTestId('policies-tile')).not.toBeInTheDocument();
  });

  it('renders policies and access tile only for proxy repositories', async () => {
    renderComponent(preloadedState);

    expect(await screen.findByTestId('repositories_access')).toBeVisible();
    expect(await screen.findByTestId('policies-tile')).toBeVisible();
    expect(await screen.queryByTestId('namespace-confusion-protection-pill-configuration')).not.toBeInTheDocument();
  });

  it('renders dropdown menu for proxy repositories', async () => {
    renderComponent(preloadedState);

    expect(await screen.findByText('repository 1')).toBeVisible();
    expect(await screen.findByText('(maven2 : proxy)')).toBeVisible();
    const actionButton = await screen.findByRole('button', { name: 'Actions' });
    expect(actionButton).toBeVisible();
  });

  it('does not renders dropdown menu for hosted repositories', async () => {
    axiosMock.onGet(getRepositoryInfoUrl(ownerId)).reply(200, {
      managerInstanceId: '7EDE7A0F-41612922-1613498A-165AA9D9-D1031992',
      managerName: '7EDE7A0F-41612922-1613498A-165AA9D9-D1031992',
      oldestEvalTimestamp: '1681749017083',
      repository: {
        auditEnabled: true,
        format: 'maven2',
        id: ownerId,
        name: 'repository 1',
        lastManualConfigureTime: null,
        namespaceConfusionProtectionEnabled: false,
        policyCompliantComponentSelectionEnabled: false,
        publicId: 'repository 1',
        quarantineEnabled: true,
        repositoryManagerId: '655831bc0477421998b5600e64c05247',
        repositoryType: 'hosted',
      },
    });
    renderComponent(preloadedState);

    expect(await screen.findByText('repository 1')).toBeVisible();
    expect(await screen.findByText('(maven2 : hosted)')).toBeVisible();
    const actionButton = screen.queryByRole('button', { name: 'Actions' });
    expect(actionButton).toBeNull();
  });
});
