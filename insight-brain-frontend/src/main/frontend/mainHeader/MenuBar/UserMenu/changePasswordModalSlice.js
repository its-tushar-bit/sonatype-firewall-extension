/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { getChangeMyPasswordUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';
import { handlePasswordChangeForUser } from 'MainRoot/user/userSessionSlice';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

const REDUCER_NAME = 'changePasswordModal';

const initialState = {
  status: 'idle', // 'idle' | 'pending' | 'success' | 'failure'
  errorMessage: null,
};

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch(actions.resetStatus());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

export const changePassword = createAsyncThunk(
  `${REDUCER_NAME}/changePassword`,
  async ({ oldPassword, newPassword }, { rejectWithValue, dispatch }) => {
    try {
      await axios.put(getChangeMyPasswordUrl(), { oldPassword, newPassword });

      // Handle password change completion (clears warning if needed)
      // Pass undefined to indicate the current user changed their own password
      await dispatch(handlePasswordChangeForUser());

      // Start timer to reset status after showing success mask
      startSubmitMaskSuccessTimer(dispatch);

      return null;
    } catch (error) {
      return rejectWithValue(Messages.getHttpErrorMessage(error));
    }
  }
);

// Separate reducer functions (not inline)
const changePasswordRequested = (state) => {
  state.status = 'pending';
  state.errorMessage = null;
};

const changePasswordFulfilled = (state) => {
  state.status = 'success';
  state.errorMessage = null;
};

const changePasswordFailed = (state, { payload }) => {
  state.status = 'failure';
  state.errorMessage = payload;
};

const resetStatus = (state) => {
  state.status = 'idle';
  state.errorMessage = null;
};

const changePasswordModalSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetStatus,
  },
  extraReducers: {
    [changePassword.pending]: changePasswordRequested,
    [changePassword.fulfilled]: changePasswordFulfilled,
    [changePassword.rejected]: changePasswordFailed,
  },
});

export const actions = {
  ...changePasswordModalSlice.actions,
  changePassword,
};

export default changePasswordModalSlice.reducer;
