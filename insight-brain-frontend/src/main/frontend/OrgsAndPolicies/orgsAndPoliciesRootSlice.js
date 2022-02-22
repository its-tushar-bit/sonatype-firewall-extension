/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSlice } from '@reduxjs/toolkit';

const REDUCER_NAME = 'orgsAndPolicies';

export const initialState = {
  ownerName: null,
};

const updatedOwnerHandler = (state, { payload }) => {
  state.ownerName = payload;
};

const orgsAndPoliciesRootSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    updatedOwnerHandler,
  },
});

export const actions = {
  ...orgsAndPoliciesRootSlice.actions,
};

export default orgsAndPoliciesRootSlice.reducer;
