/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios, { HttpStatusCode } from 'axios';
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import { allPass, always, complement, is, isEmpty, isNil } from 'ramda';
import {
  combineValidationErrors,
  nxFileUploadStateHelpers,
  nxTextInputStateHelpers,
} from '@sonatype/react-shared-components';

import { getCommitImportedSbomUrl, getImportSbomUrl, getSbomSummaryUrl } from 'MainRoot/util/CLMLocation';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';
import { selectSelectedOwnerId, selectSelectedOwnerPublicId } from '../orgsAndPoliciesSelectors';
import { selectImportSbomModalSlice, selectSelectedFile } from './importSbomModalSelectors';
import { selectCurrentRouteName, selectRouterCurrentParams } from '../../reduxUiRouter/routerSelectors';
import { Messages } from 'MainRoot/util/CommonServices';
import { validateMaxLength, validateNonEmpty } from 'MainRoot/util/validationUtil';
import { BASE_URL } from 'MainRoot/util/urlUtil';
import { actions as sbomTileActions } from 'MainRoot/OrgsAndPolicies/ownerSummary/sbomsTile/sbomsTileSlice';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';

const DEFAULT_ERROR_MESSAGE = 'Encountered unexpected error while attempting to upload.';

const TOAST_DEFAULT_ERROR_MESSAGE = 'An unexpected error occurred.';

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

const setImportStateEvaluationComplete = (state) => {
  if (state.isModalOpen && state.importState === IMPORT_STATE.EVALUATION_IN_PROGRESS) {
    state.importState = IMPORT_STATE.EVALUATION_COMPLETE;
  }
};

const setImportStateSummary = (state) => {
  if (state.isModalOpen && state.importState === IMPORT_STATE.EVALUATION_COMPLETE) {
    state.importState = IMPORT_STATE.SUMMARY;
  }
};

