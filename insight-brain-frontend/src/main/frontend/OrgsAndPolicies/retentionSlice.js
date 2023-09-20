/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';
import { selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectEntityId, selectSelectedOwnerParentId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectRetentionSlice } from 'MainRoot/OrgsAndPolicies/retentionSelectors';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import { getParentRetentionPoliciesUrl, getRetentionPoliciesUrl } from 'MainRoot/util/CLMLocation';
import {
  combineValidationErrors,
  hasValidationErrors,
  nxTextInputStateHelpers,
} from '@sonatype/react-shared-components';
import { validateNonEmpty, validatePatternMatch } from 'MainRoot/util/validationUtil';
import { clone, equals } from 'ramda';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { propSet } from 'MainRoot/util/jsUtil';

const { initialState: rscInitialState, userInput } = nxTextInputStateHelpers;

export const NOT_ENABLED = "Don't Purge";
export const NOT_APPLICABLE = 'N/A';

const REDUCER_NAME = 'retention';
const MAX_AGE_DAYS = 18249;
const MAX_AGE_WEEKS = 2607;
const MAX_AGE_MONTHS = 608;
const MAX_AGE_YEARS = 49;
const MAX_REPORTS = 9999;

export const initialState = {
  applicationReports: null,
  applicationReportsServerData: null,
  applicationReportsParent: null,
  isDirty: false,
  successMetrics: {},
  successMetricsServerData: {},
  successMetricsParent: {},
  loading: false,
  loadError: null,
  submitMaskState: null,
  submitError: null,
  validationErrors: {},
};

const goToEditRetention = createAsyncThunk(`${REDUCER_NAME}/goToEditRetention`, (_, { getState, dispatch }) => {
  const router = selectRouterSlice(getState());
  const { to, params } = deriveEditRoute(router, 'edit-data-retention');

  dispatch(stateGo(to, params));
});

const loadRetention = createAsyncThunk(
  `${REDUCER_NAME}/loadRetention`,
  async (_, { getState, rejectWithValue, dispatch }) => {
    await dispatch(rootActions.loadSelectedOwner());
    const state = getState();
    const parentId = selectSelectedOwnerParentId(state);
    const entityId = selectEntityId(state);

    if (parentId) {
      const parentOrgRetentionRequest = axios.get(getParentRetentionPoliciesUrl(entityId));
      const entityRetentionRequest = axios.get(getRetentionPoliciesUrl(entityId));
      return axios
        .all([parentOrgRetentionRequest, entityRetentionRequest])
        .then(
          axios.spread((...responses) => {
            return { parentRetentionData: responses[0].data, entityRetentionData: responses[1].data };
          })
        )
        .catch((err) => rejectWithValue(err));
    }
    return axios
      .get(getRetentionPoliciesUrl(entityId))
      .then((response) => {
        return { parentRetentionData: null, entityRetentionData: response.data };
      })
      .catch((err) => rejectWithValue(err));
  }
);

const loadRetentionRequested = (state) => {
  state.loading = true;
  state.loadError = null;
  state.submitError = null;
};

const loadRetentionFulfilled = (state, { payload }) => {
  state.loading = false;
  state.isDirty = false;

  if (payload.parentRetentionData) {
    state.applicationReportsParent = payload.parentRetentionData.applicationReports;
    state.successMetricsParent = payload.parentRetentionData.successMetrics;
  }

  const appReportsData = payload.entityRetentionData.applicationReports;
  const stages = appReportsData?.stages ?? {};
  Object.keys(stages).forEach((stage) => {
    const { maxAge, maxCount } = appReportsData.stages[stage];

    const age = maxAge?.toString().split(' ')[0] ?? '';
    const unit = maxAge?.toString().split(' ')[1] ?? 'years';
    const count = maxCount?.toString() ?? '';

    stages[stage].maxAge = rscInitialState(age);
    stages[stage].maxAgeUnit = unit.charAt(unit.length - 1) === 's' ? unit : `${unit}s`;
    stages[stage].maxCount = rscInitialState(count);
    state.validationErrors[stage] = { age: null, count: null };
  });

  const successMetricsData = payload.entityRetentionData?.successMetrics ?? {};
  const age = successMetricsData.maxAge?.toString().split(' ')[0] ?? '';
  state.validationErrors.successMetrics = { age: null, count: null };
  successMetricsData.maxAge = rscInitialState(age);
  successMetricsData.maxAgeUnit = 'years';

  state.applicationReports = appReportsData;
  state.applicationReportsServerData = appReportsData;
  state.successMetrics = successMetricsData;
  state.successMetricsServerData = successMetricsData;
};

const loadRetentionFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const changeToServerDataFormat = (applicationReports, successMetrics) => {
  const applicationReportsTemp = clone(applicationReports);
  Object.keys(applicationReportsTemp.stages).forEach((stage) => {
    const {
      maxAge: { trimmedValue: maxAgeValue },
      maxCount: { trimmedValue: maxCountValue },
      maxAgeUnit,
    } = applicationReportsTemp.stages[stage];

    delete applicationReportsTemp.stages[stage].maxAge;
    delete applicationReportsTemp.stages[stage].maxAgeUnit;
    delete applicationReportsTemp.stages[stage].maxCount;

    if (maxAgeValue) {
      applicationReportsTemp.stages[stage].maxAge = `${maxAgeValue} ${maxAgeUnit}`;
    }

    if (maxCountValue) {
      applicationReportsTemp.stages[stage].maxCount = maxCountValue;
    }
  });

  const successMetricsTemp = clone(successMetrics);
  const {
    maxAge: { trimmedValue: maxAgeValue },
    maxAgeUnit,
  } = successMetricsTemp;
  delete successMetricsTemp.maxAge;
  delete successMetricsTemp.maxAgeUnit;
  if (maxAgeValue) {
    successMetricsTemp.maxAge = `${maxAgeValue} ${maxAgeUnit}`;
  }

  return {
    applicationReports: applicationReportsTemp,
    successMetrics: successMetricsTemp,
  };
};

const updateRetention = createAsyncThunk(
  `${REDUCER_NAME}/updateRetention`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const entityId = selectEntityId(state);
    const { applicationReports, successMetrics } = selectRetentionSlice(state);

    return axios
      .put(getRetentionPoliciesUrl(entityId), changeToServerDataFormat(applicationReports, successMetrics))
      .then(() => {
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone).then(() => dispatch(actions.loadRetention()));
      })
      .catch((error) => rejectWithValue(error));
  }
);

const updateRetentionRequested = (state) => {
  state.submitMaskState = false;
  state.submitError = null;
};

const updateRetentionFulfilled = (state) => {
  state.submitMaskState = true;
  state.isDirty = false;
};

const updateRetentionFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const setRadio = (state, { payload }) => {
  const { stage, val } = payload;
  const currentStage = stage === 'successMetrics' ? state.successMetrics : state.applicationReports.stages[stage];
  const currentStageServerData =
    stage === 'successMetrics' ? state.successMetricsServerData : state.applicationReportsServerData.stages[stage];
  if (val === 'inherit') {
    currentStage.inheritPolicy = true;
    if (currentStageServerData.maxAge) currentStage.maxAge = currentStageServerData.maxAge;
    if (currentStageServerData.maxCount) currentStage.maxCount = currentStageServerData.maxCount;
    currentStage.enablePurging = currentStageServerData.enablePurging;
    currentStage.maxAgeUnit = currentStageServerData.maxAgeUnit;
  } else if (val === 'dont-purge') {
    currentStage.inheritPolicy = false;
    currentStage.enablePurging = false;
    currentStage.maxAge = rscInitialState('');
    currentStage.maxAgeUnit = 'years';
    if (stage !== 'successMetrics') currentStage.maxCount = rscInitialState('');
  } else if (val === 'custom') {
    currentStage.inheritPolicy = false;
    currentStage.enablePurging = true;
    if (!currentStageServerData.inheritPolicy && currentStageServerData.maxAge) {
      currentStage.maxAge = currentStageServerData.maxAge;
      currentStage.maxAgeUnit = currentStageServerData.maxAgeUnit;
    } else {
      currentStage.maxAge = rscInitialState('');
      currentStage.maxAgeUnit = 'years';
    }

    if (!currentStageServerData.inheritPolicy && currentStageServerData.maxCount) {
      currentStage.maxCount = currentStageServerData.maxCount;
    } else {
      if (stage !== 'successMetrics') currentStage.maxCount = rscInitialState('');
    }
  }
  state.validationErrors[stage] = { age: null, count: null };
  computeIsDirty(state);
};

