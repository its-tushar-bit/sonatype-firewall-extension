/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { actions } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsSlice';
import { getArtifactoryConnectionUrl } from 'MainRoot/util/CLMLocation';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { omit } from 'ramda';
import { getInitialState } from 'TestRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsTestData';

import 'TestRoot/SpecUtil';

describe('artifactoryRepositoryBaseConfigurationsSliceActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;

  beforeEach(function () {
    state = {
      router: {
        currentParams: {
          organizationId: 'someOwnerId',
        },
      },
      artifactoryRepositoryBaseConfigurations: getInitialState(),
    };
  });

  function verifySimpleAction(name) {
    store = SpecUtil.mockReduxStore(state);

    store.dispatch(actions[name]());

    expect(store.getActions()).toHaveAction({
      type: `artifactoryRepositoryBaseConfigurations/${name}`,
    });
  }

  describe('setEnabled', () => {
    it('immediately dispatches a artifactoryRepositoryBaseConfigurations/setEnabled action', () => {
      verifySimpleAction('setEnabled');
    });
  });

  describe('setAllowOverride', () => {
    it('immediately dispatches a artifactoryRepositoryBaseConfigurations/setAllowOverride action', () => {
      verifySimpleAction('setAllowOverride');
    });
  });

  describe('cancel', () => {
    it('immediately dispatches a artifactoryRepositoryBaseConfigurations/cancel action', () => {
      verifySimpleAction('cancel');
    });
  });

  describe('submitMaskTimerDone', () => {
    it('immediately dispatches a artifactoryRepositoryBaseConfigurations/submitMaskTimerDone action', () => {
      verifySimpleAction('submitMaskTimerDone');
    });
  });

  describe('load', () => {
    it('immediately dispatches a artifactoryRepositoryBaseConfigurations/load/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        get: {
          [getArtifactoryConnectionUrl('organization', 'someOwnerId', null)]: Promise.resolve({}),
        },
      });

      store.dispatch(actions.load());

      expect(store.getActions()).toHaveAction({
        type: 'artifactoryRepositoryBaseConfigurations/load/pending',
      });
      expect(axios.get).toHaveBeenCalledTimes(1);
      expect(axios.get).toHaveBeenCalledWith('/api/v2/config/artifactoryConnection/organization/someOwnerId');
    });

    it('dispatches a artifactoryRepositoryBaseConfigurations/load/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      const payload = 'payload';
      mockAxiosCalls({
        get: {
          [getArtifactoryConnectionUrl('organization', 'someOwnerId', null)]: Promise.resolve({
            data: payload,
          }),
        },
      });

      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'artifactoryRepositoryBaseConfigurations/load/pending',
          },
          {
            type: 'artifactoryRepositoryBaseConfigurations/load/fulfilled',
            payload,
          },
        ]);
        done();
      });
    });

    it('dispatches a artifactoryRepositoryBaseConfigurations/load/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        get: {
          [getArtifactoryConnectionUrl('organization', 'someOwnerId', null)]: () => Promise.reject('someError'),
        },
      });

      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'artifactoryRepositoryBaseConfigurations/load/pending',
          },
          {
            type: 'artifactoryRepositoryBaseConfigurations/load/rejected',
            payload: 'someError',
          },
        ]);
        done();
      });
    });
  });

  describe('save', () => {
    it('immediately dispatches a artifactoryRepositoryBaseConfigurations/save/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        put: {
          [getArtifactoryConnectionUrl('organization', 'someOwnerId')]: Promise.resolve({}),
        },
      });

      store.dispatch(actions.save());
      expect(store.getActions()).toHaveAction({
        type: 'artifactoryRepositoryBaseConfigurations/save/pending',
      });
      expect(axios.put).toHaveBeenCalledTimes(1);
      expect(axios.put).toHaveBeenCalledWith('/api/v2/config/artifactoryConnection/organization/someOwnerId', {
        enabled: null,
        allowOverride: true,
      });
    });

    it('dispatches a artifactoryRepositoryBaseConfigurations/save/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        put: {
          [getArtifactoryConnectionUrl('organization', 'someOwnerId')]: Promise.resolve({}),
        },
      });

      jest.useFakeTimers();
      store.dispatch(actions.save()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'artifactoryRepositoryBaseConfigurations/save/pending',
          },
          {
            type: 'artifactoryRepositoryBaseConfigurations/save/fulfilled',
          },
          {
            type: 'artifactoryRepositoryBaseConfigurations/submitMaskTimerDone',
          },
          {
            type: 'artifactoryRepositoryBaseConfigurations/load/pending',
          },
        ]);
        done();
      });
    });

    it('dispatches a artifactoryRepositoryBaseConfigurations/save/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        put: {
          [getArtifactoryConnectionUrl('organization', 'someOwnerId')]: () => Promise.reject('someError'),
        },
      });

      store.dispatch(actions.save()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'artifactoryRepositoryBaseConfigurations/save/pending',
          },
          {
            type: 'artifactoryRepositoryBaseConfigurations/save/rejected',
            payload: 'someError',
          },
        ]);
        done();
      });
    });
  });
});
