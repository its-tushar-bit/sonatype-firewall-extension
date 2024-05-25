/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import {
  getApplicationSummaryUrl,
  getSbomComponentDependencyTreeUrl,
  getSbomComponentDetailsUrl,
  getSbomVulnerabibilityAnalysisReferenceData,
  getVulnerabilityJsonDetailUrl,
  getVulnerabilityOverrideUrl,
  saveSbomVulnerabilityAnnotationUrl,
} from 'MainRoot/util/CLMLocation';
import { checkPermissions } from 'MainRoot/util/authorizationUtil';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';
import { always } from 'ramda';

const REDUCER_NAME = 'sbomComponentDetailsPage';

export const initialState = {
  loading: false,
  loadError: null,
  loadingDependencyTree: false,
  loadDependencyTreeError: null,
  loadingVulnerabilityDetail: false,
  loadVulnerabilityDetailError: null,
  submitMaskStateForVexAnnotationForm: null,
  loadSaveVexAnnotationFormError: null,
  loadingVulnerabilityAnalysisReferenceData: false,
  loadVulnerabilityAnalysisReferenceDataError: null,
  publicAppId: null,
  componentDetails: null,
  dependencyTreeSubset: null,
  vulnerabilityDetails: null,
  vulnerabilityAnalysisReferenceData: {
    responses: [],
    justifications: [],
    states: [],
  },
};

const loadComponentDependencyTreeDataRequested = (state) => {
  state.loadingDependencyTree = true;
  state.loadDependencyTreeError = null;
};

const loadComponentDependencyTreeDataFulfilled = (state, { payload }) => {
  state.loadingDependencyTree = false;
  state.loadDependencyTreeError = null;
  state.dependencyTreeSubset = payload;
};

const loadComponentDependencyTreeDataRejected = (state, { payload }) => {
  state.loadingDependencyTree = false;
  state.loadDependencyTreeError = payload.response.data;
  state.dependencyTreeSubset = null;
};

const loadComponentDetailsRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadComponentDetailsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.componentDetails = payload;
};

const loadComponentDetailsFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = payload.response.data;
  state.componentDetails = null;
};

const loadVulnerabilityDetailsRequested = (state) => {
  state.loadingVulnerabilityDetail = true;
  state.loadVulnerabilityDetailError = null;
};

const loadVulnerabilityDetailsFulfilled = (state, { payload }) => {
  state.loadingVulnerabilityDetail = false;
  state.loadVulnerabilityDetailError = null;
  state.vulnerabilityDetails = payload;
};

const loadVulnerabilityDetailsRejected = (state, { payload }) => {
  state.loadingVulnerabilityDetail = false;
  state.loadVulnerabilityDetailError = payload.response.data;
  state.vulnerabilityDetails = null;
};

const saveVexAnnotationRequested = function (state) {
  state.submitMaskStateForVexAnnotationForm = false;
  state.loadSaveVexAnnotationFormError = null;
};

const saveVexAnnotationFulfilled = function (state) {
  state.submitMaskStateForVexAnnotationForm = true;
  state.loadSaveVexAnnotationFormError = null;
};

const saveVexAnnotationRejected = function (state, { payload }) {
  state.submitMaskStateForVexAnnotationForm = null;
  state.loadSaveVexAnnotationFormError = payload.message;
};

const getVulnerabilityAnalysisReferenceDataRequested = function (state) {
  state.loadingVulnerabilityAnalysisReferenceData = true;
  state.loadVulnerabilityAnalysisReferenceDataError = null;
  state.vulnerabilityAnalysisReferenceData = { responses: [], justifications: [], states: [] };
};
const getVulnerabilityAnalysisReferenceDataFulfilled = function (state, { payload }) {
  state.loadingVulnerabilityAnalysisReferenceData = false;
  state.loadVulnerabilityAnalysisReferenceDataError = null;
  state.vulnerabilityAnalysisReferenceData = { ...payload };
};

const getVulnerabilityAnalysisReferenceDataRejected = function (state, { payload }) {
  state.loadingVulnerabilityAnalysisReferenceData = false;
  state.loadVulnerabilityAnalysisReferenceDataError = payload.response.data;
};

