/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  checkUserTokenExistence,
  deleteUserToken,
  generateUserToken,
  hideUserTokenModal,
  showUserTokenModal,
  USER_TOKEN_CHECK_TOKEN_EXISTENCE_FAILED,
  USER_TOKEN_CHECK_TOKEN_EXISTENCE_FULFILLED,
  USER_TOKEN_CHECK_TOKEN_EXISTENCE_REQUESTED,
  USER_TOKEN_DELETE_TOKEN_FAILED,
  USER_TOKEN_DELETE_TOKEN_FULFILLED,
  USER_TOKEN_DELETE_TOKEN_REQUESTED,
  USER_TOKEN_GENERATE_TOKEN_FAILED,
  USER_TOKEN_GENERATE_TOKEN_FULFILLED,
  USER_TOKEN_GENERATE_TOKEN_REQUESTED,
  USER_TOKEN_HIDE_MODAL,
  USER_TOKEN_MASK_TIMER_DONE,
  USER_TOKEN_SHOW_MODAL,
} from '../../../../../../main/frontend/mainHeader/MenuBar/UserMenu/UserToken/userTokenActions';
import { checkUserTokenExistenceUrl, userTokenUrl } from '../../../../../../main/frontend/util/CLMLocation';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import 'TestRoot/SpecUtil';

