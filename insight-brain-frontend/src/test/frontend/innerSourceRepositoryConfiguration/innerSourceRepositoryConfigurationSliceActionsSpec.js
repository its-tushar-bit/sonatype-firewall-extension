/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { actions } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationSlice';
import { getRepositoryConnectionUrl, getTestRepositoryConnectionUrl } from 'MainRoot/util/CLMLocation';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { omit } from 'ramda';
import {
  getInitialState,
  getPayload,
} from 'TestRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationTestData';
import { STATE_GO } from 'MainRoot/reduxUiRouter/routerActions';

describe('innerSourceRepositoryConfigurationSliceActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;

  beforeEach(function () {
    state = {
      router: {
        currentParams: {
          organizationId: 'someOwnerId',
          repositoryConnectionId: 'someRepositoryConnectionId',
        },
      },
      innerSourceRepositoryConfiguration: getInitialState(),
    };
  });

  function verifySimpleAction(name) {
    store = SpecUtil.mockReduxStore(state);

    store.dispatch(actions[name]());

    expect(store.getActions()).toHaveAction({
      type: `innerSourceRepositoryConfiguration/${name}`,
    });
  }

  describe('setFormat', () => {
    it('immediately dispatches a innerSourceRepositoryConfiguration/setFormat action', () => {
      verifySimpleAction('setFormat');
    });
  });

  describe('setBaseUrl', () => {
    it('immediately dispatches a innerSourceRepositoryConfiguration/setBaseUrl action', () => {
      verifySimpleAction('setBaseUrl');
    });
  });

  describe('setAnonymous', () => {
    it('immediately dispatches a innerSourceRepositoryConfiguration/setAnonymous action', () => {
      verifySimpleAction('setAnonymous');
    });
  });

  describe('setUsername', () => {
    it('immediately dispatches a innerSourceRepositoryConfiguration/setUsername action', () => {
      verifySimpleAction('setUsername');
    });
  });

  describe('setPassword', () => {
    it('immediately dispatches a innerSourceRepositoryConfiguration/setPassword action', () => {
      verifySimpleAction('setPassword');
    });
  });

  describe('cancel', () => {
    it('immediately dispatches a innerSourceRepositoryConfiguration/cancel action', () => {
      verifySimpleAction('cancel');
    });
  });

  describe('submitMaskTimerDone', () => {
    it('immediately dispatches a innerSourceRepositoryConfiguration/submitMaskTimerDone action', () => {
      verifySimpleAction('submitMaskTimerDone');
    });
  });

  describe('setShowDeleteModal', () => {
    it('immediately dispatches a innerSourceRepositoryConfiguration/setShowDeleteModal action', () => {
      verifySimpleAction('setShowDeleteModal');
    });
  });

  describe('loadConfiguration', () => {
    it('immediately dispatches a innerSourceRepositoryConfiguration/loadConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        get: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId', 'someRepositoryConnectionId')]: Promise.resolve(
            {}
          ),
        },
      });

      store.dispatch(actions.loadConfiguration());

      expect(store.getActions()).toHaveAction({
        type: 'innerSourceRepositoryConfiguration/loadConfiguration/pending',
      });
      expect(axios.get).toHaveBeenCalledTimes(1);
      expect(axios.get).toHaveBeenCalledWith(
        '/api/v2/config/repositoryConnection/organization/someOwnerId/someRepositoryConnectionId'
      );
    });

    it('dispatches a innerSourceRepositoryConfiguration/loadConfiguration/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      const payload = getPayload(false);
      delete payload.password;
      mockAxiosCalls({
        get: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId', 'someRepositoryConnectionId')]: Promise.resolve({
            data: payload,
          }),
        },
      });

      store.dispatch(actions.loadConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryConfiguration/loadConfiguration/pending',
          },
          {
            type: 'innerSourceRepositoryConfiguration/loadConfiguration/fulfilled',
            payload,
          },
        ]);
        done();
      });
    });

    it('dispatches a innerSourceRepositoryConfiguration/loadConfiguration/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        get: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId', 'someRepositoryConnectionId')]: () =>
            Promise.reject('someError'),
        },
      });

      store.dispatch(actions.loadConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryConfiguration/loadConfiguration/pending',
          },
          {
            type: 'innerSourceRepositoryConfiguration/loadConfiguration/rejected',
            payload: 'someError',
          },
        ]);
        done();
      });
    });
  });

  describe('saveConfiguration create', () => {
    beforeEach(function () {
      delete state.router.currentParams.repositoryConnectionId;
      state.innerSourceRepositoryConfiguration.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someBaseUrl'
      );
    });

    it('immediately dispatches a innerSourceRepositoryConfiguration/saveConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        post: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId')]: Promise.resolve({}),
        },
      });

      store.dispatch(actions.saveConfiguration());

      expect(store.getActions()).toHaveAction({
        type: 'innerSourceRepositoryConfiguration/saveConfiguration/pending',
      });
      expect(axios.post).toHaveBeenCalledTimes(1);
      expect(axios.post).toHaveBeenCalledWith('/api/v2/config/repositoryConnection/organization/someOwnerId', {
        format: 'generic',
        baseUrl: 'someBaseUrl',
        isAnonymous: true,
      });
    });

    it('dispatches a innerSourceRepositoryConfiguration/saveConfiguration/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      const expectedPostResponsePayload = {
        repositoryConnectionId: 'someRepositoryConnectionId',
        format: 'generic',
        baseUrl: 'someBaseUrl',
        isAnonymous: true,
      };
      mockAxiosCalls({
        post: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId')]: Promise.resolve({
            data: expectedPostResponsePayload,
          }),
        },
      });

      jasmine.clock().install();
      store.dispatch(actions.saveConfiguration()).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jasmine.clock().uninstall();
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryConfiguration/saveConfiguration/pending',
          },
          {
            type: 'innerSourceRepositoryConfiguration/saveConfiguration/fulfilled',
            payload: expectedPostResponsePayload,
          },
          { type: 'innerSourceRepositoryConfiguration/submitMaskTimerDone' },
          {
            type: STATE_GO,
            payload: {
              to: 'repositoryConfiguration.organization.edit',
              params: { ownerId: 'someOwnerId', repositoryConnectionId: 'someRepositoryConnectionId' },
              options: undefined,
            },
          },
        ]);
        done();
      });
    });

    it('dispatches a innerSourceRepositoryConfiguration/saveConfiguration/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        post: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId')]: () => Promise.reject('someError'),
        },
      });

      store.dispatch(actions.saveConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryConfiguration/saveConfiguration/pending',
          },
          {
            type: 'innerSourceRepositoryConfiguration/saveConfiguration/rejected',
            payload: 'someError',
          },
        ]);
        done();
      });
    });
  });

  describe('saveConfiguration update', () => {
    beforeEach(function () {
      state.innerSourceRepositoryConfiguration.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someOtherBaseUrl'
      );
    });

    it('immediately dispatches a innerSourceRepositoryConfiguration/saveConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        put: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId', 'someRepositoryConnectionId')]: Promise.resolve(
            {}
          ),
        },
      });

      store.dispatch(actions.saveConfiguration());

      expect(store.getActions()).toHaveAction({
        type: 'innerSourceRepositoryConfiguration/saveConfiguration/pending',
      });
      expect(axios.put).toHaveBeenCalledTimes(1);
      expect(axios.put).toHaveBeenCalledWith(
        '/api/v2/config/repositoryConnection/organization/someOwnerId/someRepositoryConnectionId',
        {
          format: 'generic',
          baseUrl: 'someOtherBaseUrl',
          isAnonymous: true,
        }
      );
    });

    it('dispatches a innerSourceRepositoryConfiguration/saveConfiguration/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      const expectedPutResponsePayload = {
        repositoryConnectionId: 'someRepositoryConnectionId',
        format: 'generic',
        baseUrl: 'someOtherBaseUrl',
        isAnonymous: true,
      };
      mockAxiosCalls({
        put: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId', 'someRepositoryConnectionId')]: Promise.resolve({
            data: expectedPutResponsePayload,
          }),
        },
      });

      store.dispatch(actions.saveConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryConfiguration/saveConfiguration/pending',
          },
          {
            type: 'innerSourceRepositoryConfiguration/saveConfiguration/fulfilled',
            payload: expectedPutResponsePayload,
          },
        ]);
        done();
      });
    });

    it('dispatches a innerSourceRepositoryConfiguration/saveConfiguration/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        put: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId', 'someRepositoryConnectionId')]: () =>
            Promise.reject('someError'),
        },
      });

      store.dispatch(actions.saveConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryConfiguration/saveConfiguration/pending',
          },
          {
            type: 'innerSourceRepositoryConfiguration/saveConfiguration/rejected',
            payload: 'someError',
          },
        ]);
        done();
      });
    });
  });

  describe('testConfiguration new or changed', () => {
    beforeEach(function () {
      delete state.router.currentParams.repositoryConnectionId;
      state.innerSourceRepositoryConfiguration.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someBaseUrl'
      );
    });

    it('immediately dispatches a innerSourceRepositoryConfiguration/testConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        post: {
          [getTestRepositoryConnectionUrl('organization', 'someOwnerId')]: Promise.resolve({}),
        },
      });

      store.dispatch(actions.testConfiguration());

      expect(store.getActions()).toHaveAction({
        type: 'innerSourceRepositoryConfiguration/testConfiguration/pending',
      });
      expect(axios.post).toHaveBeenCalledTimes(1);
      expect(axios.post).toHaveBeenCalledWith('/api/v2/config/repositoryConnection/organization/someOwnerId/test', {
        format: 'generic',
        baseUrl: 'someBaseUrl',
        isAnonymous: true,
      });
    });

    it('dispatches a innerSourceRepositoryConfiguration/testConfiguration/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        post: {
          [getTestRepositoryConnectionUrl('organization', 'someOwnerId')]: Promise.resolve({ data: {} }),
        },
      });

      store.dispatch(actions.testConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryConfiguration/testConfiguration/pending',
          },
          {
            type: 'innerSourceRepositoryConfiguration/testConfiguration/fulfilled',
            payload: {},
          },
        ]);
        done();
      });
    });

    it('dispatches a innerSourceRepositoryConfiguration/testConfiguration/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        post: {
          [getTestRepositoryConnectionUrl('organization', 'someOwnerId')]: () => Promise.reject('someError'),
        },
      });

      store.dispatch(actions.testConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryConfiguration/testConfiguration/pending',
          },
          {
            type: 'innerSourceRepositoryConfiguration/testConfiguration/rejected',
            payload: 'someError',
          },
        ]);
        done();
      });
    });
  });

  describe('testConfiguration existing', () => {
    beforeEach(function () {
      state.innerSourceRepositoryConfiguration.serverData = getPayload(true);
      state.innerSourceRepositoryConfiguration.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someBaseUrl'
      );
    });

    it('immediately dispatches a innerSourceRepositoryConfiguration/testConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        post: {
          [getTestRepositoryConnectionUrl(
            'organization',
            'someOwnerId',
            'someRepositoryConnectionId'
          )]: Promise.resolve({}),
        },
      });

      store.dispatch(actions.testConfiguration());

      expect(store.getActions()).toHaveAction({
        type: 'innerSourceRepositoryConfiguration/testConfiguration/pending',
      });
      expect(axios.post).toHaveBeenCalledTimes(1);
      expect(axios.post).toHaveBeenCalledWith(
        '/api/v2/config/repositoryConnection/organization/someOwnerId/someRepositoryConnectionId/test',
        null
      );
    });

    it('dispatches a innerSourceRepositoryConfiguration/testConfiguration/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        post: {
          [getTestRepositoryConnectionUrl(
            'organization',
            'someOwnerId',
            'someRepositoryConnectionId'
          )]: Promise.resolve({ data: {} }),
        },
      });
      store.dispatch(actions.testConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryConfiguration/testConfiguration/pending',
          },
          {
            type: 'innerSourceRepositoryConfiguration/testConfiguration/fulfilled',
            payload: {},
          },
        ]);
        done();
      });
    });

    it('dispatches a innerSourceRepositoryConfiguration/testConfiguration/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        post: {
          [getTestRepositoryConnectionUrl('organization', 'someOwnerId', 'someRepositoryConnectionId')]: () =>
            Promise.reject('someError'),
        },
      });

      store.dispatch(actions.testConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryConfiguration/testConfiguration/pending',
          },
          {
            type: 'innerSourceRepositoryConfiguration/testConfiguration/rejected',
            payload: 'someError',
          },
        ]);
        done();
      });
    });
  });

  describe('deleteConfiguration', () => {
    it('immediately dispatches a innerSourceRepositoryConfiguration/deleteConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        del: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId', 'someRepositoryConnectionId')]: Promise.resolve(),
        },
      });

      store.dispatch(actions.deleteConfiguration());

      expect(store.getActions()).toHaveAction({
        type: 'innerSourceRepositoryConfiguration/deleteConfiguration/pending',
      });
      expect(axios.delete).toHaveBeenCalledTimes(1);
      expect(axios.delete).toHaveBeenCalledWith(
        '/api/v2/config/repositoryConnection/organization/someOwnerId/someRepositoryConnectionId'
      );
    });
  });

  it('dispatches a innerSourceRepositoryConfiguration/deleteConfiguration/fulfilled action after successful requests', (done) => {
    store = SpecUtil.mockReduxStore(state);
    mockAxiosCalls({
      del: {
        [getRepositoryConnectionUrl('organization', 'someOwnerId', 'someRepositoryConnectionId')]: Promise.resolve({
          data: {},
        }),
      },
    });

    jasmine.clock().install();
    store.dispatch(actions.deleteConfiguration()).then(() => {
      jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      jasmine.clock().uninstall();

      const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
      expect(actions).toHaveActionsInOrder([
        {
          type: 'innerSourceRepositoryConfiguration/deleteConfiguration/pending',
        },
        {
          type: 'innerSourceRepositoryConfiguration/deleteConfiguration/fulfilled',
          payload: {},
        },
        { type: 'innerSourceRepositoryConfiguration/submitMaskTimerDone' },
        {
          type: STATE_GO,
          payload: {
            to: 'repositoryConfiguration.organization',
            params: { ownerId: 'someOwnerId' },
            options: undefined,
          },
        },
      ]);
      done();
    });
  });

  it('dispatches a innerSourceRepositoryConfiguration/deleteConfiguration/rejected action after failed requests', (done) => {
    store = SpecUtil.mockReduxStore(state);
    mockAxiosCalls({
      del: {
        [getRepositoryConnectionUrl('organization', 'someOwnerId', 'someRepositoryConnectionId')]: () =>
          Promise.reject('someError'),
      },
    });

    store.dispatch(actions.deleteConfiguration()).then(() => {
      const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
      expect(actions).toHaveActionsInOrder([
        {
          type: 'innerSourceRepositoryConfiguration/deleteConfiguration/pending',
        },
        {
          type: 'innerSourceRepositoryConfiguration/deleteConfiguration/rejected',
          payload: 'someError',
        },
      ]);
      done();
    });
  });
});
