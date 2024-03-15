/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getAuditReportSummary, getRepositoryInfoUrl, getRepositoryEvaluateUrl } from 'MainRoot/util/CLMLocation';
import { prop } from 'ramda';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { propSet } from 'MainRoot/util/reduxToolkitUtil';
import { getRepositoryComponentsUrl } from 'MainRoot/util/CLMLocation';
import {
  selectComponentsRequestBody,
  selectCurrentPage,
  selectHasMoreResults,
  selectRepositoryInformation,
} from './repositoryResultsSummaryPageSelectors';
import { debounce } from 'debounce';
import { verifyFiltersAreValid } from 'MainRoot/util/validationUtil';

const REDUCER_NAME = 'repositoryResultsSummaryPage';
const PAGE_SIZE = 12;
const FILTER_DEBOUNCE_TIME = 500;

const initialState = {
  repositoryInfo: null,
  affectedComponentCount: null,
  criticalViolationCount: null,
  knownComponentCount: null,
  moderateViolationCount: null,
  quarantinedComponentCount: null,
  severeViolationCount: null,
  totalComponentCount: null,
  loadingSummaryTile: false,
  loadingRepositoryInformation: false,
  loadingRepositoryComponents: false,
  errorSummaryTile: null,
  errorRepositoryInformation: null,
  selectedMatchStateFilters: new Set([]),
  selectedViolationStateFilters: new Set([]),
  selectedThreatLevelFilters: [0, 10],
  showFilterPopover: false,
  errorComponentsTable: null,
  repositoryComponents: [],
  componentsRequestBody: {
    page: 1,
    pageSize: PAGE_SIZE,
    searchFilters: [],
    sortFields: [
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
      {
        sortableField: 'POLICY_NAME',
        asc: true,
        sortPriority: 3,
      },
      {
        sortableField: 'COMPONENT_COORDINATES',
        asc: true,
        sortPriority: 4,
      },
    ],
    aggregate: true,
    matchStateFilters: [],
    violationStateFilters: [],
    threatLevelFilters: [0, 10],
  },
  hasMoreResults: null,
  searchFiltersValues: {
    POLICY_NAME: '',
    QUARANTINE_TIME: '',
    COMPONENT_COORDINATES: '',
  },
  reEvaluateMaskSuccess: false,
  showMaskSuccessDialog: false,
};
const sortComponents = (sortData) => (dispatch, getState) => {
  const repository = selectRepositoryInformation(getState());
  dispatch(actions.setSorting(sortData));
  dispatch(actions.getRepositoryComponents(repository?.id));
};

const setSorting = (state, { payload }) => {
  const sortOption = state.componentsRequestBody.sortFields.find((sortOption) => sortOption.sortableField === payload);

  state.componentsRequestBody.sortFields = [
    {
      sortableField: sortOption.sortableField,
      asc: !sortOption.asc,
      sortPriority: 1,
    },
  ].concat(
    state.componentsRequestBody.sortFields
      .filter((option) => option.sortableField !== sortOption.sortableField)
      .map(({ sortableField, asc }, index) => {
        return { sortableField, asc, sortPriority: index + 2 };
      })
  );
  state.componentsRequestBody.page = 1;
};

const toggleAggregateAndGetRepositoryComponents = (state) => (dispatch, getState) => {
  const repository = selectRepositoryInformation(getState());
  dispatch(actions.toggleAggregate(state));
  dispatch(actions.getRepositoryComponents(repository?.id));
};

const toggleAggregate = (state) => {
  state.componentsRequestBody.aggregate = !state.componentsRequestBody.aggregate;
  state.componentsRequestBody.page = 1;
};

const getRepositoryInformation = createAsyncThunk(
  `${REDUCER_NAME}/getRepositoryInformation`,
  (repoId, { rejectWithValue }) => {
    return axios.get(getRepositoryInfoUrl(repoId)).then(prop('data')).catch(rejectWithValue);
  }
);

const reevaluateReport = createAsyncThunk(`${REDUCER_NAME}/reevaluateReport`, (repoId, { rejectWithValue }) => {
  return axios.post(getRepositoryEvaluateUrl(repoId)).then(prop('data')).catch(rejectWithValue);
});

const reevaluateReportPending = (state) => {
  state.showMaskSuccessDialog = true;
  state.reEvaluateMaskSuccess = false;
  state.errorReevaluate = false;
};

const reevaluateReportFulfilled = (state) => {
  state.showMaskSuccessDialog = true;
  state.reEvaluateMaskSuccess = true;
  state.errorReevaluate = false;
};

