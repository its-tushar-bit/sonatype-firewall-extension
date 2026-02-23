/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { getSessionUrl } from 'MainRoot/util/CLMLocation';
import { actions as userLoginActions } from 'MainRoot/user/LoginModal/userLoginSlice';

import 'TestRoot/SpecUtil';

const { submitUserLogin } = userLoginActions;

const SUBMIT_USER_LOGIN_REQUESTED = 'userLogin/submitUserLogin/pending';
const SUBMIT_USER_LOGIN_FULFILLED = 'userLogin/submitUserLogin/fulfilled';
const SUBMIT_USER_LOGIN_FAILED = 'userLogin/submitUserLogin/rejected';

describe('userLoginActions', () => {
  let store, state, mockCredentials, sessionUrl;

  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  beforeEach(function () {
    window.Base64 = {
      encode: (args) => args,
    };

    state = {
      loginSubmitError: null,
      loginSubmitMaskState: null,
    };
    mockCredentials = {
      loginUsername: 'testUser',
      loginPassword: 'testPassword',
    };

    sessionUrl = getSessionUrl(mockCredentials);

    store = SpecUtil.mockReduxStore(state);
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  describe('submitLogin action', () => {
    it('immediately dispatches SUBMIT_USER_LOGIN_REQUESTED action', () => {
      mockAxiosCalls({
        post: {
          [sessionUrl]: Promise.resolve(),
        },
      });

      store.dispatch(submitUserLogin());

      const actions = store.getActions();
      expect(actions).toHaveActionType(SUBMIT_USER_LOGIN_REQUESTED);
    });

    it('sends a POST request to the appropriate url', (done) => {
      mockAxiosCalls({
        post: {
          [sessionUrl]: Promise.resolve(),
        },
      });

      store.dispatch(submitUserLogin(mockCredentials)).then(() => {
        expect(axios.post).toHaveBeenCalledWith(
          sessionUrl,
          {} /* empty data on post */,
          {
            waitForLogin: false,
            headers: {
              Authorization: `Basic testUser:testPassword`,
            },
          }
        );
        done();
      });
    });

    it('dispatches SUBMIT_USER_LOGIN_FULFILLED after a successful response', (done) => {
      const mockResponse = { data: { someData: 'Some data' } };
      mockAxiosCalls({
        post: {
          [sessionUrl]: Promise.resolve(mockResponse),
        },
      });

      store.dispatch(submitUserLogin(mockCredentials)).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();

        expect(actions).toHaveActionType(SUBMIT_USER_LOGIN_FULFILLED);

        done();
      });
    });

    it('dispatches SUBMIT_USER_LOGIN_FAILED after a failed reponse', (done) => {
      mockAxiosCalls({
        post: {
          [sessionUrl]: () => Promise.reject(),
        },
      });

      store.dispatch(submitUserLogin()).then(() => {
        const actions = store.getActions();

        expect(actions).toHaveActionType(SUBMIT_USER_LOGIN_FAILED);
        done();
      });
    });
  });
});
