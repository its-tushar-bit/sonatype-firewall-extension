/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { allPass, always, complement, is, isEmpty, isNil } from 'ramda';
import {
  combineValidationErrors,
  nxFileUploadStateHelpers,
  nxTextInputStateHelpers,
} from '@sonatype/react-shared-components';

import { getImportSbomUrl, getCommitImportedSbomUrl } from 'MainRoot/util/CLMLocation';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';
import { selectSelectedOwnerId } from '../orgsAndPoliciesSelectors';
import { selectSelectedFile, selectImportSbomModalSlice } from './importSbomModalSelectors';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { validateMaxLength, validateNonEmpty } from 'MainRoot/util/validationUtil';

const DEFAULT_ERROR_MESSAGE = 'Encountered unexpected error while attempting to upload.';

const REDUCER_NAME = `${OWNER_ACTIONS}/importSbomModal`;

const isNonEmptyString = allPass([is(String), complement(isEmpty)]);

const MAX_VERSION_LENGTH = 1100;

export const IMPORT_STATE = Object.freeze({
  INITIAL: null,
  UPLOADING: 0,
  VERSION_CONFIRM: 1,
  COMMITTING: 2,
  SUMMARY: 3,
  ERROR: -1,
});

const sbomSummaryInitialState = Object.freeze({
  totalComponents: null,
  totalVulnerabilities: null,
});

export const initialState = Object.freeze({
  isModalOpen: false,
  isSkipValidation: false,
  isValidationErrorIgnorable: false,
  importState: IMPORT_STATE.INITIAL,
  fileInputState: nxFileUploadStateHelpers.initialState(null),
  scanType: null,
  uploadProgress: 0,
  errorMessage: null,
  validationErrors: null,
  sbomSummary: sbomSummaryInitialState,
  savedVersion: null,
  versionTextInput: null,
  submitError: null,
});

const setIsSkipValidation = (state, { payload }) => {
  state.isSkipValidation = payload;
};

const setIsModalOpen = (state, { payload }) => {
  state.isModalOpen = payload;
};

const setUploadProgress = (state, { payload }) => {
  state.uploadProgress = payload;
};

const setSelectedFile = (state, { payload }) => {
  state.fileInputState = nxFileUploadStateHelpers.userInput(payload);
};

const setVersionTextInput = (state, { payload }) => {
  state.versionTextInput = nxTextInputStateHelpers.userInput(validateVersion, payload);
  state.submitError = null;
};

const validateVersion = (value) => {
  return combineValidationErrors(validateNonEmpty(value), validateMaxLength(MAX_VERSION_LENGTH, value));
};

const setSavedVersion = (state, { payload }) => {
  state.savedVersion = payload;
};

const uploadFile = createAsyncThunk(`${REDUCER_NAME}/uploadFile`, (_, { dispatch, getState, rejectWithValue }) => {
  const state = getState();
  const appId = selectSelectedOwnerId(state);
  const file = selectSelectedFile(state);
  const { isSkipValidation } = selectImportSbomModalSlice(state);

  if (isNil(file)) {
    return;
  }

  const formData = new FormData();
  formData.append('file', file);

  return axios
    .post(getImportSbomUrl(appId), formData, {
      params: { ignoreValidationError: isSkipValidation },
      onUploadProgress: (progressEvent) => {
        const percentCompleted = Math.round((progressEvent.loaded * 10) / progressEvent.total);
        dispatch(actions.setUploadProgress(percentCompleted));
      },
    })
    .then(({ data }) => {
      if (data.scanType === 'SBOM' && isNonEmptyString(data.errorMessage)) {
        return rejectWithValue(data);
      }
      return data;
    })
    .catch(rejectWithValue);
});

const uploadFilePending = (state) => {
  state.importState = IMPORT_STATE.UPLOADING;
};

const uploadFileFulfilled = (state, { payload }) => {
  state.importState = IMPORT_STATE.VERSION_CONFIRM;
  if (payload.sbomSummary) {
    state.sbomSummary.totalComponents = payload.sbomSummary.componentCount;
    state.sbomSummary.totalVulnerabilities = payload.sbomSummary.vulnerabilityCount;
  }
  state.versionTextInput = nxTextInputStateHelpers.initialState(payload.savedVersion);
  state.scanType = payload.scanType;
  state.savedVersion = payload.savedVersion;
};

const uploadFileFailed = (state, { payload }) => {
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
    state.isValidationErrorIgnorable = payload.isValidationErrorIgnorable;
  }
};

const commitFile = createAsyncThunk(
  `${REDUCER_NAME}/commitFile`,
  async (applicationVersion, { dispatch, getState, rejectWithValue }) => {
    const state = getState();
    const appId = selectSelectedOwnerId(state);
    const { savedVersion, versionTextInput } = selectImportSbomModalSlice(state);

    const overrideVersion = versionTextInput.trimmedValue !== savedVersion ? versionTextInput.trimmedValue : null;
    return axios
      .post(getCommitImportedSbomUrl(appId, savedVersion, overrideVersion))
      .then(({ data }) => {
        if (isNonEmptyString(data.errorMessage)) {
          return rejectWithValue(data);
        }
        //update the version id for next page render
        if (overrideVersion) {
          dispatch(actions.setSavedVersion(overrideVersion));
        }
        return data;
      })
      .catch(rejectWithValue);
  }
);

const commitFilePending = (state) => {
  state.importState = IMPORT_STATE.COMMITTING;
  state.submitError = null;
};

const commitFileFulfilled = (state) => {
  state.importState = IMPORT_STATE.SUMMARY;
  state.submitError = null;
};

const commitFileFailed = (state, { payload }) => {
  state.importState = IMPORT_STATE.VERSION_CONFIRM;
  state.submitError = payload.response?.data?.errorMessage || Messages.getHttpErrorMessage(payload);
};

const importSbomModal = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setIsModalOpen,
    setUploadProgress,
    setSelectedFile,
    setIsSkipValidation,
    setSavedVersion,
    setVersionTextInput,
    reset: always(initialState),
  },
  extraReducers: {
    [uploadFile.pending]: uploadFilePending,
    [uploadFile.fulfilled]: uploadFileFulfilled,
    [uploadFile.rejected]: uploadFileFailed,
    [commitFile.pending]: commitFilePending,
    [commitFile.fulfilled]: commitFileFulfilled,
    [commitFile.rejected]: commitFileFailed,
  },
});

export default importSbomModal.reducer;

export const actions = {
  ...importSbomModal.actions,
  uploadFile,
  commitFile,
};
