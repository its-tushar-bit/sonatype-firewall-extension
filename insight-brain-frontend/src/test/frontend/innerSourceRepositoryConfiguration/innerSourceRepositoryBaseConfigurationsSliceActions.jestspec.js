/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { actions } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSlice';
import { getRepositoryConnectionUrl } from 'MainRoot/util/CLMLocation';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { omit } from 'ramda';
import { getInitialState } from 'TestRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsTestData';

import 'TestRoot/SpecUtil';

describe('innerSourceRepositoryBaseConfigurationsSliceActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;

  beforeEach(function () {
    state = {
      router: {
        currentParams: {
          organizationId: 'someOwnerId',
        },
      },
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            id: 'someOwnerId',
          },
        },
      },
      innerSourceRepositoryBaseConfigurations: getInitialState(),
    };
  });

  function verifySimpleAction(name) {
    store = SpecUtil.mockReduxStore(state);

    store.dispatch(actions[name]());

    expect(store.getActions()).toHaveAction({
      type: `innerSourceRepositoryBaseConfigurations/${name}`,
    });
  }

  describe('setEnabled', () => {
    it('immediately dispatches a innerSourceRepositoryBaseConfigurations/setEnabled action', () => {
      verifySimpleAction('setEnabled');
    });
  });

  describe('setAllowOverride', () => {
    it('immediately dispatches a innerSourceRepositoryBaseConfigurations/setAllowOverride action', () => {
      verifySimpleAction('setAllowOverride');
    });
  });

  describe('cancel', () => {
    it('immediately dispatches a innerSourceRepositoryBaseConfigurations/cancel action', () => {
      verifySimpleAction('cancel');
    });
  });

  describe('submitMaskTimerDone', () => {
    it('immediately dispatches a innerSourceRepositoryBaseConfigurations/submitMaskTimerDone action', () => {
      verifySimpleAction('submitMaskTimerDone');
    });
  });

  describe('load', () => {
    it('immediately dispatches a innerSourceRepositoryBaseConfigurations/load/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        get: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId', null)]: Promise.resolve({}),
        },
      });

      store.dispatch(actions.load());

      expect(store.getActions()).toHaveAction({
        type: 'innerSourceRepositoryBaseConfigurations/load/pending',
      });
      expect(axios.get).toHaveBeenCalledTimes(1);
      expect(axios.get).toHaveBeenCalledWith('/api/v2/config/repositoryConnection/organization/someOwnerId');
    });

    it('dispatches a innerSourceRepositoryBaseConfigurations/load/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      const payload = 'payload';
      mockAxiosCalls({
        get: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId', null)]: Promise.resolve({
            data: payload,
          }),
        },
      });

      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryBaseConfigurations/load/pending',
          },
          {
            type: 'innerSourceRepositoryBaseConfigurations/load/fulfilled',
            payload,
          },
        ]);
        done();
      });
    });

    it('dispatches a innerSourceRepositoryBaseConfigurations/load/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        get: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId', null)]: () => Promise.reject('someError'),
        },
      });

      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryBaseConfigurations/load/pending',
          },
          {
            type: 'innerSourceRepositoryBaseConfigurations/load/rejected',
            payload: 'someError',
          },
        ]);
        done();
      });
    });
  });

  describe('save', () => {
    it('immediately dispatches a innerSourceRepositoryBaseConfigurations/save/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        put: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId')]: Promise.resolve({}),
        },
      });

      store.dispatch(actions.save());
      expect(store.getActions()).toHaveAction({
        type: 'innerSourceRepositoryBaseConfigurations/save/pending',
      });
      expect(axios.put).toHaveBeenCalledTimes(1);
      expect(axios.put).toHaveBeenCalledWith('/api/v2/config/repositoryConnection/organization/someOwnerId', {
        enabled: null,
        allowOverride: true,
      });
    });

    it('dispatches a innerSourceRepositoryBaseConfigurations/save/fulfilled action after successful requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        put: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId')]: Promise.resolve({}),
        },
      });

      jest.useFakeTimers();
      store.dispatch(actions.save()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jest.useRealTimers();
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryBaseConfigurations/save/pending',
          },
          {
            type: 'innerSourceRepositoryBaseConfigurations/save/fulfilled',
          },
          {
            type: 'innerSourceRepositoryBaseConfigurations/submitMaskTimerDone',
          },
          {
            type: 'innerSourceRepositoryBaseConfigurations/load/pending',
          },
        ]);
        done();
      });
    });

    it('dispatches a innerSourceRepositoryBaseConfigurations/save/rejected action after failed requests', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        put: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId')]: () => Promise.reject('someError'),
        },
      });

      store.dispatch(actions.save()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'innerSourceRepositoryBaseConfigurations/save/pending',
          },
          {
            type: 'innerSourceRepositoryBaseConfigurations/save/rejected',
            payload: 'someError',
          },
        ]);
        done();
      });
    });
  });
});
