/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSlice } from '@reduxjs/toolkit';
import { propEq, reject } from 'ramda';
const REDUCER_NAME = 'toast';

export const initialState = {
  toasts: [],
  toastIdInc: 0,
};

const addToast = (state, { payload }) => {
  const { type, message } = payload;
  const toastId = state.toastIdInc + 1;
  state.toastIdInc = toastId;
  state.toasts = [{ id: toastId, type, message }, ...state.toasts];
};

const removeToast = (state, { payload }) => {
  state.toasts = reject(propEq('id', payload), state.toasts);
};

const removeAllToasts = (state) => {
  state.toasts = [];
};

const toastSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    addToast,
    removeToast,
    removeAllToasts,
  },
});

export default toastSlice.reducer;

export const actions = toastSlice.actions;
