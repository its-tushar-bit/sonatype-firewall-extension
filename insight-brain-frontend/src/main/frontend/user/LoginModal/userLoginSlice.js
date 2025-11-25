/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global Base64 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getSessionUrl, assign } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';
import { pathSet } from 'MainRoot/util/reduxToolkitUtil';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { unwrapResult } from '@reduxjs/toolkit';
import { clearRequests, rejectAll } from 'MainRoot/utility/services/unauthenticatedRequestQueue';
import { selectIsLicenseInstalled, selectProducts } from 'MainRoot/productFeatures/productLicenseSelectors';
import {
  selectIsUnauthenticatedPagesEnabled,
  selectIsQuarantinedComponentViewAnonymousAccessEnabled,
  selectSsoLoginUrl,
} from 'MainRoot/user/LoginModal/userLoginSelectors';
import { actions as userSessionActions, ensureUserLoggedIn } from 'MainRoot/user/userSessionSlice';

const REDUCER_NAME = 'userLogin';
const { initialState: rscInitialState } = nxTextInputStateHelpers;

function redirect(destination) {
  assign(destination);
}

function isBackupLogin() {
  return window.location.hash === '#/backupLogin';
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
    isLicensed: false,
    products: [],
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
  const encodedCredentials = Base64.encode(loginUsername + ':' + loginPassword);
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

const authenticate = (wwwAuthenticateHeader, ssoLoginUrl) => {
  return async (dispatch) => {
    let isSsoOnlyEnabled = false;
    try {
      isSsoOnlyEnabled = await dispatch(productFeaturesActions.loadIsSsoOnlyEnabled()).then(unwrapResult);
    } catch (error) {
      isSsoOnlyEnabled = false;
    }

    const hasSso =
      wwwAuthenticateHeader && (wwwAuthenticateHeader.includes('SAML') || wwwAuthenticateHeader.includes('OIDC'));

    if (isSsoOnlyEnabled && hasSso && !isBackupLogin()) {
      clearRequests();
      redirectToIdP(ssoLoginUrl);
      return;
    }

    dispatch(open(hasSso, ssoLoginUrl));
    return dispatch(ensureUserLoggedIn());
  };
};

const open = (showSsoButton, ssoLoginUrl) => {
  return (dispatch, getState) => {
    const state = getState();
    const isLicensed = selectIsLicenseInstalled(state);
    const products = selectProducts(state);
    const isUnauthenticatedPagesEnabled = selectIsUnauthenticatedPagesEnabled(state);
    const isQuarantinedComponentViewEnabled = selectIsQuarantinedComponentViewAnonymousAccessEnabled(state);

    dispatch(actions.setIsLicensed(isLicensed));
    dispatch(actions.setProducts(products));
    dispatch(actions.setUnauthenticatedPagesEnabled(isUnauthenticatedPagesEnabled));
    dispatch(actions.setQuarantinedComponentViewAnonymousAccessEnabled(isQuarantinedComponentViewEnabled));

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
      isLicensed: false,
      products: [],
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
    setIsLicensed: pathSet(['loginModalState', 'isLicensed']),
    setProducts: pathSet(['loginModalState', 'products']),
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
  authenticate,
  open,
  dismiss,
};
