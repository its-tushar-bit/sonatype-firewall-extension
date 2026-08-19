/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getWaiveTransitiveViolationsUrl } from '../../../main/frontend/util/CLMLocation';
import { actions } from '../../../main/frontend/violation/waiveTransitiveViolationsSlice';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { getExpiryTime } from '../../../main/frontend/util/waiverUtils';

import 'TestRoot/SpecUtil';

const { setScope, setExpiration, setComments, cancel, save } = actions;

describe('waiveTransitiveViolationsActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  let store, state;

  beforeEach(function () {
    state = {
      router: {
        currentParams: {
          scanId: 'someScanId',
          hash: 'someHash',
        },
      },
      waiveTransitiveViolations: {
        scope: 'someScope',
        expiration: 'never',
        comments: '',
        submitMaskState: null,
        saveError: null,
      },
    };
    store = SpecUtil.mockReduxStore(state);
  });

  describe('setScope', function () {
    it('dispatches waiveTransitiveViolationsReducer/setScope', function () {
      store.dispatch(setScope('someOtherScope'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('waiveTransitiveViolationsReducer/setScope');
      expect(actions[0].payload).toBe('someOtherScope');
    });
  });

  describe('setExpiration', function () {
    it('dispatches waiveTransitiveViolationsReducer/setExpiration', function () {
      store.dispatch(setExpiration('someOtherExpiration'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('waiveTransitiveViolationsReducer/setExpiration');
      expect(actions[0].payload).toBe('someOtherExpiration');
    });
  });

  describe('setComments', function () {
    it('dispatches waiveTransitiveViolationsReducer/setComments', function () {
      store.dispatch(setComments('someComments'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('waiveTransitiveViolationsReducer/setComments');
      expect(actions[0].payload).toBe('someComments');
    });
  });

  describe('cancel', function () {
    it('dispatches waiveTransitiveViolationsReducer/cancel', function () {
      store.dispatch(cancel());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('waiveTransitiveViolationsReducer/cancel');
    });
  });

  describe('save', function () {
    it('immediately dispatches a waiveTransitiveViolationsReducer/save/pending action', function () {
      mockAxiosCalls({
        post: {
          [getWaiveTransitiveViolationsUrl('someScope', 'someScanId', 'someHash')]: Promise.resolve({}),
        },
      });

      store.dispatch(save());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('waiveTransitiveViolationsReducer/save/pending');
      expect(actions[0].payload).toBeUndefined();
      expect(axios.post).toHaveBeenCalledWith(getWaiveTransitiveViolationsUrl('someScope', 'someScanId', 'someHash'), {
        expiryTime: null,
        comment: null,
        applyToAllComponents: false,
      });
    });

    it('sets an expiry time if expiration is not never', function () {
      mockAxiosCalls({
        post: {
          [getWaiveTransitiveViolationsUrl('someScope', 'someScanId', 'someHash')]: Promise.resolve({}),
        },
      });
      store = SpecUtil.mockReduxStore({
        ...state,
        waiveTransitiveViolations: { ...state.waiveTransitiveViolations, expiration: '7' },
      });
      store.dispatch(save());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('waiveTransitiveViolationsReducer/save/pending');
      expect(actions[0].payload).toBeUndefined();
      expect(axios.post).toHaveBeenCalledWith(getWaiveTransitiveViolationsUrl('someScope', 'someScanId', 'someHash'), {
        expiryTime: getExpiryTime('7'),
        comment: null,
        applyToAllComponents: false,
      });
    });

    it('sets a comment if comments is not empty', function () {
      mockAxiosCalls({
        post: {
          [getWaiveTransitiveViolationsUrl('someScope', 'someScanId', 'someHash')]: Promise.resolve({}),
        },
      });
      store = SpecUtil.mockReduxStore({
        ...state,
        waiveTransitiveViolations: { ...state.waiveTransitiveViolations, comments: 'someComments' },
      });
      store.dispatch(save());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('waiveTransitiveViolationsReducer/save/pending');
      expect(actions[0].payload).toBeUndefined();
      expect(axios.post).toHaveBeenCalledWith(getWaiveTransitiveViolationsUrl('someScope', 'someScanId', 'someHash'), {
        expiryTime: null,
        comment: 'someComments',
        applyToAllComponents: false,
      });
    });

    describe('after successful POST call', function () {
      beforeEach(function () {
        mockAxiosCalls({
          post: {
            [getWaiveTransitiveViolationsUrl('someScope', 'someScanId', 'someHash')]: Promise.resolve({}),
          },
        });
      });

      it('dispatches waiveTransitiveViolationsReducer/save/fulfilled', function (done) {
        store.dispatch(save()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('waiveTransitiveViolationsReducer/save/fulfilled');
          expect(actions[1].payload).toBeUndefined();
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });

      it('dispatches waiveTransitiveViolationsReducer/submitMaskTimerDone and TRANSITIVE_VIOLATIONS_TOGGLE_WAIVE after timeout', function (done) {
        jest.useFakeTimers();

        store.dispatch(save()).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          jest.useRealTimers();

          actions = store.getActions();
          expect(actions.length).toBe(4);
          expect(actions[2].type).toBe('waiveTransitiveViolationsReducer/submitMaskTimerDone');
          expect(actions[3].type).toBe('TRANSITIVE_VIOLATIONS_TOGGLE_WAIVE');

          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after failed POST call', function () {
      beforeEach(function () {
        mockAxiosCalls({
          post: {
            [getWaiveTransitiveViolationsUrl('someScope', 'someScanId', 'someHash')]: () => Promise.reject('error!'),
          },
        });
      });

      it('dispatches waiveTransitiveViolationsReducer/save/rejected action', function (done) {
        store.dispatch(save()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('waiveTransitiveViolationsReducer/save/rejected');
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });
});
