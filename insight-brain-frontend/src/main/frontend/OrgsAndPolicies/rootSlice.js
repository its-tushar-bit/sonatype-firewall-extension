/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSlice } from '@reduxjs/toolkit';

const REDUCER_NAME = 'orgsAndPolicies';

export const initialState = {
  selectedOwner: {},
};

// TODO:
// now payload contains all Request properties
// ['$clone', '$getOriginal', '$new', '$revert', '$updateOriginal', '$delete', '$save', 'isDirty'];
// Once we get rid off  EventNameConstant.OWNER_UPDATED and 'owner.deleted' events it won't store
// those additional values
const setSelectedOwner = (state, { payload }) => {
  state.selectedOwner = payload;
};

const setSelectedOwnerContact = (state, { payload }) => {
  state.selectedOwner.contact = payload;
};

const selectedOwnerParentOrganizationUpdated = (state, { payload: { organizationName, organizationId } }) => {
  state.selectedOwner.organizationName = organizationName;
  state.selectedOwner.organizationId = organizationId;
};

const rootSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setSelectedOwner,
    setSelectedOwnerContact,
    selectedOwnerParentOrganizationUpdated,
  },
});

export const actions = {
  ...rootSlice.actions,
};

export default rootSlice.reducer;
