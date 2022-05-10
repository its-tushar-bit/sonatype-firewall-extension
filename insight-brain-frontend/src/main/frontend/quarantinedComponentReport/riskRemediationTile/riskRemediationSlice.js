/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getQuarantinedComponentRemediationUrl, getQuarantinedComponentDetailsUrl } from 'MainRoot/util/CLMLocation';
import { pathSet } from 'MainRoot/util/reduxToolkitUtil';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectCurrentVersion, selectSelectedVersion } from './riskRemediationSelectors';

const REDUCER_NAME = 'quarantinedReportRiskRemediation';

const HTTP_CLIENT_CLOSED_REQUEST = 499;

export const initialState = {
  currentVersion: '',
  stageId: 'proxy',
  routeName: '',
  currentVersionComparisonData: null,
  selectedVersionComparisonData: null,
  versionExplorerData: {
    versions: [],
    remediation: {},
    source: '',
    loading: false,
    loadError: null,
    currentVersionDetails: null,
  },
  selectedVersionData: {
    loading: false,
    loadError: null,
    selectedVersionDetails: null,
    selectedVersion: null,
  },
};

function loadVersionExplorerDataRequested(state) {
  state.versionExplorerData.loading = true;
  state.versionExplorerData.loadError = null;
}

function loadVersionExplorerDataFulfilled(state, { payload }) {
  state.versionExplorerData.loading = false;
  state.versionExplorerData.loadError = null;
  state.versionExplorerData.versions = payload.componentVersionsData.allVersions;
  state.versionExplorerData.remediation = payload.componentVersionsData.remediation;
  state.versionExplorerData.source = payload.componentVersionsData.source;
  state.versionExplorerData.currentVersionDetails = payload.currentVersionDetails;
}

function loadVersionExplorerDataFailed(state, { payload }) {
  state.versionExplorerData.loading = false;
  state.versionExplorerData.loadError = Messages.getHttpErrorMessage(payload);
}

function loadComponentDetailsByVersionNumberRequested(state) {
  state.selectedVersionData.loading = true;
  state.selectedVersionData.loadError = null;
  state.selectedVersionData.selectedVersionDetails = null;
}

function loadComponentDetailsByVersionNumberFulfilled(state, { payload }) {
  state.selectedVersionData.loading = false;
  state.selectedVersionData.loadError = null;
  state.selectedVersionData.selectedVersionDetails = payload;
}

function loadComponentDetailsByVersionNumberFailed(state, { payload }) {
  if (payload.message === HTTP_CLIENT_CLOSED_REQUEST) {
    state.selectedVersionData.selectedVersionDetails = null;
  } else {
    state.selectedVersionData.loading = false;
    state.selectedVersionData.loadError = Messages.getHttpErrorMessage(payload);
    state.selectedVersionData.selectedVersionDetails = null;
  }
}

function resetSelectedVersionData(state) {
  state.selectedVersionData = {
    loading: false,
    loadError: null,
    selectedVersion: null,
    selectedVersionDetails: null,
  };
}

const loadVersionExplorerData = createAsyncThunk(
  `${REDUCER_NAME}/loadVersionExplorerData`,
  (token, { rejectWithValue }) => {
    const promises = [
      axios.get(getQuarantinedComponentRemediationUrl(token)),
      axios.get(getQuarantinedComponentDetailsUrl(token)),
    ];

    return Promise.all(promises)
      .then((results) => {
        const componentVersionsData = results[0].data;
        const currentVersionDetails = results[1].data;
        return { componentVersionsData, currentVersionDetails };
      })
      .catch(rejectWithValue);
  }
);

let loadSelectedVersionCancelToken = null;
const loadSelectedVersionData = createAsyncThunk(
  `${REDUCER_NAME}/loadSelectedVersionData`,
  (input, { getState, dispatch }) => {
    const token = input.token;
    const version = input.version;
    const prevSelectedVersion = selectSelectedVersion(getState());
    const currentVersion = selectCurrentVersion(getState());

    if (prevSelectedVersion === version) {
      return;
    }

    loadSelectedVersionCancelToken?.cancel(HTTP_CLIENT_CLOSED_REQUEST);

    if (currentVersion === version) {
      return dispatch(actions.resetSelectedVersionData());
    }

    loadSelectedVersionCancelToken = axios.CancelToken.source();

    dispatch(actions.setSelectedVersion(version));
    dispatch(
      loadComponentDetailsByVersionNumber({
        token: token,
        cancelToken: loadSelectedVersionCancelToken.token,
      })
    );
  }
);

const loadComponentDetailsByVersionNumber = createAsyncThunk(
  `${REDUCER_NAME}/loadComponentDetailsByVersionNumber`,
  (input, { getState, rejectWithValue }) => {
    const token = input.token;
    const cancelToken = input.cancelToken;

    return axios
      .get(getQuarantinedComponentDetailsUrl(token, selectSelectedVersion(getState())), { cancelToken })
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

const loadVersionExplorerDataSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setSelectedVersion: pathSet(['selectedVersionData', 'selectedVersion']),
    resetSelectedVersionData,
  },
  extraReducers: {
    [loadVersionExplorerData.pending]: loadVersionExplorerDataRequested,
    [loadVersionExplorerData.fulfilled]: loadVersionExplorerDataFulfilled,
    [loadVersionExplorerData.rejected]: loadVersionExplorerDataFailed,
    [loadComponentDetailsByVersionNumber.pending]: loadComponentDetailsByVersionNumberRequested,
    [loadComponentDetailsByVersionNumber.fulfilled]: loadComponentDetailsByVersionNumberFulfilled,
    [loadComponentDetailsByVersionNumber.rejected]: loadComponentDetailsByVersionNumberFailed,
  },
});

export default loadVersionExplorerDataSlice.reducer;
export const actions = {
  ...loadVersionExplorerDataSlice.actions,
  loadVersionExplorerData,
  loadSelectedVersionData,
  loadComponentDetailsByVersionNumber,
};
