/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reducer from 'MainRoot/user/LoginModal/userLoginSlice';

const SUBMIT_USER_LOGIN_REQUESTED = 'userLogin/submitUserLogin/pending';
const SUBMIT_USER_LOGIN_FULFILLED = 'userLogin/submitUserLogin/fulfilled';
const SUBMIT_USER_LOGIN_FAILED = 'userLogin/submitUserLogin/rejected';

describe('userLoginReducer', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      loginModalState: {
        username: { value: '', isPristine: true },
        password: { value: '', isPristine: true },
        isLicensed: false,
        products: [],
        showLoginModal: false,
        showSso: false,
        isFormValid: false,
        isUnauthenticatedPagesEnabled: undefined,
        isQuarantinedComponentViewAnonymousAccessEnabled: undefined,
      },
      loginModalSubmitState: {
        loginSubmitError: null,
        loginSubmitMaskState: null,
      },
    };
  });

  describe('SUBMIT_USER_LOGIN_REQUESTED action', function () {
    it('sets submit mask state to show mask', function () {
      const newState = reducer(mockState, {
        type: SUBMIT_USER_LOGIN_REQUESTED,
      });
      expect(newState.loginModalSubmitState.loginSubmitMaskState).toEqual(false);
    });
  });

  describe('SUBMIT_USER_LOGIN_FULFILLED action', function () {
    it('sets successful login mask state', function () {
      const newState = reducer(mockState, {
        type: SUBMIT_USER_LOGIN_FULFILLED,
      });

      expect(newState.loginModalSubmitState.loginSubmitMaskState).toEqual(true);
    });
  });

  describe('SUBMIT_USER_LOGIN_FAILED action', function () {
    it('sets error and clears mask state', function () {
      const newState = reducer(mockState, {
        type: SUBMIT_USER_LOGIN_FAILED,
        payload: { data: 'Error' },
      });

      expect(newState.loginModalSubmitState.loginSubmitMaskState).toEqual(null);
      expect(newState.loginModalSubmitState.loginSubmitError).toEqual('Error');
    });

    it('clears error state on retry', function () {
      const newState = reducer(mockState, {
        type: SUBMIT_USER_LOGIN_FAILED,
        payload: { data: 'Error' },
      });

      expect(newState.loginModalSubmitState.loginSubmitError).toEqual('Error');

      const retryState = reducer(newState, {
        type: SUBMIT_USER_LOGIN_REQUESTED,
      });
      expect(retryState.loginModalSubmitState.loginSubmitError).toBeNull();
    });
  });
});
