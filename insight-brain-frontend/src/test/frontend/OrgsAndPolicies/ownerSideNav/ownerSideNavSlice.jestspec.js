/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { initialState, actions } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

describe('ownerSideNavSlice (FIRE-663)', () => {
  it('expandVirtualRepositoryManagers flips the collapse check to true', () => {
    const state = { ...initialState, toggleVirtualRepositoryManagersCheck: false };

    const newState = reducer(state, actions.expandVirtualRepositoryManagers());

    expect(newState.toggleVirtualRepositoryManagersCheck).toBe(true);
  });

  describe('load.fulfilled', () => {
    it('preserves toggleVirtualRepositoryManagersCheck instead of resetting it', () => {
      const state = { ...initialState, toggleVirtualRepositoryManagersCheck: true };

      const newState = reducer(state, {
        type: 'ownerSideNav/load/fulfilled',
        payload: { loading: false, displayedOrganization: null, flattenEntries: {} },
      });

      expect(newState.toggleVirtualRepositoryManagersCheck).toBe(true);
    });
  });

  describe('onRouterFinish', () => {
    it('preserves toggleVirtualRepositoryManagersCheck across management.view route changes', () => {
      const state = {
        ...initialState,
        ownersMap: { ROOT_ORGANIZATION_ID: { id: 'ROOT_ORGANIZATION_ID' } },
        topParentOrganizationId: 'ROOT_ORGANIZATION_ID',
        toggleVirtualRepositoryManagersCheck: true,
      };

      const newState = reducer(state, {
        type: UI_ROUTER_ON_FINISH,
        payload: { toState: { name: 'management.view.virtual_repository_container' }, toParams: {} },
      });

      expect(newState.toggleVirtualRepositoryManagersCheck).toBe(true);
    });
  });

  describe('removeRepositoryManagerFromOwnerHierarchy', () => {
    it('splices the id from virtualRepositoryManagerIds on the parent container', () => {
      const state = {
        ...initialState,
        ownersMap: {
          REPOSITORY_CONTAINER_ID: {
            id: 'REPOSITORY_CONTAINER_ID',
            type: 'repository_container',
            repositoryManagerIds: ['keep-traditional'],
            virtualRepositoryManagerIds: ['vrm-1', 'vrm-2'],
          },
          'vrm-1': {
            id: 'vrm-1',
            type: 'repository_manager',
            managerType: 'virtual',
            parentId: 'REPOSITORY_CONTAINER_ID',
          },
        },
        topParentOrganizationId: 'ROOT_ORGANIZATION_ID',
      };

      const newState = reducer(state, actions.removeRepositoryManagerFromOwnerHierarchy('vrm-1'));

      expect(newState.ownersMap['vrm-1']).toBeUndefined();
      expect(newState.ownersMap['REPOSITORY_CONTAINER_ID'].virtualRepositoryManagerIds).toEqual(['vrm-2']);
      expect(newState.ownersMap['REPOSITORY_CONTAINER_ID'].repositoryManagerIds).toEqual(['keep-traditional']);
    });

    it('still splices traditional repositoryManagerIds and leaves virtual list intact', () => {
      const state = {
        ...initialState,
        ownersMap: {
          REPOSITORY_CONTAINER_ID: {
            id: 'REPOSITORY_CONTAINER_ID',
            type: 'repository_container',
            repositoryManagerIds: ['rm-1', 'rm-2'],
            virtualRepositoryManagerIds: ['vrm-1'],
          },
          'rm-1': {
            id: 'rm-1',
            type: 'repository_manager',
            parentId: 'REPOSITORY_CONTAINER_ID',
          },
        },
        topParentOrganizationId: 'ROOT_ORGANIZATION_ID',
      };

      const newState = reducer(state, actions.removeRepositoryManagerFromOwnerHierarchy('rm-1'));

      expect(newState.ownersMap['REPOSITORY_CONTAINER_ID'].repositoryManagerIds).toEqual(['rm-2']);
      expect(newState.ownersMap['REPOSITORY_CONTAINER_ID'].virtualRepositoryManagerIds).toEqual(['vrm-1']);
    });
  });
});
