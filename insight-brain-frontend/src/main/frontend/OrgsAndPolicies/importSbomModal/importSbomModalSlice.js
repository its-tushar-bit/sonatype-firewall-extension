/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { allPass, always, complement, is, isEmpty, isNil } from 'ramda';

import { getImportSbomUrl, getCommitImportedSbomUrl } from 'MainRoot/util/CLMLocation';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';
import { selectSelectedOwnerId } from '../orgsAndPoliciesSelectors';

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
  scanType: null,
  uploadProgress: 0,
  errorMessage: null,
  sbomSummary: { ...sbomSummaryInitialState },
});

const setIsModalOpen = (state, { payload }) => {
  state.isModalOpen = payload;
};

const setUploadProgress = (state, { payload }) => {
  state.uploadProgress = payload;
};

const uploadFile = createAsyncThunk(`${REDUCER_NAME}/uploadFile`, (file, { dispatch, getState, rejectWithValue }) => {
  const state = getState();
  const appId = selectSelectedOwnerId(state);

  if (isNil(file)) {
    return;
  }

  const formData = new FormData();
  formData.append('file', file);
  formData.append('filename', file.name);

  return axios
    .post(getImportSbomUrl(appId), formData, {
      onUploadProgress: (progressEvent) => {
        const percentCompleted = Math.round((progressEvent.loaded * 10) / progressEvent.total);
        dispatch(actions.setUploadProgress(percentCompleted));
      },
    })
    .then(({ data }) => {
      if (data.scanType === 'SBOM' && isNonEmptyString(data.errorMessage)) {
        throw new Error(data.errorMessage);
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
        throw new Error(data.errorMessage);
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
  state.errorMessage = isNonEmptyString(payload.message) ? payload.message : DEFAULT_ERROR_MESSAGE;
};

const importSbomModal = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setIsModalOpen,
    setUploadProgress,
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
