/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getPrioritiesPageTableData, getReportMetadataUrl, getVersionGraphUrl } from 'MainRoot/util/CLMLocation';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { isNil, keys } from 'ramda';
import { selectPrioritiesPageSlice } from 'MainRoot/development/prioritiesPage/selectors/prioritiesPageSelectors';

export const PRIORITIES_PAGE_REDUCER_NAME = 'prioritiesPage';

const TABLE_PAGE_SIZE = 10;

const loadTableDataRequested = (state) => {
  return {
    ...state,
    topPrioritiesData: null,
    additionalPrioritiesData: null,
    loadingTableData: true,
    loadErrorTableData: null,
  };
};

const loadTableDataFulfilled = (state, { payload }) => {
  const {
    topPriorities,
    additionalPriorities: { total, page, pageSize, pageCount, results },
  } = payload;
  return {
    ...state,
    topPrioritiesData: topPriorities,
    additionalPrioritiesData: results,
    loadingTableData: false,
    loadErrorTableData: null,
    pageSize,
    pageCount,
    page,
    total,
  };
};

const loadTableDataFailed = (state, { payload }) => {
  return {
    ...state,
    topPrioritiesData: null,
    additionalPrioritiesData: null,
    loadingTableData: false,
    loadErrorTableData: Messages.getHttpErrorMessage(payload),
  };
};

const loadTableData = createAsyncThunk(
  `${PRIORITIES_PAGE_REDUCER_NAME}/loadTableData`,
  (_, { getState, rejectWithValue }) => {
    const state = getState();
    const { publicAppId, scanId } = selectRouterCurrentParams(state);
    const tableDataUrl = getPrioritiesPageTableData(publicAppId, scanId);
    const { page } = selectPrioritiesPageSlice(state);

    return axios
      .get(tableDataUrl, { params: { pageSize: TABLE_PAGE_SIZE, page } })
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

const loadMetadataRequested = (state) => {
  return {
    ...state,
    metadata: null,
    loadingMetadata: true,
    loadErrorMetadata: null,
  };
};

const loadMetadataFulfilled = (state, { payload }) => {
  return {
    ...state,
    metadata: payload,
    loadingMetadata: false,
    loadErrorMetadata: null,
  };
};

const loadMetadataFailed = (state, { payload }) => {
  return {
    ...state,
    metadata: null,
    loadingMetadata: false,
    loadErrorMetadata: Messages.getHttpErrorMessage(payload),
  };
};

const loadMetadata = createAsyncThunk(
  `${PRIORITIES_PAGE_REDUCER_NAME}/loadMetadata`,
  (_, { getState, rejectWithValue }) => {
    const state = getState();
    const { publicAppId, scanId } = selectRouterCurrentParams(state);
    const metadataUrl = getReportMetadataUrl(publicAppId, scanId);

    return axios
      .get(metadataUrl)
      .then(({ data }) => ({ ...data, scanId }))
      .catch(rejectWithValue);
  }
);

const loadRecommendations = createAsyncThunk(
  `${PRIORITIES_PAGE_REDUCER_NAME}/loadRecommendations`,
  (requestData, { rejectWithValue }) => {
    return axios
      .get(getVersionGraphUrl(requestData))
      .then(({ data }) => {
        return {
          [requestData.hash]: { remediation: data.remediation },
        };
      })
      .catch(rejectWithValue);
  }
);

const loadRecommendationsRequested = (state, { meta }) => {
  return {
    ...state,
    recommendations: {
      ...state.recommendations,
      [meta.arg.hash]: { loading: true, error: null, remediation: null },
    },
  };
};

const loadRecommendationsFulfilled = (state, { payload }) => {
  const hash = keys(payload)[0];
  return {
    ...state,
    recommendations: {
      ...state.recommendations,
      [hash]: { loading: false, error: null, ...payload[hash] },
    },
  };
};

const loadRecommendationsFailed = (state, { payload, meta }) => {
  return {
    ...state,
    recommendations: {
      ...state.recommendations,
      [meta.arg.hash]: { loading: false, error: Messages.getHttpErrorMessage(payload), remediation: null },
    },
  };
};

export const checkIfLoadRecommendationsNeeded = (requestData) => (dispatch, getState) => {
  const state = getState();
  const { recommendations } = selectPrioritiesPageSlice(state);
  const { hash } = requestData;

  if (!isNil(recommendations[hash]?.remediation)) {
    return Promise.resolve({
      [hash]: recommendations[hash],
    });
  }

  return dispatch(loadRecommendations(requestData));
};

const setPage = (state, { payload }) => {
  return {
    ...state,
    page: payload + 1,
    loadingTableData: true,
  };
};

const resetState = (state) => {
  return {
    ...state,
    recommendations: {},
  };
};

const prioritiesPageSlice = createSlice({
  name: PRIORITIES_PAGE_REDUCER_NAME,
  initialState: initialState(),
  reducers: { resetState, setPage },
  extraReducers: {
    [loadTableData.pending]: loadTableDataRequested,
    [loadTableData.fulfilled]: loadTableDataFulfilled,
    [loadTableData.rejected]: loadTableDataFailed,
    [loadMetadata.pending]: loadMetadataRequested,
    [loadMetadata.fulfilled]: loadMetadataFulfilled,
    [loadMetadata.rejected]: loadMetadataFailed,
    [loadRecommendations.pending]: loadRecommendationsRequested,
    [loadRecommendations.fulfilled]: loadRecommendationsFulfilled,
    [loadRecommendations.rejected]: loadRecommendationsFailed,
  },
});

function initialState() {
  return {
    topPrioritiesData: null,
    additionalPrioritiesData: null,
    loadingTableData: false,
    loadErrorTableData: null,
    metadata: null,
    loadingMetadata: false,
    loadErrorMetaData: null,
    recommendations: {},
    pageSize: TABLE_PAGE_SIZE,
    pageCount: 1,
    page: 1,
    total: null,
  };
}

export default prioritiesPageSlice.reducer;

export const actions = {
  ...prioritiesPageSlice.actions,
  loadTableData,
  loadMetadata,
  loadRecommendations,
  checkIfLoadRecommendationsNeeded,
};
