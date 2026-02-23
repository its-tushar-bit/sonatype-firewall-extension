/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { mergeDeepRight } from 'ramda';

import MenuBarStatefulBreadcrumb from 'MainRoot/mainHeader/MenuBar/MenuBarStatefulBreadcrumb';
import { render, screen } from 'TestRoot/SpecUtil';
import { getOwnersMap } from 'TestRoot/OrgsAndPolicies/ownerSideNav/nLevelMockData';
import router from 'MainRoot/router/routerInstance';
import { fireEvent } from '@testing-library/react';

describe('MenuBarStatefulBreadcrumb', () => {
  let c = 0;
  const generateFakeLink = () => {
    const routerUrls = '/link';
    c++;
    return routerUrls + c;
  };

  beforeEach(() => {
    c = 0;
    jest.spyOn(router.stateService, 'href').mockImplementation(generateFakeLink);
    jest.spyOn(router.stateService, 'includes').mockImplementation((stateName) => stateName === 'my.state');
    jest.spyOn(router.stateService, 'get').mockReturnValue(null);
  });

  const renderComponent = (preloadedState) => {
    return render(<MenuBarStatefulBreadcrumb />, { preloadedState });
  };
  const applicationPublicId = 'application publicId 2 at organization 2';
  const organizationsDepth = 2;
  const ownersMap = getOwnersMap(organizationsDepth);
  const sbomOwnersMap = {
    'REPOSITORY-CONTAINER-ID': {
      type: 'repository_container',
      id: 'REPOSITORY-CONTAINER-ID',
      name: 'Repository Managers',
      repositoryManagerIds: [],
      parentId: 'ROOT-ORGANIZATION-ID',
    },
    'ORG-TEST': {
      type: 'organization',
      id: 'ORG-TEST',
      name: 'ORG-TEST',
      synthetic: false,
      parentOrganizationId: 'ROOT-ORGANIZATION-ID',
      applicationIds: ['APPLICATION-PUBLIC-ID'],
      subOrgs: 1,
      totalApps: 1,
      organizationIds: [],
      parentId: 'ROOT-ORGANIZATION-ID',
    },
    'APPLICATION-PUBLIC-ID': {
      type: 'application',
      id: 'APPLICATION-INTERNAL-ID',
      name: 'APPLICATION-PUBLIC-ID',
      publicId: 'APPLICATION-PUBLIC-ID',
      organizationId: 'ORG-TEST',
      provider: null,
      repositoryUrl: null,
      parentId: 'ORG-TEST',
    },
    'ROOT-ORGANIZATION-ID': {
      type: 'organization',
      id: 'ROOT-ORGANIZATION-ID',
      name: 'Root Organization',
      synthetic: false,
      parentOrganizationId: null,
      applicationIds: null,
      subOrgs: 1,
      totalApps: 1,
      organizationIds: ['ORG-TEST'],
      repositoryContainerId: 'REPOSITORY-CONTAINER-ID',
      parentId: null,
    },
  };
  const sbomDisplayedOrg = {
    type: 'organization',
    id: 'ORG-TEST',
    name: 'ORG-TEST',
    synthetic: false,
    parentOrganizationId: 'ROOT-ORGANIZATION-ID',
    applicationIds: ['APPLICATION-PUBLIC-ID'],
    subOrgs: 1,
    totalApps: 1,
    organizationIds: [],
    parentId: 'ROOT-ORGANIZATION-ID',
  };

  describe('When displaying an organization', () => {
    let state;
    const displayedApplication = ownersMap[applicationPublicId];
    const displayedOrganization = ownersMap[displayedApplication.organizationId];

    beforeEach(() => {
      state = {
        router: {
          currentParams: { applicationPublicId },
          currentState: { name: 'management.view.application' },
        },
        orgsAndPolicies: {
          ownerSideNav: {
            ownersMap,
            displayedOrganization,
          },
        },
      };
    });

    it('renders bread crumb', async () => {
      renderComponent(state);

      const expectedBreadCrumbs = [
        'ROOT_ORGANIZATION_NAME',
        'organization name 1',
        'organization name 2',
        'application name 2 at organization 2',
      ];

      expectedBreadCrumbs.forEach((breadCrumbName) => {
        expect(screen.getByText(breadCrumbName)).toBeVisible();
      });
    });

    it('renders root org as first item', async () => {
      renderComponent(
        mergeDeepRight(state, {
          router: { currentParams: { applicationPublicId: 'null' } },
          orgsAndPolicies: {
            ownerSideNav: {
              ownersMap: {
                'organization id 2': {
                  applicationIds: ['null'],
                },
                null: {
                  id: '1e60f6d0dc514bf9bd90ab2e57311fda',
                  publicId: 'null',
                  type: 'application',
                  organizationId: 'organization name 2',
                  name: 'null',
                },
              },
            },
          },
        })
      );

      const expectedBreadCrumbs = ['ROOT_ORGANIZATION_NAME', 'organization name 1', 'organization name 2', 'null'];

      expectedBreadCrumbs.forEach((breadCrumbName) => {
        expect(screen.getByText(breadCrumbName)).toBeVisible();
      });
    });
  });

  describe('When displaying a repository manager', () => {
    it('renders bread crumbs for repository manager page', async () => {
      const displayedOrganization = {
        parentId: 'REPOSITORY_CONTAINER_ID',
        name: 'Repo Manager Name',
        type: 'repository_manager',
      };
      const ownersMapWithRepoManagers = {
        ...ownersMap,
        repositoryManagerOne: {
          parentId: 'REPOSITORY_CONTAINER_ID',
          name: 'Repo Manager Name',
          type: 'repository_manager',
        },
        REPOSITORY_CONTAINER_ID: {
          parentId: 'ROOT_ORGANIZATION_ID',
          name: 'Repository Managers',
          type: 'repository_container',
        },
      };
      const stateWithRouterInRepoContainerPage = {
        router: {
          currentState: { name: 'management.view.repository_manager' },
          currentParams: { repositoryManagerId: 'repositoryManagerOne' },
        },
        orgsAndPolicies: {
          ownerSideNav: {
            ownersMap: ownersMapWithRepoManagers,
            displayedOrganization,
          },
        },
      };

      renderComponent(stateWithRouterInRepoContainerPage);

      const expectedBreadCrumbs = ['ROOT_ORGANIZATION_NAME', 'Repository Managers', 'Repo Manager Name'];

      expectedBreadCrumbs.forEach((breadCrumbName) => {
        expect(screen.getByText(breadCrumbName)).toBeVisible();
      });
    });
  });

  describe('When displaying a repository', () => {
    it('renders bread crumbs for repository page', async () => {
      const displayedOrganization = {
        parentId: 'REPOSITORY_CONTAINER_ID',
        name: 'Repo Manager Name',
        type: 'repository_manager',
      };
      const ownersMapWithRepoManagers = {
        ...ownersMap,
        repository: {
          parentId: 'repositoryManagerOne',
          name: 'Repository',
          type: 'repository',
          format: 'maven2',
          repositoryType: 'proxy',
        },
        repositoryManagerOne: {
          parentId: 'REPOSITORY_CONTAINER_ID',
          name: 'Repo Manager Name',
          type: 'repository_manager',
        },
        REPOSITORY_CONTAINER_ID: {
          parentId: 'ROOT_ORGANIZATION_ID',
          name: 'Repository Managers',
          type: 'repository_container',
        },
      };
      const stateWithRouterInRepoContainerPage = {
        router: {
          currentState: { name: 'management.view.repository' },
          currentParams: { repositoryId: 'repository' },
        },
        orgsAndPolicies: {
          ownerSideNav: {
            ownersMap: ownersMapWithRepoManagers,
            displayedOrganization,
          },
        },
      };

      renderComponent(stateWithRouterInRepoContainerPage);

      const expectedBreadCrumbs = [
        'ROOT_ORGANIZATION_NAME',
        'Repository Managers',
        'Repo Manager Name',
        'Repository (maven2 : proxy)',
      ];
      expectedBreadCrumbs.forEach((breadCrumbName) => {
        expect(screen.getByText(breadCrumbName)).toBeVisible();
      });
    });
  });

  describe('When displaying on the SBOM Bill of Materials page', () => {
    const bomState = {
      productFeatures: {
        loading: false,
        productFeatures: {
          'sbom-manager': true,
        },
      },
      router: {
        currentParams: {
          applicationPublicId: 'APPLICATION-PUBLIC-ID',
          versionId: 'VERSION-ID',
        },
        currentState: { name: 'sbomManager.management.view.bom' },
      },
      orgsAndPolicies: {
        ownerSideNav: {
          ownersMap: sbomOwnersMap,
          displayedOrganization: sbomDisplayedOrg,
        },
      },
    };

    const bomStateSbomManagerNotEnabled = {
      productFeatures: {
        loading: false,
        productFeatures: {
          'sbom-manager': false,
        },
      },
      router: {
        currentState: { name: 'sbomManager.management.view.bom' },
      },
    };

    it('renders the correct breadcrumbs', async () => {
      renderComponent(bomState);

      const expectedBreadCrumbs = ['VERSION-ID', 'APPLICATION-PUBLIC-ID', 'ORG-TEST', 'Root Organization'];

      expectedBreadCrumbs.forEach((breadCrumbName) => {
        expect(screen.getByText(breadCrumbName)).toBeVisible();
      });
    });

    it('does not render breadcrumbs when SBOM Manager is NOT enabled', async () => {
      renderComponent(bomStateSbomManagerNotEnabled);
      expect(screen.queryByText('Root Organization')).not.toBeInTheDocument();
    });
  });

  describe('When displaying on the SBOM Component Details page', () => {
    const bomState = {
      productFeatures: {
        loading: false,
        productFeatures: {
          'sbom-manager': true,
        },
      },
      router: {
        currentParams: {
          applicationPublicId: 'APPLICATION-PUBLIC-ID',
          sbomVersion: 'VERSION-ID',
          componentHash: 'abc',
        },
        currentState: { name: 'sbomManager.component' },
      },
      orgsAndPolicies: {
        ownerSideNav: {
          ownersMap: sbomOwnersMap,
          displayedOrganization: sbomDisplayedOrg,
        },
      },
      sbomComponentDetailsPage: {
        componentDetails: {
          displayName: 'COMPONENT',
        },
      },
    };

    it('renders the correct breadcrumbs', async () => {
      renderComponent(bomState);

      const expectedBreadCrumbsOnScreen = ['COMPONENT', 'VERSION-ID', 'Root Organization'];
      const expectedHiddenBreadCrumbs = ['APPLICATION-PUBLIC-ID', 'ORG-TEST'];
      expectedBreadCrumbsOnScreen.forEach((breadCrumbName) => {
        expect(screen.getByText(breadCrumbName)).toBeVisible();
      });

      const showOtherBreadcrumsButton = screen.getByRole('button');
      fireEvent.click(showOtherBreadcrumsButton);
      expectedHiddenBreadCrumbs.forEach((breadCrumbName) => {
        expect(screen.getByText(breadCrumbName)).toBeVisible();
      });
    });
  });
});
