/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSlice } from '@reduxjs/toolkit';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

const REDUCER_NAME = 'sbomManager';

export const initialState = {
  showSbomManagerSidebar: false,
};

const onRouterFinish = (state, { payload }) => {
  state.showSbomManagerSidebar = payload.toState.name.includes('sbomManager');
};

export const sbomManagerPageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  extraReducers: {
    [UI_ROUTER_ON_FINISH]: onRouterFinish,
  },
});

export default sbomManagerPageSlice.reducer;
export const actions = {
  ...sbomManagerPageSlice.actions,
};