const loadComponentDetails = createAsyncThunk(
  `${REDUCER_NAME}/loadComponentDetails`,
  async ({ internalAppId, sbomVersion, componentHash }, { rejectWithValue }) => {
    try {
      const response = await axios.get(getSbomComponentDetailsUrl(internalAppId, sbomVersion, componentHash));
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const loadComponentDependencyTreeData = createAsyncThunk(
  `${REDUCER_NAME}/loadComponentDependencyTreeData`,
  async ({ hash: componentHash }, { rejectWithValue }) => {
    try {
      const response = await axios.get(getSbomComponentDependencyTreeUrl(componentHash));
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const loadVulnerabilityDetails = createAsyncThunk(
  `${REDUCER_NAME}/loadVulnerabilityDetails`,
  ({ componentIdentifier, vulnerability, extraParams }, { rejectWithValue }) => {
    const { ownerId, hash, isRepositoryComponent } = extraParams;
    const ownerType = isRepositoryComponent ? 'repository' : 'application';
    const extraQueryParameters = {
      ownerType,
      ownerId,
    };

    const vulnerabilityJsonDetailUrl = getVulnerabilityJsonDetailUrl(
      vulnerability.refId,
      componentIdentifier,
      extraQueryParameters
    );
    const vulnerabilityOverrideUrl = getVulnerabilityOverrideUrl(ownerType, ownerId, hash, vulnerability);

    return axios
      .all([axios.get(vulnerabilityJsonDetailUrl), axios.get(vulnerabilityOverrideUrl)])
      .then(([{ data: vulnerabilityDetails }, { data: vulnerabilityOverride }]) => {
        if (ownerType === 'application') {
          return axios
            .get(getApplicationSummaryUrl(ownerId))
            .then(({ data }) => {
              return checkPermissions(['WRITE'], ownerType, data.id);
            })
            .then(() => {
              return { ...vulnerabilityDetails, comment: vulnerabilityOverride.comment, hasEditIqPermission: true };
            })
            .catch(() => {
              return { ...vulnerabilityDetails, comment: vulnerabilityOverride.comment };
            });
        } else {
          return checkPermissions(['WRITE'], 'repository', ownerId)
            .then((_) => {
              return { ...vulnerabilityDetails, comment: vulnerabilityOverride.comment, hasEditIqPermission: true, _ };
            })
            .catch(() => {
              return { ...vulnerabilityDetails, comment: vulnerabilityOverride.comment };
            });
        }
      })
      .catch(rejectWithValue);
  }
);

const saveVexAnnotation = createAsyncThunk(
  `${REDUCER_NAME}/saveVexAnnotation`,
  async ({ internalAppId, sbomVersion, vulnerabilityRefId, vexAnnotationFormData }, { rejectWithValue }) => {
    const urlSaveUpdate = saveSbomVulnerabilityAnnotationUrl(internalAppId, sbomVersion, vulnerabilityRefId);
    try {
      const response = await axios.put(urlSaveUpdate, vexAnnotationFormData);
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const getVulnerabilityAnalysisReferenceData = createAsyncThunk(
  `${REDUCER_NAME}/loadVulnerabilityAnalysisReferenceData`,
  async (_, { rejectWithValue }) => {
    try {
      const response = await axios.get(getSbomVulnerabibilityAnalysisReferenceData());
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const sbomComponentDetailsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    clearFormSubmitMask: function (state) {
      state.submitMaskStateForVexAnnotationForm = null;
    },
  },
  extraReducers: {
    [loadComponentDetails.pending]: loadComponentDetailsRequested,
    [loadComponentDetails.fulfilled]: loadComponentDetailsFulfilled,
    [loadComponentDetails.rejected]: loadComponentDetailsFailed,
    [loadComponentDependencyTreeData.pending]: loadComponentDependencyTreeDataRequested,
    [loadComponentDependencyTreeData.fulfilled]: loadComponentDependencyTreeDataFulfilled,
    [loadComponentDependencyTreeData.rejected]: loadComponentDependencyTreeDataRejected,
    [loadVulnerabilityDetails.pending]: loadVulnerabilityDetailsRequested,
    [loadVulnerabilityDetails.fulfilled]: loadVulnerabilityDetailsFulfilled,
    [loadVulnerabilityDetails.rejected]: loadVulnerabilityDetailsRejected,
    [saveVexAnnotation.pending]: saveVexAnnotationRequested,
    [saveVexAnnotation.fulfilled]: saveVexAnnotationFulfilled,
    [saveVexAnnotation.rejected]: saveVexAnnotationRejected,
    [getVulnerabilityAnalysisReferenceData.pending]: getVulnerabilityAnalysisReferenceDataRequested,
    [getVulnerabilityAnalysisReferenceData.fulfilled]: getVulnerabilityAnalysisReferenceDataFulfilled,
    [getVulnerabilityAnalysisReferenceData.rejected]: getVulnerabilityAnalysisReferenceDataRejected,
    [UI_ROUTER_ON_FINISH]: always(initialState),
  },
});

export const actions = {
  ...sbomComponentDetailsSlice.actions,
  loadComponentDetails,
  loadComponentDependencyTreeData,
  loadVulnerabilityDetails,
  getVulnerabilityAnalysisReferenceData,
  saveVexAnnotation,
};

export default sbomComponentDetailsSlice.reducer;
