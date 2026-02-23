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
  save,
  load,
} from '../../../../main/frontend/configuration/advancedSearch/advancedSearchConfigActions';
import { getAdvancedSearchConfigUrl, getAdvancedSearchIndexUrl } from '../../../../main/frontend/util/CLMLocation';

import 'TestRoot/SpecUtil';

describe('advancedSearchConfigActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
    advancedSearchConfigUrl = getAdvancedSearchConfigUrl(),
    advancedSearchIndexUrl = getAdvancedSearchIndexUrl();

  describe('save', function () {
    let store, state;

    beforeEach(function () {
      state = {
        advancedSearchConfig: {
          formState: {
            lastIndexTime: '42',
          },
        },
      };

      store = SpecUtil.mockReduxStore(state);
    });

    afterEach(function () {
      expect(axios.put).toHaveBeenCalledWith(advancedSearchConfigUrl, state.advancedSearchConfig.formState);
    });

    it('immediately dispatches a ADVANCED_SEARCH_CONFIG_SAVE_REQUESTED action', function () {
      mockAxiosCalls({
        put: {
          [advancedSearchConfigUrl]: Promise.resolve({}),
        },
      });

      store.dispatch(save());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('ADVANCED_SEARCH_CONFIG_SAVE_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after successful PUT call', function () {
      beforeEach(function () {
        mockAxiosCalls({
          put: {
            [advancedSearchConfigUrl]: Promise.resolve({}),
          },
        });
      });

      it('dispatches ADVANCED_SEARCH_CONFIG_SAVE_FULFILLED', function (done) {
        store.dispatch(save()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('ADVANCED_SEARCH_CONFIG_SAVE_FULFILLED');
          expect(actions[1].payload).toBeUndefined();
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });

      it('dispatches ADVANCED_SEARCH_CONFIG_SAVE_SUBMIT_MASK_TIMER_DONE after timeout', function (done) {
        jest.useFakeTimers();

        store.dispatch(save()).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          jest.useRealTimers();

          actions = store.getActions();
          expect(actions.length).toBe(3);
          expect(actions[2].type).toBe('ADVANCED_SEARCH_CONFIG_SAVE_SUBMIT_MASK_TIMER_DONE');

          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after failed PUT call', function () {
      beforeEach(function () {
        mockAxiosCalls({
          put: {
            [advancedSearchConfigUrl]: () => Promise.reject('error!'),
          },
        });
      });

      it('dispatches ADVANCED_SEARCH_CONFIG_SAVE_FAILED action', function (done) {
        store.dispatch(save()).then(() => {
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

  describe('reIndex', function () {
    let store, state;

    beforeEach(function () {
      state = {
        advancedSearchConfig: {
          formState: {
            lastIndexTime: '42',
          },
        },
      };

      store = SpecUtil.mockReduxStore(state);
    });

    afterEach(function () {
      expect(axios.post).toHaveBeenCalledWith(advancedSearchIndexUrl, {});
    });

    describe('after a successful POST call', function () {
      it(
        'dispatches an ADVANCED_SEARCH_TRIGGER_RE_INDEX action, an ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING action, ' +
          'and schedules a call to pollState',
        function (done) {
          mockAxiosCalls({
            post: {
              [advancedSearchIndexUrl]: Promise.resolve({}),
            },
          });

          jest.spyOn(window, 'setTimeout');

          store.dispatch(reIndex()).then(() => {
            actions = store.getActions();
            expect(actions.length).toBe(2);
            expect(actions[0].type).toBe('ADVANCED_SEARCH_TRIGGER_RE_INDEX');
            expect(actions[0].payload).toBeUndefined();
            expect(actions[1].type).toBe('ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING');
            expect(actions[1].payload).toBeTruthy();
            expect(window.setTimeout).toHaveBeenCalled();
            const setTimeoutArgs = window.setTimeout.mock.calls[window.setTimeout.mock.calls.length - 1];
            expect(typeof setTimeoutArgs[0]).toBe('function');
            expect(setTimeoutArgs[1]).toBe(2000);
            state.router = {
              currentState: {
                name: 'notAdvancedSearchConfig',
              },
            };
            setTimeoutArgs[0]();
            expect(actions.length).toBe(3);
            expect(actions[2].type).toBe('ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING');
            expect(actions[2].payload).toBeFalsy();
            done();
          });

          let actions = store.getActions();
          expect(actions.length).toBe(0);
        }
      );
    });

    describe('after a failed POST call', function () {
      it('dispatches an ADVANCED_SEARCH_RE_INDEX_FAILED action', function (done) {
        mockAxiosCalls({
          post: {
            [advancedSearchIndexUrl]: () => Promise.reject('error!'),
          },
        });

        store.dispatch(reIndex()).then(() => {
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

  describe('pollState', function () {
    let store, state;

    beforeEach(function () {
      state = {
        advancedSearchConfig: {
          formState: {
            lastIndexTime: '42',
          },
        },
        router: {
          currentState: {
            name: 'advancedSearchConfig',
          },
        },
      };

      store = SpecUtil.mockReduxStore(state);
    });

    describe('immediately after being called', function () {
      it('dispatches ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING with false if on a different page', function (done) {
        mockAxiosCalls({
          get: {
            [advancedSearchConfigUrl]: Promise.resolve({
              data: { isFullIndexTriggered: true },
            }),
          },
        });
        state.router.currentState.name = 'notAdvancedSearchConfig';
        store.dispatch(pollState(store.dispatch, store.getState)).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(1);
          expect(actions[0].type).toBe('ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING');
          expect(actions[0].payload).toBeFalsy();
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(axios.get).not.toHaveBeenCalledWith(advancedSearchConfigUrl);
      });
    });

    describe('after a REST call', function () {
      afterEach(function () {
        expect(axios.get).toHaveBeenCalledWith(advancedSearchConfigUrl);
      });

      describe('after a successful GET call', function () {
        it('dispatches ADVANCED_SEARCH_POLL_STATE_SUCCESS and ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING actions', function (done) {
          mockAxiosCalls({
            get: {
              [advancedSearchConfigUrl]: Promise.resolve({
                data: { isFullIndexTriggered: false },
              }),
            },
          });

          store.dispatch(pollState(store.dispatch, store.getState)).then(() => {
            actions = store.getActions();
            expect(actions.length).toBe(2);
            expect(actions[0].type).toBe('ADVANCED_SEARCH_POLL_STATE_SUCCESS');
            expect(actions[0].payload).toEqual({ isFullIndexTriggered: false });
            expect(actions[1].type).toBe('ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING');
            expect(actions[1].payload).toBeFalsy();
            done();
          });

          let actions = store.getActions();
          expect(actions.length).toBe(0);
        });

        it(
          'dispatches ADVANCED_SEARCH_POLL_STATE_SUCCESS and ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING actions and ' +
            'schedules a call to pollState if a full index is happening and we are still on the page',
          function (done) {
            mockAxiosCalls({
              get: {
                [advancedSearchConfigUrl]: Promise.resolve({
                  data: { isFullIndexTriggered: true },
                }),
              },
            });

            jest.spyOn(window, 'setTimeout');

            store.dispatch(pollState(store.dispatch, store.getState)).then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(2);
              expect(actions[0].type).toBe('ADVANCED_SEARCH_POLL_STATE_SUCCESS');
              expect(actions[0].payload).toEqual({
                isFullIndexTriggered: true,
              });
              expect(actions[1].type).toBe('ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING');
              expect(actions[1].payload).toBeTruthy();
              expect(window.setTimeout).toHaveBeenCalled();
              const setTimeoutArgs = window.setTimeout.mock.calls[window.setTimeout.mock.calls.length - 1];
              expect(typeof setTimeoutArgs[0]).toBe('function');
              expect(setTimeoutArgs[1]).toBe(2000);
              state.router = {
                currentState: {
                  name: 'notAdvancedSearchConfig',
                },
              };
              setTimeoutArgs[0]();
              expect(actions.length).toBe(3);
              expect(actions[2].type).toBe('ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING');
              expect(actions[2].payload).toBeFalsy();
              done();
            });

            let actions = store.getActions();
            expect(actions.length).toBe(0);
          }
        );
      });

      describe('after a failed GET call', function () {
        it('dispatches an ADVANCED_SEARCH_POLL_STATE_FAILED action', function (done) {
          mockAxiosCalls({
            get: {
              [advancedSearchConfigUrl]: () => Promise.reject('error!'),
            },
          });

          store.dispatch(pollState(store.dispatch, store.getState)).then(() => {
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

  describe('load', function () {
    let store, state;

    beforeEach(function () {
      state = {
        advancedSearchConfig: {
          formState: {
            lastIndexTime: '42',
          },
          currentlyPolling: false,
        },
      };

      store = SpecUtil.mockReduxStore(state);
    });

    afterEach(function () {
      expect(axios.get).toHaveBeenCalledWith(advancedSearchConfigUrl);
    });

    it('immediately dispatches a ADVANCED_SEARCH_CONFIG_LOAD_REQUESTED action', function () {
      mockAxiosCalls({
        get: {
          [advancedSearchConfigUrl]: Promise.resolve({
            data: { isFullIndexTriggered: false },
          }),
        },
      });

      store.dispatch(load());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('ADVANCED_SEARCH_CONFIG_LOAD_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function () {
      it('dispatches ADVANCED_SEARCH_CONFIG_LOAD_FULFILLED and ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING actions', function (done) {
        mockAxiosCalls({
          get: {
            [advancedSearchConfigUrl]: Promise.resolve({
              data: { isFullIndexTriggered: false },
            }),
          },
        });

        store.dispatch(load()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(3);
          expect(actions[0].type).toBe('ADVANCED_SEARCH_CONFIG_LOAD_REQUESTED');
          expect(actions[0].payload).toBeUndefined();
          expect(actions[1].type).toBe('ADVANCED_SEARCH_CONFIG_LOAD_FULFILLED');
          expect(actions[1].payload).toEqual({ isFullIndexTriggered: false });
          expect(actions[2].type).toBe('ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING');
          expect(actions[2].payload).toBeFalsy();
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });

      it(
        'dispatches ADVANCED_SEARCH_CONFIG_LOAD_FULFILLED and ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING actions and ' +
          'schedules a call to pollState if a full index is happening and we are not already polling',
        function (done) {
          mockAxiosCalls({
            get: {
              [advancedSearchConfigUrl]: Promise.resolve({
                data: { isFullIndexTriggered: true },
              }),
            },
          });

          jest.spyOn(window, 'setTimeout');

          store.dispatch(load()).then(() => {
            actions = store.getActions();
            expect(actions.length).toBe(3);
            expect(actions[0].type).toBe('ADVANCED_SEARCH_CONFIG_LOAD_REQUESTED');
            expect(actions[0].payload).toBeUndefined();
            expect(actions[1].type).toBe('ADVANCED_SEARCH_CONFIG_LOAD_FULFILLED');
            expect(actions[1].payload).toEqual({ isFullIndexTriggered: true });
            expect(actions[2].type).toBe('ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING');
            expect(actions[2].payload).toBeTruthy();
            expect(window.setTimeout).toHaveBeenCalled();
            const setTimeoutArgs = window.setTimeout.mock.calls[window.setTimeout.mock.calls.length - 1];
            expect(typeof setTimeoutArgs[0]).toBe('function');
            expect(setTimeoutArgs[1]).toBe(2000);
            state.router = {
              currentState: {
                name: 'notAdvancedSearchConfig',
              },
            };
            setTimeoutArgs[0]();
            expect(actions.length).toBe(4);
            expect(actions[3].type).toBe('ADVANCED_SEARCH_UPDATE_CURRENTLY_POLLING');
            expect(actions[3].payload).toBeFalsy();
            done();
          });

          let actions = store.getActions();
          expect(actions.length).toBe(1);
        }
      );
    });

    describe('after a failed GET call', function () {
      it('dispatches an ADVANCED_SEARCH_CONFIG_LOAD_FAILED action', function (done) {
        mockAxiosCalls({
          get: {
            [advancedSearchConfigUrl]: () => Promise.reject('error!'),
          },
        });

        store.dispatch(load()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('ADVANCED_SEARCH_CONFIG_LOAD_FAILED');
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });
});
