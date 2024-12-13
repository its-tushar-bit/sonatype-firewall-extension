/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { always, complement, compose, isNil, omit, pick, pickBy, prop } from 'ramda';

import {
  getApplicationSummaryUrl,
  getAllApplicationSbomVersions,
  getSbomMetadataUrl,
  getSbomSummaryUrl,
} from 'MainRoot/util/CLMLocation';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';
import { propSet } from 'MainRoot/util/jsUtil';

const REDUCER_NAME = 'billOfMaterialsPage';

export const sbomMetadataInitialState = Object.freeze({
  author: [],
  manufacturer: [],
  supplier: [],
  person: [],
  organization: [],
  specification: null,
  specVersion: null,
  fileFormat: null,
  originalFile: null,
  createdAt: null,
  isValid: null,
  displayNameSortingEnabled: false,
});

export const vulnerabilitiesSummaryInitialState = Object.freeze({
  critical: 0,
  high: 0,
  medium: 0,
  low: 0,
});

export const componentSummaryInitialState = Object.freeze({
  direct: 0,
  transitive: 0,
  unspecified: 0,
});

export const policyViolationSummaryInitialState = Object.freeze({
  critical: 0,
  severe: 0,
  moderate: 0,
  low: 0,
});

export const initialState = Object.freeze({
  publicAppId: null,

  // internal-application-id
  loadingInternalAppId: true,
  errorInternalAppId: null,
  internalAppId: null,
  applicationName: null,

  // sbom-versions
  loadingSbomVersions: true,
  errorSbomVersions: null,
  sbomVersions: null,

  // sbom-metadata
  loadingSbomMetadata: true,
  errorSbomMetadata: null,
  sbomMetadata: { ...sbomMetadataInitialState },
  scanId: null,

  // sbom-summary
  loadingSbomSummary: true,
  errorSbomSummary: null,
  componentSummary: { ...componentSummaryInitialState },
  vulnerabilitiesSummary: { ...vulnerabilitiesSummaryInitialState },
  policyViolationSummary: { ...policyViolationSummaryInitialState },
  releaseStatusPercentage: null,
  validationErrorAlertDismissed: false,
});

// internal-application-id
const loadInternalAppIdRequested = (state) => {
  state.loadingInternalAppId = true;
  state.errorInternalAppId = null;
  state.applicationName = null;
};

const loadInternalAppIdFulfilled = (state, { payload }) => {
  state.loadingInternalAppId = false;
  state.errorInternalAppId = null;
  state.internalAppId = payload.id;
  state.applicationName = payload.name;
};

const loadInternalAppIdFailed = (state, { payload }) => {
  state.loadingInternalAppId = false;
  state.errorInternalAppId = payload.response.data;
  state.internalAppId = null;
  state.publicAppId = null;
};

const loadInternalAppId = createAsyncThunk(
  `${REDUCER_NAME}/loadInternalAppId`,
  async (publicApplicationId, { rejectWithValue }) =>
    axios
      .get(getApplicationSummaryUrl(publicApplicationId))
      .then((response) => response.data)
      .catch((err) => rejectWithValue(err))
);

// sbom-versions
const loadApplicationSbomVersionsRequested = (state) => {
  state.loadingSbomVersions = true;
  state.errorSbomVersions = null;
};

const loadApplicationSbomVersionsFulfilled = (state, { payload }) => {
  state.loadingSbomVersions = false;
  state.errorSbomVersions = null;
  state.sbomVersions = payload;
};

const loadApplicationSbomVersionsFailed = (state, { payload }) => {
  state.loadingSbomVersions = false;
  state.errorSbomVersions = payload;
  state.sbomVersions = null;
};

