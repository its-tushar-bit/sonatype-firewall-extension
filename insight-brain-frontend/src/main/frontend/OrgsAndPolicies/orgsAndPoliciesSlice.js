/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSlice } from '@reduxjs/toolkit';

const REDUCER_NAME = 'orgsAndPolicies';

export const initialState = {};

const orgsAndPoliciesSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
});

export default orgsAndPoliciesSlice.reducer;
export const actions = {
  ...orgsAndPoliciesSlice.actions,
};
