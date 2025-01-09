/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getDeleteSbomByApplicationIdAndVersionUrl, getSbomsByApplicationUrl } from 'MainRoot/util/CLMLocation';
import { selectSelectedOwnerId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectSbomsTile } from './sbomsTileSelectors';
import { propSet, propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { always, compose, cond, equals, findIndex, includes, T, values, without } from 'ramda';

const PAGE_SIZE = 10;

const REDUCER_NAME = 'sbomsTile';

export const SORT_BY_FIELDS = Object.freeze({
  importDate: 'import_date',
  releaseStatus: 'release_status',
});

export const SORT_DIRECTION = Object.freeze({
  ASC: 'asc',
  DESC: 'desc',
  DEFAULT: null,
});

export const defaultSortConfiguration = Object.freeze({
  sortBy: SORT_BY_FIELDS.importDate,
  sortDirection: SORT_DIRECTION.DESC,
});

export const initialState = Object.freeze({
  loading: false,
  error: null,

  applicationId: null,
  sboms: null,
  sbomsTotalCount: null,

  sortConfiguration: { ...defaultSortConfiguration },

  currentPage: 0,
  pageCount: 0,

  showDeleteModal: false,
  deleteMaskState: null,
  deleteError: null,
  selectedVersionForActions: null,
});

const resetConfigurations = (state) => {
  state.sortConfiguration = { ...defaultSortConfiguration };
  state.currentPage = 0;
  state.pageCount = 0;
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
  state.sboms = null;
  state.loading = true;
  state.error = null;
};

const loadSbomTableDataFailed = (state, { payload }) => {
  state.error = payload.response.data;
  state.loading = false;
};

const loadSbomTableDataFulfilled = (state, { payload }) => {
  state.applicationId = payload.applicationId;
  state.sboms = payload.results;
  state.sbomsTotalCount = payload.totalResultsCount;
  state.pageCount = Math.ceil(payload.totalResultsCount / PAGE_SIZE);
  state.loading = false;
  state.error = null;
};

const loadSbomTableData = createAsyncThunk(
  `${REDUCER_NAME}/loadSbomTableData`,
  async (_, { getState, rejectWithValue }) => {
    const state = getState();
    const { sortConfiguration, currentPage } = selectSbomsTile(state);
    const ownerId = selectSelectedOwnerId(state);
    const sortDirection = cond([
      [equals(SORT_DIRECTION.ASC), always(true)],
      [equals(SORT_DIRECTION.DESC), always(false)],
      [T, always(null)],
    ])(sortConfiguration.sortDirection);
    return axios
      .get(getSbomsByApplicationUrl(ownerId, currentPage + 1, PAGE_SIZE, sortConfiguration.sortBy, sortDirection))
      .then((response) => {
        return { ...response.data, applicationId: ownerId };
      })
      .catch((err) => rejectWithValue(err));
  }
);

// sort-configuration
const setSortByAndDirection = (state, { payload }) => {
  const { sortBy, sortDirection } = payload;
  state.sortConfiguration.sortBy = includes(sortBy, values(SORT_BY_FIELDS)) ? sortBy : defaultSortConfiguration.sortBy;
  state.sortConfiguration.sortDirection = includes(sortDirection, values(SORT_DIRECTION))
    ? sortDirection
    : defaultSortConfiguration.sortDirection;
};

const setSortByAndCycleDirection = (state, { payload: newSortBy }) => {
  const cycleList = (list, current) => {
    const index = findIndex((item) => item === current, list);
    return list[(index + 1) % list.length];
  };

  const currentSortBy = state.sortConfiguration.sortBy;
  const currentDirection = state.sortConfiguration.sortDirection;
  const { sortBy: defaultSortBy, sortDirection: defaultSortDirection } = defaultSortConfiguration;
  const sortDirections = values(SORT_DIRECTION);

  if (newSortBy === defaultSortBy) {
    const complement = [defaultSortDirection, SORT_DIRECTION.DEFAULT];
    if (newSortBy !== currentSortBy) {
      state.sortConfiguration.sortDirection = sortDirections[0];
    } else if (includes(currentDirection, complement)) {
      state.sortConfiguration.sortDirection = cycleList(without(complement, sortDirections), currentDirection);
    } else if (includes(cycleList(sortDirections, currentDirection), complement)) {
      state.sortConfiguration.sortDirection = defaultSortDirection;
    }
    state.sortConfiguration.sortBy = newSortBy;
  } else {
    const nextDirection = newSortBy !== currentSortBy ? sortDirections[0] : cycleList(sortDirections, currentDirection);
    state.sortConfiguration =
      nextDirection === SORT_DIRECTION.DEFAULT
        ? { ...defaultSortConfiguration }
        : { sortBy: newSortBy, sortDirection: nextDirection };
  }
};

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
    setSortByAndDirection,
    setSortByAndCycleDirection,
    setShowDeleteModal: compose(propSetConst('deleteError', null), propSet('showDeleteModal')),
    deleteMaskTimerDone: propSetConst('deleteMaskState', null),
    resetConfigurations,
  },
  extraReducers: {
    [loadSbomTableData.pending]: loadSbomTableDataRequested,
    [loadSbomTableData.fulfilled]: loadSbomTableDataFulfilled,
    [loadSbomTableData.rejected]: loadSbomTableDataFailed,

    [deleteSbomFromTable.pending]: deleteSbomFromTableRequested,
    [deleteSbomFromTable.fulfilled]: deleteSbomFromTableFulfilled,
    [deleteSbomFromTable.rejected]: deleteSbomFromTableFailed,
    [UI_ROUTER_ON_FINISH]: always(initialState),
  },
});

export const actions = {
  ...sbomsTileSlice.actions,
  loadSbomTableData,
  deleteSbomFromTable,
};

export default sbomsTileSlice.reducer;