const setSbomSummaryValues = (state, { payload }) => {
  state.sbomSummary.lowVulnerabilities = payload.low;
  state.sbomSummary.mediumVulnerabilities = payload.medium;
  state.sbomSummary.highVulnerabilities = payload.high;
  state.sbomSummary.criticalVulnerabilities = payload.critical;
  state.evaluationError = null;
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

const dispatchSuccessToast = (dispatch, savedVersion, appPublicId) =>
  dispatch(
    toastActions.addToast({
      type: 'success',
      message: `SBOM ${savedVersion} from application ${appPublicId} is now ready for review in the SBOM table.`,
    })
  );

const dispatchErrorToast = (dispatch, savedVersion, appPublicId, error) => {
  const statusCode = error.status || error.response?.status;
  let errorMessage = statusCode
    ? Messages.getHttpErrorMessage({ status: statusCode })
    : error.data?.errorMessage || Messages.getHttpErrorMessage(error);

  errorMessage = errorMessage || TOAST_DEFAULT_ERROR_MESSAGE;
  const formattedErrorMessage = errorMessage.trim().endsWith('.') ? errorMessage : `${errorMessage}.`;

  dispatch(
    toastActions.addToast({
      type: 'error',
      message: `SBOM ${savedVersion} evaluation from application ${appPublicId} failed: ${formattedErrorMessage}`,
    })
  );
};

/**
 * checking whether the modal is currently open for the specified savedVersion and appId
 */
const isStateOutdated = (currentState, savedVersion, appId) => {
  const currentSavedVersion = selectImportSbomModalSlice(currentState).savedVersion;
  const currentAppId = selectSelectedOwnerId(currentState);
  return savedVersion !== currentSavedVersion || appId !== currentAppId;
};

const commitFile = createAsyncThunk(
  `${REDUCER_NAME}/commitFile`,
  async (applicationVersion, { dispatch, getState, rejectWithValue }) => {
    const state = getState();
    const appId = selectSelectedOwnerId(state);
    const appPublicId = selectSelectedOwnerPublicId(state);
    const { savedVersion, versionTextInput } = selectImportSbomModalSlice(state);

    const overrideVersion = versionTextInput.trimmedValue !== savedVersion ? versionTextInput.trimmedValue : null;
    return axios
      .post(getCommitImportedSbomUrl(appId, savedVersion, overrideVersion))
      .then(({ data }) => {
        if (overrideVersion && !isStateOutdated(getState(), savedVersion, appId)) {
          dispatch(actions.setSavedVersion(overrideVersion));
        }

        dispatch(
          actions.pollEvaluationStatus({
            statusUrl: data.statusUrl,
            savedVersion: overrideVersion || savedVersion,
            appId,
            appPublicId,
          })
        );
        return data;
      })
      .catch((error) => {
        if (isStateOutdated(getState(), savedVersion, appId)) {
          dispatchErrorToast(dispatch, overrideVersion || savedVersion, appPublicId, error);
        } else {
          return rejectWithValue(error);
        }
      });
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

function restartModalWithError(action) {
  return {
    ...initialState,
    isModalOpen: true,
    evaluationError:
      action.payload?.data?.errorMessage ||
      action.payload?.response?.data?.errorMessage ||
      Messages.getHttpErrorMessage(action.payload),
  };
}

const fetchEvaluationSummary = async (appId, savedVersion) => {
  const url = getSbomSummaryUrl(appId, savedVersion);
  return axios.get(url);
};

const fetchEvaluationStatus = async (url) => {
  return axios.get(url, {
    validateStatus: (status) => (status >= 200 && status < 300) || status === HttpStatusCode.NotFound,
  });
};

const reloadSbomTable = (appPublicId, currentState, dispatch) => {
  if (
    selectCurrentRouteName(currentState) === 'sbomManager.management.view.application' &&
    selectRouterCurrentParams(currentState).applicationPublicId === appPublicId
  ) {
    dispatch(sbomTileActions.loadSbomTableData());
  }
};

const pollEvaluationStatus = createAsyncThunk(
  `${REDUCER_NAME}/pollEvaluationStatus`,
  async ({ statusUrl, savedVersion, appId, appPublicId }, { dispatch, rejectWithValue, getState }) => {
    const doPoll = async () => {
      try {
        const url = BASE_URL + `/${statusUrl}`;
        const response = await fetchEvaluationStatus(url);
        if (response.status === HttpStatusCode.NotFound) {
          await new Promise((resolve) => setTimeout(resolve, EVALUATION_POLLING_FREQUENCY));
          return doPoll();
        }

        // Polling is complete; handling the response

        if (response.data?.isError) {
          if (isStateOutdated(getState(), savedVersion, appId)) {
            dispatchErrorToast(dispatch, savedVersion, appPublicId, response);
            return;
          }
          return rejectWithValue(response);
        }

        reloadSbomTable(appPublicId, getState(), dispatch);

        if (isStateOutdated(getState(), savedVersion, appId)) {
          dispatchSuccessToast(dispatch, savedVersion, appPublicId);
          return;
        }

        const summaryResponse = await fetchEvaluationSummary(appId, savedVersion);
        if (isStateOutdated(getState(), savedVersion, appId)) {
          return;
        }

        dispatch(actions.setImportStateEvaluationComplete());
        dispatch(actions.setSbomSummaryValues(summaryResponse.data));
        startSaveMaskSuccessTimer(dispatch, actions.setImportStateSummary);
      } catch (error) {
        if (isStateOutdated(getState(), savedVersion, appId)) {
          dispatchErrorToast(dispatch, savedVersion, appPublicId, error);
        } else {
          return rejectWithValue(error);
        }
      }
    };

    return doPoll();
  }
);

const pollEvaluationStatusFailed = (state, action) => {
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
    setImportStateEvaluationComplete,
    setImportStateSummary,
    setSbomSummaryValues,
    reset: always(initialState),
  },
  extraReducers: {
    [uploadFile.pending]: uploadFilePending,
    [uploadFile.fulfilled]: uploadFileFulfilled,
    [uploadFile.rejected]: uploadFileFailed,
    [commitFile.pending]: commitFilePending,
    [commitFile.rejected]: commitFileFailed,
    [pollEvaluationStatus.rejected]: pollEvaluationStatusFailed,
  },
});

export default importSbomModal.reducer;

export const actions = {
  ...importSbomModal.actions,
  uploadFile,
  commitFile,
  pollEvaluationStatus,
};