const computeIsDirty = (state) => {
  const successMetrics = state.successMetrics;
  const successMetricsServerData = state.successMetricsServerData;
  const appReports = state.applicationReports;
  const appReportsServerData = state.applicationReportsServerData;

  if (
    successMetrics.enablePurging &&
    !successMetrics.inheritPolicy &&
    successMetrics.maxAge.isPristine &&
    successMetrics.maxAge.trimmedValue === ''
  ) {
    state.isDirty = false;
    return;
  }

  let incompleteFields = false;

  for (const stage of Object.keys(appReports.stages)) {
    const { enablePurging, inheritPolicy, maxAge, maxCount } = appReports.stages[stage];
    if (
      enablePurging &&
      !inheritPolicy &&
      maxAge.isPristine &&
      maxAge.trimmedValue === '' &&
      maxCount.isPristine &&
      maxCount.trimmedValue === ''
    ) {
      incompleteFields = true;
      break;
    }
  }

  state.isDirty = incompleteFields
    ? false
    : !equals(
        changeToServerDataFormat(appReports, successMetrics),
        changeToServerDataFormat(appReportsServerData, successMetricsServerData)
      );
};

const handleInputChange = (state, { payload }) => {
  if (payload.stage === 'successMetrics') {
    // maxAge is a string i.e. '10 years'
    if (!state.successMetrics.maxAge) state.successMetrics = { ...state.successMetrics, maxAge: '10 years' };
    const maxLimit = getMaxAgeLimit(state.successMetrics.maxAgeUnit);
    state.successMetrics.maxAge = userInput(
      () => inputValidator(payload.value, maxLimit, 'ageNum', payload.stage, state),
      payload.value
    );
  } else {
    let stageSelector = state.applicationReports.stages[payload.stage];
    if (!stageSelector.maxAge) stageSelector = { ...stageSelector, maxAge: '10 years' };
    const maxLimit = getMaxAgeLimit(stageSelector.maxAgeUnit);
    switch (payload.inputType) {
      case 'ageNum': {
        const { trimmedValue: reportValue, isPristine } = state.applicationReports.stages[payload.stage].maxCount;
        stageSelector.maxAge = userInput(
          () => inputValidator(payload.value, maxLimit, 'ageNum', payload.stage, state),
          payload.value
        );
        if (!isPristine) {
          stageSelector.maxCount = userInput(
            () => inputValidator(reportValue, MAX_REPORTS, 'report', payload.stage, state),
            reportValue
          );
        }
        break;
      }
      case 'ageUnit': {
        const { trimmedValue: ageValue } = stageSelector.maxAge;
        stageSelector.maxAgeUnit = payload.value;
        stageSelector.maxAge = userInput(
          () => inputValidator(ageValue, getMaxAgeLimit(payload.value), 'ageNum', payload.stage, state),
          ageValue
        );
        break;
      }
      case 'report': {
        const { trimmedValue: ageValue, isPristine } = state.applicationReports.stages[payload.stage].maxAge;
        stageSelector.maxCount = userInput(
          () => inputValidator(payload.value, MAX_REPORTS, 'report', payload.stage, state),
          payload.value
        );
        if (!isPristine) {
          stageSelector.maxAge = userInput(
            () => inputValidator(ageValue, maxLimit, 'ageNum', payload.stage, state),
            ageValue
          );
        }
        break;
      }
      default:
    }
    state.applicationReports.stages[payload.stage] = stageSelector;
  }
  computeIsDirty(state);
};

