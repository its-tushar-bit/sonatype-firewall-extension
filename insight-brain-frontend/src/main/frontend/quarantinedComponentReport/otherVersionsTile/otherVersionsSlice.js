/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getQuarantinedComponentOtherVersionsUrl } from 'MainRoot/util/CLMLocation';
import {
  selectCurrentPage,
  selectPageSize,
  selectSortAsc,
} from 'MainRoot/quarantinedComponentReport/otherVersionsTile/otherVersionsSelectors';

const REDUCER_NAME = 'otherVersions';

export const initialState = {
  loading: false,
  loadError: null,
  otherVersions: [],
  pageCount: 0,
  pageSize: 5,
  currentPage: null,
  sortAsc: false,
};

const loadOtherVersionsRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadOtherVersionsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.otherVersions = payload.results;
  state.pageCount = payload.pageCount;
  state.currentPage = payload.pageCount === 0 ? null : payload.page - 1;
};

const loadOtherVersionsFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

function setCurrentPage(state, { payload }) {
  state.currentPage = payload.currentPage;
}

const loadOtherVersions = createAsyncThunk(
  `${REDUCER_NAME}/loadOtherVersions`,
  (token, { getState, rejectWithValue }) => {
    const state = getState();
    const currentPage = selectCurrentPage(state);
    const apiPage = currentPage ? currentPage + 1 : 1;

    return axios
      .get(getQuarantinedComponentOtherVersionsUrl(token, apiPage, selectPageSize(state), selectSortAsc(state)))
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

const setOtherVersionsGridPage = (token, page) => {
  return (dispatch) => {
    dispatch(actions.setCurrentPage({ currentPage: page }));
    dispatch(actions.loadOtherVersions(token));
  };
};

const loadOtherVersionsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: { setCurrentPage },
  extraReducers: {
    [loadOtherVersions.pending]: loadOtherVersionsRequested,
    [loadOtherVersions.fulfilled]: loadOtherVersionsFulfilled,
    [loadOtherVersions.rejected]: loadOtherVersionsFailed,
  },
});

export default loadOtherVersionsSlice.reducer;

export const actions = {
  ...loadOtherVersionsSlice.actions,
  loadOtherVersions,
  setOtherVersionsGridPage,
};
