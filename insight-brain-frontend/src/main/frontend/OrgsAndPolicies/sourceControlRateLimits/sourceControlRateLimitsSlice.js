/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getSourceControlRateLimitsUrl } from 'MainRoot/util/CLMLocation';
import { ascend, clone, descend, prop, sortWith } from 'ramda';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import moment from 'moment';
const REDUCER_NAME = 'sourceControlRateLimits';

export const initialState = {
  loading: false,
  loadError: null,
  lastUpdated: null,
  serverData: null,
  data: null,
  userRateLimitsExpanded: {},
  userDefiningOwnersExpanded: {},
  userAssociatedApplicationsExpanded: {},
  sortColumn: 'user',
  sortDirection: 'asc',
};

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { getState, rejectWithValue }) => {
  const { ownerType, ownerId } = selectRouterCurrentParams(getState());
  return axios.get(getSourceControlRateLimitsUrl(ownerType, ownerId)).then(prop('data')).catch(rejectWithValue);
});

function loadRequested(state) {
  return {
    ...state,
    loading: true,
    loadError: null,
  };
}

function loadFulfilled(state, { payload }) {
  return sort({
    ...state,
    loading: false,
    serverData: addExtraData(payload),
    lastUpdated: new Date(),
  });
}

function loadFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    loadError: Messages.getHttpErrorMessage(payload),
  };
}

function addExtraData(data) {
  data.userRateLimits.map((userRateLimit) => {
    userRateLimit.averageRemainingPercent = 0;
    userRateLimit.rateLimits.map((rateLimit) => {
      rateLimit.remainingPercent = (rateLimit.remaining * 100.0) / rateLimit.limit;
      rateLimit.timeUntilReset = moment(rateLimit.resetEpochTime * 1000).from(Date.now(), true);
      userRateLimit.averageRemainingPercent += rateLimit.remainingPercent;
    });
    userRateLimit.averageRemainingPercent /= userRateLimit.rateLimits.length;
  });
  return data;
}

function setSort(state, { payload }) {
  const newSortColumn = payload;
  const newSortDirection = getSortDirection(state.sortColumn, newSortColumn, state.sortDirection);
  return sort({ ...state, sortColumn: newSortColumn, sortDirection: newSortDirection });
}

function getSortDirection(sortColumn, newSortColumn, sortDirection) {
  if (sortColumn !== newSortColumn) {
    return 'asc';
  }
  if (sortDirection === 'asc') {
    return 'desc';
  }
  if (sortDirection === 'desc') {
    return 'asc';
  }
}

function sort(state) {
  let newData = clone(state.serverData);
  if (state.sortDirection === 'desc') {
    newData.userRateLimits = sortWith([descend(prop(state.sortColumn))])(newData.userRateLimits);
  }
  if (state.sortDirection === 'asc') {
    newData.userRateLimits = sortWith([ascend(prop(state.sortColumn))])(newData.userRateLimits);
  }
  return { ...state, data: newData };
}

function toggleUserRateLimitsExpanded(state, { payload }) {
  return {
    ...state,
    userRateLimitsExpanded: {
      ...state.userRateLimitsExpanded,
      [payload]: !state.userRateLimitsExpanded[payload],
    },
  };
}

function toggleUserDefiningOwnersExpanded(state, { payload }) {
  return {
    ...state,
    userDefiningOwnersExpanded: {
      ...state.userDefiningOwnersExpanded,
      [payload]: !state.userDefiningOwnersExpanded[payload],
    },
  };
}

function toggleUserAssociatedApplicationsExpanded(state, { payload }) {
  return {
    ...state,
    userAssociatedApplicationsExpanded: {
      ...state.userAssociatedApplicationsExpanded,
      [payload]: !state.userAssociatedApplicationsExpanded[payload],
    },
  };
}

const sourceControlRateLimitsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setSort,
    toggleUserRateLimitsExpanded,
    toggleUserDefiningOwnersExpanded,
    toggleUserAssociatedApplicationsExpanded,
  },
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
  },
});

export default sourceControlRateLimitsSlice.reducer;
export const actions = {
  ...sourceControlRateLimitsSlice.actions,
  load,
};