const getMaxAgeLimit = (unit) => {
  switch (unit) {
    case 'days':
      return MAX_AGE_DAYS;
    case 'weeks':
      return MAX_AGE_WEEKS;
    case 'months':
      return MAX_AGE_MONTHS;
  }
  return MAX_AGE_YEARS;
};

const validateMin = function validateMin(limit, message, val) {
  if (!val) {
    return null;
  }
  return val < limit ? message : null;
};

const validateMax = function validateMax(limit, message, val) {
  if (!val) {
    return null;
  }
  return val > limit ? message : null;
};

const validateBothNonEmptyTogether = (val, inputField, state, stage) => {
  const currentStage = state.applicationReports.stages[stage];
  if (
    inputField === 'ageNum' &&
    !(val && val.length) &&
    !currentStage.maxCount.trimmedValue &&
    !currentStage.maxCount.isPristine
  )
    return 'Age or number of reports is required';
  return null;
};

const inputValidator = (val, max, inputField, stage, state) => {
  let errors = combineValidationErrors(
    validateMin(1, 'Minimum allowed value is 1', val),
    validateMax(max, `Maximum allowed value is ${max}`, val),
    validatePatternMatch(/^(0|[1-9][0-9]*|\s*)$/, 'This field only accepts numbers 0-9', val)
  );

  if (inputField === 'ageNum') {
    if (stage === 'successMetrics') {
      errors = combineValidationErrors(
        validateNonEmpty(val.trim()),
        validateMin(1, 'Minimum allowed value is 1', val),
        validateMax(max, `Maximum allowed value is ${max}`, val),
        validatePatternMatch(/^(0|[1-9][0-9]*)$/, 'This field only accepts numbers 0-9', val) //no spaces for success metrics
      );
      state.validationErrors[stage] = {
        ...state.validationErrors[stage],
        age: hasValidationErrors(errors) ? errors : null,
      };
    } else {
      const countValue = state.applicationReports.stages[stage].maxCount.trimmedValue;

      // if the count input has an input with no validation errors
      if (countValue && !hasValidationErrors(state.validationErrors?.[stage]?.count)) {
        state.validationErrors[stage] = {
          ...state.validationErrors[stage],
          age: hasValidationErrors(errors) ? errors : null,
        };
      } else {
        errors = combineValidationErrors([
          ...errors,
          validateBothNonEmptyTogether(val.trim(), inputField, state, stage),
          validateNonEmpty(val.trim()),
        ]);
        state.validationErrors[stage] = {
          ...state.validationErrors[stage],
          age: hasValidationErrors(errors) ? errors : null,
        };
      }
    }
  } else if (inputField === 'report') {
    const ageValue = state.applicationReports.stages[stage].maxAge.trimmedValue;
    if (ageValue && !hasValidationErrors(state.validationErrors?.[stage]?.age)) {
      state.validationErrors[stage] = {
        ...state.validationErrors[stage],
        count: hasValidationErrors(errors) ? errors : null,
      };
    } else {
      errors = combineValidationErrors([
        ...errors,
        validateBothNonEmptyTogether(val.trim(), inputField, state, stage),
        validateNonEmpty(val.trim()),
      ]);
      state.validationErrors[stage] = {
        ...state.validationErrors[stage],
        count: hasValidationErrors(errors) ? errors : null,
      };
    }
  }
  return errors;
};

const retentionSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setRadio,
    handleInputChange,
    saveMaskTimerDone: propSet('submitMaskState', null),
  },
  extraReducers: {
    [loadRetention.pending]: loadRetentionRequested,
    [loadRetention.fulfilled]: loadRetentionFulfilled,
    [loadRetention.rejected]: loadRetentionFailed,
    [updateRetention.pending]: updateRetentionRequested,
    [updateRetention.fulfilled]: updateRetentionFulfilled,
    [updateRetention.rejected]: updateRetentionFailed,
  },
});

export const actions = {
  ...retentionSlice.actions,
  loadRetention,
  goToEditRetention,
  updateRetention,
};

export default retentionSlice.reducer;
