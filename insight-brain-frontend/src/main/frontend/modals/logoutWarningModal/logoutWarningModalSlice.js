/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSlice } from '@reduxjs/toolkit';
import { showNotification } from 'MainRoot/utility/services/notificationService';
import axios from 'axios';
import { getSessionUrl } from 'MainRoot/util/CLMLocation';
import { selectLogoutWarningModalSlice } from 'MainRoot/modals/logoutWarningModal/logoutWarningModalSelectors';

const REDUCER_NAME = 'logoutWarningModal';

const initialState = {
  open: false,
  secondsLeft: null,
  intervalId: null,
};

const open = (payload) => (dispatch, getState) => {
  const { startingCount, productEdition } = payload;
  dispatch(actions.setSecondsLeftAndOpen(startingCount));

  const state = getState();
  const { intervalId } = selectLogoutWarningModalSlice(state);
  if (intervalId) {
    clearInterval(intervalId);
  }

  const newIntervalId = setInterval(() => {
    dispatch(actions.decrementSecondsLeft());
  }, 1000);
  dispatch(actions.setIntervalId(newIntervalId));

  showNotification('Session Timeout Warning', {
    body: `Your ${productEdition} session will expire in 2 minutes due to inactivity.`,
  });
};

const setSecondsLeftAndOpen = (state, { payload }) => {
  state.secondsLeft = payload;
  state.open = true;
};

const decrementSecondsLeft = (state) => {
  state.secondsLeft = state.secondsLeft - 1;
};

const setIntervalId = (state, { payload }) => {
  state.intervalId = payload;
};

const close = () => async (dispatch, getState) => {
  try {
    await axios.get(getSessionUrl());
  } catch (e) {
    // Ignore errors
  }
  const state = getState();
  const { intervalId } = selectLogoutWarningModalSlice(state);
  if (intervalId) {
    clearInterval(intervalId);
  }
  dispatch(actions.reset());
};

const reset = (state) => {
  state.open = false;
  state.secondsLeft = null;
  state.intervalId = null;
};

const logoutWarningModalSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setSecondsLeftAndOpen,
    decrementSecondsLeft,
    setIntervalId,
    reset,
  },
});

export default logoutWarningModalSlice.reducer;

export const actions = {
  ...logoutWarningModalSlice.actions,
  open,
  close,
};
