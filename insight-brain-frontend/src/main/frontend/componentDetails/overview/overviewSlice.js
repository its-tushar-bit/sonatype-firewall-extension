/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import {
  getVersionGraphUrl,
  getComponentDetailsUrl,
  getApplicationReportsUrl,
  getInnerSourceComponentLatestVersionUrl,
  getPolicyEvaluationTimestampUrl,
} from '../../util/CLMLocation';
import { BASE_URL } from '../../util/urlUtil';
import { Messages } from '../../utilAngular/CommonServices';
import {
  selectComponentDetailsRequestData,
  selectVersionExplorerRequestData,
  selectInnerSourceProducerUrl,
  selectLatestInnerSourceComponentVersion,
  selectInsufficientPermission,
  selectComponentDetailsSelectedRequestData,
  selectSelectedVersion,
  selectCurrentVersion,
} from './overviewSelectors';
import { always, comparator, path, sort } from 'ramda';
import { selectSelectedComponent } from '../../applicationReport/applicationReportSelectors';
import { pathSet } from '../../util/reduxToolkitUtil';
import { togglePath } from '../../util/jsUtil';
import { toggleBooleanProp } from '../../util/reduxUtil';
import { SELECT_COMPONENT } from 'MainRoot/applicationReport/applicationReportActions';

import {
  selectComponentDetailsSelectedRequestData as firewallSelectComponentDetailsSelectedRequestData,
  selectComponentDetailsFromParams,
} from 'MainRoot/firewall/firewallComponentDetailsPage/overview/firewallOverviewSelectors';

export const HTTP_CLIENT_CLOSED_REQUEST = 499;

export const REDUCER_NAME = 'componentDetailsOverview';

