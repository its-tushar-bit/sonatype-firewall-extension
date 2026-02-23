/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  actions,
  CONFIG_PROPERTIES_PARAMS,
  initialState,
} from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSlice';
import { omit } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { getConfigurationUrl } from 'MainRoot/util/CLMLocation';

import 'TestRoot/SpecUtil';

describe('baseUrlConfigurationSliceAction', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;
  const configurationUrl = getConfigurationUrl();
  const formState = {
    baseUrl: { value: '' },
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
      type: `baseUrlConfiguration/${name}`,
    });
  }

  describe('load', () => {
    beforeEach(() => {
      store = SpecUtil.mockReduxStore(initialState);
    });

    it('dispatches baseUrlConfiguration/load/pending', (done) => {
      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: 'baseUrlConfiguration/load/pending',
        });
        done();
      });
    });

    it('dispatches baseUrlConfiguration/load/rejected on loading error', (done) => {
      const errorMessage = 'error on load';
      mockAxiosCalls({
        get: {
          [configurationUrl.concat(CONFIG_PROPERTIES_PARAMS)]: () => Promise.reject(errorMessage),
        },
      });

      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('baseUrlConfiguration/load/pending');
        expect(actions[1].type).toBe('baseUrlConfiguration/load/rejected');
        expect(actions[1].payload).toEqual(errorMessage);
        done();
      });
    });

    it('dispatches baseUrlConfiguration/load/fulfilled', (done) => {
      const baseUrlConfiguration = { baseUrl: 'http://localhost:8080' };
      mockAxiosCalls({
        get: {
          [configurationUrl.concat(CONFIG_PROPERTIES_PARAMS)]: () => Promise.resolve({ data: baseUrlConfiguration }),
        },
      });

      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions[0].type).toBe('baseUrlConfiguration/load/pending');
        expect(actions[1]).toEqual({
          type: 'baseUrlConfiguration/load/fulfilled',
          payload: baseUrlConfiguration,
        });
        done();
      });
    });
  });

  describe('update', () => {
    let store;

    beforeEach(() => {
      store = SpecUtil.mockReduxStore({ baseUrlConfiguration: { formState } });
    });

    it('dispatches baseUrlConfiguration/update/pending', (done) => {
      store.dispatch(actions.update()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: 'baseUrlConfiguration/update/pending',
        });
        done();
      });
    });

    it('dispatches baseUrlConfiguration/update/rejected', (done) => {
      const errorMessage = 'error on update';
      mockAxiosCalls({
        put: {
          [configurationUrl]: () => Promise.reject(errorMessage),
        },
      });

      store.dispatch(actions.update()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: 'baseUrlConfiguration/update/pending',
        });
        expect(actions).toHaveAction({
          type: 'baseUrlConfiguration/update/rejected',
          payload: errorMessage,
        });
        done();
      });
    });

    it('dispatches baseUrlConfiguration/update/fulfilled', (done) => {
      mockAxiosCalls({
        put: {
          [configurationUrl]: () => Promise.resolve(),
        },
      });

      store.dispatch(actions.update()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        const actions = store.getActions().map((action) => omit(['meta', 'error', 'payload'], action));
        expect(actions).toHaveAction({
          type: 'baseUrlConfiguration/update/pending',
        });
        expect(actions).toHaveAction({
          type: 'baseUrlConfiguration/update/fulfilled',
        });
        done();
      });
    });
  });

  describe('delete', () => {
    beforeEach(() => {
      store = SpecUtil.mockReduxStore({ baseUrlConfiguration: {} });
    });

    it('dispatches baseUrlConfiguration/delete/pending', (done) => {
      store.dispatch(actions.del()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: 'baseUrlConfiguration/delete/pending',
        });
        done();
      });
    });

    it('dispatches baseUrlConfiguration/delete/rejected', (done) => {
      const errorMessage = 'error on delete';
      mockAxiosCalls({
        del: {
          [configurationUrl.concat(CONFIG_PROPERTIES_PARAMS)]: () => Promise.reject(errorMessage),
        },
      });

      store.dispatch(actions.del()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: 'baseUrlConfiguration/delete/pending',
        });
        expect(actions).toHaveAction({
          type: 'baseUrlConfiguration/delete/rejected',
          payload: errorMessage,
        });
        done();
      });
    });

    it('dispatches baseUrlConfiguration/delete/fulfilled', (done) => {
      mockAxiosCalls({
        del: {
          [configurationUrl.concat(CONFIG_PROPERTIES_PARAMS)]: () => Promise.resolve(),
        },
      });

      store
        .dispatch(actions.del())
        .then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          const actions = store.getActions().map((action) => omit(['meta', 'error', 'payload'], action));
          expect(actions).toHaveAction({
            type: 'baseUrlConfiguration/delete/pending',
          });
          expect(actions).toHaveAction({
            type: 'baseUrlConfiguration/delete/fulfilled',
          });
        })
        .then(() => {
          const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
          expect(actions).toHaveAction({
            type: 'baseUrlConfiguration/setShowDeleteModal',
            payload: false,
          });
          done();
        });
    });
  });

  describe('setInputValue', () => {
    it('dispatches baseUrlConfiguration/setInputValueBaseUrl', () => {
      const payload = 'http://localhost:8080/';
      verifySimpleAction('setInputValueBaseUrl', payload);
    });
  });

  describe('resetForm', () => {
    it('dispatches baseUrlConfiguration/resetForm', () => {
      verifySimpleAction('resetForm', null);
    });
  });

  describe('submitMaskTimerDone', () => {
    it('dispatches baseUrlConfiguration/submitMaskTimerDone', () => {
      verifySimpleAction('submitMaskTimerDone', null);
    });
  });

  describe('deleteMaskTimerDone', () => {
    it('dispatches baseUrlConfiguration/deleteMaskTimerDone', () => {
      verifySimpleAction('deleteMaskTimerDone', null);
    });
  });

  describe('setShowDeleteModal', () => {
    it('dispatches baseUrlConfiguration/setShowDeleteModal', () => {
      verifySimpleAction('setShowDeleteModal', null);
    });
  });
});
