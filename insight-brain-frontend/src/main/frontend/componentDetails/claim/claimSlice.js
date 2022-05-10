/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import moment from 'moment';
import { pick, map, compose, any, curryN, always, clone, isNil } from 'ramda';
import {
  nxTextInputStateHelpers,
  nxDateInputStateHelpers,
  SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS,
} from '@sonatype/react-shared-components';

import { propSetConst } from '../../util/reduxToolkitUtil';
import { isNilOrEmpty, pathSet } from '../../util/jsUtil';
import { toggleBooleanProp } from '../../util/reduxUtil';
import { Messages } from '../../utilAngular/CommonServices';
import { validateForm, validateNonEmpty, validatePatternMatch } from '../../util/validationUtil';
import { getClaimComponentUrl } from '../../util/CLMLocation';
import { selectSelectedComponentHash, selectClaimRequestData, selectClaimId, DATE_FORMAT } from './claimSelectors';

const { initialState: initUserInput, userInput } = nxTextInputStateHelpers;
const { initialState: initUserDateInput, userInput: userDateInput } = nxDateInputStateHelpers;

const CREATE_TIME_REGEX = /^(199\d|[2-9]\d{3})-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$/;

const REDUCER_NAME = 'componentDetailsClaim';

export const initialState = {
  loading: false,
  loadError: null,
  inputFields: {
    artifactId: initUserInput(''),
    classifier: initUserInput(''),
    extension: initUserInput(''),
    groupId: initUserInput(''),
    version: initUserInput(''),
    comment: initUserInput(''),
    createTime: initUserDateInput(''),
  },
  serverData: null,
  isDirty: false,
  claimMaskState: null,
  revokeMaskState: null,
  claimError: null,
  revokeError: null,
  validationError: null,
  showRevokeModal: false,
};

const loadComponentIdentified = createAsyncThunk(
  `${REDUCER_NAME}/loadComponentIdentified`,
  (_, { getState, rejectWithValue }) => {
    const hash = selectSelectedComponentHash(getState());
    return axios
      .get(getClaimComponentUrl(hash))
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

const claim = createAsyncThunk(`${REDUCER_NAME}/claim`, (_, { getState, dispatch, rejectWithValue }) => {
  const data = selectClaimRequestData(getState());
  const update = !!selectClaimId(getState());

  const request = axios[update ? 'put' : 'post'](getClaimComponentUrl(), data);
  return request
    .then(({ data }) => {
      startClaimMaskSuccessTimer(dispatch);
      return data;
    })
    .catch(rejectWithValue);
});

const revoke = createAsyncThunk(`${REDUCER_NAME}/revoke`, (_, { getState, dispatch, rejectWithValue }) => {
  const hash = selectSelectedComponentHash(getState());

  return axios
    .delete(getClaimComponentUrl(hash))
    .then(() => startRevokeMaskSuccessTimer(dispatch))
    .catch(rejectWithValue);
});

const extractInputFieldsData = (payload) => {
  const coordinates = pick(
    ['artifactId', 'classifier', 'extension', 'groupId', 'version'],
    payload.componentIdentifier.coordinates
  );
  const createTime = isNil(payload.createTime) ? '' : moment(payload.createTime).format(DATE_FORMAT);

  return {
    ...map((coordinate) => initUserInput(coordinate ?? ''), coordinates),
    comment: initUserInput(payload.comment ?? ''),
    createTime: initUserDateInput(createTime),
  };
};

const loadFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.inputFields = extractInputFieldsData(payload);
  state.serverData = payload;
};

const loadFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = payload.response?.status === 404 ? null : Messages.getHttpErrorMessage(payload);
};

const claimPending = (state) => {
  state.claimMaskState = false;
  state.claimError = null;
};

const claimFulfilled = (state, { payload }) => {
  state.claimMaskState = true;
  state.claimError = null;
  state.inputFields = extractInputFieldsData(payload);
  state.serverData = payload;
  state.loading = false;
  state.isDirty = false;
};

const claimFailed = (state, { payload }) => {
  state.claimMaskState = null;
  state.loading = false;
  state.claimError = Messages.getHttpErrorMessage(payload);
};

const revokePending = (state) => {
  state.revokeMaskState = false;
  state.revokeError = null;
};

const revokeFulfilled = (state) => {
  state.revokeMaskState = true;
  state.revokeError = null;
  state.inputFields = initialState.inputFields;
  state.serverData = null;
  state.loading = false;
  state.isDirty = false;
};

