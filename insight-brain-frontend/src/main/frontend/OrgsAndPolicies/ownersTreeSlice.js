/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSlice } from '@reduxjs/toolkit';

const REDUCER_NAME = 'ownersTree';
export const TREE_NODE_STATUS = Object.freeze({ expanded: true, collapsed: false });

export const initialState = {
  nodesStatus: {},
  initialStatus: TREE_NODE_STATUS.expanded,
};

const toogleTreeNode = (state, { payload = {} }) => {
  const { ownerId } = payload;
  const isExpanded = state.nodesStatus?.[ownerId] ?? state.initialStatus;
  state.nodesStatus[ownerId] = !isExpanded;
};

const expandAllTreeNodes = (state) => {
  state.nodesStatus = {};
  state.initialStatus = TREE_NODE_STATUS.expanded;
};

const collapseAllTreeNodes = (state) => {
  state.nodesStatus = {};
  state.initialStatus = TREE_NODE_STATUS.collapsed;
};

const ownersTreeSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    collapseAllTreeNodes,
    expandAllTreeNodes,
    toogleTreeNode,
  },
  extraReducers: {},
});

export const actions = { ...ownersTreeSlice.actions };

export default ownersTreeSlice.reducer;
