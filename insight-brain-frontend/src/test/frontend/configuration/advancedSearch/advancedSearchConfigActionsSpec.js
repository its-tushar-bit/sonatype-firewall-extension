/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import {
  pollState,
  reIndex,
  save
} from '../../../../main/frontend/configuration/advancedSearch/advancedSearchConfigActions';
import { getAdvancedSearchConfigUrl, getAdvancedSearchIndexUrl } from '../../../../main/frontend/util/CLMLocation';

describe('advancedSearchConfigActions', function() {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
      advancedSearchConfigUrl = getAdvancedSearchConfigUrl(),
      advancedSearchIndexUrl = getAdvancedSearchIndexUrl();

  describe('save', function() {
    let store, state;

    beforeEach(function() {
      state = {
        advancedSearchConfig: {
          formState: {
            lastIndexTime: '42'
          }
        }
      };

      store = SpecUtil.mockReduxStore(state);
    });

    afterEach(function() {
      expect(axios.put).toHaveBeenCalledWith(advancedSearchConfigUrl, state.advancedSearchConfig.formState);
    });

    it('immediately dispatches a ADVANCED_SEARCH_CONFIG_SAVE_REQUESTED action', function() {
      mockAxiosCalls({
        put: {
          [advancedSearchConfigUrl]: Promise.resolve({})
        }
      });

      store.dispatch(save());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('ADVANCED_SEARCH_CONFIG_SAVE_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after successful PUT call', function() {

      beforeEach(function() {
        mockAxiosCalls({
          put: {
            [advancedSearchConfigUrl]: Promise.resolve({})
          }
        });
      });

      it('dispatches ADVANCED_SEARCH_CONFIG_SAVE_FULFILLED', function(done) {

        store.dispatch(save())
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(2);
              expect(actions[1].type).toBe('ADVANCED_SEARCH_CONFIG_SAVE_FULFILLED');
              expect(actions[1].payload).toBeUndefined();
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });

      it('dispatches ADVANCED_SEARCH_CONFIG_SAVE_SUBMIT_MASK_TIMER_DONE after timeout', function(done) {

        store.dispatch(save())
            .then(() => {

              setTimeout(function() {
                actions = store.getActions();
                expect(actions.length).toBe(3);
                expect(actions[2].type).toBe('ADVANCED_SEARCH_CONFIG_SAVE_SUBMIT_MASK_TIMER_DONE');

                done();
              }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
            });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after failed PUT call', function() {

      beforeEach(function() {
        mockAxiosCalls({
          put: {
            [advancedSearchConfigUrl]: Promise.reject('error!')
          }
        });
      });

      it('dispatches ADVANCED_SEARCH_CONFIG_SAVE_FAILED action', function(done) {
        store.dispatch(save())
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(2);
              expect(actions[1].type).toBe('ADVANCED_SEARCH_CONFIG_SAVE_FAILED');
              expect(actions[1].payload).toBe('error!');
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('reIndex', function() {
    let store, state;

    beforeEach(function() {
      state = {
        advancedSearchConfig: {
          formState: {
            lastIndexTime: '42'
          }
        }
      };

      store = SpecUtil.mockReduxStore(state);
    });

    afterEach(function() {
      expect(axios.post).toHaveBeenCalledWith(advancedSearchIndexUrl, {});
    });

    describe('after a successful POST call', function() {
      it('dispatches an ADVANCED_SEARCH_TRIGGER_RE_INDEX action', function(done) {
        mockAxiosCalls({
          post: {
            [advancedSearchIndexUrl]: Promise.resolve({})
          }
        });

        store.dispatch(reIndex())
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(1);
              expect(actions[0].type).toBe('ADVANCED_SEARCH_TRIGGER_RE_INDEX');
              expect(actions[0].payload).toBeUndefined();
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(0);
      });
    });

    describe('after a failed POST call', function() {
      it('dispatches an ADVANCED_SEARCH_RE_INDEX_FAILED action', function(done) {
        mockAxiosCalls({
          post: {
            [advancedSearchIndexUrl]: Promise.reject('error!')
          }
        });

        store.dispatch(reIndex())
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(1);
              expect(actions[0].type).toBe('ADVANCED_SEARCH_RE_INDEX_FAILED');
              expect(actions[0].payload).toBe('error!');
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(0);
      });
    });
  });

  describe('pollState', function() {
    let store, state;

    beforeEach(function() {
      state = {
        advancedSearchConfig: {
          formState: {
            lastIndexTime: '42'
          }
        }
      };

      store = SpecUtil.mockReduxStore(state);
    });

    afterEach(function() {
      expect(axios.get).toHaveBeenCalledWith(advancedSearchConfigUrl);
    });

    describe('after a successful GET call', function() {
      it('dispatches an ADVANCED_SEARCH_POLL_STATE_SUCCESS action', function(done) {
        mockAxiosCalls({
          get: {
            [advancedSearchConfigUrl]: Promise.resolve({ data: {} })
          }
        });

        store.dispatch(pollState())
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(1);
              expect(actions[0].type).toBe('ADVANCED_SEARCH_POLL_STATE_SUCCESS');
              expect(actions[0].payload).toEqual({});
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(0);
      });
    });

    describe('after a failed GET call', function() {
      it('dispatches an ADVANCED_SEARCH_POLL_STATE_FAILED action', function(done) {
        mockAxiosCalls({
          get: {
            [advancedSearchConfigUrl]: Promise.reject('error!')
          }
        });

        store.dispatch(pollState())
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(1);
              expect(actions[0].type).toBe('ADVANCED_SEARCH_POLL_STATE_FAILED');
              expect(actions[0].payload).toBe('error!');
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(0);
      });
    });
  });
});
