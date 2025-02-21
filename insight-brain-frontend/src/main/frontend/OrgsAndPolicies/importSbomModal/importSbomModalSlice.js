/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios, { HttpStatusCode } from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { allPass, always, complement, is, isEmpty, isNil } from 'ramda';
import {
  combineValidationErrors,
  nxFileUploadStateHelpers,
  nxTextInputStateHelpers,
} from '@sonatype/react-shared-components';

import { getCommitImportedSbomUrl, getImportSbomUrl, getSbomSummaryUrl } from 'MainRoot/util/CLMLocation';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';
import { selectSelectedOwnerId } from '../orgsAndPoliciesSelectors';
import { selectImportSbomModalSlice, selectSelectedFile } from './importSbomModalSelectors';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { validateMaxLength, validateNonEmpty } from 'MainRoot/util/validationUtil';
import { BASE_URL } from 'MainRoot/util/urlUtil';
import { actions as sbomTileActions } from 'MainRoot/OrgsAndPolicies/ownerSummary/sbomsTile/sbomsTileSlice';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';

const DEFAULT_ERROR_MESSAGE = 'Encountered unexpected error while attempting to upload.';

const REDUCER_NAME = `${OWNER_ACTIONS}/importSbomModal`;

const isNonEmptyString = allPass([is(String), complement(isEmpty)]);

const MAX_VERSION_LENGTH = 1100;

const EVALUATION_POLLING_FREQUENCY = 500;

export const IMPORT_STATE = Object.freeze({
  INITIAL: null,
  UPLOADING: 0,
  VERSION_CONFIRM: 1,
  EVALUATION_IN_PROGRESS: 3,
  EVALUATION_COMPLETE: 4,
  SUMMARY: 5,
  ERROR: -1,
});

const sbomSummaryInitialState = Object.freeze({
  totalComponents: null,
  totalVulnerabilities: null,
  lowVulnerabilities: null,
  mediumVulnerabilities: null,
  highVulnerabilities: null,
  criticalVulnerabilities: null,
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
  evaluationPollingRef: null,
  evaluationError: null,
  versionTextInput: null,
  submitError: null,
});

const setImportState = (state, { payload }) => {
  state.importState = payload;
};

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

const setEvaluationPollingRef = (state, { payload }) => {
  state.evaluationPollingRef = payload;
};

const setImportStateSummary = (state) => {
  if (state.isModalOpen && state.importState === IMPORT_STATE.EVALUATION_COMPLETE) {
    state.importState = IMPORT_STATE.SUMMARY;
  }
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

const versionConfirm = (state) => {
  state.importState = IMPORT_STATE.VERSION_CONFIRM;
};

const uploadFileFulfilled = (state, { payload }) => {
  state.importState = IMPORT_STATE.VERSION_CONFIRM;
  recordSbomInformation(state, payload);
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
  if (payload.isValidationErrorIgnorable) {
    recordSbomInformation(state, payload);
  }
};

const recordSbomInformation = (state, payload) => {
  if (payload.sbomSummary) {
    state.sbomSummary.totalComponents = payload.sbomSummary.componentCount;
    state.sbomSummary.totalVulnerabilities = payload.sbomSummary.vulnerabilityCount;
  }
  state.versionTextInput = nxTextInputStateHelpers.initialState(payload.savedVersion);
  state.scanType = payload.scanType;
  state.savedVersion = payload.savedVersion;
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
        const pollingRef = dispatch(actions.pollEvaluationStatus(data.statusUrl));
        dispatch(actions.setEvaluationPollingRef(pollingRef));
        return data;
      })
      .catch(rejectWithValue);
  }
);

const commitFilePending = (state) => {
  state.importState = IMPORT_STATE.EVALUATION_IN_PROGRESS;
  state.submitError = null;
};

const commitFileFailed = (state, { payload }) => {
  state.importState = IMPORT_STATE.VERSION_CONFIRM;
  state.submitError = payload.response?.data?.errorMessage || Messages.getHttpErrorMessage(payload);
};

