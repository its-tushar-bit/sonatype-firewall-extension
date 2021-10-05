/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSlice } from '@reduxjs/toolkit';

import { toggleBooleanProp } from '../../util/reduxUtil';

const REDUCER_NAME = 'componentDetailsLegal';

const initialState = {
  showEditLicensesPopover: false,
};

const componentDetailsLegalSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    toggleShowEditLicensesPopover: toggleBooleanProp('showEditLicensesPopover'),
  },
});

export default componentDetailsLegalSlice.reducer;
export const actions = {
  ...componentDetailsLegalSlice.actions,
};