const revokeFailed = (state, { payload }) => {
  state.revokeMaskState = null;
  state.loading = false;
  state.revokeError = Messages.getHttpErrorMessage(payload);
};

function computeValidationError(state) {
  const { inputFields } = state;
  const requiredFields = ['artifactId', 'extension', 'groupId', 'version'];
  if (inputFields.createTime.trimmedValue) {
    requiredFields.push('createTime');
  }
  const fields = pick(requiredFields, inputFields);
  const validationError = validateForm(clone(fields));

  return propSetConst(['validationError'], validationError, state);
}

function computeIsDirty(state) {
  const { inputFields, serverData } = state;
  let isTextPropDirty;

  if (!isNilOrEmpty(serverData)) {
    const serverDataValues = {
      ...pick(
        ['artifactId', 'classifier', 'extension', 'groupId', 'version'],
        serverData.componentIdentifier.coordinates
      ),
      comment: serverData.comment,
      createTime: isNil(serverData.createTime) ? '' : moment(serverData.createTime).format(DATE_FORMAT),
    };

    isTextPropDirty = (prop) => inputFields[prop].trimmedValue !== serverDataValues[prop];
  } else {
    isTextPropDirty = (prop) => inputFields[prop].trimmedValue !== '';
  }

  const textPropsDirty = any(isTextPropDirty, [
    'artifactId',
    'classifier',
    'extension',
    'groupId',
    'version',
    'comment',
    'createTime',
  ]);

  return propSetConst('isDirty', textPropsDirty, state);
}

const updatedComputedProps = compose(computeValidationError, computeIsDirty);

const getStateWithUpdatedValue = (fieldName, validator, state, { payload }) =>
  pathSet(['inputFields', fieldName], userInput(validator, payload), state);

const getStateWithUpdatedDateValue = (fieldName, validator, state, { payload }) =>
  pathSet(['inputFields', fieldName], userDateInput(validator, payload), state);

const setTextInput = curryN(4, function setTextInput(fieldName, validator, state, { payload }) {
  return updatedComputedProps(getStateWithUpdatedValue(fieldName, validator, state, { payload }));
});

const setDateInput = curryN(4, function setDateInput(fieldName, validator, state, { payload }) {
  return updatedComputedProps(getStateWithUpdatedDateValue(fieldName, validator, state, { payload }));
});

function startClaimMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch(actions.claimMaskTimerDone());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

function startRevokeMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch(actions.revokeMaskTimerDone());
    dispatch(actions.toggleShowRevokeModal());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const resetForm = (state) => {
  state.inputFields = isNilOrEmpty(state.serverData)
    ? initialState.inputFields
    : extractInputFieldsData(state.serverData);
  state.isDirty = false;
  state.claimError = null;
};

const validateDateBoundaries = (value) => {
  if (value) {
    return validatePatternMatch(CREATE_TIME_REGEX, 'Date format is DD.MM.YYYY')(value);
  }

  return null;
};

const componentDetailsClaimSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetForm,
    resetTab: always(initialState),
    setGroupId: setTextInput('groupId', validateNonEmpty),
    setExtension: setTextInput('extension', validateNonEmpty),
    setArtifactId: setTextInput('artifactId', validateNonEmpty),
    setCreatedTime: setDateInput('createTime', validateDateBoundaries),
    setVersion: setTextInput('version', validateNonEmpty),
    setClassifier: setTextInput('classifier', null),
    setComment: setTextInput('comment', null),
    claimMaskTimerDone: propSetConst('claimMaskState', null),
    revokeMaskTimerDone: propSetConst('revokeMaskState', null),
    resetRevokeError: propSetConst('revokeError', null),
    toggleShowRevokeModal: toggleBooleanProp('showRevokeModal'),
  },
  extraReducers: {
    [loadComponentIdentified.pending]: propSetConst('loading', true),
    [loadComponentIdentified.fulfilled]: loadFulfilled,
    [loadComponentIdentified.rejected]: loadFailed,
    [claim.pending]: claimPending,
    [claim.fulfilled]: claimFulfilled,
    [claim.rejected]: claimFailed,
    [revoke.pending]: revokePending,
    [revoke.fulfilled]: revokeFulfilled,
    [revoke.rejected]: revokeFailed,
  },
});

export default componentDetailsClaimSlice.reducer;
export const actions = {
  ...componentDetailsClaimSlice.actions,
  loadComponentIdentified,
  claim,
  revoke,
};
