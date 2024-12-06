/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import {
  __,
  always,
  complement,
  compose,
  gt,
  ifElse,
  includes,
  isNil,
  length,
  match,
  nth,
  omit,
  pick,
  pickBy,
  prop,
  replace,
  trim,
  values,
} from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import {
  getApplicationSummaryUrl,
  getAllApplicationSbomVersions,
  getSbomMetadataUrl,
  getSbomSummaryUrl,
  getDownloadSbomFileUrl,
} from 'MainRoot/util/CLMLocation';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectBillOfMaterialsPage } from './billOfMaterialsSelectors';

import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';
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

// export-and-download-sbom
const DEFAULT_SBOM_FILENAME = 'exported_sbom';

const extractFileNameFromResponseDisposition = (disposition, defaultFileName) =>
  compose(ifElse(compose(gt(__, 1), length), nth(1), always(defaultFileName)), match(/filename="(.+)"/))(disposition);

// export-and-download-submit-mask
export const EXPORT_AND_DOWNLOAD_SBOM_SUBMIT_MASK_SUCCESS_MESSAGE = 'SBOM export completed successfully!';
const EXPORT_AND_DOWNLOAD_SBOM_SUBMIT_MASK_ERROR_MESSAGE = 'SBOM export failed.';
export const EXPORT_AND_DOWNLOAD_SBOM_SUBMIT_MASK_EXPORTING_MESSAGE = 'SBOM export in progress...';

export const exportAndDownloadSbomSubmitMaskInitialState = Object.freeze({
  showSubmitMask: false,
  success: false,
  successMessage: null,
});

// sbom-additional-export-options-modal
export const EXPORT_SBOM_FILE_FORMAT = Object.freeze({
  json: 'application/json',
  xml: 'application/xml',
});

export const EXPORT_SBOM_SPECIFICATION = Object.freeze({
  cyclonedx: 'cyclonedx1.6',
  spdx: 'spdx2.3',
});

export const EXPORT_SBOM_STATE = Object.freeze({
  original: 'original',
  current: 'current',
});

