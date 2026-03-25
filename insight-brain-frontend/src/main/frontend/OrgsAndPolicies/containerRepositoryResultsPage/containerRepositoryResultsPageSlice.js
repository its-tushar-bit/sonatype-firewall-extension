/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { debounce } from 'debounce';
import * as R from 'ramda';

import {
  getContainerRepositoryReportSummaryUrl,
  getContainerRepositoryResultsUrl,
  getRepositoryInfoUrl,
} from 'MainRoot/util/CLMLocation';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { Messages } from 'MainRoot/util/CommonServices';

import selectContainerRepositoryResultsPage from './containerRepositoryResultsPageSelectors';

const LOAD_TABLE_DEBOUNCE_TIME = 250; // milliseconds

const REDUCER_NAME = 'containerRepositoryResultsPage';

export const ITEMS_PER_PAGE = 50;

export const defaultEvaluationSummary = Object.freeze({
  totalContainerImageCount: 0,
  totalContainerImageViolationCount: 0,
  criticalViolationCount: 0,
  severeViolationCount: 0,
  moderateViolationCount: 0,
  affectedContainerImageCount: 0,
  quarantinedContainerImageCount: 0,
});

export const defaultPagination = Object.freeze({
  page: 1,
  pageSize: ITEMS_PER_PAGE,
  hasNextPage: false,
});

export const defaultSortConfiguration = Object.freeze([
  {
    sortableField: 'QUARANTINE_TIME',
    asc: false,
    sortPriority: 1,
  },
  {
    sortableField: 'POLICY_THREAT_LEVEL',
    asc: false,
    sortPriority: 2,
  },
  // Disabled sorting for POLICY_NAME until non-aggregate is implemented
  // in the future.
  // {
  //   sortableField: 'POLICY_NAME',
  //   asc: true,
  //   sortPriority: 3,
  // },
  {
    sortableField: 'OBJECT_NAME',
    asc: true,
    sortPriority: 4,
  },
]);

export const initialState = Object.freeze({
  repositoryId: null,
  loading: true,
  errorMessage: null,

  repositoryInformation: null,

  // summary
  evaluationSummary: { ...defaultEvaluationSummary },

  results: [],

  sortConfiguration: [...defaultSortConfiguration],
  pagination: { ...defaultPagination },

  // filters
  showFilterDrawer: false,
  // Endpoint accepts:
  // searchFilters: [
  //   { filterableField: 'QUARANTINE_TIME', value: '' },
  // ]
  columnFilters: [],
  violationStateFilters: [],
  threatLevelRange: [0, 10],

  // re-evaluation-modal
  showReevaluationModal: false,
  submitMask: {
    show: false,
    success: false,
  },
});

const setRepositoryId = (state, { payload }) => {
  state.repositoryId = payload;
};

const setLoading = (state, { payload }) => {
  state.loading = payload;
};

// filter-drawer
const setShowFilterDrawer = (state, { payload }) => {
  state.showFilterDrawer = payload;
};

// sorting
const sortColumn = (state, { payload: column }) => {
  const columnConfig = R.find(R.propEq('sortableField', column), state.sortConfiguration);
  const first = R.pipe(R.assoc('asc', !columnConfig.asc), R.assoc('sortPriority', 1))(columnConfig);
  state.sortConfiguration = R.pipe(
    R.reject(R.propEq('sortableField', column)),
    R.prepend(first),
    R.addIndex(R.map)((config, index) => R.assoc('sortPriority', index + 1, config))
  )(state.sortConfiguration);

  state.pagination.page = 1;
};

// pagination
const setPage = (state, { payload }) => {
  state.pagination.page = payload;
};

const incrementPage = (state) => {
  state.pagination.page += 1;
};

const decrementPage = (state) => {
  state.pagination.page -= 1;
};

const loadNextPage = () => (dispatch, getState) => {
  const {
    pagination: { hasNextPage },
  } = selectContainerRepositoryResultsPage(getState());
  if (hasNextPage) {
    dispatch(actions.incrementPage());
    dispatch(actions.loadTable());
  }
};

const loadPreviousPage = () => (dispatch, getState) => {
  const {
    pagination: { page },
  } = selectContainerRepositoryResultsPage(getState());
  if (page > 1) {
    dispatch(actions.decrementPage());
    dispatch(actions.loadTable());
  }
};

// filters
const setViolationStateFilters = (state, { payload }) => {
  state.violationStateFilters = Array.isArray(payload) ? payload : [];
};

const setThreatLevelRange = (state, { payload }) => {
  state.threatLevelRange = [...payload];
};

const clearDrawerFilters = (state) => {
  state.violationStateFilters = [];
  state.threatLevelRange = [0, 10];
  state.pagination.page = 1;
};

