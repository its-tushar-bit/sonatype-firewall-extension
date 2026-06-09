/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { groupBy, prop } from 'ramda';
import RepositoryManagerSummaryView from 'MainRoot/OrgsAndPolicies/repositories/RepositoryManagerSummaryView';
import {
  getRepositoryManagerById,
  getActionStageUrl,
  getApplicablePolicies,
  getRepositoryComponentNameUrl,
  getRepositoryInfoUrl,
  getRepositoryListUrl,
  getPermissionContextTestUrl,
  getAccessPageRolesUrl,
  getOwnerListUrl,
  getAddRepositoryUrl,
} from 'MainRoot/util/CLMLocation';
import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';
import { actionStagesPayload } from 'TestRoot/OrgsAndPolicies/ownerSummary/policiesTile/policiesTileTestData';
import { render, axiosMockAdapter, within, screen, fireEvent, mockInterceptionObserver } from 'TestRoot/SpecUtil';
import { actions as repositoriesActions } from 'MainRoot/OrgsAndPolicies/repositories/repositoriesConfigurationSlice';
import * as repositoriesSelectors from 'MainRoot/OrgsAndPolicies/repositories/repositoriesConfigurationSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as ownerSideNavSelectors from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import * as ownerSummarySelectors from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';

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

    axiosMock.onGet(getAccessPageRolesUrl(ownerType, ownerId)).reply(200, { membersByRole: [] });
    axiosMock.onPut(getPermissionContextTestUrl(ownerType, ownerId)).reply(200, ['WRITE']);
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

    const policiesTile = await screen.findByTestId('policies-tile');

    expect(await screen.findByText('repo-manager-name')).toBeVisible();
    expect(await screen.findByRole('button', { name: 'Policies' })).toBeVisible();

    expect(await within(policiesTile).findByText('Local to repo-manager-name')).toBeVisible();
    expect(await within(policiesTile).findByText('No local policies defined')).toBeVisible();

    expect(await within(policiesTile).findByText('Inherited from Repository Managers')).toBeVisible();
    expect(await within(policiesTile).findByText('repo-container-policy-1')).toBeVisible();
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

  it('renders namespace confusion protection tile', async () => {
    jest.spyOn(orgsAndPoliciesSelectors, 'selectOwnerProperties').mockReturnValue({ ownerId, ownerType });

    axiosMock
      .onPost(getRepositoryComponentNameUrl(ownerType, ownerId), {
        page: 1,
        pageSize: 6,
        searchFilters: [],
        sortFields: [{ sortableField: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME', asc: true, sortPriority: 1 }],
      })
      .reply(200, {
        proprietaryComponentNamePatterns: [
          {
            id: 'eb23d7dab5004c7496ba9195e5a4b862',
            format: 'maven',
            namespacePattern: 'Test',
            namePattern: null,
            repositoryManagerInstanceId: '9E111629-6B9EDCBA-B5989887-132718F9-8C354DFA',
            repositoryPublicId: 'maven-releases',
            enabled: true,
          },
        ],
        hasNextPage: false,
      });

    renderComponent(preloadedState);

    expect(await screen.findByTestId('namespace-confusion-protection-pill-configuration')).toBeVisible();
    expect(await screen.findByText('Test')).toBeVisible();
    expect(await screen.findByText('maven-releases')).toBeVisible();
    expect(screen.getByRole('switch')).toBeChecked();
  });

  it('renders configuration tile', async () => {
    jest.spyOn(routerSelectors, 'selectIsRepositoryManager').mockReturnValue(true);
    jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').mockReturnValue({
      ...ownerInfo,
      instanceId: 'managerInstanceId',
    });
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

  it('renders Access tile', async () => {
    renderComponent(preloadedState);
    const accessTile = await screen.findByTestId('repositories_access');

    expect(accessTile).toBeVisible();
  });

  describe('IQ Proxy Repo tile', () => {
    it('does not render IQ Proxy tile for non-virtual repository manager', async () => {
      jest.spyOn(ownerSideNavSelectors, 'selectIsVirtualRepositoryManager').mockReturnValue(false);
      renderComponent(preloadedState);

      await screen.findByTestId('repositories_access');

      expect(screen.queryByText('IQ Proxy Repo')).not.toBeInTheDocument();
    });

    it('renders IQ Proxy tile for virtual repository manager with view permission', async () => {
      jest.spyOn(ownerSideNavSelectors, 'selectIsVirtualRepositoryManager').mockReturnValue(true);
      jest.spyOn(ownerSummarySelectors, 'selectHasViewIqPermission').mockReturnValue(true);
      jest.spyOn(ownerSummarySelectors, 'selectHasEditIqPermission').mockReturnValue(true);
      jest.spyOn(routerSelectors, 'selectRepositoryManagerId').mockReturnValue(ownerId);
      axiosMock.onGet(getOwnerListUrl()).reply(200, { topParentOrganizationId: 'ROOT_ORGANIZATION_ID', ownersMap: {} });
      axiosMock.onPost(getAddRepositoryUrl(ownerId)).reply(201, {});
      renderComponent({
        ...preloadedState,
        firewallIqProxy: { saving: false, saveError: null, saveErrorId: 0 },
      });

      const tile = await screen.findByTestId('iq-proxy-repo-tile');
      expect(within(tile).getByRole('heading', { name: 'IQ proxy' })).toBeVisible();
      expect(within(tile).getByPlaceholderText('Repository name')).toBeInTheDocument();
      expect(within(tile).getByPlaceholderText('Upstream repository URL')).toBeInTheDocument();
      expect(within(tile).getByRole('button', { name: 'Save' })).toBeInTheDocument();
    });

    it('does not render IQ Proxy tile for virtual repository manager without view permission', async () => {
      jest.spyOn(ownerSideNavSelectors, 'selectIsVirtualRepositoryManager').mockReturnValue(true);
      jest.spyOn(ownerSummarySelectors, 'selectHasViewIqPermission').mockReturnValue(false);
      renderComponent(preloadedState);

      await screen.findByTestId('repositories_access');

      expect(screen.queryByTestId('iq-proxy-repo-tile')).not.toBeInTheDocument();
    });

    it('does not render IQ Proxy tile for virtual repository manager without write permission', async () => {
      jest.spyOn(ownerSideNavSelectors, 'selectIsVirtualRepositoryManager').mockReturnValue(true);
      jest.spyOn(ownerSummarySelectors, 'selectHasViewIqPermission').mockReturnValue(true);
      jest.spyOn(ownerSummarySelectors, 'selectHasEditIqPermission').mockReturnValue(false);
      renderComponent(preloadedState);

      await screen.findByTestId('repositories_access');

      expect(screen.queryByTestId('iq-proxy-repo-tile')).not.toBeInTheDocument();
    });
  });

  describe('RepositoriesConfigurationTile showHostedRepoLink prop wiring', () => {
    const hostedRepo = {
      oldestEvalTimestamp: null,
      managerInstanceId: 'managerInstanceId',
      managerName: 'managerName',
      repository: {
        id: 'hostedRepo',
        repositoryManagerId: 'c47da5d840b84eda8585381de5ebb189',
        publicId: 'hostedRepoName',
        auditEnabled: false,
        quarantineEnabled: false,
        format: 'maven',
        repositoryType: 'hosted',
      },
    };

    const stateWithFeatureEnabled = {
      productFeatures: { productFeatures: { 'hosted-repository-evaluation': true } },
    };

    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectIsRepositoryManager').mockReturnValue(true);
      jest.spyOn(repositoriesSelectors, 'selectRepositoriesLoading').mockReturnValue(false);
      jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').mockReturnValue({
        ...ownerInfo,
        instanceId: 'managerInstanceId',
      });
      jest
        .spyOn(repositoriesSelectors, 'selectRepositoriesByManagerInstanceId')
        .mockReturnValue(groupBy(prop('managerInstanceId'))([hostedRepo]));
      axiosMock.onGet(getRepositoryManagerById(ownerId)).reply(200, ownerInfo);
      axiosMock.onGet(getRepositoryListUrl(ownerInfo.id)).reply(200, [hostedRepo]);
    });

    it('passes showHostedRepoLink=false when in Firewall context', async () => {
      jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(true);
      jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(false);

      renderComponent({ ...preloadedState, ...stateWithFeatureEnabled });

      await screen.findByTestId('repositories_configuration');
      expect(screen.queryByTestId('repositories_configuration-hosted-link')).not.toBeInTheDocument();
    });

    it('passes showHostedRepoLink=true when in Lifecycle context with feature enabled', async () => {
      jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(false);
      jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(false);

      renderComponent({ ...preloadedState, ...stateWithFeatureEnabled });

      await screen.findByTestId('repositories_configuration');
      expect(screen.getByTestId('repositories_configuration-hosted-link')).toBeVisible();
    });

    it('passes showHostedRepoLink=false when in Lifecycle context with feature disabled', async () => {
      jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(false);
      jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(false);

      renderComponent(preloadedState);

      await screen.findByTestId('repositories_configuration');
      expect(screen.queryByTestId('repositories_configuration-hosted-link')).not.toBeInTheDocument();
    });

    it('passes showHostedRepoLink=false when in SBOM Manager context with feature enabled', async () => {
      jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(false);
      jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(true);

      renderComponent({ ...preloadedState, ...stateWithFeatureEnabled });

      await screen.findByTestId('repositories_configuration');
      expect(screen.queryByTestId('repositories_configuration-hosted-link')).not.toBeInTheDocument();
    });
  });

  describe('Limited Firewall Access Alert', () => {
    it('shows limited firewall access alert when showLimitedFirewallAccessAlert is true', async () => {
      // Mock the repository manager API to return 403, which will set showLimitedFirewallAccessAlert to true
      axiosMock.onGet(getRepositoryManagerById(ownerId)).reply(403, { message: 'Forbidden' });
      axiosMock.onGet(getApplicablePolicies(ownerType, ownerId)).reply(200, { policiesByOwner: [] });

      renderComponent(preloadedState);

      expect(
        await screen.findByText(/You have limited access to Repository Firewall based on your current permissions/)
      ).toBeVisible();
      expect(screen.getByText(/Some data or settings may not be visible. Contact your administrator/)).toBeVisible();
    });

    it('does not show limited firewall access alert when showLimitedFirewallAccessAlert is false', async () => {
      preloadedState.orgsAndPolicies.root = {
        ...preloadedState.orgsAndPolicies.root,
        showLimitedFirewallAccessAlert: false,
        selectedOwner: ownerInfo,
      };

      renderComponent(preloadedState);

      expect(await screen.findByRole('heading', { name: /repo-manager-name/ })).toBeVisible();
      expect(
        screen.queryByText(/You have limited access to Repository Firewall based on your current permissions/)
      ).not.toBeInTheDocument();
    });

    it('hides tiles when showing limited firewall access alert', async () => {
      // Mock the repository manager API to return 403, which will set showLimitedFirewallAccessAlert to true
      axiosMock.onGet(getRepositoryManagerById(ownerId)).reply(403, { message: 'Forbidden' });
      axiosMock.onGet(getApplicablePolicies(ownerType, ownerId)).reply(200, { policiesByOwner: [] });

      renderComponent(preloadedState);

      expect(
        await screen.findByText(/You have limited access to Repository Firewall based on your current permissions/)
      ).toBeVisible();

      // Verify tiles are not rendered when alert is shown
      expect(screen.queryByTestId('repositories_configuration')).not.toBeInTheDocument();
      expect(screen.queryByTestId('repositories_access')).not.toBeInTheDocument();
      expect(screen.queryByTestId('policies-tile')).not.toBeInTheDocument();
    });
  });
});
