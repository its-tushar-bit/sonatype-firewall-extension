/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSlice } from '@reduxjs/toolkit';

const REDUCER_NAME = 'externalLinkModal';

const initialState = {
  open: false,
  href: null,
};

const open = (state, { payload }) => {
  state.open = true;
  state.href = payload;
};

const close = () => {
  return initialState;
};

const externalLinkModalSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    open,
    close,
  },
});

export default externalLinkModalSlice.reducer;

export const actions = {
  ...externalLinkModalSlice.actions,
};
