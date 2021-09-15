/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import {
  getVersionGraphUrl,
  getApplicationReportsUrl,
  getInnerSourceComponentLatestVersionUrl,
} from '../../util/CLMLocation';
import { BASE_URL } from '../../util/urlUtil';
import { Messages } from '../../util/CommonServices';
import {
  selectVersionExplorerRequestData,
  selectInnerSourceProducerUrl,
  selectLatestInnerSourceComponentVersion,
  selectInsufficientPermission,
} from './overviewSelectors';
import { comparator, path, sort } from 'ramda';
import { selectSelectedComponent } from '../../applicationReport/applicationReportSelectors';
import { pathSet } from '../../util/reduxToolkitUtil';
import { togglePath } from '../../util/jsUtil';

const REDUCER_NAME = 'componentDetailsOverview';

const initialState = {
  remediation: null,
  graphExplorerData: {
    loading: false,
    loadError: null,
    data: null,
  },
  innerSourceProducerData: {
    reportUrl: '',
    latestInnerSourceComponentVersion: '',
    insufficientPermission: false,
    loading: false,
    loadError: null,
    showInnerSourcePermissionsModal: false,
    showInnerSourceProducerReportModal: false,
  },
};

const stagesOrder = {
  operate: 1,
  release: 2,
  stage: 3,
  build: 4,
  develop: 5,
  proxy: 6,
};

const getStageOrder = (report) => {
  return stagesOrder[report.stage] !== undefined ? stagesOrder[report.stage] : 7;
};

const byStage = comparator((reportA, reportB) => getStageOrder(reportA) < getStageOrder(reportB));

const loadInnerSourceProducerData = createAsyncThunk(
  `${REDUCER_NAME}/loadInnerSourceProducerData`,
  (_, { dispatch, getState, rejectWithValue }) => {
    const { innerSource, innerSourceData, componentIdentifier } = selectSelectedComponent(getState());
    const ownerApplicationId = path(['0', 'ownerApplicationId'], innerSourceData);
    if (!innerSource || !ownerApplicationId) {
      return;
    }

    return axios
      .get(getApplicationReportsUrl(ownerApplicationId))
      .then(({ data }) => {
        const lastInnerSourceReportData = sort(byStage, data)[0],
          // cannot use uriTemplate from CLMLocation as it escapes the url
          url = `${BASE_URL}/${lastInnerSourceReportData.latestReportHtmlUrl}`;

        dispatch(actions.setInnerSourceProducerReportUrl(url));
        dispatch(actions.setInsufficientPermission(false));
        return axios.get(getInnerSourceComponentLatestVersionUrl(componentIdentifier));
      })
      .then(({ data: latestInnerSourceComponentVersion }) => {
        dispatch(actions.setLatestInnerSourceComponentVersion(latestInnerSourceComponentVersion));
      })
      .catch((error) => {
        if (error.response.status === 403) {
          return dispatch(actions.setInsufficientPermission(true));
        } else {
          return rejectWithValue(error.response.data);
        }
      });
  }
);

const loadInnerSourceProducerDataFulfilled = (state) => {
  state.innerSourceProducerData.loading = false;
  state.innerSourceProducerData.loadError = null;
};

const loadInnerSourceProducerDataRequested = (state) => {
  state.innerSourceProducerData.loading = true;
  state.innerSourceProducerData.loadError = null;
};

const loadInnerSourceProducerDataFailed = (state, { payload }) => {
  state.innerSourceProducerData.loading = false;
  state.innerSourceProducerData.loadError = Messages.getHttpErrorMessage(payload);
};

const loadRequested = (state) => {
  return {
    ...state,
    graphExplorerData: {
      ...state.graphExplorerData,
      loading: true,
      loadError: null,
    },
  };
};

const loadFulfilled = (state, { payload }) => ({
  ...state,
  remediation: payload.data.remediation,
  graphExplorerData: {
    loading: false,
    loadError: null,
    data: {
      versions: payload.data.allVersions,
    },
  },
});

function loadFailed(state, { payload }) {
  return {
    ...state,
    graphExplorerData: {
      ...state.graphExplorerData,
      loading: false,
      loadError: Messages.getHttpErrorMessage(payload),
    },
  };
}

const loadVersionGraphData = createAsyncThunk(
  `${REDUCER_NAME}/loadVersionGraphData`,
  (_, { getState, rejectWithValue }) => {
    return axios
      .get(getVersionGraphUrl(selectVersionExplorerRequestData(getState())))
      .then((result) => result)
      .catch(rejectWithValue);
  }
);

const openInnerSourceProducerReport = () => {
  return (dispatch, getState) => {
    const state = getState();
    const insufficientPermission = selectInsufficientPermission(state);

    if (insufficientPermission) {
      dispatch(actions.toggleInnerSourcePermissionsModal());
      return;
    }

    const currentComponentVersion = path(
      ['componentIdentifier', 'coordinates', 'version'],
      selectSelectedComponent(state)
    );
    const latestComponentVersion = selectLatestInnerSourceComponentVersion(state);
    const newVersionShownInLatestReport = currentComponentVersion && currentComponentVersion !== latestComponentVersion;

    if (newVersionShownInLatestReport) {
      dispatch(actions.toggleInnerSourceProducerReportModal());
    } else {
      const reportUrl = selectInnerSourceProducerUrl(state);
      window.open(reportUrl, '_blank');
    }
  };
};

const componentDetailsOverviewSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    toggleInnerSourceProducerReportModal: togglePath(['innerSourceProducerData', 'showInnerSourceProducerReportModal']),
    toggleInnerSourcePermissionsModal: togglePath(['innerSourceProducerData', 'showInnerSourcePermissionsModal']),
    setInsufficientPermission: pathSet(['innerSourceProducerData', 'insufficientPermission']),
    setInnerSourceProducerReportUrl: pathSet(['innerSourceProducerData', 'reportUrl']),
    setLatestInnerSourceComponentVersion: pathSet(['innerSourceProducerData', 'latestInnerSourceComponentVersion']),
  },
  extraReducers: {
    [loadVersionGraphData.pending]: loadRequested,
    [loadVersionGraphData.fulfilled]: loadFulfilled,
    [loadVersionGraphData.rejected]: loadFailed,
    [loadInnerSourceProducerData.pending]: loadInnerSourceProducerDataRequested,
    [loadInnerSourceProducerData.fulfilled]: loadInnerSourceProducerDataFulfilled,
    [loadInnerSourceProducerData.rejected]: loadInnerSourceProducerDataFailed,
  },
});

export default componentDetailsOverviewSlice.reducer;
export const actions = {
  ...componentDetailsOverviewSlice.actions,
  loadVersionGraphData,
  loadInnerSourceProducerData,
  openInnerSourceProducerReport,
};
