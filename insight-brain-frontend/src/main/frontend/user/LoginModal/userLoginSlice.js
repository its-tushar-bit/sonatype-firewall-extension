/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global Base64 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { always } from 'ramda';
import { getSessionUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';
import { pathSet } from 'MainRoot/util/reduxToolkitUtil';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

const REDUCER_NAME = 'userLogin';
const { initialState: rscInitialState } = nxTextInputStateHelpers;

export const initialState = Object.freeze({
  loginModalState: {
    username: rscInitialState(''),
    password: rscInitialState(''),
    isLicensed: false,
    showLoginModal: false,
    showSamlSso: false,
    isFormValid: false,
    isUnauthenticatedPagesEnabled: true,
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
      loginSubmitError: Messages.getHttpErrorMessage(payload.data),
      loginSubmitMaskState: null,
    },
  };
};

const userLoginSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetLoginSubmitState: always(initialState),
    setUsername: pathSet(['loginModalState', 'username']),
    setPassword: pathSet(['loginModalState', 'password']),
    setIsLicensed: pathSet(['loginModalState', 'isLicensed']),
    setShowLoginModal: pathSet(['loginModalState', 'showLoginModal']),
    setShowSamlSso: pathSet(['loginModalState', 'showSamlSso']),
    setUnauthenticatedPagesEnabled: pathSet(['loginModalState', 'isUnauthenticatedPagesEnabled']),
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
};
