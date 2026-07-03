/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getRepositoryManagerUrl, getRepositoryInfoUrl } from 'MainRoot/util/CLMLocation';
import { actions } from 'MainRoot/OrgsAndPolicies/repositories/repositoriesConfigurationSlice';
import { omit } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

describe('repositoriesConfigurationSliceActions', () => {
  let axiosMock, store, state;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    const repos = [
      {
        managerInstanceId: 'someManagerInstanceId',
        repository: {
          id: 'repositoryId',
          repositoryManagerId: 'someManagerId',
        },
      },
    ];
    state = {
      repositories: {
        originalRepositories: repos,
        repositories: repos,
        loading: false,
        editRepositoryManagerNameError: null,
        showEditRepositoryManagerNameModal: false,
        submitMaskState: null,
        editRepositoryManagerNameModalInfo: {
          managerInstanceId: 'someManagerInstanceId',
          managerName: 'someManagerName',
          repoManagerId: 'repoManagerId',
        },
      },
    };
  });

  describe('editRepositoryManagerName', () => {
    it('immediately dispatches a repositories/editRepositoryManagerName/pending action and an appropriate request', () => {
      store = SpecUtil.mockReduxStore(state);
      axiosMock.onPut(getRepositoryManagerUrl('repoManagerId', 'someManagerName')).reply(200, {});

      store.dispatch(actions.editRepositoryManagerName());

      expect(store.getActions()).toHaveAction({
        type: 'repositories/editRepositoryManagerName/pending',
      });
      expect(axiosMock.history.put.length).toBe(1);
      expect(axiosMock.history.put[0].url).toBe('/rest/repositories/repositoryManager/repoManagerId/someManagerName');
    });

    it('dispatches a repositories/editRepositoryManagerName/fulfilled action after a successful request', (done) => {
      store = SpecUtil.mockReduxStore(state);
      axiosMock.onPut(getRepositoryManagerUrl('repoManagerId', 'someManagerName')).reply(200, {});
      jest.useFakeTimers();

      store.dispatch(actions.editRepositoryManagerName()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'repositories/editRepositoryManagerName/pending',
          },
          {
            type: 'repositories/editRepositoryManagerName/fulfilled',
          },
          {
            type: 'repositories/resetSubmitMaskState',
          },
          {
            type: 'repositories/setShowEditRepositoryManagerNameModal',
            payload: false,
          },
          {
            type: 'repositories/loadRepositories/pending',
          },
          {
            type: 'namespaceConfusionProtectionTile/getComponentNamePatterns/pending',
          },
          { type: 'ownerSideNav/load/pending' },
          { type: 'ownerSideNav/loadOwnerList/pending' },
        ]);
        done();
      });
    });

    it('dispatches a repositories/editRepositoryManagerName/rejected action after a failed request', (done) => {
      store = SpecUtil.mockReduxStore(state);
      const payload = 'someError';
      axiosMock.onPut(getRepositoryManagerUrl('someManagerId', 'someManagerName')).reply(500, payload);

      store.dispatch(actions.editRepositoryManagerName()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionTypesInOrder([
          'repositories/editRepositoryManagerName/pending',
          'repositories/editRepositoryManagerName/rejected',
        ]);
        done();
      });
    });
  });

  describe('setShowEditRepositoryManagerNameModal', () => {
    it('immediately dispatches a repositories/setShowEditRepositoryManagerNameModal action', () => {
      store.dispatch(actions.setShowEditRepositoryManagerNameModal(true));

      expect(store.getActions()).toHaveAction({
        type: 'repositories/setShowEditRepositoryManagerNameModal',
        payload: true,
      });
    });
  });

  describe('setRepositoryManagerName', () => {
    it('immediately dispatches a repositories/setRepositoryManagerName action', () => {
      store.dispatch(actions.setRepositoryManagerName('someRepositoryManagerName'));

      expect(store.getActions()).toHaveAction({
        type: 'repositories/setRepositoryManagerName',
        payload: 'someRepositoryManagerName',
      });
    });
  });

  describe('openEditRepositoryManagerNameModal', () => {
    it('immediately dispatches a repositories/openEditRepositoryManagerNameModal action', () => {
      store.dispatch(
        actions.openEditRepositoryManagerNameModal({
          managerInstanceId: 'someManagerInstanceId',
          managerName: 'someManagerName',
          repoManagerId: 'repoManagerId',
        })
      );

      expect(store.getActions()).toHaveAction({
        type: 'repositories/openEditRepositoryManagerNameModal',
        payload: {
          managerInstanceId: 'someManagerInstanceId',
          managerName: 'someManagerName',
          repoManagerId: 'repoManagerId',
        },
      });
    });
  });

  describe('goToRepositorySummaryView', () => {
    it('calls stateGo with the appropriate parameters', () => {
      const mockRouterParams = {
        repositoryId: 'repositoryId',
      };

      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue(mockRouterParams);
      const store = SpecUtil.mockReduxStore({});
      store.dispatch(actions.goToRepositorySummaryView('repositoryId'));

      expect(store.getActions()).toHaveAction({
        type: '@@reduxUiRouter/stateGo',
        payload: {
          to: 'management.view.repository',
          params: { ...mockRouterParams, repositoryId: 'repositoryId' },
          options: undefined,
        },
      });
    });
  });

  describe('deleteRepository', () => {
    beforeEach(() => {
      jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').mockReturnValue({ id: 'selectedOwnerId' });
    });

    it('dispatches appropriate actions when deleting a repository as a repository manager on management view route', (done) => {
      const stateWithDeleteModal = {
        ...state,
        router: {
          currentState: { name: 'management.view.repository' },
        },
        repositories: {
          ...state.repositories,
          deleteModalInfo: {
            id: 'deletedRepositoryId',
          },
        },
      };
      store = SpecUtil.mockReduxStore(stateWithDeleteModal);
      axiosMock.onDelete(getRepositoryInfoUrl('deletedRepositoryId')).reply(200, {});

      jest.spyOn(routerSelectors, 'selectIsRepositoryManager').mockReturnValue(true);
      jest.spyOn(routerSelectors, 'selectIncludesManagementView').mockReturnValue(true);
      jest.useFakeTimers();

      store.dispatch(actions.deleteRepository()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

        const dispatchedActions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(dispatchedActions).toHaveActionsInOrder([
          { type: 'repositories/deleteRepository/pending' },
          { type: 'repositories/deleteRepository/fulfilled' },
          { type: 'repositories/resetSubmitMaskState' },
          { type: 'repositories/setShowDeleteModal', payload: false },
          { type: 'repositories/loadRepositoriesByManagerId/pending' },
          { type: 'ownerSideNav/loadOwnerList/pending' },
        ]);

        // On the management view the forced reload is the source of truth, so the
        // optimistic reducer is not dispatched.
        const actionTypes = dispatchedActions.map((a) => a.type);
        expect(actionTypes).not.toContain('ownerSideNav/removeRepositoryFromOwnerHierarchy');
        done();
      });
    });

    it('dispatches appropriate actions when deleting a repository as a container view', (done) => {
      const stateWithDeleteModal = {
        ...state,
        router: {
          currentState: { name: 'management.view.container' },
        },
        repositories: {
          ...state.repositories,
          deleteModalInfo: {
            id: 'deletedRepositoryId',
          },
        },
      };
      store = SpecUtil.mockReduxStore(stateWithDeleteModal);
      axiosMock.onDelete(getRepositoryInfoUrl('deletedRepositoryId')).reply(200, {});

      jest.spyOn(routerSelectors, 'selectIsRepositoryManager').mockReturnValue(false);
      jest.spyOn(routerSelectors, 'selectIncludesManagementView').mockReturnValue(false);
      jest.useFakeTimers();

      store.dispatch(actions.deleteRepository()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

        const dispatchedActions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(dispatchedActions).toHaveActionsInOrder([
          { type: 'repositories/deleteRepository/pending' },
          { type: 'repositories/deleteRepository/fulfilled' },
          { type: 'repositories/resetSubmitMaskState' },
          { type: 'repositories/setShowDeleteModal', payload: false },
          { type: 'repositories/loadRepositories/pending' },
          { type: 'ownerSideNav/loadOwnerList/pending' },
        ]);

        // In the container view the forced reload is the source of truth, so the
        // optimistic reducer is not dispatched.
        const actionTypes = dispatchedActions.map((a) => a.type);
        expect(actionTypes).not.toContain('ownerSideNav/removeRepositoryFromOwnerHierarchy');
        done();
      });
    });

    it('dispatches appropriate actions when deleting a repository as a repository manager on non-management view route', (done) => {
      const stateWithDeleteModal = {
        ...state,
        router: {
          currentState: { name: 'management.repository' },
        },
        repositories: {
          ...state.repositories,
          deleteModalInfo: {
            id: 'deletedRepositoryId',
          },
        },
      };
      store = SpecUtil.mockReduxStore(stateWithDeleteModal);
      axiosMock.onDelete(getRepositoryInfoUrl('deletedRepositoryId')).reply(200, {});

      jest.spyOn(routerSelectors, 'selectIsRepositoryManager').mockReturnValue(true);
      jest.spyOn(routerSelectors, 'selectIncludesManagementView').mockReturnValue(false);
      jest.useFakeTimers();

      store.dispatch(actions.deleteRepository()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();

        const dispatchedActions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(dispatchedActions).toHaveActionsInOrder([
          { type: 'repositories/deleteRepository/pending' },
          { type: 'repositories/deleteRepository/fulfilled' },
          { type: 'repositories/resetSubmitMaskState' },
          { type: 'repositories/setShowDeleteModal', payload: false },
          { type: 'repositories/loadRepositoriesByManagerId/pending' },
          { type: 'ownerSideNav/removeRepositoryFromOwnerHierarchy', payload: 'deletedRepositoryId' },
        ]);

        const actionTypes = dispatchedActions.map((a) => a.type);
        expect(actionTypes).not.toContain('ownerSideNav/loadOwnerList/pending');
        done();
      });
    });

    it('dispatches a repositories/deleteRepository/rejected action after a failed request', (done) => {
      const stateWithDeleteModal = {
        ...state,
        router: {
          currentState: { name: 'management.view.repository' },
        },
        repositories: {
          ...state.repositories,
          deleteModalInfo: {
            id: 'deletedRepositoryId',
          },
        },
      };
      store = SpecUtil.mockReduxStore(stateWithDeleteModal);
      const payload = 'someError';
      axiosMock.onDelete(getRepositoryInfoUrl('deletedRepositoryId')).reply(500, payload);

      store.dispatch(actions.deleteRepository()).then(() => {
        const dispatchedActions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(dispatchedActions).toHaveActionTypesInOrder([
          'repositories/deleteRepository/pending',
          'repositories/deleteRepository/rejected',
        ]);
        done();
      });
    });
  });
});