function restartModalWithError(action, state) {
  if (!action.meta.aborted) {
    return {
      ...initialState,
      isModalOpen: true,
      evaluationError:
        action.payload?.data?.errorMessage ||
        action.payload?.response?.data?.errorMessage ||
        Messages.getHttpErrorMessage(action.payload),
    };
  }
  return state;
}

const pollEvaluationStatus = createAsyncThunk(
  `${REDUCER_NAME}/pollEvaluationStatus`,
  async (evaluationStatusUri, { dispatch, signal, rejectWithValue }) => {
    const doPoll = async () => {
      if (signal.aborted) {
        return;
      }
      try {
        const url = BASE_URL + `/${evaluationStatusUri}`;
        const response = await axios.get(url, {
          signal,
          validateStatus: (status) => (status >= 200 && status < 300) || status === HttpStatusCode.NotFound,
        });
        if (response.status === HttpStatusCode.NotFound) {
          await new Promise((resolve) => setTimeout(resolve, EVALUATION_POLLING_FREQUENCY));
          return doPoll();
        } else if (response.data?.isError) {
          return rejectWithValue(response);
        }
        const pollingRef = dispatch(fetchEvaluationSummary());
        dispatch(actions.setEvaluationPollingRef(pollingRef));
        return response.data;
      } catch (error) {
        return rejectWithValue(error);
      }
    };

    return doPoll();
  }
);

const pollEvaluationStatusPending = (state) => {
  state.importState = IMPORT_STATE.EVALUATION_IN_PROGRESS;
  state.evaluationError = null;
};

const pollEvaluationStatusFailed = (state, action) => {
  return restartModalWithError(action, state);
};

const fetchEvaluationSummary = createAsyncThunk(
  `${REDUCER_NAME}/fetchEvaluationSummary`,
  async (_, { dispatch, getState, signal, rejectWithValue }) => {
    const state = getState();
    const appId = selectSelectedOwnerId(state);
    const { savedVersion } = selectImportSbomModalSlice(state);
    try {
      const url = getSbomSummaryUrl(appId, savedVersion);
      const response = await axios.get(url, { signal });
      if (response.data?.isError) {
        return rejectWithValue(response);
      }

      dispatch(sbomTileActions.loadSbomTableData());
      startSaveMaskSuccessTimer(dispatch, actions.setImportStateSummary);
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const fetchEvaluationSummaryFulfilled = (state, { payload }) => {
  state.sbomSummary.lowVulnerabilities = payload.low;
  state.sbomSummary.mediumVulnerabilities = payload.medium;
  state.sbomSummary.highVulnerabilities = payload.high;
  state.sbomSummary.criticalVulnerabilities = payload.critical;
  state.importState = IMPORT_STATE.EVALUATION_COMPLETE;
  state.evaluationError = null;
};

const fetchEvaluationSummaryPending = (state) => {
  state.importState = IMPORT_STATE.EVALUATION_IN_PROGRESS;
  state.evaluationError = null;
};

const fetchEvaluationSummaryFailed = (state, action) => {
  return restartModalWithError(action, state);
};

const importSbomModal = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setImportState,
    setIsModalOpen,
    setUploadProgress,
    setSelectedFile,
    setIsSkipValidation,
    setSavedVersion,
    setVersionTextInput,
    versionConfirm,
    setEvaluationPollingRef,
    setImportStateSummary,
    reset: always(initialState),
  },
  extraReducers: {
    [uploadFile.pending]: uploadFilePending,
    [uploadFile.fulfilled]: uploadFileFulfilled,
    [uploadFile.rejected]: uploadFileFailed,
    [commitFile.pending]: commitFilePending,
    [commitFile.rejected]: commitFileFailed,
    [pollEvaluationStatus.pending]: pollEvaluationStatusPending,
    [pollEvaluationStatus.rejected]: pollEvaluationStatusFailed,
    [fetchEvaluationSummary.fulfilled]: fetchEvaluationSummaryFulfilled,
    [fetchEvaluationSummary.pending]: fetchEvaluationSummaryPending,
    [fetchEvaluationSummary.rejected]: fetchEvaluationSummaryFailed,
  },
});

export default importSbomModal.reducer;

export const actions = {
  ...importSbomModal.actions,
  uploadFile,
  commitFile,
  pollEvaluationStatus,
};
