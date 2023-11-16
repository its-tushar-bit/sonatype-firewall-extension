/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getRepositoryManagerUrl } from 'MainRoot/util/CLMLocation';
import { actions } from 'MainRoot/OrgsAndPolicies/repositories/repositoriesConfigurationSlice';
import { omit } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

describe('repositoriesConfigurationSliceActions', () => {
  let axiosMock, store, state;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    const repos = [
      {
        managerInstanceId: 'someManagerInstanceId',
        repository: {
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
        },
      },
    };
  });

  describe('editRepositoryManagerName', () => {
    it('immediately dispatches a repositories/editRepositoryManagerName/pending action and an appropriate request', () => {
      store = SpecUtil.mockReduxStore(state);
      axiosMock.onPut(getRepositoryManagerUrl('someManagerId', 'someManagerName')).reply(200, {});

      store.dispatch(actions.editRepositoryManagerName());

      expect(store.getActions()).toHaveAction({
        type: 'repositories/editRepositoryManagerName/pending',
      });
      expect(axiosMock.history.put.length).toBe(1);
      expect(axiosMock.history.put[0].url).toBe('/rest/repositories/repositoryManager/someManagerId/someManagerName');
    });

    it('dispatches a repositories/editRepositoryManagerName/fulfilled action after a successful request', (done) => {
      store = SpecUtil.mockReduxStore(state);
      axiosMock.onPut(getRepositoryManagerUrl('someManagerId', 'someManagerName')).reply(200, {});
      jasmine.clock().install();

      store.dispatch(actions.editRepositoryManagerName()).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jasmine.clock().uninstall();
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
        })
      );

      expect(store.getActions()).toHaveAction({
        type: 'repositories/openEditRepositoryManagerNameModal',
        payload: { managerInstanceId: 'someManagerInstanceId', managerName: 'someManagerName' },
      });
    });
  });
});
