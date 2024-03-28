/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getDeleteSbomByApplicationIdAndVersionUrl, getSbomsByApplicationUrl } from 'MainRoot/util/CLMLocation';
import { selectSelectedOwnerId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectCurrentPage, selectSbomsTile, selectSortDir } from './sbomsTileSelectors';
import { propSet, propSetConst } from 'MainRoot/util/reduxToolkitUtil';

import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { compose } from 'ramda';

const REDUCER_NAME = 'sbomsTile';

const PAGE_SIZE = 10;

export const initialState = {
  results: null,
  numResults: null,
  loading: false,
  error: null,
  deleteError: null,
  currentPage: 0,
  pageCount: 0,
  selectedVersionForActions: null,
  applicationId: null,
  sortDir: 'desc',
  deleteMaskState: null,
  showDeleteModal: false,
};

const setCurrentPage = (state, { payload }) => {
  state.currentPage = payload;
};

const toggleSortDir = (state) => {
  state.sortDir = state.sortDir === 'desc' ? 'asc' : 'desc';
};

const setSelectedVersionForActions = (state, { payload }) => {
  if (state.selectedVersionForActions === payload) {
    state.selectedVersionForActions = null;
  } else {
    state.selectedVersionForActions = payload;
  }
};

const loadSbomTableDataRequested = (state) => {
  state.results = null;
  state.loading = true;
  state.error = null;
};

const loadSbomTableDataFailed = (state, { payload }) => {
  state.error = payload.response.data;
  state.loading = false;
};

const loadSbomTableDataFulfilled = (state, { payload }) => {
  state.applicationId = payload.applicationId;
  state.results = payload.results;
  state.numResults = payload.totalResultsCount;
  state.pageCount = Math.ceil(payload.totalResultsCount / PAGE_SIZE);
  state.loading = false;
  state.error = null;
};

const loadSbomTableData = createAsyncThunk(
  `${REDUCER_NAME}/loadSbomTableData`,
  async (_, { getState, rejectWithValue }) => {
    const state = getState();
    const ownerId = selectSelectedOwnerId(state);
    const page = selectCurrentPage(state) + 1;
    const sortDir = selectSortDir(state);
    return axios
      .get(getSbomsByApplicationUrl(ownerId, PAGE_SIZE, page, sortDir))
      .then((response) => {
        return { ...response.data, applicationId: ownerId };
      })
      .catch((err) => rejectWithValue(err));
  }
);

const deleteSbomFromTableRequested = (state) => {
  state.deleteMaskState = false;
};

const deleteSbomFromTableFailed = (state, { payload }) => {
  state.deleteError = payload.response.data;
  state.deleteMaskState = null;
};

const deleteSbomFromTableFulfilled = (state) => {
  state.deleteMaskState = true;
  state.deleteError = null;
};

const startMaskSuccessTimer = (dispatch, action) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(dispatch(action()));
    }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
  });
};

const deleteSbomFromTable = createAsyncThunk(
  `${REDUCER_NAME}/deleteSbomFromTable`,
  async (applicationVersion, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const sbomsTileState = selectSbomsTile(state);
    const applicationId = sbomsTileState.applicationId;
    return axios
      .delete(getDeleteSbomByApplicationIdAndVersionUrl(applicationId, applicationVersion))
      .then(() => {
        startMaskSuccessTimer(dispatch, actions.deleteMaskTimerDone).then(() =>
          dispatch(actions.setShowDeleteModal(false))
        );
        dispatch(actions.loadSbomTableData());
      })
      .catch((err) => rejectWithValue(err));
  }
);

const sbomsTileSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setCurrentPage,
    setSelectedVersionForActions,
    toggleSortDir,
    setShowDeleteModal: compose(propSetConst('deleteError', null), propSet('showDeleteModal')),
    deleteMaskTimerDone: propSetConst('deleteMaskState', null),
  },
  extraReducers: {
    [loadSbomTableData.pending]: loadSbomTableDataRequested,
    [loadSbomTableData.fulfilled]: loadSbomTableDataFulfilled,
    [loadSbomTableData.rejected]: loadSbomTableDataFailed,

    [deleteSbomFromTable.pending]: deleteSbomFromTableRequested,
    [deleteSbomFromTable.fulfilled]: deleteSbomFromTableFulfilled,
    [deleteSbomFromTable.rejected]: deleteSbomFromTableFailed,
  },
});

export const actions = {
  ...sbomsTileSlice.actions,
  loadSbomTableData,
  deleteSbomFromTable,
};

export default sbomsTileSlice.reducer;