const sanitizeFilterValue = R.pipe(R.when(R.is(String), R.trim), R.when(isNilOrEmpty, R.always(null)));
const setColumnFilter = (state, { payload: { column, value } }) => {
  state.columnFilters = R.pipe(
    R.reject(R.propEq('filterableField', column)),
    R.append({ filterableField: column, value: sanitizeFilterValue(value) }),
    R.reject(R.propSatisfies(isNilOrEmpty, 'value'))
  )(state.columnFilters);

  state.pagination.page = 1;
};

const searchFilterColumn = ({ column, value }) => (dispatch) => {
  dispatch(actions.setColumnFilter({ column, value }));
  debouncedLoadTable(dispatch);
};

// load-table
const loadTable = createAsyncThunk(`${REDUCER_NAME}/loadTable`, (_, { getState, rejectWithValue }) => {
  const {
    columnFilters,
    pagination,
    repositoryId,
    sortConfiguration,
    threatLevelRange,
    violationStateFilters,
  } = selectContainerRepositoryResultsPage(getState());

  return axios
    .post(getContainerRepositoryResultsUrl(repositoryId), {
      violationStateFilters,
      threatLevelFilters: threatLevelRange,
      searchFilters: columnFilters,
      sortFields: sortConfiguration,
      page: pagination.page,
      pageSize: pagination.pageSize,
      aggregate: true,
    })
    .then(R.prop('data'))
    .catch(rejectWithValue);
});

const debouncedLoadTable = debounce((dispatch) => {
  dispatch(actions.loadTable());
}, LOAD_TABLE_DEBOUNCE_TIME);

const loadTablePending = (state) => {
  state.errorMessage = null;
  state.results = null;
};

const loadTableFailed = (state, { payload }) => {
  state.errorMessage = Messages.getHttpErrorMessage(payload);
  state.results = null;
};

const loadTableFulfilled = (state, { payload }) => {
  state.errorMessage = null;
  state.results = payload.repositoryResultsDetails;
  state.pagination.hasNextPage = payload.hasNextPage;
};

// load-repository-information
const loadRepositoryInformation = createAsyncThunk(
  `${REDUCER_NAME}/loadRepositoryInformation`,
  (_, { getState, rejectWithValue }) => {
    const { repositoryId } = selectContainerRepositoryResultsPage(getState());
    return axios.get(getRepositoryInfoUrl(repositoryId)).then(R.prop('data')).catch(rejectWithValue);
  }
);

const loadRepositoryInformationPending = (state) => {
  state.errorMessage = null;
  state.repositoryInformation = null;
};

const loadRepositoryInformationFailed = (state, { payload }) => {
  state.errorMessage = Messages.getHttpErrorMessage(payload);
  state.repositoryInformation = null;
};

const loadRepositoryInformationFulfilled = (state, { payload }) => {
  state.errorMessage = null;
  state.repositoryInformation = payload.repository;
};

// load-evaluation-summary
const loadEvaluationSummary = createAsyncThunk(
  `${REDUCER_NAME}/loadEvaluationSummary`,
  (_, { getState, rejectWithValue }) => {
    const { repositoryId } = selectContainerRepositoryResultsPage(getState());
    return axios.get(getContainerRepositoryReportSummaryUrl(repositoryId)).then(R.prop('data')).catch(rejectWithValue);
  }
);

const loadEvaluationSummaryPending = (state) => {
  state.errorMessage = null;
  state.evaluationSummary = { ...defaultEvaluationSummary };
};

const loadEvaluationSummaryFailed = (state, { payload }) => {
  state.errorMessage = Messages.getHttpErrorMessage(payload);
  state.evaluationSummary = { ...defaultEvaluationSummary };
};

const loadEvaluationSummaryFulfilled = (state, { payload }) => {
  state.errorMessage = null;
  state.evaluationSummary = payload;
};

const containerRepositoryResultsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setRepositoryId,
    setLoading,

    // Filter Drawer
    setShowFilterDrawer,
    setThreatLevelRange,
    setViolationStateFilters,
    clearDrawerFilters,

    // Table
    setColumnFilter,
    sortColumn,
    incrementPage,
    decrementPage,
    setPage,
  },
  extraReducers: {
    [loadRepositoryInformation.pending]: loadRepositoryInformationPending,
    [loadRepositoryInformation.fulfilled]: loadRepositoryInformationFulfilled,
    [loadRepositoryInformation.rejected]: loadRepositoryInformationFailed,

    [loadEvaluationSummary.pending]: loadEvaluationSummaryPending,
    [loadEvaluationSummary.fulfilled]: loadEvaluationSummaryFulfilled,
    [loadEvaluationSummary.rejected]: loadEvaluationSummaryFailed,

    [loadTable.pending]: loadTablePending,
    [loadTable.fulfilled]: loadTableFulfilled,
    [loadTable.rejected]: loadTableFailed,
  },
});

export const actions = {
  ...containerRepositoryResultsSlice.actions,
  loadRepositoryInformation,
  loadEvaluationSummary,
  loadTable,

  loadNextPage,
  loadPreviousPage,
  searchFilterColumn,
};

export default containerRepositoryResultsSlice.reducer;
