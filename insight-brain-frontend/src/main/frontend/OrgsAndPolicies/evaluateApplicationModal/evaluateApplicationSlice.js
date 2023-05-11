/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice, unwrapResult } from '@reduxjs/toolkit';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/stagesSlice';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { nxFileUploadStateHelpers } from '@sonatype/react-shared-components';
import { selectEvaluateApplicationSlice } from 'MainRoot/OrgsAndPolicies/evaluateApplicationModal/evaluateApplicationSelectors';
import axios from 'axios';
import { getBundleUploadUrl, getEvaluationStatusUrl } from 'MainRoot/util/CLMLocation';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectIsNotificationsSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';

const REDUCER_NAME = `${OWNER_ACTIONS}/evaluateApplication`;
const validEvaluateBundleStages = ['build', 'stage-release', 'release', 'operate'];
const { initialState: rscInitialFileUploadState, userInput: userFileUploadInput } = nxFileUploadStateHelpers;

const initialState = {
  isEvaluationModalOpen: false,
  isStatusModalOpen: false,
  isValid: false,
  loading: false,
  loadError: null,
  submitError: null,
  stages: [],
  selectedStageId: null,
  notify: 'true',
  file: rscInitialFileUploadState(null),
  uploadFileProgress: 0,
  isUploadingFile: false,
  evaluationStatus: {
    currentStep: 0,
    totalSteps: 1,
    currentStepName: 'Uploading',
    scanId: '',
    error: null,
  },
};

const resetState = (state) => {
  if (state.isEvaluationModalOpen || state.isStatusModalOpen) return;
  state.file = rscInitialFileUploadState(null);
  state.selectedStageId = null;
  state.uploadFileProgress = 0;
  state.isUploadingFile = false;
  state.notify = 'true';
  state.isValid = false;
  state.evaluationStatus = initialState.evaluationStatus;
};

const openEvaluateAppModal = (state) => {
  state.isEvaluationModalOpen = true;
};

const closeEvaluateAppModal = (state) => {
  state.isEvaluationModalOpen = false;
  resetState(state);
};

const openEvalStatusModal = (state) => {
  state.isStatusModalOpen = true;
};

const closeEvalStatusModal = (state) => {
  state.isStatusModalOpen = false;
  resetState(state);
};

const selectFile = (state, { payload }) => {
  state.file = userFileUploadInput(payload);
  state.isValid = computeIsValid(state);
};

const selectStageId = (state, { payload }) => {
  state.selectedStageId = payload;
  state.isValid = computeIsValid(state);
};

const changeNotification = (state, { payload }) => {
  state.notify = payload;
};

const computeIsValid = (state) => state.file.files && !!state.selectedStageId;

const setEvaluationStatus = (state, { payload: { currentStepName, currentStep, totalSteps, scanId } }) => {
  state.evaluationStatus = {
    currentStepName,
    currentStep,
    totalSteps,
    scanId,
  };
};

const doLoad = createAsyncThunk(`${REDUCER_NAME}/doLoad`, (_, { dispatch, rejectWithValue }) => {
  const promises = [
    dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded()),
    dispatch(stagesActions.loadCliStages()),
  ];
  return Promise.all(promises)
    .then(([productFeaturesPayload, stagesPayload]) => {
      const { data: stages } = unwrapResult(stagesPayload);
      const validStages = [];
      unwrapResult(productFeaturesPayload);
      stages.forEach(function (stage) {
        if (validEvaluateBundleStages.includes(stage.stageTypeId)) {
          validStages.push(stage);
        }
      });
      return validStages;
    })
    .catch(rejectWithValue);
});

const doLoadPending = (state) => {
  state.loading = true;
  state.loadError = null;
};

const doLoadFulfilled = (state, { payload }) => {
  state.stages = payload;
  state.loading = false;
};

const doLoadFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const evaluate = createAsyncThunk(`${REDUCER_NAME}/evaluate`, (_, { dispatch, getState, rejectWithValue }) => {
  const state = getState();
  const { file, selectedStageId, notify } = selectEvaluateApplicationSlice(state);
  const { publicId } = selectSelectedOwner(state);
  const isNotificationSupported = selectIsNotificationsSupported(state);
  const isNotify = isNotificationSupported ? notify : false;
  const url = getBundleUploadUrl(publicId, selectedStageId, isNotify);
  const formData = new FormData();
  formData.append('file', file.files?.[0]);
  formData.append('filename', file.files?.[0].name);
  dispatch(actions.isUploadingFile(true));
  return axios
    .post(url, formData, {
      onUploadProgress: function (progressEvent) {
        let percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        dispatch(actions.updateUploadFileProgress(percentCompleted));
      },
    })
    .then(({ data: { applicationPublicId, ticketId } }) => {
      const pollingUrl = getEvaluationStatusUrl(applicationPublicId, ticketId);
      dispatch(actions.openEvalStatusModal());
      dispatch(actions.closeEvaluateAppModal());
      dispatch(doPoll(pollingUrl));
    })
    .catch(rejectWithValue)
    .finally(() => {
      dispatch(actions.isUploadingFile(false));
    });
});

const evaluatePending = (state) => {
  state.submitError = null;
};

const isUploadingFile = (state, { payload }) => {
  state.isUploadingFile = payload;
};

const evaluateFailed = (state, { payload }) => {
  state.submitError = Messages.getHttpErrorMessage(payload);
  state.isUploadingFile = false;
};

const updateUploadFileProgress = (state, { payload }) => {
  state.uploadFileProgress = payload;
};

const doPoll = createAsyncThunk(`${REDUCER_NAME}/doPoll`, (url, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const { isStatusModalOpen } = selectEvaluateApplicationSlice(state);
  if (isStatusModalOpen) {
    return axios
      .get(url)
      .then(({ data: { currentStep, totalSteps, currentStepName, scanId, error } }) => {
        dispatch(actions.setEvaluationStatus({ currentStep, totalSteps, currentStepName, scanId }));
        if (error) {
          return rejectWithValue(error);
        } else if (currentStep < totalSteps) {
          setTimeout(() => dispatch(doPoll(url)), 500);
        }
      })
      .catch(rejectWithValue);
  }
});

const doPollFailed = (state, { payload }) => {
  state.evaluationStatus.error = Messages.getHttpErrorMessage(payload);
};

const evaluateApplication = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    openEvaluateAppModal,
    closeEvaluateAppModal,
    resetState,
    selectFile,
    selectStageId,
    changeNotification,
    openEvalStatusModal,
    closeEvalStatusModal,
    setEvaluationStatus,
    updateUploadFileProgress,
    isUploadingFile,
  },
  extraReducers: {
    [doLoad.pending]: doLoadPending,
    [doLoad.fulfilled]: doLoadFulfilled,
    [doLoad.rejected]: doLoadFailed,
    [evaluate.pending]: evaluatePending,
    [evaluate.rejected]: evaluateFailed,
    [doPoll.rejected]: doPollFailed,
  },
});

export default evaluateApplication.reducer;

export const actions = {
  ...evaluateApplication.actions,
  doLoad,
  evaluate,
};
