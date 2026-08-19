/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { actions, initialState } from 'MainRoot/configuration/crowd/atlassianCrowdConfigurationSlice';
import { getCrowdConfigurationTestUrl, getCrowdConfigurationUrl } from 'MainRoot/util/CLMLocation';
import { omit } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import 'TestRoot/SpecUtil';

describe('atlassianCrowdConfigurationSliceAction', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;
  const crowdConfigurationUrl = getCrowdConfigurationUrl();
  const crowdConfigurationTestUrl = getCrowdConfigurationTestUrl();
  const formState = {
    serverUrl: { value: '' },
    applicationName: { value: '' },
    applicationPassword: { value: '' },
  };

  beforeEach(() => {
    state = initialState;
    jest.useFakeTimers();
  });

  afterEach(() => jest.useRealTimers());

  function verifySimpleAction(name, payload) {
    store = SpecUtil.mockReduxStore(state);

    store.dispatch(actions[name](), payload);

    expect(store.getActions()).toHaveAction({
      type: `atlassianCrowdConfiguration/${name}`,
    });
  }

  describe('load', () => {
    beforeEach(() => {
      store = SpecUtil.mockReduxStore(initialState);
    });

    it('dispatches atlassianCrowdConfiguration/load/pending', (done) => {
      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: 'atlassianCrowdConfiguration/load/pending',
        });
        done();
      });
    });

    it('dispatches atlassianCrowdConfiguration/load/rejected on loading error', (done) => {
      const errorMessage = 'error on load';
      mockAxiosCalls({
        get: {
          [crowdConfigurationUrl]: () => Promise.reject(errorMessage),
        },
      });

      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('atlassianCrowdConfiguration/load/pending');
        expect(actions[1].type).toBe('atlassianCrowdConfiguration/load/rejected');
        expect(actions[1].payload).toEqual(errorMessage);
        done();
      });
    });

    it('dispatches atlassianCrowdConfiguration/load/fulfilled', (done) => {
      const crowdConfiguration = { serverUrl: 'http://localhost:8080', applicationName: 'Sonatype' };
      mockAxiosCalls({
        get: {
          [crowdConfigurationUrl]: () => Promise.resolve({ data: crowdConfiguration }),
        },
      });

      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions[0].type).toBe('atlassianCrowdConfiguration/load/pending');
        expect(actions[1]).toEqual({
          type: 'atlassianCrowdConfiguration/load/fulfilled',
          payload: crowdConfiguration,
        });
        done();
      });
    });
  });

  describe('update', () => {
    let store;

    beforeEach(() => {
      store = SpecUtil.mockReduxStore({ atlassianCrowdConfiguration: { formState } });
    });

    it('dispatches atlassianCrowdConfiguration/update/pending', (done) => {
      store.dispatch(actions.update()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: 'atlassianCrowdConfiguration/update/pending',
        });
        done();
      });
    });

    it('dispatches atlassianCrowdConfiguration/update/rejected', (done) => {
      const errorMessage = 'error on update';
      mockAxiosCalls({
        put: {
          [crowdConfigurationUrl]: () => Promise.reject(errorMessage),
        },
      });

      store.dispatch(actions.update()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: 'atlassianCrowdConfiguration/update/pending',
        });
        expect(actions).toHaveAction({
          type: 'atlassianCrowdConfiguration/update/rejected',
          payload: errorMessage,
        });
        done();
      });
    });

    it('dispatches atlassianCrowdConfiguration/update/fulfilled', (done) => {
      mockAxiosCalls({
        put: {
          [crowdConfigurationUrl]: () => Promise.resolve(),
        },
      });

      store.dispatch(actions.update()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error', 'payload'], action));
        expect(actions).toHaveAction({
          type: 'atlassianCrowdConfiguration/update/pending',
        });
        expect(actions).toHaveAction({
          type: 'atlassianCrowdConfiguration/update/fulfilled',
        });
        done();
      });
    });
  });

  describe('delete', () => {
    beforeEach(() => {
      store = SpecUtil.mockReduxStore({ atlassianCrowdConfiguration: {} });
    });

    it('dispatches atlassianCrowdConfiguration/delete/pending', (done) => {
      store.dispatch(actions.del()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: 'atlassianCrowdConfiguration/delete/pending',
        });
        done();
      });
    });

    it('dispatches atlassianCrowdConfiguration/delete/rejected', (done) => {
      const errorMessage = 'error on delete';
      mockAxiosCalls({
        del: {
          [crowdConfigurationUrl]: () => Promise.reject(errorMessage),
        },
      });

      store.dispatch(actions.del()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: 'atlassianCrowdConfiguration/delete/pending',
        });
        expect(actions).toHaveAction({
          type: 'atlassianCrowdConfiguration/delete/rejected',
          payload: errorMessage,
        });
        done();
      });
    });

    it('dispatches atlassianCrowdConfiguration/delete/fulfilled', (done) => {
      mockAxiosCalls({
        del: {
          [crowdConfigurationUrl]: () => Promise.resolve(),
        },
      });

      store
        .dispatch(actions.del())
        .then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          const actions = store.getActions().map((action) => omit(['meta', 'error', 'payload'], action));
          expect(actions).toHaveAction({
            type: 'atlassianCrowdConfiguration/delete/pending',
          });
          expect(actions).toHaveAction({
            type: 'atlassianCrowdConfiguration/delete/fulfilled',
          });
          expect(actions).toHaveAction({
            type: 'atlassianCrowdConfiguration/deleteMaskTimerDone',
          });
        })
        .then(() => {
          const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
          expect(actions).toHaveAction({
            type: 'atlassianCrowdConfiguration/setShowModal',
            payload: false,
          });
          done();
        });
    });
  });

  describe('test', () => {
    let store;

    beforeEach(() => {
      store = SpecUtil.mockReduxStore({ atlassianCrowdConfiguration: { formState: formState } });
    });

    it('dispatches atlassianCrowdConfiguration/test/pending', (done) => {
      store.dispatch(actions.test()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: 'atlassianCrowdConfiguration/test/pending',
        });
        done();
      });
    });

    it('dispatches atlassianCrowdConfiguration/test/rejected', (done) => {
      const errorMessage = 'test connection failed';
      mockAxiosCalls({
        post: {
          [crowdConfigurationTestUrl]: () => Promise.reject(errorMessage),
        },
      });

      store.dispatch(actions.test()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: 'atlassianCrowdConfiguration/test/pending',
        });
        expect(actions).toHaveAction({
          type: 'atlassianCrowdConfiguration/test/rejected',
          payload: errorMessage,
        });
        done();
      });
    });

    it('dispatches atlassianCrowdConfiguration/test/fulfilled', (done) => {
      const payload = { data: { code: 200, message: 'success' } };
      mockAxiosCalls({
        post: {
          [crowdConfigurationTestUrl]: () => Promise.resolve(payload),
        },
      });

      store.dispatch(actions.test()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error', 'payload'], action));
        expect(actions).toHaveAction({
          type: 'atlassianCrowdConfiguration/test/pending',
        });
        expect(actions).toHaveAction({
          type: 'atlassianCrowdConfiguration/test/fulfilled',
        });
        done();
      });
    });
  });

  describe('setInputValue', () => {
    it('dispatches atlassianCrowdConfiguration/setInputValueServerUrl', () => {
      const payload = 'http://localhost:8080/crowd';
      verifySimpleAction('setInputValueServerUrl', payload);
    });

    it('dispatches atlassianCrowdConfiguration/setInputValueApplicationName', () => {
      const payload = 'sonatype';
      verifySimpleAction('setInputValueApplicationName', payload);
    });

    it('dispatches atlassianCrowdConfiguration/setInputValueApplicationPassword', () => {
      const payload = 'admin321';
      verifySimpleAction('setInputValueApplicationPassword', payload);
    });
  });

  describe('resetTestAlertMessages', () => {
    it('dispatches atlassianCrowdConfiguration/resetTestAlertMessages', () => {
      verifySimpleAction('resetTestAlertMessages', null);
    });
  });

  describe('resetForm', () => {
    it('dispatches atlassianCrowdConfiguration/resetForm', () => {
      verifySimpleAction('resetForm', null);
    });
  });

  describe('submitMaskTimerDone', () => {
    it('dispatches atlassianCrowdConfiguration/submitMaskTimerDone', () => {
      verifySimpleAction('submitMaskTimerDone', null);
    });
  });

  describe('deleteMaskTimerDone', () => {
    it('dispatches atlassianCrowdConfiguration/deleteMaskTimerDone', () => {
      verifySimpleAction('deleteMaskTimerDone', null);
    });
  });

  describe('setShowModal', () => {
    it('dispatches atlassianCrowdConfiguration/setShowModal', () => {
      verifySimpleAction('setShowModal', null);
    });
  });
});
