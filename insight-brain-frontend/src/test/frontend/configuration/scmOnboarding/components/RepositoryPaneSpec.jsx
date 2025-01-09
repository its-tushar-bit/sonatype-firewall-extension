/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { fakeRouterState } from 'TestRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavTestingUtils';
import React from 'react';
import { render, screen, axiosMockAdapter, fireEvent, within } from 'TestRoot/SpecUtil';
import {
  getApplicationsUrl,
  getOwnerListUrl,
  getPermissionContextTestUrl,
  getRepositoriesUrl,
} from 'MainRoot/util/CLMLocation';
import RepositoryPane from 'MainRoot/configuration/scmOnboarding/components/RepositoryPane';
import RouterStateContext from 'MainRoot/react/RouterStateContext';
import { getOwnersMap } from '../../../OrgsAndPolicies/ownerSideNav/nLevelMockData';
import { initialState } from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import { createOrgWithToken, createRepo } from './utils';

describe('RepositoryPane', function () {
  let mockAxiosCalls;
  let state;
  let routerContext;
  const organizationsDepth = 4;
  const ownersMap = getOwnersMap(organizationsDepth);
  const topParentOrganizationId = 'ROOT_ORGANIZATION_ID';
  const ownerListPayload = { topParentOrganizationId, ownersMap };
  const repositoriesList = {
    repositories: [
      {
        oldestEvalTimestamp: null,
        managerInstanceId: '54342D8A-8FBE62A7-98C2B285-C37C2DC0-5FDBFC12',
        repository: {
          id: 'c192fc00375948dfbe1e8702ef6a3e44',
          repositoryManagerId: '17ee89ffd86649c49ce32f7d0328072a',
          publicId: 'maven-central',
          enabled: true,
          quarantineEnabled: false,
          format: 'maven2',
        },
      },
      {
        oldestEvalTimestamp: null,
        managerInstanceId: '54342D8A-8FBE62A7-98C2B285-C37C2DC0-5FDBFC12',
        repository: {
          id: 'c192fc00375948dfbe1e8702ef6a3e45',
          repositoryManagerId: '17ee89ffd86649c49ce32f7d0328072a',
          publicId: 'maven-central',
          enabled: true,
          quarantineEnabled: false,
          format: 'maven2',
        },
      },
      {
        oldestEvalTimestamp: null,
        managerInstanceId: '54342D8A-8FBE62A7-98C2B285-C37C2DC0-5FDBFC12',
        repository: {
          id: 'c192fc00375948dfbe1e8702ef6a3e46',
          repositoryManagerId: '17ee89ffd86649c49ce32f7d0328072a',
          publicId: 'maven-central',
          enabled: true,
          quarantineEnabled: false,
          format: 'maven2',
        },
      },
    ],
  };
  const ownerType = 'repository_container';
  const ownerListUrl = getOwnerListUrl();
  const permissionContextTestUrl = getPermissionContextTestUrl(ownerType);
  const repositories = ['aaaa', 'bbbb', 'aabb'].map((prefix) => createRepo(prefix));
  const organizations = ['org1', 'org2', 'org3'].map((prefix) => createOrgWithToken(prefix));
  const mock$State = jasmine.createSpyObj('$state', ['get', 'href']);
  mock$State.href.and.returnValue('routerUrl');

  const initialProps = {
    loadingRepositories: false,
    loadingPage: false,
    repositories,
    totalRepositories: 5,
    organizations,
    selectedOrganization: createOrgWithToken('org1'),
    onRepositorySelectionChanged: jasmine.createSpy('onRepositorySelectionChanged'),
    loadRepositoriesErrorCode: null,
    generalError: null,
    scmProvider: 'github',
    currentHostUrlState: initialState('https://github.com/'),
    defaultHostUrl: '',
    isGitHostNeeded: false,
    isGitHostDialogVisible: false,
    isSelectingOrganization: false,
    isScmTokenConfigured: true,
    isImporting: false,
    $state: mock$State,

    // sorting
    sortConfiguration: {
      key: 'namespace',
      sortingOrder: ['namespace', 'project', 'description', 'defaultBranch'],
      dir: 'asc',
    },

    // actions
    setSorting: jasmine.createSpy('setSorting'),
    setSortingParameters: jasmine.createSpy('setSortingParameters'),
    importSelectedRepositories: jasmine.createSpy('importSelectedRepositories'),
    loadRepositories: jasmine.createSpy('loadRepositories'),
    setShowHostDialog: jasmine.createSpy('setShowHostDialog'),
    setIsGitHostNeeded: jasmine.createSpy('setIsGitHostNeeded'),
  };

  beforeAll(() => {
    mockAxiosCalls = axiosMockAdapter();
  });

  beforeEach(() => {
    state = {
      router: {
        currentParams: {},
        currentState: { name: 'management.view.organization' },
      },
    };

    routerContext = { href: null, includes: () => false };
    spyOn(routerContext, 'href').and.callFake(fakeRouterState);

    mockAxiosCalls.onGet(ownerListUrl).reply(200, ownerListPayload);
    mockAxiosCalls.onGet(getApplicationsUrl()).reply(200, []);
    mockAxiosCalls.onPut(permissionContextTestUrl).reply(200, []);
    mockAxiosCalls.onGet(getRepositoriesUrl()).reply(200, repositoriesList);
  });

  const renderComponent = (props = initialProps) => {
    return render(
      <RouterStateContext.Provider value={routerContext}>
        <RepositoryPane {...props} />
      </RouterStateContext.Provider>,
      { preloadedState: state }
    );
  };

  describe('render errors', () => {
    it('renders token not configured error', () => {
      renderComponent({ ...initialProps, isScmTokenConfigured: false });
      const errorAlert = screen.getByRole('alert');
      expect(errorAlert).toBeVisible();
      expect(errorAlert).toHaveTextContent(
        "An error occurred loading data. We could not find a token. You can configure a token to be shared across organizations in the Root Organization's Source Control Configuration page, or you can provide a custom token for the org-org1 Organization."
      );
    });

    it('renders host not configured error', () => {
      renderComponent({ ...initialProps, isGitHostNeeded: true });
      const errorAlert = screen.getByRole('alert');
      expect(errorAlert).toBeVisible();
      expect(errorAlert).toHaveTextContent(
        'IQ Server was unable to identify the URL for your GitHub host. You need to provide a SCM URL in order to proceed'
      );
    });

    it('renders load repositories error', () => {
      renderComponent({ ...initialProps, loadRepositoriesErrorCode: true });
      const errorAlert = screen.getByRole('alert');
      expect(errorAlert).toBeVisible();
      expect(errorAlert).toHaveTextContent(
        'IQ Server was unable to connect to GitHub using the credentials associated with the org-org1 Organization. You may try a different host URL or manage your SCM configuration in the Orgs & Policies page'
      );
    });

    it('renders general error', () => {
      renderComponent({ ...initialProps, generalError: { message: true } });
      const errorAlert = screen.getByRole('alert');
      expect(errorAlert).toBeVisible();
      expect(errorAlert).toHaveTextContent('An error occurred loading data. Click here to change the git host URL.');
    });
  });

  describe('organizations dropdown', () => {
    it('renders organizations dropdown', () => {
      renderComponent();
      const orgsDropdown = screen.getAllByRole('button')[0];
      expect(orgsDropdown).toBeVisible();
      expect(orgsDropdown).toHaveTextContent('org-org1');

      let firstLink = screen.queryByRole('link', { name: 'org-org1' });
      let secondLink = screen.queryByRole('link', { name: 'org-org2' });
      let thirdLink = screen.queryByRole('link', { name: 'org-org3' });

      expect(firstLink).toBeNull();
      expect(secondLink).toBeNull();
      expect(thirdLink).toBeNull();

      fireEvent.click(orgsDropdown);

      firstLink = screen.getByRole('link', { name: 'org-org1' });
      secondLink = screen.getByRole('link', { name: 'org-org2' });
      thirdLink = screen.getByRole('link', { name: 'org-org3' });

      expect(firstLink).toBeVisible();
      expect(secondLink).toBeVisible();
      expect(thirdLink).toBeVisible();
    });
  });

  it('renders SCM Server Needed', () => {
    renderComponent({ ...initialProps, isGitHostDialogVisible: true });
    const errorModal = screen.getByRole('dialog');
    let errorAlert = within(errorModal).getByRole('alert');
    const hostUrlInput = within(errorModal).getByRole('textbox');
    const continueBtn = within(errorModal).getByRole('button', { name: 'Continue' });
    expect(errorAlert).toBeVisible();
    expect(hostUrlInput).toBeVisible();
    expect(continueBtn).toBeVisible();
    expect(errorAlert).toHaveTextContent(
      'IQ Server was unable to identify the URL for your GitHub host. You need to provide a SCM URL in order to proceed'
    );
  });

  it('renders new organization modal', () => {
    renderComponent();
    const newOrgButton = screen.getByRole('button', { name: /New Organization/i });
    expect(newOrgButton).toBeVisible();
    fireEvent.click(newOrgButton);

    const newOrgModal = screen.getByRole('dialog');
    expect(newOrgModal).toBeVisible();
  });

  it('renders repositories status', () => {
    renderComponent();
    const repoStatus = screen.getByTestId('repo-status');
    expect(repoStatus).toHaveTextContent('3Repositories found2imported');
  });

  describe('repositories table', () => {
    it('renders loading indicator', () => {
      renderComponent({ ...initialProps, loadingRepositories: true });
      const loading = screen.getByText('Loading…');
      expect(loading).toBeVisible();
    });

    it('renders loading indicator', () => {
      renderComponent({ ...initialProps, isSelectingOrganization: true });
      const loading = screen.getByText('Loading…');
      expect(loading).toBeVisible();
    });

    it('renders repositories', () => {
      renderComponent();
      const reposTable = screen.getByRole('table');
      expect(reposTable).toBeVisible();
      const repos = within(reposTable).getAllByRole('row');
      expect(repos.length).toBe(5);
      repos.splice(2, 5).map((repo, index) => {
        const repoCells = within(repo).getAllByRole('cell');
        expect(repoCells[2]).toHaveTextContent(`${repositories[index].project}`);
      });
    });
  });
});
