/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import RepositoryManagerSummaryView from 'MainRoot/OrgsAndPolicies/repositories/RepositoryManagerSummaryView';
import { getRepositoryManagerById, getActionStageUrl, getApplicablePolicies } from 'MainRoot/util/CLMLocation';
import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';
import { actionStagesPayload } from 'TestRoot/OrgsAndPolicies/ownerSummary/policiesTile/policiesTileTestData';
import { render, axiosMockAdapter, within, screen, fireEvent } from 'TestRoot/SpecUtil';

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
    ownerName: 'All Repositories',
    ownerType: 'repository_container',
    policies: [inheretedFromRepoContainerPolicy],
    policyTags: [],
  },
];

describe('RepositoryManagerSummaryView', () => {
  let axiosMock, preloadedState, goToEditPolicySpy;
  const renderComponent = (preloadedState) => render(<RepositoryManagerSummaryView />, { preloadedState });

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
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

    goToEditPolicySpy = spyOn(actions, 'goToEditPolicy').and.callThrough();
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

    expect(await screen.findByText('Loading…')).toBeVisible();
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

    expect(await screen.findByText('Inherited from All Repositories')).toBeVisible();
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
});
