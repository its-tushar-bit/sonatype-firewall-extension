/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSlice } from '@reduxjs/toolkit';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';

const REDUCER_NAME = 'ownersTree';
export const TREE_NODE_STATUS = Object.freeze({ expanded: true, collapsed: false });

export const initialState = {
  nodesStatus: {},
  filteredNodesStatus: {},
  initialStatus: TREE_NODE_STATUS.expanded,
  initialFilteredStatus: TREE_NODE_STATUS.expanded,
  searchTerm: '',
  filteredOwners: [],
};

const toogleTreeNode = (state, { payload = {} }) => {
  const { ownerId } = payload;
  const currentNodeStatus = state.searchTerm ? state.filteredNodesStatus : state.nodesStatus;
  const currentInitialStatus = state.searchTerm ? state.initialFilteredStatus : state.initialStatus;
  const isExpanded = currentNodeStatus?.[ownerId] ?? currentInitialStatus;
  if (state.searchTerm) {
    state.filteredNodesStatus[ownerId] = !isExpanded;
  } else {
    state.nodesStatus[ownerId] = !isExpanded;
  }
};

const expandAllTreeNodes = (state) => {
  if (state.searchTerm) {
    state.filteredNodesStatus = {};
    state.initialFilteredStatus = TREE_NODE_STATUS.expanded;
  } else {
    state.nodesStatus = {};
    state.initialStatus = TREE_NODE_STATUS.expanded;
  }
};

const collapseAllTreeNodes = (state) => {
  if (state.searchTerm) {
    state.filteredNodesStatus = {};
    state.initialFilteredStatus = TREE_NODE_STATUS.collapsed;
  } else {
    state.nodesStatus = {};
    state.initialStatus = TREE_NODE_STATUS.collapsed;
  }
};

const filterOwnersByName = (searchTerm, ownerId, ownersMap, isParentFiltered = false) => {
  const { name, organizationIds, applicationIds } = ownersMap[ownerId] || {};
  const ownerNameContainsSearchTerm = name.toUpperCase().includes(searchTerm.toUpperCase());

  const children = [...(organizationIds || []), ...(applicationIds || [])];
  const filteredChildren = children.reduce((selectedChildren, child) => {
    const newSelectedChildren = filterOwnersByName(
      searchTerm,
      child,
      ownersMap,
      isParentFiltered || ownerNameContainsSearchTerm
    );
    return [...selectedChildren, ...newSelectedChildren];
  }, []);
  if (ownerNameContainsSearchTerm || isParentFiltered || !isNilOrEmpty(filteredChildren)) {
    return [...filteredChildren, ownerId];
  }
  return filteredChildren;
};

const setOwnersTreeSearchTerm = (state, { payload }) => {
  const { ownersMap, topParentOrganizationId, searchTerm } = payload;

  const filteredOwners = searchTerm ? filterOwnersByName(searchTerm, topParentOrganizationId, ownersMap) : [];
  state.searchTerm = searchTerm;
  state.filteredOwners = filteredOwners;
  state.filteredNodesStatus = {};
  state.initialFilteredStatus = TREE_NODE_STATUS.expanded;
};

const ownersTreeSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    collapseAllTreeNodes,
    expandAllTreeNodes,
    toogleTreeNode,
    setOwnersTreeSearchTerm,
  },
});

export const actions = {
  ...ownersTreeSlice.actions,
};

export default ownersTreeSlice.reducer;