const reevaluateReportRejected = (state) => {
  state.showMaskSuccessDialog = false;
  state.reEvaluateMaskSuccess = false;
};

const changeViolationStateFilters = (state, { payload }) => {
  state.selectedViolationStateFilters = payload;
};

const changeMachstateFilters = (state, { payload }) => {
  state.selectedMatchStateFilters = payload;
};

const changeThreatLevelFilters = (state, { payload }) => {
  state.selectedThreatLevelFilters = payload;
};

const clearFilters = (state) => {
  state.componentsRequestBody.page = 1;
  state.selectedMatchStateFilters = new Set([]);
  state.selectedViolationStateFilters = new Set([]);
  state.selectedThreatLevelFilters = [0, 10];
  state.componentsRequestBody.matchStateFilters = Array.from(state.selectedMatchStateFilters);
  state.componentsRequestBody.violationStateFilters = Array.from(state.selectedViolationStateFilters);
  state.componentsRequestBody.threatLevelFilters = [...state.selectedThreatLevelFilters];
};

const CancelToken = axios.CancelToken;
let source = CancelToken.source();

const getRepositoryComponents = createAsyncThunk(
  `${REDUCER_NAME}/getRepositoryComponents`,
  (repoId, { getState, rejectWithValue }) => {
    source.cancel();
    source = CancelToken.source();
    const componentsRequestBody = selectComponentsRequestBody(getState());
    return axios
      .post(getRepositoryComponentsUrl('repository', repoId), componentsRequestBody, { cancelToken: source.token })
      .then(prop('data'))
      .catch(rejectWithValue);
  }
);

const applyFilters = (state) => {
  state.componentsRequestBody.page = 1;
  state.componentsRequestBody.matchStateFilters = Array.from(state.selectedMatchStateFilters);
  state.componentsRequestBody.violationStateFilters = Array.from(state.selectedViolationStateFilters);
  state.componentsRequestBody.threatLevelFilters = [...state.selectedThreatLevelFilters];
};

const getRepositorySummary = createAsyncThunk(`${REDUCER_NAME}/getRepositorySummary`, (repoId, { rejectWithValue }) => {
  return axios.get(getAuditReportSummary(repoId)).then(prop('data')).catch(rejectWithValue);
});

const loadData = (repoId) => (dispatch) => {
  return Promise.all([
    dispatch(getRepositoryInformation(repoId)),
    dispatch(getRepositorySummary(repoId)),
    dispatch(getRepositoryComponents(repoId)),
  ]);
};

const getRepositoryComponentsPending = (state) => {
  state.loadingRepositoryComponents = true;
  state.errorComponentsTable = null;
};

const getRepositoryComponentsFulfilled = (state, { payload }) => {
  state.loadingRepositoryComponents = false;
  state.errorComponentsTable = null;
  state.repositoryComponents = payload.repositoryResultsDetails;
  state.unsortedComponents = state.repositoryComponents;
  state.hasMoreResults = payload.hasNextPage;
};

const getRepositoryComponentsRejected = (state, { payload }) => {
  if (!axios.isCancel(payload)) {
    state.loadingRepositoryComponents = false;
    state.errorComponentsTable = Messages.getHttpErrorMessage(payload);
  }
};

const getRepositoryInformationFulfilled = (state, action) => {
  state.repositoryInfo = action.payload.repository;
  state.loadingRepositoryInformation = false;
};

const getRepositoryInformationRejected = (state, action) => {
  state.repositoryInfo = null;
  state.loadingRepositoryInformation = false;
  state.errorRepositoryInformation = Messages.getHttpErrorMessage(action.payload);
};

const getRepositorySummaryPending = (state) => {
  state.loadingSummaryTile = true;
  state.errorSummaryTile = null;
};

const getRepositoryInformationPending = (state) => {
  state.loadingRepositoryInformation = true;
  state.errorRepositoryInformation = null;
};

const getRepositorySummaryFulfilled = (state, action) => {
  const summary = action.payload;
  state.affectedComponentCount = summary.affectedComponentCount;
  state.criticalViolationCount = summary.criticalViolationCount;
  state.knownComponentCount = summary.knownComponentCount;
  state.moderateViolationCount = summary.moderateViolationCount;
  state.quarantinedComponentCount = summary.quarantinedComponentCount;
  state.severeViolationCount = summary.severeViolationCount;
  state.totalComponentCount = summary.totalComponentCount;
  state.loadingSummaryTile = false;
  state.errorSummaryTile = null;
};

