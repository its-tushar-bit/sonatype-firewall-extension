/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { actions } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSlice';
import { getArtifactoryConnectionUrl, getTestArtifactoryConnectionUrl } from 'MainRoot/util/CLMLocation';
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { omit } from 'ramda';
import {
  getInitialState,
  getPayload,
} from 'TestRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalTestData';

import 'TestRoot/SpecUtil';

describe('artifactoryRepositoryConfigurationModalSliceActions', () => {
  const TEST_ARTIFACTORY_CONNECTION_ID = 'someArtifactoryConnectionId';
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state, stateWithSetArtifactoryConnectionId;

  beforeEach(function () {
    state = {
      router: {
        currentParams: {
          organizationId: 'someOwnerId',
        },
      },
      artifactoryRepositoryConfigurationModal: getInitialState(),
    };
    stateWithSetArtifactoryConnectionId = {
      ...state,
      artifactoryRepositoryConfigurationModal: {
        ...getInitialState(),
        artifactoryConnectionId: TEST_ARTIFACTORY_CONNECTION_ID,
      },
    };
  });

  function verifySimpleAction(name) {
    store = SpecUtil.mockReduxStore(state);

    store.dispatch(actions[name]());

    expect(store.getActions()).toHaveAction({
      type: `artifactoryRepositoryConfigurationModal/${name}`,
    });
  }

  describe('setBaseUrl', () => {
    it('immediately dispatches a artifactoryRepositoryConfigurationModal/setBaseUrl action', () => {
      verifySimpleAction('setBaseUrl');
    });
  });

  describe('setAnonymous', () => {
    it('immediately dispatches a artifactoryRepositoryConfigurationModal/setAnonymous action', () => {
      verifySimpleAction('setAnonymous');
    });
  });

  describe('setUsername', () => {
    it('immediately dispatches a artifactoryRepositoryConfigurationModal/setUsername action', () => {
      verifySimpleAction('setUsername');
    });
  });

  describe('setPassword', () => {
    it('immediately dispatches a artifactoryRepositoryConfigurationModal/setPassword action', () => {
      verifySimpleAction('setPassword');
    });
  });

  describe('reset', () => {
    it('immediately dispatches a artifactoryRepositoryConfigurationModal/reset action', () => {
      verifySimpleAction('reset');
    });
  });

  describe('resetSubmitMask', () => {
    it('immediately dispatches a artifactoryRepositoryConfigurationModal/resetSubmitMask action', () => {
      verifySimpleAction('resetSubmitMask');
    });
  });

  describe('resetTestConfigurationError', () => {
    it('immediately dispatches a artifactoryRepositoryConfigurationModal/resetTestConfigurationError action', () => {
      verifySimpleAction('resetTestConfigurationError');
    });
  });

  describe('loadConfiguration', () => {
    it('immediately dispatches a artifactoryRepositoryConfigurationModal/loadConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(stateWithSetArtifactoryConnectionId);
      mockAxiosCalls({
        get: {
          [getArtifactoryConnectionUrl('organization', 'someOwnerId', TEST_ARTIFACTORY_CONNECTION_ID)]: Promise.resolve(
            {}
          ),
        },
      });

      store.dispatch(actions.loadConfiguration(TEST_ARTIFACTORY_CONNECTION_ID));

      expect(store.getActions()).toHaveAction({
        type: 'artifactoryRepositoryConfigurationModal/loadConfiguration/pending',
      });
      expect(axios.get).toHaveBeenCalledTimes(1);
      expect(axios.get).toHaveBeenCalledWith(
        '/api/v2/config/artifactoryConnection/organization/someOwnerId/someArtifactoryConnectionId'
      );
    });

    it('dispatches a artifactoryRepositoryConfigurationModal/loadConfiguration/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(stateWithSetArtifactoryConnectionId);
      const payload = getPayload(false);
      delete payload.password;
      mockAxiosCalls({
        get: {
          [getArtifactoryConnectionUrl('organization', 'someOwnerId', TEST_ARTIFACTORY_CONNECTION_ID)]: Promise.resolve(
            {
              data: payload,
            }
          ),
        },
      });

      store.dispatch(actions.loadConfiguration(TEST_ARTIFACTORY_CONNECTION_ID)).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'artifactoryRepositoryConfigurationModal/loadConfiguration/pending',
          },
          {
            type: 'artifactoryRepositoryConfigurationModal/loadConfiguration/fulfilled',
            payload,
          },
        ]);
        done();
      });
    });

    it('dispatches a artifactoryRepositoryConfigurationModal/loadConfiguration/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(stateWithSetArtifactoryConnectionId);
      mockAxiosCalls({
        get: {
          [getArtifactoryConnectionUrl('organization', 'someOwnerId', TEST_ARTIFACTORY_CONNECTION_ID)]: () =>
            Promise.reject('someError'),
        },
      });

      store.dispatch(actions.loadConfiguration(TEST_ARTIFACTORY_CONNECTION_ID)).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'artifactoryRepositoryConfigurationModal/loadConfiguration/pending',
          },
          {
            type: 'artifactoryRepositoryConfigurationModal/loadConfiguration/rejected',
            payload: 'someError',
          },
        ]);
        done();
      });
    });
  });

  describe('saveConfiguration create', () => {
    beforeEach(function () {
      state.artifactoryRepositoryConfigurationModal.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someBaseUrl'
      );
    });

    it('immediately dispatches a artifactoryRepositoryConfigurationModal/saveConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        post: {
          [getArtifactoryConnectionUrl('organization', 'someOwnerId')]: Promise.resolve({}),
        },
      });

      store.dispatch(actions.saveConfiguration());

      expect(store.getActions()).toHaveAction({
        type: 'artifactoryRepositoryConfigurationModal/saveConfiguration/pending',
      });
      expect(axios.post).toHaveBeenCalledTimes(1);
      expect(axios.post).toHaveBeenCalledWith('/api/v2/config/artifactoryConnection/organization/someOwnerId', {
        baseUrl: 'someBaseUrl',
        isAnonymous: true,
      });
    });

    it('dispatches a artifactoryRepositoryConfigurationModal/saveConfiguration/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      const expectedPostResponsePayload = {
        artifactoryConnectionId: TEST_ARTIFACTORY_CONNECTION_ID,
        baseUrl: 'someBaseUrl',
        isAnonymous: true,
      };
      jest.useFakeTimers();

      mockAxiosCalls({
        post: {
          [getArtifactoryConnectionUrl('organization', 'someOwnerId')]: Promise.resolve({
            data: expectedPostResponsePayload,
          }),
        },
      });

      store.dispatch(actions.saveConfiguration()).then(() => {
        let actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions.length).toBe(2);
        expect(actions).toHaveAction({
          type: 'artifactoryRepositoryConfigurationModal/saveConfiguration/pending',
        });
        expect(actions).toHaveAction({
          type: 'artifactoryRepositoryConfigurationModal/saveConfiguration/fulfilled',
        });
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions.length).toBe(5);
        expect(actions).toHaveAction({
          type: 'artifactoryRepositoryConfigurationModal/resetSubmitMask',
        });
        expect(actions).toHaveAction({
          type: 'artifactoryRepositoryConfigurationModal/reset',
        });
        expect(actions).toHaveAction({
          type: 'artifactoryRepositoryBaseConfigurations/load/pending',
        });
        jest.useRealTimers();
        done();
      });
    });

    it('dispatches a artifactoryRepositoryConfigurationModal/saveConfiguration/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        post: {
          [getArtifactoryConnectionUrl('organization', 'someOwnerId')]: () => Promise.reject('someError'),
        },
      });

      store.dispatch(actions.saveConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveAction({
          type: 'artifactoryRepositoryConfigurationModal/saveConfiguration/pending',
        });
        expect(actions).toHaveAction({
          type: 'artifactoryRepositoryConfigurationModal/saveConfiguration/rejected',
          payload: 'someError',
        });
        done();
      });
    });
  });

  describe('saveConfiguration update', () => {
    beforeEach(function () {
      stateWithSetArtifactoryConnectionId.artifactoryRepositoryConfigurationModal.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someOtherBaseUrl'
      );
    });

    it('immediately dispatches a artifactoryRepositoryConfigurationModal/saveConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(stateWithSetArtifactoryConnectionId);

      mockAxiosCalls({
        put: {
          [getArtifactoryConnectionUrl('organization', 'someOwnerId', TEST_ARTIFACTORY_CONNECTION_ID)]: Promise.resolve(
            {}
          ),
        },
      });

      store.dispatch(actions.saveConfiguration());

      expect(store.getActions()).toHaveAction({
        type: 'artifactoryRepositoryConfigurationModal/saveConfiguration/pending',
      });
      expect(axios.put).toHaveBeenCalledTimes(1);
      expect(axios.put).toHaveBeenCalledWith(
        `/api/v2/config/artifactoryConnection/organization/someOwnerId/${TEST_ARTIFACTORY_CONNECTION_ID}`,
        {
          baseUrl: 'someOtherBaseUrl',
          isAnonymous: true,
        }
      );
    });

    it('dispatches a artifactoryRepositoryConfigurationModal/saveConfiguration/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(stateWithSetArtifactoryConnectionId);
      const expectedPutResponsePayload = {
        artifactoryConnectionId: TEST_ARTIFACTORY_CONNECTION_ID,
        baseUrl: 'someOtherBaseUrl',
        isAnonymous: true,
      };
      mockAxiosCalls({
        put: {
          [getArtifactoryConnectionUrl('organization', 'someOwnerId', TEST_ARTIFACTORY_CONNECTION_ID)]: Promise.resolve(
            {
              data: expectedPutResponsePayload,
            }
          ),
        },
      });

      store.dispatch(actions.saveConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveAction({
          type: 'artifactoryRepositoryConfigurationModal/saveConfiguration/pending',
        });
        expect(actions).toHaveAction({
          type: 'artifactoryRepositoryConfigurationModal/saveConfiguration/fulfilled',
        });
        done();
      });
    });

    it('dispatches a artifactoryRepositoryConfigurationModal/saveConfiguration/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(stateWithSetArtifactoryConnectionId);
      mockAxiosCalls({
        put: {
          [getArtifactoryConnectionUrl('organization', 'someOwnerId', TEST_ARTIFACTORY_CONNECTION_ID)]: () =>
            Promise.reject('someError'),
        },
      });

      store.dispatch(actions.saveConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveAction({
          type: 'artifactoryRepositoryConfigurationModal/saveConfiguration/pending',
        });
        expect(actions).toHaveAction({
          type: 'artifactoryRepositoryConfigurationModal/saveConfiguration/rejected',
          payload: 'someError',
        });
        done();
      });
    });
  });

  describe('testConfiguration new or changed', () => {
    beforeEach(function () {
      state.artifactoryRepositoryConfigurationModal.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someBaseUrl'
      );
    });

    it('immediately dispatches a artifactoryRepositoryConfigurationModal/testConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        post: {
          [getTestArtifactoryConnectionUrl('organization', 'someOwnerId')]: Promise.resolve({}),
        },
      });

      store.dispatch(actions.testConfiguration());

      expect(store.getActions()).toHaveAction({
        type: 'artifactoryRepositoryConfigurationModal/testConfiguration/pending',
      });
      expect(axios.post).toHaveBeenCalledTimes(1);
      expect(axios.post).toHaveBeenCalledWith('/api/v2/config/artifactoryConnection/organization/someOwnerId/test', {
        baseUrl: 'someBaseUrl',
        isAnonymous: true,
      });
    });

    it('dispatches a artifactoryRepositoryConfigurationModal/testConfiguration/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        post: {
          [getTestArtifactoryConnectionUrl('organization', 'someOwnerId')]: Promise.resolve({ data: {} }),
        },
      });

      store.dispatch(actions.testConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'artifactoryRepositoryConfigurationModal/testConfiguration/pending',
          },
          {
            type: 'artifactoryRepositoryConfigurationModal/testConfiguration/fulfilled',
            payload: {},
          },
        ]);
        done();
      });
    });

    it('dispatches a artifactoryRepositoryConfigurationModal/testConfiguration/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        post: {
          [getTestArtifactoryConnectionUrl('organization', 'someOwnerId')]: () => Promise.reject('someError'),
        },
      });

      store.dispatch(actions.testConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'artifactoryRepositoryConfigurationModal/testConfiguration/pending',
          },
          {
            type: 'artifactoryRepositoryConfigurationModal/testConfiguration/rejected',
            payload: 'someError',
          },
        ]);
        done();
      });
    });
  });

  describe('testConfiguration existing', () => {
    beforeEach(function () {
      state.artifactoryRepositoryConfigurationModal.serverData = getPayload(true);
      state.artifactoryRepositoryConfigurationModal.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someBaseUrl'
      );
    });

    it('immediately dispatches a artifactoryRepositoryConfigurationModal/testConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(stateWithSetArtifactoryConnectionId);
      mockAxiosCalls({
        post: {
          [getTestArtifactoryConnectionUrl(
            'organization',
            'someOwnerId',
            TEST_ARTIFACTORY_CONNECTION_ID
          )]: Promise.resolve({}),
        },
      });

      store.dispatch(actions.testConfiguration());

      expect(store.getActions()).toHaveAction({
        type: 'artifactoryRepositoryConfigurationModal/testConfiguration/pending',
      });
      expect(axios.post).toHaveBeenCalledTimes(1);
      expect(axios.post).toHaveBeenCalledWith(
        '/api/v2/config/artifactoryConnection/organization/someOwnerId/someArtifactoryConnectionId/test',
        null
      );
    });

    it('dispatches a artifactoryRepositoryConfigurationModal/testConfiguration/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(stateWithSetArtifactoryConnectionId);
      mockAxiosCalls({
        post: {
          [getTestArtifactoryConnectionUrl(
            'organization',
            'someOwnerId',
            TEST_ARTIFACTORY_CONNECTION_ID
          )]: Promise.resolve({ data: {} }),
        },
      });
      store.dispatch(actions.testConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'artifactoryRepositoryConfigurationModal/testConfiguration/pending',
          },
          {
            type: 'artifactoryRepositoryConfigurationModal/testConfiguration/fulfilled',
            payload: {},
          },
        ]);
        done();
      });
    });

    it('dispatches a artifactoryRepositoryConfigurationModal/testConfiguration/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(stateWithSetArtifactoryConnectionId);
      mockAxiosCalls({
        post: {
          [getTestArtifactoryConnectionUrl('organization', 'someOwnerId', TEST_ARTIFACTORY_CONNECTION_ID)]: () =>
            Promise.reject('someError'),
        },
      });

      store.dispatch(actions.testConfiguration()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'artifactoryRepositoryConfigurationModal/testConfiguration/pending',
          },
          {
            type: 'artifactoryRepositoryConfigurationModal/testConfiguration/rejected',
            payload: 'someError',
          },
        ]);
        done();
      });
    });
  });
});
