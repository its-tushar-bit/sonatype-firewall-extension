/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSlice } from '@reduxjs/toolkit';
import { propSet } from '../util/reduxToolkitUtil';

const REDUCER_NAME = 'ownerDetailTree';

export const initialState = {
  loading: false,
  loadError: null,
};

const ownerDetailTreeSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setLoading: propSet('loading'),
    setLoadError: propSet('loadError'),
  },
});

export const actions = {
  ...ownerDetailTreeSlice.actions,
};

export default ownerDetailTreeSlice.reducer;