const getRepositorySummaryRejected = (state, action) => {
  state.affectedComponentCount = initialState.affectedComponentCount;
  state.criticalViolationCount = initialState.criticalViolationCount;
  state.knownComponentCount = initialState.knownComponentCount;
  state.moderateViolationCount = initialState.moderateViolationCount;
  state.quarantinedComponentCount = initialState.quarantinedComponentCount;
  state.severeViolationCount = initialState.severeViolationCount;
  state.totalComponentCount = initialState.totalComponentCount;
  state.loadingSummaryTile = false;
  state.errorSummaryTile = Messages.getHttpErrorMessage(action.payload);
};

const increasePage = (state) => {
  state.componentsRequestBody.page += 1;
};

const decreasePage = (state) => {
  state.componentsRequestBody.page -= 1;
};

const loadNextPage = () => (dispatch, getState) => {
  const repository = selectRepositoryInformation(getState());
  const hasMoreResults = selectHasMoreResults(getState());
  if (hasMoreResults) {
    dispatch(actions.increasePage());
    dispatch(actions.getRepositoryComponents(repository?.id));
  }
};

const loadPreviousPage = () => (dispatch, getState) => {
  const repository = selectRepositoryInformation(getState());
  const currentPage = selectCurrentPage(getState());
  if (currentPage > 1) {
    dispatch(actions.decreasePage());
    dispatch(actions.getRepositoryComponents(repository?.id));
  }
};

const setFilter = (state, { payload: { filterName, filterValue } }) => {
  let searchFilter = state.componentsRequestBody.searchFilters.find((filter) => filter.filterableField === filterName);
  if (searchFilter) {
    searchFilter.value = filterValue;
  } else
    state.componentsRequestBody.searchFilters.push({
      filterableField: filterName,
      value: filterValue,
    });

  state.searchFiltersValues[filterName] = filterValue;
  state.componentsRequestBody.searchFilters = state.componentsRequestBody.searchFilters.filter(
    (searchFilter) => searchFilter.value !== ''
  );
  state.componentsRequestBody.page = 1;
};

const filterComponentsDebounce = debounce((dispatch, getState, repositoryId) => {
  const componentsRequestBody = selectComponentsRequestBody(getState());
  if (verifyFiltersAreValid(componentsRequestBody)) {
    dispatch(actions.getRepositoryComponents(repositoryId));
  }
}, FILTER_DEBOUNCE_TIME);

const searchComponents = (searchData) => (dispatch, getState) => {
  const repository = selectRepositoryInformation(getState());
  dispatch(actions.setFilter(searchData));
  filterComponentsDebounce(dispatch, getState, repository?.id);
};

export const repositoryResultsSummaryPageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    changeViolationStateFilters,
    changeMachstateFilters,
    changeThreatLevelFilters,
    clearFilters,
    setShowFilterPopover: propSet('showFilterPopover'),
    setFilter,
    setSorting,
    toggleAggregate,
    increasePage,
    decreasePage,
    applyFilters,
  },
  extraReducers: {
    [getRepositoryInformation.fulfilled]: getRepositoryInformationFulfilled,
    [getRepositoryInformation.rejected]: getRepositoryInformationRejected,
    [getRepositoryInformation.pending]: getRepositoryInformationPending,
    [getRepositorySummary.pending]: getRepositorySummaryPending,
    [getRepositorySummary.fulfilled]: getRepositorySummaryFulfilled,
    [getRepositorySummary.rejected]: getRepositorySummaryRejected,
    [getRepositoryComponents.pending]: getRepositoryComponentsPending,
    [getRepositoryComponents.fulfilled]: getRepositoryComponentsFulfilled,
    [getRepositoryComponents.rejected]: getRepositoryComponentsRejected,

    [reevaluateReport.pending]: reevaluateReportPending,
    [reevaluateReport.fulfilled]: reevaluateReportFulfilled,
    [reevaluateReport.rejected]: reevaluateReportRejected,
  },
});

export const actions = {
  ...repositoryResultsSummaryPageSlice.actions,
  getRepositoryInformation,
  getRepositorySummary,
  getRepositoryComponents,
  sortComponents,
  searchComponents,
  toggleAggregateAndGetRepositoryComponents,
  loadNextPage,
  loadPreviousPage,
  loadData,
  reevaluateRepository: reevaluateReport,
};
export default repositoryResultsSummaryPageSlice.reducer;
