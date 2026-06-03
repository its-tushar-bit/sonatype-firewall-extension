/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { getSessionUrl, assign } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';
import { pathSet } from 'MainRoot/util/reduxToolkitUtil';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { rejectAll } from 'MainRoot/utility/services/unauthenticatedRequestQueue';
import { selectSsoLoginUrl } from 'MainRoot/user/LoginModal/userLoginSelectors';
import { actions as userSessionActions } from 'MainRoot/user/userSessionSlice';

const REDUCER_NAME = 'userLogin';
const { initialState: rscInitialState } = nxTextInputStateHelpers;

function redirect(destination) {
  assign(destination);
}

export function redirectToIdP(ssoLoginUrl) {
  const destination = window.location.hash
    ? `${ssoLoginUrl}?hash=${encodeURIComponent(window.location.hash)}`
    : ssoLoginUrl;

  redirect(destination);
}

export const initialState = Object.freeze({
  loginModalState: {
    username: rscInitialState(''),
    password: rscInitialState(''),
    showLoginModal: false,
    showSso: false,
    ssoLoginUrl: null,
    isFormValid: false,
    isUnauthenticatedPagesEnabled: undefined,
    isQuarantinedComponentViewAnonymousAccessEnabled: undefined,
  },
  loginModalSubmitState: {
    loginSubmitError: null,
    loginSubmitMaskState: null,
  },
});

/**
 * Send axios request for user login
 */
const submitUserLogin = createAsyncThunk(`${REDUCER_NAME}/submitUserLogin`, (userCredentials, { rejectWithValue }) => {
  const { loginUsername, loginPassword } = userCredentials;
  const encodedCredentials = btoa(
    String.fromCharCode(...new TextEncoder().encode(loginUsername + ':' + loginPassword))
  );
  const headers = {
    Authorization: `Basic ${encodedCredentials}`,
  };

  return axios.post(getSessionUrl(), {}, { waitForLogin: false, headers }).catch(rejectWithValue);
});

const onSubmit = ({ loginUsername, loginPassword }) => {
  return (dispatch) => {
    return dispatch(submitUserLogin({ loginUsername, loginPassword })).then(() => {
      dispatch(userSessionActions.fetchUserSession(false));
    });
  };
};

const onClickSSO = () => {
  return async (dispatch, getState) => {
    const state = getState();
    const ssoLoginUrl = selectSsoLoginUrl(state);
    await redirectToIdP(ssoLoginUrl);
  };
};

const open = (showSsoButton, ssoLoginUrl) => {
  return (dispatch) => {
    dispatch(actions.setShowLoginModal(true));
    dispatch(actions.setShowSso(showSsoButton));
    dispatch(actions.setSsoLoginUrl(ssoLoginUrl));
  };
};

const dismiss = () => {
  return (dispatch) => {
    rejectAll();
    dispatch(actions.resetLoginSubmitState());
  };
};

const userLoginRequested = (state) => {
  return {
    ...state,
    loginModalSubmitState: {
      loginSubmitError: null,
      loginSubmitMaskState: false,
    },
  };
};

const userLoginFulfilled = (state) => {
  return {
    loginModalState: {
      ...state.loginModalState,
      showLoginModal: false,
    },
    loginModalSubmitState: {
      loginSubmitError: null,
      loginSubmitMaskState: true,
    },
  };
};

const userLoginFailed = (state, { payload }) => {
  return {
    ...state,
    loginModalSubmitState: {
      loginSubmitError: Messages.getHttpErrorMessage(payload),
      loginSubmitMaskState: null,
    },
  };
};

const resetLoginSubmitState = (state) => {
  return {
    loginModalState: {
      username: rscInitialState(''),
      password: rscInitialState(''),
      showLoginModal: false,
      showSso: false,
      isFormValid: false,
      isUnauthenticatedPagesEnabled: state.loginModalState.isUnauthenticatedPagesEnabled,
      isQuarantinedComponentViewAnonymousAccessEnabled:
        state.loginModalState.isQuarantinedComponentViewAnonymousAccessEnabled,
    },
    loginModalSubmitState: {
      loginSubmitError: null,
      loginSubmitMaskState: null,
    },
  };
};

const userLoginSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetLoginSubmitState: resetLoginSubmitState,
    setUsername: pathSet(['loginModalState', 'username']),
    setPassword: pathSet(['loginModalState', 'password']),
    setShowLoginModal: pathSet(['loginModalState', 'showLoginModal']),
    setShowSso: pathSet(['loginModalState', 'showSso']),
    setSsoLoginUrl: pathSet(['loginModalState', 'ssoLoginUrl']),
    setUnauthenticatedPagesEnabled: pathSet(['loginModalState', 'isUnauthenticatedPagesEnabled']),
    setQuarantinedComponentViewAnonymousAccessEnabled: pathSet([
      'loginModalState',
      'isQuarantinedComponentViewAnonymousAccessEnabled',
    ]),
  },
  extraReducers: {
    [submitUserLogin.pending]: userLoginRequested,
    [submitUserLogin.fulfilled]: userLoginFulfilled,
    [submitUserLogin.rejected]: userLoginFailed,
  },
});

export default userLoginSlice.reducer;
export const actions = {
  ...userLoginSlice.actions,
  submitUserLogin,
  onSubmit,
  onClickSSO,
  open,
  dismiss,
};