const initialState = {
  versionExplorerData: {
    loading: false,
    loadError: null,
    versions: null,
    remediation: null,
    currentVersionDetails: null,
    sourceResponse: null,
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
  showSimilarMatchesPopover: false,
  showComponentCoordinatesPopover: false,
  selectedVersionData: {
    loading: false,
    loadError: null,
    selectedVersionDetails: null,
    selectedVersion: null,
  },
  expanded: false,
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

export const loadRequested = (state) => {
  return {
    ...state,
    versionExplorerData: {
      ...state.versionExplorerData,
      loading: true,
      loadError: null,
    },
    expanded: false,
  };
};

export const loadFulfilled = (state, { payload }) => {
  return {
    ...state,
    versionExplorerData: {
      loading: false,
      loadError: null,
      versions: payload.componentVersionsData.allVersions,
      remediation: payload.componentVersionsData.remediation,
      currentVersionDetails: payload.currentVersionDetails,
      sourceResponse: payload.componentVersionsData.sourceResponse,
    },
  };
};

export function loadFailed(state, { payload }) {
  if (payload.message === HTTP_CLIENT_CLOSED_REQUEST) {
    state.versionExplorerData.versions = null;
    state.versionExplorerData.remediation = null;
    state.versionExplorerData.currentVersionDetails = null;
    state.versionExplorerData.sourceResponse = null;
  } else {
    state.versionExplorerData.loading = false;
    state.versionExplorerData.loadError = Messages.getHttpErrorMessage(payload);
  }
}

export function loadComponentDetailsByVerionsNumberRequested(state) {
  state.selectedVersionData.loading = true;
  state.selectedVersionData.loadError = null;
  state.selectedVersionData.selectedVersionDetails = null;
}
export function loadComponentDetailsByVerionsNumberFulfilled(state, { payload }) {
  state.selectedVersionData.loading = false;
  state.selectedVersionData.loadError = null;
  state.selectedVersionData.selectedVersionDetails = payload;
}

export function loadComponentDetailsByVerionsNumberFailed(state, { payload }) {
  if (payload.message === HTTP_CLIENT_CLOSED_REQUEST) {
    state.selectedVersionData.selectedVersionDetails = null;
  } else {
    state.selectedVersionData.loading = false;
    state.selectedVersionData.loadError = Messages.getHttpErrorMessage(payload);
    state.selectedVersionData.selectedVersionDetails = null;
  }
}

export function resetSelectedVersionData(state) {
  state.selectedVersionData = {
    loading: false,
    loadError: null,
    selectedVersion: null,
    selectedVersionDetails: null,
  };
}

let loadVersionExplorerDataCancelToken = null;
const loadVersionExplorerData = createAsyncThunk(
  `${REDUCER_NAME}/loadVersionExplorerData`,
  (_, { getState, dispatch }) => {
    dispatch(actions.resetSelectedVersionData());
    const { currentParams, prevParams } = getState().router;

    if (prevParams.hash && prevParams.hash !== currentParams.hash) {
      loadVersionExplorerDataCancelToken?.cancel(HTTP_CLIENT_CLOSED_REQUEST);
    }

    loadVersionExplorerDataCancelToken = axios.CancelToken.source();
    dispatch(loadVersionExplorerDataWithCancelToken(loadVersionExplorerDataCancelToken.token));
  }
);

const loadVersionExplorerDataWithCancelToken = createAsyncThunk(
  `${REDUCER_NAME}/loadVersionExplorerDataWithCancelToken`,
  (cancelToken, { getState, rejectWithValue }) => {
    const requestData = selectComponentDetailsRequestData(getState());
    const promises = [
      axios.get(getVersionGraphUrl(selectVersionExplorerRequestData(getState())), { cancelToken }),
      axios.get(getComponentDetailsUrl(requestData), { cancelToken }),
    ];

    return Promise.all(promises)
      .then((results) => ({
        componentVersionsData: results[0].data,
        currentVersionDetails: results[1].data,
      }))
      .catch(rejectWithValue);
  }
);

let loadSelectedVersionCancelToken = null;
const loadSelectedVersionData = createAsyncThunk(
  `${REDUCER_NAME}/loadSelectedVersionData`,
  (version, { getState, dispatch }) => {
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
    dispatch(loadComponentDetailsByVerionsNumber(loadSelectedVersionCancelToken.token));
  }
);

const loadComponentDetailsByVerionsNumber = createAsyncThunk(
  `${REDUCER_NAME}/loadComponentDetailsByVerionsNumber`,
  (cancelToken, { getState, rejectWithValue }) => {
    return axios
      .get(getComponentDetailsUrl(selectComponentDetailsSelectedRequestData(getState())), { cancelToken })
      .then(({ data }) => data)
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

/*
Firewall Overview slices are almost the same as overview slices, the same structure on the store (componentDetailsOverview)
and in some cases the same selectors and actions, the main difference between them is the absence of application report for 
firewall/repository components, and in order to not repeat the same data structure in another store/state object (for the 
case of componentDetailsOverview) this two group of actions, selectors and reducers for firewall overview and application 
report overview will be shared.

If the firewall overview slices are separated in different file, that would colide with the idea of having a single 
REDUCER_NAME per file, and changing the REDUCER_NAME would leads to repetition on selectors, actions, reducer code and 
a repeated componentDetailsOverview store/state structure.

For further context please look for the conversation and refered comments on the pull request: 
https://github.com/sonatype/insight-brain/pull/8077#discussion_r903057471
*/

let firewallLoadVersionExplorerDataCancelToken = null;
const firewallLoadVersionExplorerData = createAsyncThunk(
  `${REDUCER_NAME}/firewallLoadVersionExplorerData`,
  (_, { getState, dispatch }) => {
    dispatch(actions.resetSelectedVersionData());
    const { currentParams, prevParams } = getState().router;

    if (prevParams.hash && prevParams.hash !== currentParams.hash) {
      firewallLoadVersionExplorerDataCancelToken?.cancel(HTTP_CLIENT_CLOSED_REQUEST);
    }

    firewallLoadVersionExplorerDataCancelToken = axios.CancelToken.source();
    dispatch(firewallLoadVersionExplorerDataWithCancelToken(firewallLoadVersionExplorerDataCancelToken.token));
  }
);

const firewallLoadVersionExplorerDataWithCancelToken = createAsyncThunk(
  `${REDUCER_NAME}/firewallLoadVersionExplorerDataWithCancelToken`,
  (cancelToken, { getState, rejectWithValue }) => {
    const requestData = selectComponentDetailsFromParams(getState());

    const promises = [
      axios.get(getVersionGraphUrl(selectComponentDetailsFromParams(getState()), { cancelToken })),
      axios.get(getComponentDetailsUrl(requestData), { cancelToken }),
      axios.get(getPolicyEvaluationTimestampUrl(requestData.ownerId, requestData.componentIdentifier), { cancelToken }),
    ];

    return Promise.all(promises)
      .then((results) => ({
        componentVersionsData: results[0].data,
        currentVersionDetails: { ...results[1].data, policyEvaluationTimestamps: results[2].data },
      }))
      .catch(rejectWithValue);
  }
);

let firewallLoadSelectedVersionCancelToken = null;
const firewallLoadSelectedVersionData = createAsyncThunk(
  `${REDUCER_NAME}/firewallLoadSelectedVersionData`,
  (version, { getState, dispatch }) => {
    const prevSelectedVersion = selectSelectedVersion(getState());
    const currentVersion = selectCurrentVersion(getState());

    if (prevSelectedVersion === version) {
      return;
    }

    firewallLoadSelectedVersionCancelToken?.cancel(HTTP_CLIENT_CLOSED_REQUEST);

    if (currentVersion === version) {
      return dispatch(actions.resetSelectedVersionData());
    }

    firewallLoadSelectedVersionCancelToken = axios.CancelToken.source();

    dispatch(actions.setSelectedVersion(version));
    dispatch(firewallLoadComponentDetailsByVerionsNumber(firewallLoadSelectedVersionCancelToken.token));
  }
);

const firewallLoadComponentDetailsByVerionsNumber = createAsyncThunk(
  `${REDUCER_NAME}/firewallLoadComponentDetailsByVerionsNumber`,
  (cancelToken, { getState, rejectWithValue }) => {
    const requestData = firewallSelectComponentDetailsSelectedRequestData(getState());
    const promises = [
      axios.get(getComponentDetailsUrl(requestData), { cancelToken }),
      axios.get(getPolicyEvaluationTimestampUrl(requestData.ownerId, requestData.componentIdentifier), { cancelToken }),
    ];

    return Promise.all(promises)
      .then((results) => {
        return { ...results[0].data, policyEvaluationTimestamps: results[1].data };
      })
      .catch(rejectWithValue);
  }
);

const componentDetailsOverviewSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    toggleInnerSourceProducerReportModal: togglePath(['innerSourceProducerData', 'showInnerSourceProducerReportModal']),
    toggleInnerSourcePermissionsModal: togglePath(['innerSourceProducerData', 'showInnerSourcePermissionsModal']),
    setInsufficientPermission: pathSet(['innerSourceProducerData', 'insufficientPermission']),
    setInnerSourceProducerReportUrl: pathSet(['innerSourceProducerData', 'reportUrl']),
    setLatestInnerSourceComponentVersion: pathSet(['innerSourceProducerData', 'latestInnerSourceComponentVersion']),
    toggleShowSimilarMatches: toggleBooleanProp('showSimilarMatchesPopover'),
    toggleShowComponentCoordinatesPopover: toggleBooleanProp('showComponentCoordinatesPopover'),
    setSelectedVersion: pathSet(['selectedVersionData', 'selectedVersion']),
    resetSelectedVersionData,
    toggleAncestorsList: toggleBooleanProp('expanded'),
  },
  extraReducers: {
    [loadVersionExplorerDataWithCancelToken.pending]: loadRequested,
    [loadVersionExplorerDataWithCancelToken.fulfilled]: loadFulfilled,
    [loadVersionExplorerDataWithCancelToken.rejected]: loadFailed,
    [loadComponentDetailsByVerionsNumber.pending]: loadComponentDetailsByVerionsNumberRequested,
    [loadComponentDetailsByVerionsNumber.fulfilled]: loadComponentDetailsByVerionsNumberFulfilled,
    [loadComponentDetailsByVerionsNumber.rejected]: loadComponentDetailsByVerionsNumberFailed,
    [loadInnerSourceProducerData.pending]: loadInnerSourceProducerDataRequested,
    [loadInnerSourceProducerData.fulfilled]: loadInnerSourceProducerDataFulfilled,
    [loadInnerSourceProducerData.rejected]: loadInnerSourceProducerDataFailed,
    [firewallLoadVersionExplorerDataWithCancelToken.pending]: loadRequested,
    [firewallLoadVersionExplorerDataWithCancelToken.fulfilled]: loadFulfilled,
    [firewallLoadVersionExplorerDataWithCancelToken.rejected]: loadFailed,
    [firewallLoadComponentDetailsByVerionsNumber.pending]: loadComponentDetailsByVerionsNumberRequested,
    [firewallLoadComponentDetailsByVerionsNumber.fulfilled]: loadComponentDetailsByVerionsNumberFulfilled,
    [firewallLoadComponentDetailsByVerionsNumber.rejected]: loadComponentDetailsByVerionsNumberFailed,
    [SELECT_COMPONENT]: always(initialState),
  },
});

export default componentDetailsOverviewSlice.reducer;
export const actions = {
  ...componentDetailsOverviewSlice.actions,
  loadVersionExplorerData,
  loadVersionExplorerDataWithCancelToken,
  loadSelectedVersionData,
  loadInnerSourceProducerData,
  openInnerSourceProducerReport,
  loadComponentDetailsByVerionsNumber,
  firewallLoadVersionExplorerData,
  firewallLoadVersionExplorerDataWithCancelToken,
  firewallLoadSelectedVersionData,
  firewallLoadComponentDetailsByVerionsNumber,
};
