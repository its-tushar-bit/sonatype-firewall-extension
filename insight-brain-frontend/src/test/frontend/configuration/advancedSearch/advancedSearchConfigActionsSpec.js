/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { save } from '../../../../main/frontend/configuration/advancedSearch/advancedSearchConfigActions';
import { getAdvancedSearchConfigUrl } from '../../../../main/frontend/util/CLMLocation';

describe('advancedSearchConfigActions', function() {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
      advancedSearchConfigUrl = getAdvancedSearchConfigUrl();

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
});
