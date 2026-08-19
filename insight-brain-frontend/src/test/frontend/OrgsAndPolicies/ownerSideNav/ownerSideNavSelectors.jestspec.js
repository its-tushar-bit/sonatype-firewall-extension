/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectOrgsAndPoliciesSlice } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectOwnerSideNavSlice,
  selectLoadError,
  selectLoading,
  selectDisplayedOrganization,
  selectOwnersMap,
  selectShowRepositories,
  selectOwnerById,
  selectIsDisplayedOrganizationSynthetic,
  selectAllDescendantsByParentId,
  selectTopParentOrganizationId,
  selectOwnersFlattenEntries,
  selectRepoManagerOwnersEntries,
  selectRepoManagerOwnersEntriesSorted,
} from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';

describe('ownerSideNavSelectors', () => {
  describe('selectOwnerSideNavSlice', () => {
    it('is composed from the following selector', () => {
      expect(selectOwnerSideNavSlice.dependencies).toEqual([selectOrgsAndPoliciesSlice]);
    });

    it('selects owner sidenav slice', () => {
      const state = { ownerSideNav: {} };
      const result = selectOwnerSideNavSlice.resultFunc(state);

      expect(result).toEqual({});
    });
  });

  describe('selectLoadError', () => {
    it('is composed from the following selector', () => {
      expect(selectLoadError.dependencies).toEqual([selectOwnerSideNavSlice]);
    });

    it('selects owner sidenav load error', () => {
      const state = { loadError: 'error' };
      const result = selectLoadError.resultFunc(state);

      expect(result).toEqual(state.loadError);
    });
  });

  describe('selectLoading', () => {
    it('is composed from the following selector', () => {
      expect(selectLoading.dependencies).toEqual([selectOwnerSideNavSlice]);
    });

    it('selects owner sidenav loading status', () => {
      const state = { loading: true };
      const result = selectLoading.resultFunc(state);

      expect(result).toEqual(state.loading);
    });
  });

  describe('selectDisplayedOrganization', () => {
    it('is composed from the following selector', () => {
      expect(selectDisplayedOrganization.dependencies).toEqual([selectOwnerSideNavSlice]);
    });

    it('selects displayed organization', () => {
      const state = { displayedOrganization: { id: 'id', name: 'name' } };
      const result = selectDisplayedOrganization.resultFunc(state);

      expect(result).toEqual(state.displayedOrganization);
    });
  });

  describe('selectOwnersMap', () => {
    it('is composed from the following selector', () => {
      expect(selectOwnersMap.dependencies).toEqual([selectOwnerSideNavSlice]);
    });

    it('selects owners map', () => {
      const state = { ownersMap: { nexus: { id: 'id', name: 'nexus' } } };
      const result = selectOwnersMap.resultFunc(state);

      expect(result).toEqual(state.ownersMap);
    });
  });

  describe('selectShowRepositories', () => {
    it('is composed from the following selector', () => {
      expect(selectShowRepositories.dependencies).toEqual([selectOwnerSideNavSlice]);
    });

    it('selects showRepositories flag', () => {
      const state = { showRepositories: true };
      const result = selectShowRepositories.resultFunc(state);

      expect(result).toEqual(state.showRepositories);
    });
  });

  describe('selectOwnersFlattenEntries', () => {
    it('is composed from the following selector', () => {
      expect(selectOwnersFlattenEntries.dependencies).toEqual([selectOwnersMap]);
    });

    it('selects flattern entries', () => {
      const state = {
        repository_manager_id_1: {
          type: 'repository_manager',
          id: 'repository_manager_id_1',
          name: 'repository_manager_1',
        },
        'app-1': {
          type: 'application',
          id: '8f9a99df1f5d4e68895bd0ac445d03e4',
          name: 'app-1',
        },
        '392e7830a78544c188ddd50a025fe3b2': {
          type: 'repository',
          id: '392e7830a78544c188ddd50a025fe3b2',
          name: 'npm-hosted',
        },
        repository_manager_id_2: {
          type: 'repository_manager',
          id: 'repository_manager_id_2',
          name: 'repository_manager_2',
        },
      };
      const result = selectOwnersFlattenEntries.resultFunc(state);

      expect(result).toEqual({
        organizations: [],
        applications: [
          {
            type: 'application',
            id: '8f9a99df1f5d4e68895bd0ac445d03e4',
            name: 'app-1',
          },
        ],
        repositories: [
          {
            type: 'repository',
            id: '392e7830a78544c188ddd50a025fe3b2',
            name: 'npm-hosted',
          },
        ],
        repositoryManagers: [
          {
            type: 'repository_manager',
            id: 'repository_manager_id_1',
            name: 'repository_manager_1',
          },
          {
            type: 'repository_manager',
            id: 'repository_manager_id_2',
            name: 'repository_manager_2',
          },
        ],
      });
    });
  });

  describe('selectRepoManagerOwnersEntries', () => {
    it('is composed from the following selector', () => {
      expect(selectRepoManagerOwnersEntries.dependencies).toEqual([selectOwnersFlattenEntries]);
    });

    it('selects repository managers entries', () => {
      const state = {
        organizations: [],
        applications: [],
        repositories: [],
        repositoryManagers: [
          {
            type: 'repository_manager',
            id: 'repoManagerId1',
            name: 'repoManagerName1',
          },
          {
            type: 'repository_manager',
            id: 'repoManagerId2',
            name: 'repoManagerName2',
          },
        ],
      };
      const result = selectRepoManagerOwnersEntries.resultFunc(state);

      expect(result).toEqual(state.repositoryManagers);
    });
  });

  describe('selectRepoManagerOwnersEntriesSorted', () => {
    it('is composed from the following selector', () => {
      expect(selectRepoManagerOwnersEntriesSorted.dependencies).toEqual([selectRepoManagerOwnersEntries]);
    });

    it('selects sorted repository managers entries by name ascending', () => {
      const state = [
        {
          type: 'repository_manager',
          id: 'repoManagerId2',
          name: 'repoManagerNameB',
        },
        {
          type: 'repository_manager',
          id: 'repoManagerId1',
          name: 'repoManagerNameA',
        },
        {
          type: 'repository_manager',
          id: 'repoManagerId4',
          name: 'repoManagerNameD',
        },
        {
          type: 'repository_manager',
          id: 'repoManagerId3',
          name: 'repoManagerNameC',
        },
      ];
      const result = selectRepoManagerOwnersEntriesSorted.resultFunc(state);

      expect(result).toEqual([
        {
          type: 'repository_manager',
          id: 'repoManagerId1',
          name: 'repoManagerNameA',
        },
        {
          type: 'repository_manager',
          id: 'repoManagerId2',
          name: 'repoManagerNameB',
        },
        {
          type: 'repository_manager',
          id: 'repoManagerId3',
          name: 'repoManagerNameC',
        },
        {
          type: 'repository_manager',
          id: 'repoManagerId4',
          name: 'repoManagerNameD',
        },
      ]);
    });
  });

  describe('selectOwnerById', () => {
    it('is composed from the following selector', () => {
      expect(selectOwnerById.dependencies).toEqual([
        selectOwnersMap,
        selectTopParentOrganizationId,
        expect.any(Function),
      ]);
    });

    it('selects and organization by id', () => {
      const ownersMap = { nexus: { id: 'id', name: 'nexus' } };
      const result = selectOwnerById.resultFunc(ownersMap, 'nexus', {
        organizationId: 'nexus',
        needsSyntheticRoot: false,
      });

      expect(result).toEqual(ownersMap.nexus);
    });

    it('selects and organization by id with added synthetic root', () => {
      const ownersMap = { nexus: { id: 'id', name: 'nexus' } };
      const rootOrg = {
        id: 'ROOT_ORGANIZATION_ID',
        name: 'Root Organization',
        organizationIds: ['nexus'],
        synthetic: true,
      };
      const result = selectOwnerById.resultFunc(ownersMap, 'nexus', {
        organizationId: 'ROOT_ORGANIZATION_ID',
        needsSyntheticRoot: true,
      });
      expect(result).toEqual(rootOrg);
    });
  });

  describe('selectIsDisplayedOrganizationSynthetic', () => {
    it('is composed from the following selector', () => {
      expect(selectIsDisplayedOrganizationSynthetic.dependencies).toEqual([selectDisplayedOrganization]);
    });

    it('selects synthetic field from displayed organization', () => {
      const displayedOrganization = { id: 'id', name: 'nexus', synthetic: true };
      const result = selectIsDisplayedOrganizationSynthetic.resultFunc(displayedOrganization);

      expect(result).toEqual(displayedOrganization.synthetic);
    });
  });

  describe('selectAllDescendantsByParentId', () => {
    it('is composed from the following selector', () => {
      expect(selectAllDescendantsByParentId.dependencies).toEqual([selectOwnersMap, expect.any(Function)]);
    });

    const ownersMap = {
      orgsAndAppsParent: {
        id: 'orgsAndAppsParent',
        name: 'orgsAndAppsParent',
        synthetic: false,
        applicationIds: ['nexus', 'lifecycle'],
        organizationIds: ['orgLevelTwo'],
      },
      repoParent: {
        id: 'repoParent',
        name: 'repoParent',
        synthetic: false,
        repositoryContainerId: 'repoContainer',
      },
      nexus: { publicId: 'nexus', name: 'nexus', synthetic: false },
      lifecycle: { publicId: 'lifecycle', name: 'lifecycle', synthetic: false },
      orgLevelOne: {
        id: 'orgLevelOne',
        name: 'orgLevelOne',
        synthetic: false,
        applicationIds: ['hds'],
        organizationIds: [],
      },
      hds: { publicId: 'hds', name: 'hds', synthetic: false },
      orgLevelTwo: {
        id: 'orgLevelTwo',
        name: 'orgLevelTwo',
        synthetic: false,
        applicationIds: ['childOrgAppOne', 'childOrgAppTwo'],
        organizationIds: ['orgLevelThree'],
      },
      childOrgAppOne: { publicId: 'childOrgAppOne', name: 'childOrgAppOne', synthetic: false },
      childOrgAppTwo: { publicId: 'childOrgAppTwo', name: 'childOrgAppTwo', synthetic: false },
      orgLevelThree: {
        id: 'orgLevelThree',
        name: 'orgLevelThree',
        synthetic: false,
        applicationIds: ['childOrgAppThree'],
        organizationIds: [],
      },
      childOrgAppThree: { publicId: 'childOrgAppThree', name: 'childOrgAppThree', synthetic: false },
      repoContainer: {
        type: 'repository_container',
        id: 'repoContainer',
        name: 'Repository Managers',
        repositoryManagerIds: ['repomanager1', 'repomanager2'],
        parentId: 'repoParent',
      },
      repomanager1: {
        type: 'repository_manager',
        id: 'repomanager1',
        name: '4C5ABCA7-0DBA-449B-A3D3-A7607F8E91B5',
        instanceId: '4C5ABCA7-0DBA-449B-A3D3-A7607F8E91B5',
        repositoryIds: ['repo1a', 'repo1b'],
        parentId: 'repoContainer',
      },
      repomanager2: {
        type: 'repository_manager',
        id: 'repomanager2',
        name: '91D74F09-3FE2E0B7-DF2B86A6-969AE288-DE07E9B5',
        instanceId: '91D74F09-3FE2E0B7-DF2B86A6-969AE288-DE07E9B5',
        repositoryIds: ['repo2a', 'repo2b'],
        parentId: 'repoContainer',
      },
      repo2b: {
        type: 'repository',
        id: 'repo2b',
        name: 'nuget-hosted',
        repositoryManagerId: '0b9a675da0a14deabe26ad90df74a0cf',
        repositoryType: 'hosted',
        parentId: '0b9a675da0a14deabe26ad90df74a0cf',
      },
      repo1b: {
        type: 'repository',
        id: 'repo1b',
        name: 'maven-releases',
        repositoryManagerId: 'repomanager1',
        repositoryType: 'hosted',
        parentId: 'repomanager1',
      },
      repo2a: {
        type: 'repository',
        id: 'repo2a',
        name: 'maven-snapshots',
        repositoryManagerId: '0b9a675da0a14deabe26ad90df74a0cf',
        repositoryType: 'hosted',
        parentId: '0b9a675da0a14deabe26ad90df74a0cf',
      },
      repo1a: {
        type: 'repository',
        id: 'repo1a',
        name: 'maven-central',
        repositoryManagerId: 'repomanager1',
        repositoryType: 'proxy',
        parentId: 'repomanager1',
      },
    };

    it('selects all descendant apps and orgs for a given orgsAndAppsParent org', () => {
      const result = selectAllDescendantsByParentId.resultFunc(ownersMap, 'orgsAndAppsParent');

      expect(result).toEqual({
        applicationIds: ['nexus', 'lifecycle', 'childOrgAppOne', 'childOrgAppTwo', 'childOrgAppThree'],
        organizationIds: ['orgLevelTwo', 'orgLevelThree'],
        repositoryContainerId: null,
        repositoryManagerIds: [],
        repositoryIds: [],
      });
    });

    it('selects all descendant repos, repos managers and repo containers for the given parent', () => {
      const result = selectAllDescendantsByParentId.resultFunc(ownersMap, 'repoParent');

      expect(result).toEqual({
        applicationIds: [],
        organizationIds: [],
        repositoryContainerId: 'repoContainer',
        repositoryManagerIds: ['repomanager1', 'repomanager2'],
        repositoryIds: ['repo1a', 'repo1b', 'repo2a', 'repo2b'],
      });
    });

    it('does not crash when ownersMap entry referenced in organizationIds is missing (CLM-30626)', () => {
      const brokenOwnersMap = {
        root: {
          id: 'root',
          type: 'organization',
          organizationIds: ['missingOrg'],
          applicationIds: [],
          repositoryIds: [],
          repositoryManagerIds: [],
          repositoryContainerId: null,
        },
      };

      expect(() => selectAllDescendantsByParentId.resultFunc(brokenOwnersMap, 'root')).not.toThrow();
    });
  });
});
