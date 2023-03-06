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

  describe('selectOwnerById', () => {
    it('is composed from the following selector', () => {
      expect(selectOwnerById.dependencies).toEqual([selectOwnersMap, jasmine.any(Function)]);
    });

    it('selects and organization by id', () => {
      const ownersMap = { nexus: { id: 'id', name: 'nexus' } };
      const result = selectOwnerById.resultFunc(ownersMap, 'nexus');

      expect(result).toEqual(ownersMap.nexus);
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
      expect(selectAllDescendantsByParentId.dependencies).toEqual([selectOwnersMap, jasmine.any(Function)]);
    });

    it('selects all descentand apps and orgs for a given parent org', () => {
      const ownersMap = {
        parent: {
          id: 'parent',
          name: 'parent',
          synthetic: false,
          applicationIds: ['nexus', 'lifecycle'],
          organizationIds: ['orgLevelTwo'],
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
      };
      const result = selectAllDescendantsByParentId.resultFunc(ownersMap, 'parent');

      expect(result).toEqual({
        applicationIds: ['nexus', 'lifecycle', 'childOrgAppOne', 'childOrgAppTwo', 'childOrgAppThree'],
        organizationIds: ['orgLevelTwo', 'orgLevelThree'],
      });
    });
  });
});
