/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectEntityId,
  selectOrgsAndPoliciesSlice,
  selectSelectedOwner,
  selectSelectedOwnerName,
  selectSelectedOwnerId,
  selectPoliciesByOwner,
  selectRootSlice,
  selectSelectedOwnerParentId,
  selectOwnerProperties,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

describe('orgsAndPoliciesSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            name: 'ownerName',
            id: 'ownerId',
            parentOrganizationId: 'parentId',
          },
        },
      },
      router: {
        currentState: {
          name: 'management.view.application',
        },
        currentParams: {
          organizationId: 'orgId',
          applicationPublicId: 'alpine-test',
          repositoryManagerId: 'repoManagerId',
        },
      },
    };
  });

  describe('selectOrgsAndPoliciesSlice', () => {
    it('selects orgsAndPolicies', () => {
      const appState = {
        orgsAndPolicies: null,
      };

      const selected = selectOrgsAndPoliciesSlice(appState);

      expect(selected).toBeNull();
    });
  });

  describe('selectSelectedOwner', () => {
    it('selects selected owner object', () => {
      const selected = selectSelectedOwner(mockState);

      expect(selected).toEqual({ name: 'ownerName', id: 'ownerId', parentOrganizationId: 'parentId' });
    });
  });

  describe('selectSelectedOwnerName', () => {
    it('selects selected owner name property', () => {
      const selected = selectSelectedOwnerName(mockState);

      expect(selected).toBe('ownerName');
    });

    it('selects OwnerName, when is a Repository Managers page', () => {
      mockState.router.currentState.name = 'repository_container';
      const selected = selectSelectedOwnerName(mockState);

      expect(selected).toBe('ownerName');
    });
  });

  describe('selectSelectedOwnerParentId', () => {
    it('selects selected owner id property', () => {
      const selected = selectSelectedOwnerParentId(mockState);

      expect(selected).toBe('parentId');
    });
  });

  describe('selectSelectedOwnerId', () => {
    it('selects selected owner id property', () => {
      const selected = selectSelectedOwnerId(mockState);

      expect(selected).toBe('ownerId');
    });
  });

  describe('selectEntityId', () => {
    it('returns app id', () => {
      expect(selectEntityId(mockState)).toBe('alpine-test');
    });

    it('returns org id', () => {
      mockState.router.currentState.name = 'management.view.organization';
      expect(selectEntityId(mockState)).toBe('orgId');
    });

    it('returns repository manager id', () => {
      mockState.router.currentState.name = 'management.view.repository_manager';
      expect(selectEntityId(mockState)).toBe('repoManagerId');
    });

    it('returns null', () => {
      mockState.router.currentState.name = 'management.view';
      expect(selectEntityId(mockState)).toBe(null);
    });
  });

  describe('selectPoliciesByOwner', () => {
    it('is composed from the following selector', () => {
      expect(selectPoliciesByOwner.dependencies).toEqual([selectRootSlice]);
    });

    it('selects policiesByOwner', () => {
      const actualSelection = selectPoliciesByOwner.resultFunc({ policiesByOwner: null });
      expect(actualSelection).toBeNull();
    });
  });

  describe('selectOwnerProperties', () => {
    it('returns application ownerType and ownerId', () => {
      mockState.router.currentParams = { applicationPublicId: 'alpine-test' };
      expect(selectOwnerProperties(mockState)).toEqual({
        ownerType: 'application',
        ownerId: 'alpine-test',
      });
    });

    it('returns organization ownerType and ownerId', () => {
      mockState.router.currentState.name = 'management.view.organization';
      mockState.router.currentParams = { organizationId: 'orgId' };
      expect(selectOwnerProperties(mockState)).toEqual({
        ownerType: 'organization',
        ownerId: 'orgId',
      });
    });

    it('returns repository ownerType and ownerId', () => {
      mockState.router.currentState.name = 'management.view.repository';
      mockState.router.currentParams = { repositoryId: 'repositoryId' };
      expect(selectOwnerProperties(mockState)).toEqual({
        ownerType: 'repository',
        ownerId: 'repositoryId',
      });
    });

    it('returns repository manager ownerType and ownerId', () => {
      mockState.router.currentState.name = 'management.view.repository_manager';
      mockState.router.currentParams = { repositoryManagerId: 'repoManagerId' };
      expect(selectOwnerProperties(mockState)).toEqual({
        ownerType: 'repository_manager',
        ownerId: 'repoManagerId',
      });
    });

    it('returns repository container ownerType and ownerId if router name includes repositories', () => {
      mockState.router.currentState.name = '/repositories';
      mockState.router.currentParams = {};
      expect(selectOwnerProperties(mockState)).toEqual({
        ownerType: 'repository_container',
        ownerId: 'REPOSITORY_CONTAINER_ID',
      });
    });
  });
});
