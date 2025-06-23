/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  error: null, // Global error state
};

const globalSlice = createSlice({
  name: 'global',
  initialState,
  reducers: {
    setError(state, action) {
      state.error = action.payload;
    },
  },
});

export const { setError } = globalSlice.actions;

export const selectGlobalState = (state) => state.global;
export const selectError = (state) => state.global.error;

export default globalSlice.reducer;
