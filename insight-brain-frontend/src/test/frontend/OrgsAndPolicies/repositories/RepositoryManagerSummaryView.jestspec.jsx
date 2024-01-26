/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import RepositoryManagerSummaryView from 'MainRoot/OrgsAndPolicies/repositories/RepositoryManagerSummaryView';
import {
  getRepositoryManagerById,
  getActionStageUrl,
  getApplicablePolicies,
  getRepositoryInfoUrl,
  getRepositoryListUrl,
} from 'MainRoot/util/CLMLocation';
import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';
import { actionStagesPayload } from 'TestRoot/OrgsAndPolicies/ownerSummary/policiesTile/policiesTileTestData';
import { render, axiosMockAdapter, within, screen, fireEvent, mockInterceptionObserver } from 'TestRoot/SpecUtil';
import { actions as repositoriesActions } from 'MainRoot/OrgsAndPolicies/repositories/repositoriesConfigurationSlice';
import * as repositoriesSelectors from 'MainRoot/OrgsAndPolicies/repositories/repositoriesConfigurationSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { groupBy, prop } from 'ramda';

const ownerType = 'repository_manager';
const ownerId = 'c47da5d840b84eda8585381de5ebb189';
const inheretedFromRepoContainerPolicy = {
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
const policiesByOwner = [
  {
    ownerId,
    ownerName: 'repo-manager-name',
    ownerType,
    policies: [],
    policyTags: [],
  },
  {
    ownerId: 'REPOSITORY_CONTAINER_ID',
    ownerName: 'Repository Managers',
    ownerType: 'repository_container',
    policies: [inheretedFromRepoContainerPolicy],
    policyTags: [],
  },
];

const repos = [
  {
    oldestEvalTimestamp: null,
    managerInstanceId: 'managerInstanceId',
    managerName: 'managerName',
    repository: {
      id: 'repository',
      repositoryManagerId: 'c47da5d840b84eda8585381de5ebb189',
      publicId: 'repositoryName',
      auditEnabled: true,
      quarantineEnabled: true,
      format: 'maven',
      repositoryType: 'proxy',
    },
  },
];

const ownerInfo = {
  id: 'c47da5d840b84eda8585381de5ebb189',
  name: 'repo-manager-name',
  instanceId: 'F2BC2A0B-E7D0DDA9-425601AB-F0AAD535-FDF19232',
  productName: 'Nexus',
  productVersion: '3.61.0-02',
};

describe('RepositoryManagerSummaryView', () => {
  let axiosMock, preloadedState, goToEditPolicySpy, loadRepositoriesByManagerIdSpy;

  const renderComponent = (preloadedState) => render(<RepositoryManagerSummaryView />, { preloadedState });

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    mockInterceptionObserver();
  });

  beforeEach(() => {
    preloadedState = {
      router: {
        currentState: {
          name: 'management.view.repository_manager',
          url: '/repository_manager/{repositoryManagerId}',
          data: {
            title: 'Repository manager Management',
            viewportSized: true,
          },
        },
        currentParams: {
          repositoryManagerId: ownerId,
        },
      },
      orgsAndPolicies: {
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

    axiosMock.onGet(getRepositoryManagerById(ownerId)).reply(200, {
      id: 'c47da5d840b84eda8585381de5ebb189',
      name: 'repo-manager-name',
      instanceId: 'F2BC2A0B-E7D0DDA9-425601AB-F0AAD535-FDF19232',
      productName: 'Nexus',
      productVersion: '3.61.0-02',
    });

    goToEditPolicySpy = jest.spyOn(actions, 'goToEditPolicy');
  });

  it('renders a loading indicator', () => {
    renderComponent(preloadedState);
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders an alert with retry if something goes wrong', async () => {
    axiosMock.onGet(getApplicablePolicies(ownerType, ownerId)).reply(200, { data: { policiesByOwner: {} } });
    axiosMock.onGet(getRepositoryManagerById(ownerId)).reply(() => Promise.reject('An error occurred loading data.'));

    renderComponent(preloadedState);

    let failureAlert = await screen.findByRole('alert');

    expect(failureAlert).toBeVisible();
    expect(failureAlert).toHaveTextContent('An error occurred loading data.');

    let retryButton = await within(failureAlert).getByRole('button');

    expect(retryButton).toBeVisible();
    fireEvent.click(retryButton);

    expect(screen.queryByText('Loading…')).toBeInTheDocument();
    failureAlert = await screen.findByRole('alert');
    expect(failureAlert).toBeVisible();
    expect(failureAlert).toHaveTextContent('An error occurred loading data.');
  });

  it('renders an alert if there is no matching owner', async () => {
    axiosMock.onGet(getApplicablePolicies(ownerType, ownerId)).reply(200, { policiesByOwner: [] });
    axiosMock.onGet(getRepositoryManagerById(ownerId)).reply('some error');

    renderComponent(preloadedState);

    let failureAlert = await screen.findByRole('alert');

    expect(failureAlert).toBeVisible();
    expect(failureAlert).toHaveTextContent('some error');
  });

  it('renders proper header with selected repository manager name and policy tile', async () => {
    axiosMock.onGet(getActionStageUrl()).reply(200, actionStagesPayload);
    axiosMock.onGet(getApplicablePolicies(ownerType, ownerId)).reply(200, { policiesByOwner });
    renderComponent(preloadedState);

    expect(await screen.findByText('repo-manager-name')).toBeVisible();
    expect(await screen.findByRole('button', { name: 'Policies' })).toBeVisible();

    expect(await screen.findByText('Local to repo-manager-name')).toBeVisible();
    expect(await screen.findByText('No local policies defined')).toBeVisible();

    expect(await screen.findByText('Inherited from Repository Managers')).toBeVisible();
    expect(await screen.findByText('repo-container-policy-1')).toBeVisible();
  });

  it('checks that the policy row is clickable', async () => {
    axiosMock.onGet(getActionStageUrl()).reply(200, actionStagesPayload);
    axiosMock.onGet(getApplicablePolicies(ownerType, ownerId)).reply(200, { policiesByOwner });
    renderComponent(preloadedState);

    const editButton = await screen.findByRole('button', {
      name: `Edit ${inheretedFromRepoContainerPolicy.name} policy`,
    });
    expect(editButton).not.toBeNull();
    expect(editButton).toBeVisible();

    fireEvent.click(editButton);
    expect(goToEditPolicySpy).toHaveBeenCalledWith(inheretedFromRepoContainerPolicy.id);
  });

  it('renders configuration tile', async () => {
    jest.spyOn(routerSelectors, 'selectIsRepositoryManager').mockReturnValue(true);
    jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').mockReturnValue(ownerInfo);
    jest
      .spyOn(repositoriesSelectors, 'selectRepositoriesByManagerInstanceId')
      .mockReturnValue(groupBy(prop('managerInstanceId'))(repos));

    loadRepositoriesByManagerIdSpy = jest.spyOn(repositoriesActions, 'loadRepositoriesByManagerId');

    axiosMock.onGet(getRepositoryListUrl('c47da5d840b84eda8585381de5ebb189')).reply(200, repos);
    axiosMock.onGet(getRepositoryManagerById(ownerId)).reply(200, ownerInfo);
    axiosMock.onDelete(getRepositoryInfoUrl('repository')).reply(204);

    renderComponent(preloadedState);

    expect(await screen.findByRole('button', { name: 'Configuration' })).toBeVisible();
    expect(await screen.findByText('repositoryName')).toBeVisible();
    expect(await screen.findByText('maven')).toBeVisible();
    expect(await screen.findByText('proxy')).toBeVisible();
    expect(await screen.findByText('Audit, Quarantine')).toBeVisible();
    expect(loadRepositoriesByManagerIdSpy).toHaveBeenCalled();
  });
});
