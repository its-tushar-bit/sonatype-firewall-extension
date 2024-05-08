/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { always, omit } from 'ramda';
import { getApplicationSummaryUrl, getAllApplicationSbomVersions, getSbomMetadataUrl } from 'MainRoot/util/CLMLocation';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

const REDUCER_NAME = 'billOfMaterialsPage';

export const sbomMetadataInitialState = {
  author: [],
  manufacturer: [],
  supplier: [],
  person: [],
  organization: [],
  specification: null,
  specVersion: null,
  fileFormat: null,
  createdAt: null,
};

export const initialState = {
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
};

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

const billsOfMaterialsPageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setPublicAppId: (state, { payload }) => {
      state.publicAppId = payload;
    },
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
    [UI_ROUTER_ON_FINISH]: always(initialState),
  },
});

export const actions = {
  ...billsOfMaterialsPageSlice.actions,
  loadInternalAppId,
  loadApplicationSbomVersions,
  loadSbomMetadata,
};

export default billsOfMaterialsPageSlice.reducer;
