/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { always, compose, cond, equals, findIndex, includes, is, T, trim, values, when, without } from 'ramda';

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getSbomApplicationsUrl } from 'MainRoot/util/CLMLocation';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { selectSbomApplicationsTable } from './sbomApplicationsTableSelectors';

const REDUCER_NAME = 'sbomApplicationsTable';

export const APPLICATIONS_PER_PAGE = 50;

export const SORT_BY_FIELDS = Object.freeze({
  name: 'application_name',
  latestVersion: 'latest_sbom_version',
  importDate: 'import_date',
  vulnerabilities: 'vulnerability',
  releaseStatusPercentage: 'release_status_percentage',
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

export const defaultPagination = Object.freeze({
  pageCount: 1,
  currentPage: 0,
});

export const initialState = Object.freeze({
  loading: true,
  errorMessage: null,

  applications: null,
  applicationsTotalCount: null,

  sortConfiguration: { ...defaultSortConfiguration },
  pagination: { ...defaultPagination },

  applicationNameRawFilterTerm: '',
});

const resetConfigurations = (state) => {
  state.sortConfiguration = { ...defaultSortConfiguration };
  state.pagination = { ...defaultPagination };
  state.applicationNameRawFilterTerm = '';
};

const setLoading = (state, { payload }) => {
  state.loading = payload;
};

// load-applications
const loadApplicationsRequested = (state) => {
  state.loading = true;
  state.errorMessage = null;
  state.applications = null;
  state.applicationsTotalCount = null;
};

const loadApplicationsFailed = (state, { payload }) => {
  state.loading = false;
  state.errorMessage = Messages.getHttpErrorMessage(payload);

  state.applications = null;
  state.applicationsTotalCount = null;

  state.pagination = { ...defaultPagination };
};

const loadApplicationsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.errorMessage = null;

  state.applications = payload.applications;
  state.applicationsTotalCount = payload.totalCount;

  state.pagination.pageCount = Math.ceil(payload.totalCount / APPLICATIONS_PER_PAGE);
};

const cleanFilterTerm = compose(when(isNilOrEmpty, always(null)), when(is(String), trim));

const loadApplications = createAsyncThunk(
  `${REDUCER_NAME}/loadApplications`,
  async (_, { getState, rejectWithValue }) => {
    const state = getState();
    const { sortConfiguration, pagination, applicationNameRawFilterTerm } = selectSbomApplicationsTable(state);

    const sortDirection = cond([
      [equals(SORT_DIRECTION.ASC), always(true)],
      [equals(SORT_DIRECTION.DESC), always(false)],
      [T, always(null)],
    ])(sortConfiguration.sortDirection);

    return axios
      .get(
        getSbomApplicationsUrl(
          pagination.currentPage + 1,
          APPLICATIONS_PER_PAGE,
          sortConfiguration.sortBy,
          sortDirection,
          cleanFilterTerm(applicationNameRawFilterTerm)
        )
      )
      .then((response) => response.data)
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

// pagination
const setCurrentPage = (state, { payload }) => {
  state.pagination.currentPage = payload;
};

// filter-application-name
const setApplicationNameRawFilterTerm = (state, { payload }) => {
  state.applicationNameRawFilterTerm = payload;
};

const sbomApplicationsTableSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetConfigurations,
    setLoading,
    setSortByAndDirection,
    setSortByAndCycleDirection,
    setCurrentPage,
    setApplicationNameRawFilterTerm,
  },
  extraReducers: {
    [loadApplications.pending]: loadApplicationsRequested,
    [loadApplications.fulfilled]: loadApplicationsFulfilled,
    [loadApplications.rejected]: loadApplicationsFailed,
  },
});

export const actions = {
  ...sbomApplicationsTableSlice.actions,
  loadApplications,
};

export default sbomApplicationsTableSlice.reducer;
