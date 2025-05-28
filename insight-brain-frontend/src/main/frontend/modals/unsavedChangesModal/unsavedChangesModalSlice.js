/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSlice } from '@reduxjs/toolkit';

const REDUCER_NAME = 'unsavedChangesModal';

const initialState = {
  open: false,
};

let promise = null;
let promiseResolveReject = null;

const open = () => (dispatch) => {
  if (!promise) {
    promise = new Promise((resolve, reject) => {
      promiseResolveReject = { resolve, reject };
    });
  }
  dispatch(actions.setOpen());
  return promise;
};

const cancelAndClose = () => (dispatch) => {
  if (promise) {
    promiseResolveReject.reject();
    promise = null;
    promiseResolveReject = null;
  }
  dispatch(actions.close());
};

const continueAndClose = () => (dispatch) => {
  if (promise) {
    promiseResolveReject.resolve();
    promise = null;
    promiseResolveReject = null;
  }
  dispatch(actions.close());
};

const setOpen = (state) => {
  state.open = true;
};

const close = (state) => {
  state.open = false;
};

const unsavedChangesModalSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setOpen,
    close,
  },
});

export default unsavedChangesModalSlice.reducer;

export const actions = {
  ...unsavedChangesModalSlice.actions,
  open,
  cancelAndClose,
  continueAndClose,
};