describe('userTokenActions', function () {
  let store, mockAxiosCalls;

  beforeEach(function () {
    const state = {};
    store = SpecUtil.mockReduxStore(state);
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  });

  describe('showUserTokenModal', function () {
    it('immediately dispatches an USER_TOKEN_SHOW_MODAL action', function () {
      store.dispatch(showUserTokenModal());
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toEqual(USER_TOKEN_SHOW_MODAL);
      expect(store.getActions()[0].payload).toBeUndefined();
    });
  });

  describe('hideUserTokenModal', function () {
    it('immediately dispatches an USER_TOKEN_HIDE_MODAL action', function () {
      store.dispatch(hideUserTokenModal());
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toEqual(USER_TOKEN_HIDE_MODAL);
      expect(store.getActions()[0].payload).toBeUndefined();
    });
  });

  describe('checkUserTokenExistence', function () {
    let checkTokenUrl, expectedUrl;

    beforeEach(function () {
      checkTokenUrl = checkUserTokenExistenceUrl();
      expectedUrl = '/api/v2/userTokens/currentUser/hasToken';
    });

    it('immediately dispatches an USER_TOKEN_CHECK_TOKEN_EXISTENCE_REQUESTED action', function () {
      mockAxiosCalls({
        get: {
          [checkTokenUrl]: Promise.resolve(),
        },
      });
      store.dispatch(checkUserTokenExistence());

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toEqual(USER_TOKEN_CHECK_TOKEN_EXISTENCE_REQUESTED);
      expect(store.getActions()[0].payload).toBeUndefined();
    });

    it('sends a GET request with proper config', function () {
      mockAxiosCalls({
        get: {
          [checkTokenUrl]: Promise.resolve(),
        },
      });

      store.dispatch(checkUserTokenExistence());
      expect(axios.get).toHaveBeenCalledWith(expectedUrl);
    });

    describe('after a successful GET', function () {
      it('dispatches the USER_TOKEN_CHECK_TOKEN_EXISTENCE_FULFILLED action with result data on true', function (done) {
        mockAxiosCalls({
          get: {
            [checkTokenUrl]: Promise.resolve({
              data: { userTokenExists: true },
            }),
          },
        });

        store.dispatch(checkUserTokenExistence()).then(() => {
          expect(axios.get).toHaveBeenCalledWith(expectedUrl);
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1].type).toBe(USER_TOKEN_CHECK_TOKEN_EXISTENCE_FULFILLED);
          expect(store.getActions()[1].payload).toBe(true);
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(USER_TOKEN_CHECK_TOKEN_EXISTENCE_REQUESTED);
      });

      it('dispatches the USER_TOKEN_CHECK_TOKEN_EXISTENCE_FULFILLED action with result data on false', function (done) {
        mockAxiosCalls({
          get: {
            [checkTokenUrl]: Promise.resolve({
              data: { userTokenExists: false },
            }),
          },
        });

        store.dispatch(checkUserTokenExistence()).then(() => {
          expect(axios.get).toHaveBeenCalledWith(expectedUrl);
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1].type).toBe(USER_TOKEN_CHECK_TOKEN_EXISTENCE_FULFILLED);
          expect(store.getActions()[1].payload).toBe(false);
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(USER_TOKEN_CHECK_TOKEN_EXISTENCE_REQUESTED);
      });
    });

    describe('after a failed GET', function () {
      it('dispatches the USER_TOKEN_CHECK_TOKEN_EXISTENCE_FAILED action', function (done) {
        mockAxiosCalls({
          get: {
            [checkTokenUrl]: () => Promise.reject('Err'),
          },
        });

        store.dispatch(checkUserTokenExistence()).then(() => {
          expect(axios.get).toHaveBeenCalledWith(expectedUrl);
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1].type).toBe(USER_TOKEN_CHECK_TOKEN_EXISTENCE_FAILED);
          expect(store.getActions()[1].payload).toBe('Err');
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(USER_TOKEN_CHECK_TOKEN_EXISTENCE_REQUESTED);
      });
    });
  });

  describe('generateUserToken', function () {
    let createUserTokenUrl, expectedUrl;

    beforeEach(function () {
      createUserTokenUrl = userTokenUrl();
      expectedUrl = '/api/v2/userTokens/currentUser';
    });

    it('immediately dispatches an USER_TOKEN_GENERATE_TOKEN_REQUESTED action', function () {
      mockAxiosCalls({
        post: {
          [createUserTokenUrl]: Promise.resolve(),
        },
      });
      store.dispatch(generateUserToken());

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toEqual(USER_TOKEN_GENERATE_TOKEN_REQUESTED);
      expect(store.getActions()[0].payload).toBeUndefined();
    });

    it('sends a POST request with proper config', function () {
      mockAxiosCalls({
        post: {
          [createUserTokenUrl]: Promise.resolve(),
        },
      });

      store.dispatch(generateUserToken());
      expect(axios.post).toHaveBeenCalledWith(expectedUrl);
    });

    describe('after a successful POST', function () {
      it('dispatches the USER_TOKEN_MASK_TIMER_DONE action once the timer is done', function (done) {
        mockAxiosCalls({
          post: {
            [createUserTokenUrl]: Promise.resolve({ data: {} }),
          },
        });
        jest.useFakeTimers();

        store.dispatch(generateUserToken()).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          jest.useRealTimers();

          expect(axios.post).toHaveBeenCalledWith(expectedUrl);
          expect(store.getActions().length).toBe(3);
          expect(store.getActions()[2].type).toBe(USER_TOKEN_MASK_TIMER_DONE);
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(USER_TOKEN_GENERATE_TOKEN_REQUESTED);
      });

      it('dispatches the USER_TOKEN_CHECK_TOKEN_EXISTENCE_FULFILLED action with result data', function (done) {
        mockAxiosCalls({
          post: {
            [createUserTokenUrl]: Promise.resolve({
              data: { token: 'userToken' },
            }),
          },
        });

        store.dispatch(generateUserToken()).then(() => {
          expect(axios.post).toHaveBeenCalledWith(expectedUrl);
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1].type).toBe(USER_TOKEN_GENERATE_TOKEN_FULFILLED);
          expect(store.getActions()[1].payload).toEqual({ token: 'userToken' });
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(USER_TOKEN_GENERATE_TOKEN_REQUESTED);
      });
    });

    describe('after a failed POST', function () {
      it('dispatches the USER_TOKEN_GENERATE_TOKEN_FAILED action', function (done) {
        mockAxiosCalls({
          post: {
            [createUserTokenUrl]: () => Promise.reject('Err'),
          },
        });

        store.dispatch(generateUserToken()).then(() => {
          expect(axios.post).toHaveBeenCalledWith(expectedUrl);
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1].type).toBe(USER_TOKEN_GENERATE_TOKEN_FAILED);
          expect(store.getActions()[1].payload).toBe('Err');
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(USER_TOKEN_GENERATE_TOKEN_REQUESTED);
      });
    });
  });

  describe('deleteUserToken', function () {
    let deleteTokenUrl, expectedUrl;

    beforeEach(function () {
      deleteTokenUrl = userTokenUrl();
      expectedUrl = '/api/v2/userTokens/currentUser';
    });

    it('immediately dispatches an USER_TOKEN_DELETE_TOKEN_REQUESTED action', function () {
      mockAxiosCalls({
        del: {
          [deleteTokenUrl]: Promise.resolve(),
        },
      });
      store.dispatch(deleteUserToken());

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toEqual(USER_TOKEN_DELETE_TOKEN_REQUESTED);
      expect(store.getActions()[0].payload).toBeUndefined();
    });

    it('sends a DELETE request with proper config', function () {
      mockAxiosCalls({
        del: {
          [deleteTokenUrl]: Promise.resolve(),
        },
      });

      store.dispatch(deleteUserToken());
      expect(axios.delete).toHaveBeenCalledWith(expectedUrl);
    });

    describe('after a successful DELETE', function () {
      it('dispatches the USER_TOKEN_MASK_TIMER_DONE action once the timer is done', function (done) {
        mockAxiosCalls({
          del: {
            [deleteTokenUrl]: Promise.resolve(),
          },
        });
        jest.useFakeTimers();

        store.dispatch(deleteUserToken()).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          jest.useRealTimers();

          expect(axios.delete).toHaveBeenCalledWith(expectedUrl);
          expect(store.getActions().length).toBe(3);
          expect(store.getActions()[2].type).toBe(USER_TOKEN_MASK_TIMER_DONE);
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(USER_TOKEN_DELETE_TOKEN_REQUESTED);
      });

      it('dispatches the USER_TOKEN_DELETE_TOKEN_FULFILLED action with result data', function (done) {
        mockAxiosCalls({
          del: {
            [deleteTokenUrl]: Promise.resolve(),
          },
        });

        store.dispatch(deleteUserToken()).then(() => {
          expect(axios.delete).toHaveBeenCalledWith(expectedUrl);
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1].type).toBe(USER_TOKEN_DELETE_TOKEN_FULFILLED);
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(USER_TOKEN_DELETE_TOKEN_REQUESTED);
      });
    });

    describe('after a failed DELETE', function () {
      it('dispatches the USER_TOKEN_DELETE_TOKEN_FAILED action', function (done) {
        mockAxiosCalls({
          del: {
            [deleteTokenUrl]: () => Promise.reject('Err'),
          },
        });

        store.dispatch(deleteUserToken()).then(() => {
          expect(axios.delete).toHaveBeenCalledWith(expectedUrl);
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1].type).toBe(USER_TOKEN_DELETE_TOKEN_FAILED);
          expect(store.getActions()[1].payload).toBe('Err');
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(USER_TOKEN_DELETE_TOKEN_REQUESTED);
      });
    });
  });
});
