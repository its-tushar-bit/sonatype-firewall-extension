/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { allPass, always, complement, is, isEmpty, isNil } from 'ramda';
import { nxFileUploadStateHelpers } from '@sonatype/react-shared-components';

import { getImportSbomUrl, getCommitImportedSbomUrl } from 'MainRoot/util/CLMLocation';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';
import { selectSelectedOwnerId } from '../orgsAndPoliciesSelectors';
import { selectSelectedFile } from './importSbomModalSelectors';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

const DEFAULT_ERROR_MESSAGE = 'Encountered unexpected error while attempting to upload.';

const REDUCER_NAME = `${OWNER_ACTIONS}/importSbomModal`;

const isNonEmptyString = allPass([is(String), complement(isEmpty)]);

export const IMPORT_STATE = Object.freeze({
  INITIAL: null,
  UPLOADING_COMMITTING: 0,
  SUMMARY: 1,
  ERROR: -1,
});

const sbomSummaryInitialState = Object.freeze({
  versionId: null,
  totalComponents: null,
  totalVulnerabilities: null,
});

export const initialState = Object.freeze({
  isModalOpen: false,
  importState: IMPORT_STATE.INITIAL,
  fileInputState: nxFileUploadStateHelpers.initialState(null),
  scanType: null,
  uploadProgress: 0,
  errorMessage: null,
  validationErrors: null,
  sbomSummary: sbomSummaryInitialState,
});

const setIsModalOpen = (state, { payload }) => {
  state.isModalOpen = payload;
};

const setUploadProgress = (state, { payload }) => {
  state.uploadProgress = payload;
};

const setSelectedFile = (state, { payload }) => {
  state.fileInputState = nxFileUploadStateHelpers.userInput(payload);
};

const uploadFile = createAsyncThunk(`${REDUCER_NAME}/uploadFile`, (_, { dispatch, getState, rejectWithValue }) => {
  const state = getState();
  const appId = selectSelectedOwnerId(state);
  const file = selectSelectedFile(state);

  if (isNil(file)) {
    return;
  }

  const formData = new FormData();
  formData.append('file', file);

  return axios
    .post(getImportSbomUrl(appId), formData, {
      onUploadProgress: (progressEvent) => {
        const percentCompleted = Math.round((progressEvent.loaded * 10) / progressEvent.total);
        dispatch(actions.setUploadProgress(percentCompleted));
      },
    })
    .then(({ data }) => {
      if (data.scanType === 'SBOM' && isNonEmptyString(data.errorMessage)) {
        return rejectWithValue(data);
      }
      dispatch(actions.commitFile(data.requestId));
      return data;
    })
    .catch(rejectWithValue);
});

const uploadFilePending = (state) => {
  state.importState = IMPORT_STATE.UPLOADING_COMMITTING;
};

const uploadFileFulfilled = (state, { payload }) => {
  state.importState = IMPORT_STATE.UPLOADING_COMMITTING;
  if (payload.sbomSummary) {
    state.sbomSummary.versionId = payload.sbomSummary.applicationVersion;
    state.sbomSummary.totalComponents = payload.sbomSummary.componentCount;
    state.sbomSummary.totalVulnerabilities = payload.sbomSummary.vulnerabilityCount;
  }
  state.scanType = payload.scanType;
};

const commitFile = createAsyncThunk(`${REDUCER_NAME}/commitFile`, async (requestId, { getState, rejectWithValue }) => {
  const state = getState();
  const appId = selectSelectedOwnerId(state);
  return axios
    .post(getCommitImportedSbomUrl(appId, requestId))
    .then(({ data }) => {
      if (isNonEmptyString(data.errorMessage)) {
        return rejectWithValue(data);
      }
      return data;
    })
    .catch(rejectWithValue);
});

const commitFilePending = (state) => {
  state.importState = IMPORT_STATE.UPLOADING_COMMITTING;
};

const commitFileFulfilled = (state) => {
  state.importState = IMPORT_STATE.SUMMARY;
};

const uploadOrCommitFileFailed = (state, { payload }) => {
  state.importState = IMPORT_STATE.ERROR;
  if (payload.errorMessage) {
    state.errorMessage = payload.errorMessage;
  } else {
    const potentialErrorMessage = Messages.getHttpErrorMessage(payload);
    if (potentialErrorMessage === 'Error') {
      state.errorMessage = payload.message || DEFAULT_ERROR_MESSAGE;
    } else {
      state.errorMessage = potentialErrorMessage;
    }
  }
  if (payload.validationErrors) {
    state.validationErrors = payload.validationErrors;
  }
};

const importSbomModal = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setIsModalOpen,
    setUploadProgress,
    setSelectedFile,
    reset: always(initialState),
  },
  extraReducers: {
    [uploadFile.pending]: uploadFilePending,
    [uploadFile.fulfilled]: uploadFileFulfilled,
    [uploadFile.rejected]: uploadOrCommitFileFailed,
    [commitFile.pending]: commitFilePending,
    [commitFile.fulfilled]: commitFileFulfilled,
    [commitFile.rejected]: uploadOrCommitFileFailed,
  },
});

export default importSbomModal.reducer;

export const actions = {
  ...importSbomModal.actions,
  uploadFile,
  commitFile,
};