const loadApplicationSbomVersions = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicationSbomVersions`,
  async (internalApplicationId, { rejectWithValue }) =>
    axios
      .get(getAllApplicationSbomVersions(internalApplicationId))
      .then((response) => response.data)
      .catch((err) => rejectWithValue(err))
);

// sbom-metadata
const loadSbomMetadataRequested = (state) => {
  state.loadingSbomMetadata = true;
  state.errorSbomMetadata = null;
  state.sbomMetadata = { ...sbomMetadataInitialState };
  state.scanId = null;
};

const loadSbomMetadataFailed = (state, { payload }) => {
  state.loadingSbomMetadata = false;
  state.errorSbomMetadata = payload;
  state.sbomMetadata = { ...sbomMetadataInitialState };
  state.scanId = null;
};

const loadSbomMetadataFulfilled = (state, { payload }) => {
  state.loadingSbomMetadata = false;
  state.errorSbomMetadata = null;
  state.sbomMetadata = { ...sbomMetadataInitialState, ...omit(['scanId'], payload) };
  state.scanId = payload.scanId;
};

const loadSbomMetadata = createAsyncThunk(
  `${REDUCER_NAME}/loadSbomMetadata`,
  async ({ internalAppId, version }, { rejectWithValue }) =>
    axios
      .get(getSbomMetadataUrl(internalAppId, version))
      .then((response) => response.data)
      .catch((err) => rejectWithValue(err))
);

// sbom-summary
const loadSbomSummaryRequested = (state) => {
  state.loadingSbomSummary = true;
  state.errorSbomSummary = null;
  state.componentSummary = { ...componentSummaryInitialState };
  state.vulnerabilitiesSummary = { ...vulnerabilitiesSummaryInitialState };
  state.policyViolationSummary = { ...policyViolationSummaryInitialState };
  state.releaseStatusPercentage = null;
};

const loadSbomSummaryFailed = (state, { payload }) => {
  state.loadingSbomSummary = false;
  state.errorSbomSummary = payload;
  state.componentSummary = { ...componentSummaryInitialState };
  state.vulnerabilitiesSummary = { ...vulnerabilitiesSummaryInitialState };
  state.policyViolationSummary = { ...policyViolationSummaryInitialState };
  state.releaseStatusPercentage = null;
};

const loadSbomSummaryFulfilled = (state, { payload }) => {
  state.loadingSbomSummary = false;
  state.errorSbomSummary = null;
  state.componentSummary = {
    ...componentSummaryInitialState,
    ...pickBy(complement(isNil))(payload.dependencyType),
  };
  state.vulnerabilitiesSummary = {
    ...vulnerabilitiesSummaryInitialState,
    ...compose(pick(Object.keys(vulnerabilitiesSummaryInitialState)), pickBy(complement(isNil)))(payload),
  };
  state.policyViolationSummary = {
    ...policyViolationSummaryInitialState,
    ...compose(
      pick(Object.keys(policyViolationSummaryInitialState)),
      pickBy(complement(isNil)),
      prop('policyViolationSummary')
    )(payload),
  };
  state.releaseStatusPercentage = payload.releaseStatusPercentage;
};

const loadSbomSummary = createAsyncThunk(
  `${REDUCER_NAME}/loadSbomSummary`,
  async ({ internalAppId, version }, { rejectWithValue }) =>
    axios
      .get(getSbomSummaryUrl(internalAppId, version))
      .then((response) => response.data)
      .catch((err) => rejectWithValue(err))
);

const dismissSbomInvalidAlert = propSet('validationErrorAlertDismissed', true);

const billsOfMaterialsPageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setPublicAppId: (state, { payload }) => {
      state.publicAppId = payload;
    },
    dismissSbomInvalidAlert,
  },
  extraReducers: {
    [loadInternalAppId.pending]: loadInternalAppIdRequested,
    [loadInternalAppId.fulfilled]: loadInternalAppIdFulfilled,
    [loadInternalAppId.rejected]: loadInternalAppIdFailed,
    [loadApplicationSbomVersions.pending]: loadApplicationSbomVersionsRequested,
    [loadApplicationSbomVersions.fulfilled]: loadApplicationSbomVersionsFulfilled,
    [loadApplicationSbomVersions.rejected]: loadApplicationSbomVersionsFailed,
    [loadSbomMetadata.pending]: loadSbomMetadataRequested,
    [loadSbomMetadata.fulfilled]: loadSbomMetadataFulfilled,
    [loadSbomMetadata.rejected]: loadSbomMetadataFailed,
    [loadSbomSummary.pending]: loadSbomSummaryRequested,
    [loadSbomSummary.fulfilled]: loadSbomSummaryFulfilled,
    [loadSbomSummary.rejected]: loadSbomSummaryFailed,
    [UI_ROUTER_ON_FINISH]: always(initialState),
  },
});

export const actions = {
  ...billsOfMaterialsPageSlice.actions,
  loadInternalAppId,
  loadApplicationSbomVersions,
  loadSbomMetadata,
  loadSbomSummary,
};

export default billsOfMaterialsPageSlice.reducer;
