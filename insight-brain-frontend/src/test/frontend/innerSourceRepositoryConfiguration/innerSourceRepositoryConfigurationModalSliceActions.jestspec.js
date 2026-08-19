/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { actions } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalSlice';
import { getRepositoryConnectionUrl, getTestRepositoryConnectionUrl } from 'MainRoot/util/CLMLocation';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { omit } from 'ramda';
import {
  getInitialState,
  getPayload,
} from 'TestRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalTestData';

import 'TestRoot/SpecUtil';

describe('innerSourceRepositoryConfigurationModalSliceActions', () => {
  const TEST_REPOSITORY_CONNECTION_ID = 'someRepositoryConnectionId';
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state, stateWithSetRepositoryConnectionId;

  beforeEach(function () {
    state = {
      router: {
        currentParams: {
          organizationId: 'someOwnerId',
        },
      },
      innerSourceRepositoryConfigurationModal: getInitialState(),
    };
    stateWithSetRepositoryConnectionId = {
      ...state,
      innerSourceRepositoryConfigurationModal: {
        ...getInitialState(),
        repositoryConnectionId: TEST_REPOSITORY_CONNECTION_ID,
      },
    };
  });

  function verifySimpleAction(name) {
    store = SpecUtil.mockReduxStore(state);

    store.dispatch(actions[name]());

    expect(store.getActions()).toHaveAction({
      type: `innerSourceRepositoryConfigurationModal/${name}`,
    });
  }

  describe('setFormat', () => {
    it('immediately dispatches a innerSourceRepositoryConfigurationModal/setFormat action', () => {
      verifySimpleAction('setFormat');
    });
  });

  describe('setBaseUrl', () => {
    it('immediately dispatches a innerSourceRepositoryConfigurationModal/setBaseUrl action', () => {
      verifySimpleAction('setBaseUrl');
    });
  });

  describe('setAnonymous', () => {
    it('immediately dispatches a innerSourceRepositoryConfigurationModal/setAnonymous action', () => {
      verifySimpleAction('setAnonymous');
    });
  });

  describe('setUsername', () => {
    it('immediately dispatches a innerSourceRepositoryConfigurationModal/setUsername action', () => {
      verifySimpleAction('setUsername');
    });
  });

  describe('setPassword', () => {
    it('immediately dispatches a innerSourceRepositoryConfigurationModal/setPassword action', () => {
      verifySimpleAction('setPassword');
    });
  });

  describe('reset', () => {
    it('immediately dispatches a innerSourceRepositoryConfigurationModal/reset action', () => {
      verifySimpleAction('reset');
    });
  });

  describe('resetSubmitMask', () => {
    it('immediately dispatches a innerSourceRepositoryConfigurationModal/resetSubmitMask action', () => {
      verifySimpleAction('resetSubmitMask');
    });
  });

  describe('resetTestConfigurationError', () => {
    it('immediately dispatches a innerSourceRepositoryConfigurationModal/resetTestConfigurationError action', () => {
      verifySimpleAction('resetTestConfigurationError');
    });
  });

  describe('loadConfiguration', () => {
    it('immediately dispatches a innerSourceRepositoryConfigurationModal/loadConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(stateWithSetRepositoryConnectionId);
      mockAxiosCalls({
        get: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId', TEST_REPOSITORY_CONNECTION_ID)]: Promise.resolve(
            {}
          ),
        },
      });

      store.dispatch(actions.loadConfiguration(TEST_REPOSITORY_CONNECTION_ID));

      expect(store.getActions()).toHaveAction({
        type: 'innerSourceRepositoryConfigurationModal/loadConfiguration/pending',
      });
      expect(axios.get).toHaveBeenCalledTimes(1);
      expect(axios.get).toHaveBeenCalledWith(
        '/api/v2/config/repositoryConnection/organization/someOwnerId/someRepositoryConnectionId'
      );
    });

    it('dispatches a innerSourceRepositoryConfigurationModal/loadConfiguration/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(stateWithSetRepositoryConnectionId);
      const payload = getPayload(false);
      delete payload.password;
      mockAxiosCalls({
        get: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId', TEST_REPOSITORY_CONNECTION_ID)]: Promise.resolve({
            data: payload,
          }),
        },
      });

      store.dispatch(actions.loadConfiguration(TEST_REPOSITORY_CONNECTION_ID)).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryConfigurationModal/loadConfiguration/pending',
          },
          {
            type: 'innerSourceRepositoryConfigurationModal/loadConfiguration/fulfilled',
            payload,
          },
        ]);
        done();
      });
    });

    it('dispatches a innerSourceRepositoryConfigurationModal/loadConfiguration/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(stateWithSetRepositoryConnectionId);
      mockAxiosCalls({
        get: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId', TEST_REPOSITORY_CONNECTION_ID)]: () =>
            Promise.reject('someError'),
        },
      });

      store.dispatch(actions.loadConfiguration(TEST_REPOSITORY_CONNECTION_ID)).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryConfigurationModal/loadConfiguration/pending',
          },
          {
            type: 'innerSourceRepositoryConfigurationModal/loadConfiguration/rejected',
            payload: 'someError',
          },
        ]);
        done();
      });
    });
  });

  describe('saveConfiguration create', () => {
    beforeEach(function () {
      state.innerSourceRepositoryConfigurationModal.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someBaseUrl'
      );
    });

    it('immediately dispatches a innerSourceRepositoryConfigurationModal/saveConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        post: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId')]: Promise.resolve({}),
        },
      });

      store.dispatch(actions.saveConfiguration());

      expect(store.getActions()).toHaveAction({
        type: 'innerSourceRepositoryConfigurationModal/saveConfiguration/pending',
      });
      expect(axios.post).toHaveBeenCalledTimes(1);
      expect(axios.post).toHaveBeenCalledWith('/api/v2/config/repositoryConnection/organization/someOwnerId', {
        format: 'generic',
        baseUrl: 'someBaseUrl',
        isAnonymous: true,
      });
    });

    it('dispatches a innerSourceRepositoryConfigurationModal/saveConfiguration/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      const expectedPostResponsePayload = {
        repositoryConnectionId: TEST_REPOSITORY_CONNECTION_ID,
        format: 'generic',
        baseUrl: 'someBaseUrl',
        isAnonymous: true,
      };
      jest.useFakeTimers();

      mockAxiosCalls({
        post: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId')]: Promise.resolve({
            data: expectedPostResponsePayload,
          }),
        },
      });

      store.dispatch(actions.saveConfiguration()).then(() => {
        let actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions.length).toBe(2);
        expect(actions).toHaveAction({
          type: 'innerSourceRepositoryConfigurationModal/saveConfiguration/pending',
        });
        expect(actions).toHaveAction({
          type: 'innerSourceRepositoryConfigurationModal/saveConfiguration/fulfilled',
        });
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions.length).toBe(5);
        expect(actions).toHaveAction({
          type: 'innerSourceRepositoryConfigurationModal/resetSubmitMask',
        });
        expect(actions).toHaveAction({
          type: 'innerSourceRepositoryConfigurationModal/reset',
        });
        expect(actions).toHaveAction({
          type: 'innerSourceRepositoryBaseConfigurations/load/pending',
        });
        jest.useRealTimers();
        done();
      });
    });

    it('dispatches a innerSourceRepositoryConfigurationModal/saveConfiguration/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        post: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId')]: () => Promise.reject('someError'),
        },
      });

      store.dispatch(actions.saveConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveAction({
          type: 'innerSourceRepositoryConfigurationModal/saveConfiguration/pending',
        });
        expect(actions).toHaveAction({
          type: 'innerSourceRepositoryConfigurationModal/saveConfiguration/rejected',
          payload: 'someError',
        });
        done();
      });
    });
  });

  describe('saveConfiguration update', () => {
    beforeEach(function () {
      stateWithSetRepositoryConnectionId.innerSourceRepositoryConfigurationModal.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someOtherBaseUrl'
      );
    });

    it('immediately dispatches a innerSourceRepositoryConfigurationModal/saveConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(stateWithSetRepositoryConnectionId);

      mockAxiosCalls({
        put: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId', TEST_REPOSITORY_CONNECTION_ID)]: Promise.resolve(
            {}
          ),
        },
      });

      store.dispatch(actions.saveConfiguration());

      expect(store.getActions()).toHaveAction({
        type: 'innerSourceRepositoryConfigurationModal/saveConfiguration/pending',
      });
      expect(axios.put).toHaveBeenCalledTimes(1);
      expect(axios.put).toHaveBeenCalledWith(
        `/api/v2/config/repositoryConnection/organization/someOwnerId/${TEST_REPOSITORY_CONNECTION_ID}`,
        {
          format: 'generic',
          baseUrl: 'someOtherBaseUrl',
          isAnonymous: true,
        }
      );
    });

    it('dispatches a innerSourceRepositoryConfigurationModal/saveConfiguration/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(stateWithSetRepositoryConnectionId);
      const expectedPutResponsePayload = {
        repositoryConnectionId: TEST_REPOSITORY_CONNECTION_ID,
        format: 'generic',
        baseUrl: 'someOtherBaseUrl',
        isAnonymous: true,
      };
      mockAxiosCalls({
        put: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId', TEST_REPOSITORY_CONNECTION_ID)]: Promise.resolve({
            data: expectedPutResponsePayload,
          }),
        },
      });

      store.dispatch(actions.saveConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveAction({
          type: 'innerSourceRepositoryConfigurationModal/saveConfiguration/pending',
        });
        expect(actions).toHaveAction({
          type: 'innerSourceRepositoryConfigurationModal/saveConfiguration/fulfilled',
        });
        done();
      });
    });

    it('dispatches a innerSourceRepositoryConfigurationModal/saveConfiguration/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(stateWithSetRepositoryConnectionId);
      mockAxiosCalls({
        put: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId', TEST_REPOSITORY_CONNECTION_ID)]: () =>
            Promise.reject('someError'),
        },
      });

      store.dispatch(actions.saveConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveAction({
          type: 'innerSourceRepositoryConfigurationModal/saveConfiguration/pending',
        });
        expect(actions).toHaveAction({
          type: 'innerSourceRepositoryConfigurationModal/saveConfiguration/rejected',
          payload: 'someError',
        });
        done();
      });
    });
  });

  describe('testConfiguration new or changed', () => {
    beforeEach(function () {
      state.innerSourceRepositoryConfigurationModal.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someBaseUrl'
      );
    });

    it('immediately dispatches a innerSourceRepositoryConfigurationModal/testConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        post: {
          [getTestRepositoryConnectionUrl('organization', 'someOwnerId')]: Promise.resolve({}),
        },
      });

      store.dispatch(actions.testConfiguration());

      expect(store.getActions()).toHaveAction({
        type: 'innerSourceRepositoryConfigurationModal/testConfiguration/pending',
      });
      expect(axios.post).toHaveBeenCalledTimes(1);
      expect(axios.post).toHaveBeenCalledWith('/api/v2/config/repositoryConnection/organization/someOwnerId/test', {
        format: 'generic',
        baseUrl: 'someBaseUrl',
        isAnonymous: true,
      });
    });

    it('dispatches a innerSourceRepositoryConfigurationModal/testConfiguration/fulfilled action after successful requests', (done) => {
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
            type: 'innerSourceRepositoryConfigurationModal/testConfiguration/pending',
          },
          {
            type: 'innerSourceRepositoryConfigurationModal/testConfiguration/fulfilled',
            payload: {},
          },
        ]);
        done();
      });
    });

    it('dispatches a innerSourceRepositoryConfigurationModal/testConfiguration/rejected action after failed requests', (done) => {
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
            type: 'innerSourceRepositoryConfigurationModal/testConfiguration/pending',
          },
          {
            type: 'innerSourceRepositoryConfigurationModal/testConfiguration/rejected',
            payload: 'someError',
          },
        ]);
        done();
      });
    });
  });

  describe('testConfiguration existing', () => {
    beforeEach(function () {
      state.innerSourceRepositoryConfigurationModal.serverData = getPayload(true);
      state.innerSourceRepositoryConfigurationModal.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someBaseUrl'
      );
    });

    it('immediately dispatches a innerSourceRepositoryConfigurationModal/testConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(stateWithSetRepositoryConnectionId);
      mockAxiosCalls({
        post: {
          [getTestRepositoryConnectionUrl(
            'organization',
            'someOwnerId',
            TEST_REPOSITORY_CONNECTION_ID
          )]: Promise.resolve({}),
        },
      });

      store.dispatch(actions.testConfiguration());

      expect(store.getActions()).toHaveAction({
        type: 'innerSourceRepositoryConfigurationModal/testConfiguration/pending',
      });
      expect(axios.post).toHaveBeenCalledTimes(1);
      expect(axios.post).toHaveBeenCalledWith(
        '/api/v2/config/repositoryConnection/organization/someOwnerId/someRepositoryConnectionId/test',
        null
      );
    });

    it('dispatches a innerSourceRepositoryConfigurationModal/testConfiguration/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(stateWithSetRepositoryConnectionId);
      mockAxiosCalls({
        post: {
          [getTestRepositoryConnectionUrl(
            'organization',
            'someOwnerId',
            TEST_REPOSITORY_CONNECTION_ID
          )]: Promise.resolve({ data: {} }),
        },
      });
      store.dispatch(actions.testConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryConfigurationModal/testConfiguration/pending',
          },
          {
            type: 'innerSourceRepositoryConfigurationModal/testConfiguration/fulfilled',
            payload: {},
          },
        ]);
        done();
      });
    });

    it('dispatches a innerSourceRepositoryConfigurationModal/testConfiguration/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(stateWithSetRepositoryConnectionId);
      mockAxiosCalls({
        post: {
          [getTestRepositoryConnectionUrl('organization', 'someOwnerId', TEST_REPOSITORY_CONNECTION_ID)]: () =>
            Promise.reject('someError'),
        },
      });

      store.dispatch(actions.testConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryConfigurationModal/testConfiguration/pending',
          },
          {
            type: 'innerSourceRepositoryConfigurationModal/testConfiguration/rejected',
            payload: 'someError',
          },
        ]);
        done();
      });
    });
  });
});
