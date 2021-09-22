/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSlice } from '@reduxjs/toolkit';
import { toggleBooleanProp } from '../../../util/reduxUtil';

const REDUCER_NAME = 'occurrencesPopover';

export const initialState = {
  showOccurrencesPopover: false,
};

const occurrencesPopoverSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    toggleShowOccurrencesPopover: toggleBooleanProp('showOccurrencesPopover'),
  },
});

export default occurrencesPopoverSlice.reducer;
export const occurrencesPopoverActions = {
  ...occurrencesPopoverSlice.actions,
};
