/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { nxFileUploadStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { always } from 'ramda';

import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getImportSbomUrl, getCommitImportedSbomUrl } from 'MainRoot/util/CLMLocation';

import { selectImportSbomModalSlice } from './importSbomModalSelectors';
import { selectSelectedOwnerId } from '../orgsAndPoliciesSelectors';

const { initialState: rscInitialFileUploadState, userInput: userFileUploadInput } = nxFileUploadStateHelpers;

const REDUCER_NAME = `${OWNER_ACTIONS}/importSbomModal`;
const SUBMIT_MASK_IMPORT_MESSAGE = 'Importing…';

export const initialState = {
  isModalOpen: false,

  file: rscInitialFileUploadState(null),

  // null: default, -1: error, 0: uploading, 1: success
  uploadState: null,
  uploadFileProgress: 0,

  // post upload
  requestId: null,
  componentCount: null,
  vulnerabilityCount: null,
  versionId: '',

  submitError: null,
  submitMaskState: null,
  submitMaskMessage: null,
};

const setIsModalOpen = (state, { payload }) => {
  if (payload) {
    state.isModalOpen = true;
  } else {
    return { ...initialState };
  }
};

const setVersionId = (state, { payload }) => {
  state.versionId = payload.trim();
};

const setupFileUpload = (state, { payload }) => {
  state.file = payload;
  state.uploadState = payload.files ? 0 : null;
  state.submitError = null;
};

const updateUploadFileProgress = (state, { payload }) => {
  state.uploadFileProgress = payload;
};

const uploadFile = createAsyncThunk(
  `${REDUCER_NAME}/upload`,
  (filePayload, { dispatch, getState, rejectWithValue }) => {
    const state = getState();
    const file = userFileUploadInput(filePayload);
    dispatch(actions.setupFileUpload(file));

    if (!file.files) {
      return;
    }

    const formData = new FormData();
    formData.append('file', file.files?.[0]);
    formData.append('filename', file.files?.[0].name);

    const appId = selectSelectedOwnerId(state);
    return axios
      .post(getImportSbomUrl(appId), formData, {
        onUploadProgress: function (progressEvent) {
          const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          dispatch(actions.updateUploadFileProgress(percentCompleted));
        },
      })
      .then(({ data }) => {
        if (data.errorMessage) {
          throw new Error(data.errorMessage);
        }
        return data;
      })
      .catch(rejectWithValue);
  }
);

const uploadFileFulfilled = (state, { payload }) => {
  if (state.uploadState !== null) {
    state.uploadState = 1;
    state.submitError = null;

    state.requestId = payload.requestId;
    state.versionId = payload.sbomSummary?.applicationVersion || '';
    state.componentCount = payload.sbomSummary?.componentCount;
    state.vulnerabilityCount = payload.sbomSummary?.vulnerabilityCount;
  }
};

const uploadFileFailed = (state, { payload }) => {
  if (state.uploadState !== null) {
    state.uploadState = -1;
    state.submitError = payload instanceof Error ? payload.message : Messages.getHttpErrorMessage(payload);
  }
};

const submitImport = createAsyncThunk(`${REDUCER_NAME}/import`, async (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const { requestId } = selectImportSbomModalSlice(state);
  const appId = selectSelectedOwnerId(state);

  return axios
    .post(getCommitImportedSbomUrl(appId, requestId))
    .then(({ data }) => {
      startSubmitMaskSuccessTimer(() => dispatch(actions.reset()));
      return data;
    })
    .catch(rejectWithValue);
});

const submitImportRequested = (state) => {
  state.submitMaskState = false;
  state.submitMaskMessage = SUBMIT_MASK_IMPORT_MESSAGE;
  state.submitError = null;
};

const submitImportFulfilled = (state) => {
  state.submitMaskState = true;
};

const submitImportFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

function startSubmitMaskSuccessTimer(callback) {
  setTimeout(callback, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const importSbomModal = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    reset: always(initialState),
    setIsModalOpen,
    setVersionId,
    setupFileUpload,
    updateUploadFileProgress,
  },
  extraReducers: {
    [uploadFile.fulfilled]: uploadFileFulfilled,
    [uploadFile.rejected]: uploadFileFailed,
    [submitImport.pending]: submitImportRequested,
    [submitImport.fulfilled]: submitImportFulfilled,
    [submitImport.rejected]: submitImportFailed,
  },
});

export default importSbomModal.reducer;
export const actions = {
  ...importSbomModal.actions,
  uploadFile,
  submitImport,
};