export const sbomAdditionalExportOptionsModalInitialState = Object.freeze({
  showModal: false,
  sbomSpecification: EXPORT_SBOM_SPECIFICATION.cyclonedx,
  sbomFileFormat: EXPORT_SBOM_FILE_FORMAT.json,
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
  annotatedVulnerabilitesPercentage: null,
  validationErrorAlertDismissed: false,

  // sbom-additional-export-options-modal
  sbomAdditionalExportOptionsModal: { ...sbomAdditionalExportOptionsModalInitialState },
  exportAndDownloadSbomSubmitMask: { ...exportAndDownloadSbomSubmitMaskInitialState },
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
  state.annotatedVulnerabilitesPercentage = null;
};

const loadSbomSummaryFailed = (state, { payload }) => {
  state.loadingSbomSummary = false;
  state.errorSbomSummary = payload;
  state.componentSummary = { ...componentSummaryInitialState };
  state.vulnerabilitiesSummary = { ...vulnerabilitiesSummaryInitialState };
  state.policyViolationSummary = { ...policyViolationSummaryInitialState };
  state.annotatedVulnerabilitesPercentage = null;
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
  state.annotatedVulnerabilitesPercentage = payload.annotatedPercentage;
};

const loadSbomSummary = createAsyncThunk(
  `${REDUCER_NAME}/loadSbomSummary`,
  async ({ internalAppId, version }, { rejectWithValue }) =>
    axios
      .get(getSbomSummaryUrl(internalAppId, version))
      .then((response) => response.data)
      .catch((err) => rejectWithValue(err))
);

// sbom-additional-export-options-modal
const setShowSbomAdditionalExportOptionsModal = (state, { payload }) => {
  state.sbomAdditionalExportOptionsModal.showModal = payload;
};

const setExportSbomSpecification = (state, { payload }) => {
  if (includes(payload, values(EXPORT_SBOM_SPECIFICATION))) {
    state.sbomAdditionalExportOptionsModal.sbomSpecification = payload;
  }
};

const setExportSbomFileFormat = (state, { payload }) => {
  if (includes(payload, values(EXPORT_SBOM_FILE_FORMAT))) {
    state.sbomAdditionalExportOptionsModal.sbomFileFormat = payload;
  }
};

const dismissSbomInvalidAlert = propSet('validationErrorAlertDismissed', true);

// export-and-download-sbom
const exportAndDownloadSbomRequested = (state) => {
  state.sbomAdditionalExportOptionsModal.showModal = false;
  state.exportAndDownloadSbomSubmitMask.showSubmitMask = true;
};

const exportAndDownloadSbomFailed = (state) => {
  state.sbomAdditionalExportOptionsModal = { ...sbomAdditionalExportOptionsModalInitialState };
  state.exportAndDownloadSbomSubmitMask = { ...exportAndDownloadSbomSubmitMaskInitialState };
};

const exportAndDownloadSbomFulfilled = (state) => {
  state.sbomAdditionalExportOptionsModal = { ...sbomAdditionalExportOptionsModalInitialState };

  state.exportAndDownloadSbomSubmitMask.showSubmitMask = true;
  state.exportAndDownloadSbomSubmitMask.success = true;
  state.exportAndDownloadSbomSubmitMask.successMessage = EXPORT_AND_DOWNLOAD_SBOM_SUBMIT_MASK_SUCCESS_MESSAGE;
};

const exportAndDownloadSbomSubmitMaskTimerDone = (state) => {
  state.exportAndDownloadSbomSubmitMask = { ...exportAndDownloadSbomSubmitMaskInitialState };
};

const startExportAndDownloadSbomSubmitMaskSuccessTimer = (dispatch) => {
  setTimeout(() => dispatch(actions.exportAndDownloadSbomSubmitMaskTimerDone()), SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
};

const getErrorMessageFromResponseBlob = async (error) => {
  try {
    return await error.response.data.text();
  } catch {
    return '';
  }
};

const exportAndDownloadSbom = createAsyncThunk(
  `${REDUCER_NAME}/exportAndDownloadSbom`,
  async (options = {}, { getState, dispatch, rejectWithValue }) => {
    const state = getState();

    const {
      internalAppId,
      sbomAdditionalExportOptionsModal: { sbomFileFormat, sbomSpecification },
    } = selectBillOfMaterialsPage(state);
    const { versionId: sbomVersion } = selectRouterCurrentParams(state);

    const headersAccept = options.fileFormat || sbomFileFormat;
    return axios({
      headers: {
        Accept: headersAccept,
      },
      url: getDownloadSbomFileUrl(
        internalAppId,
        sbomVersion,
        options.state,
        options.specification || sbomSpecification
      ),
      method: 'GET',
      responseType: 'blob',
    })
      .then((response) => {
        const fileExtension = replace('application/', '', headersAccept);
        const defaultFileName = DEFAULT_SBOM_FILENAME + '_' + Date.now() + '.' + fileExtension;
        const disposition = response?.headers?.get('Content-Disposition') || '';
        const fileName = extractFileNameFromResponseDisposition(disposition, defaultFileName);

        const url = window.URL.createObjectURL(new Blob([response.data]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', fileName);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);

        startExportAndDownloadSbomSubmitMaskSuccessTimer(dispatch);
      })
      .catch(async (error) => {
        const errorMessage = trim(
          EXPORT_AND_DOWNLOAD_SBOM_SUBMIT_MASK_ERROR_MESSAGE + ' ' + (await getErrorMessageFromResponseBlob(error))
        );
        dispatch(toastActions.addToast({ type: 'error', message: errorMessage }));
        return rejectWithValue(errorMessage);
      });
  }
);

const billsOfMaterialsPageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setPublicAppId: (state, { payload }) => {
      state.publicAppId = payload;
    },
    setShowSbomAdditionalExportOptionsModal,
    setExportSbomSpecification,
    setExportSbomFileFormat,
    exportAndDownloadSbomSubmitMaskTimerDone,
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
    [exportAndDownloadSbom.pending]: exportAndDownloadSbomRequested,
    [exportAndDownloadSbom.fulfilled]: exportAndDownloadSbomFulfilled,
    [exportAndDownloadSbom.rejected]: exportAndDownloadSbomFailed,
    [UI_ROUTER_ON_FINISH]: always(initialState),
  },
});

export const actions = {
  ...billsOfMaterialsPageSlice.actions,
  loadInternalAppId,
  loadApplicationSbomVersions,
  loadSbomMetadata,
  loadSbomSummary,
  exportAndDownloadSbom,
};

export default billsOfMaterialsPageSlice.reducer;
