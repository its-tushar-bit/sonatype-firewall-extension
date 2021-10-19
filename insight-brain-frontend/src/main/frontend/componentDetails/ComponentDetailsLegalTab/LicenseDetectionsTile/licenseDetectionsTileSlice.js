/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { equals, prop, find } from 'ramda';
import {
  initialState as initialStateHelper,
  userInput,
} from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';

import {
  getBaseLicenseOverrideUrl,
  getComponentLicensesUrl,
  getDeleteLicenseOverrideUrl,
  getLicenseOverrideUrl,
  getLicensesWithSyntheticFilterUrl,
} from '../../../util/CLMLocation';
import { Messages } from '../../../util/CommonServices';
import { propSet } from '../../../util/jsUtil';
import { toggleBooleanProp } from '../../../util/reduxUtil';
import { selectComponentDetailsRequestData } from '../../overview/overviewSelectors';
import { selectEditLicensesForm } from './licenseDetectionsTileSelectors';
import { selectSelectedComponent } from '../../../applicationReport/applicationReportSelectors';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { pathSetConst } from 'MainRoot/util/reduxToolkitUtil';

const REDUCER_NAME = 'componentDetailsLicenseDetectionsTile';

const initialState = {
  licenseOverride: null,
  declaredlicenses: null,
  effectiveLicenses: null,
  observedlicenses: null,
  selectableLicenses: null,
  allLicenses: null,
  loading: false,
  loadError: null,
  showEditLicensesPopover: false,
  editLicensesForm: {
    scope: null,
    comment: Object.freeze(initialStateHelper('')),
    status: null,
    isDirty: false,
    submitError: null,
    submitMaskState: null,
    fieldsPristineState: null,
  },
};

const startTimerToResetMaskAndReloadTileData = (dispatch) => {
  setTimeout(() => {
    dispatch(actions.resetSubmitMaskState());
    dispatch(actions.load());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
};

const getScopeWithLicenseOverride = find(prop('licenseOverride'));

const loadFulfilled = (state, { payload }) => {
  const {
    licenseOverride,
    declaredlicenses,
    effectiveLicenses,
    observedlicenses,
    selectableLicenses,
    allLicenses,
  } = payload;
  state.licenseOverride = licenseOverride ?? null;
  state.declaredlicenses = declaredlicenses ?? null;
  state.effectiveLicenses = effectiveLicenses ?? null;
  state.observedlicenses = observedlicenses ?? null;
  state.selectableLicenses = selectableLicenses ?? null;
  state.allLicenses = allLicenses ?? null;
  state.loading = false;
  state.loadError = null;

  const initialScope = getScopeWithLicenseOverride(licenseOverride) ?? licenseOverride[0] ?? null,
    initialStatus = initialScope?.licenseOverride?.status ?? null;
  state.editLicensesForm.isDirty = false;
  state.editLicensesForm.scope = initialScope;
  state.editLicensesForm.status = initialStatus;
  state.editLicensesForm.comment = Object.freeze(initialStateHelper(''));
  state.editLicensesForm.fieldsPristineState = {
    comment: '',
    scope: initialScope,
    status: initialStatus,
  };
};

function loadFailed(state, { payload }) {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
}

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { getState, rejectWithValue }) => {
  const {
    clientType,
    ownerType,
    ownerId,
    identificationSource,
    componentIdentifier,
    scanId,
  } = selectComponentDetailsRequestData(getState());
  const promises = [
    axios.get(getLicensesWithSyntheticFilterUrl()),
    axios.get(
      getComponentLicensesUrl({
        clientType,
        ownerType,
        ownerId,
        componentIdentifier,
        identificationSource,
        scanId,
      })
    ),
    axios.get(getLicenseOverrideUrl(ownerType, ownerId, componentIdentifier)),
  ];

  return Promise.all(promises)
    .then((results) => {
      const allLicenses = results[0].data;
      const { declaredlicenses, effectiveLicenses, observedlicenses, selectableLicenses } = results[1].data;
      const licenseOverride = results[2].data.licenseOverridesByOwner;
      return {
        licenseOverride,
        declaredlicenses,
        effectiveLicenses,
        observedlicenses,
        selectableLicenses,
        allLicenses,
      };
    })
    .catch(rejectWithValue);
});

/**
 * Checks if a form is dirty by comparing its current values with the pristine fields
 * @param {State} state the state to check if it's dirty
 */
const isFormDirty = (editLicensesFormState) => {
  const { comment, status, scope, fieldsPristineState } = editLicensesFormState;

  const currentFields = {
    status,
    scope,
    comment: comment.value,
  };

  return !equals(fieldsPristineState, currentFields);
};

const setIsDirtyFlag = (state) => ({
  ...state,
  editLicensesForm: {
    ...state.editLicensesForm,
    isDirty: isFormDirty(state.editLicensesForm),
  },
});

const setLicenseFormComment = (state, { payload }) =>
  setIsDirtyFlag({
    ...state,
    editLicensesForm: {
      ...state.editLicensesForm,
      comment: userInput(null, payload),
    },
  });

const setLicenseFormScope = (state, { payload }) =>
  setIsDirtyFlag({
    ...state,
    editLicensesForm: {
      ...state.editLicensesForm,
      scope: payload,
    },
  });

const setLicenseFormStatus = (state, { payload }) =>
  setIsDirtyFlag({
    ...state,
    editLicensesForm: {
      ...state.editLicensesForm,
      status: payload,
    },
  });

const saveEditLicensesForm = createAsyncThunk(
  `${REDUCER_NAME}/saveEditLicensesForm`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const { status, comment, scope } = selectEditLicensesForm(getState());
    const { componentIdentifier } = selectSelectedComponent(getState());
    const { ownerType, ownerId } = scope;
    const url = getBaseLicenseOverrideUrl(ownerType, ownerId),
      payload = {
        id: null,
        licenseIds: [],
        componentIdentifier,
        status: status,
        comment: comment.value || '',

        ownerId,
      };

    return axios
      .post(url, payload)
      .then(() => {
        startTimerToResetMaskAndReloadTileData(dispatch);
      })
      .catch(rejectWithValue);
  }
);

const deleteLicenseOverride = createAsyncThunk(
  `${REDUCER_NAME}/deleteLicenseOverride`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const { scope } = selectEditLicensesForm(getState());
    const { ownerType, ownerId, licenseOverride } = scope;

    if (!licenseOverride) {
      return startTimerToResetMaskAndReloadTileData(dispatch);
    }

    return axios
      .delete(getDeleteLicenseOverrideUrl(ownerType, ownerId, licenseOverride.id))
      .then(() => {
        startTimerToResetMaskAndReloadTileData(dispatch);
      })
      .catch(rejectWithValue);
  }
);

const saveEditLicensesFormRequested = (state) => {
  state.editLicensesForm.submitMaskState = false;
  state.editLicensesForm.submitError = null;
};

const saveEditLicensesFormFulfilled = (state) => {
  state.editLicensesForm.submitMaskState = true;
  state.editLicensesForm.submitError = null;
};

const saveEditLicensesFormFailed = (state, { payload }) => {
  state.editLicensesForm.submitMaskState = null;
  state.editLicensesForm.submitError = Messages.getHttpErrorMessage(payload);
};

const resetEditLicensesFormFields = (state) => {
  const { licenseOverride } = state;
  const initialScope = getScopeWithLicenseOverride(licenseOverride) ?? licenseOverride[0];
  state.editLicensesForm.isDirty = false;
  state.editLicensesForm.submitError = null;
  state.editLicensesForm.scope = initialScope ?? null;
  state.editLicensesForm.status = initialScope?.licenseOverride?.status ?? null;
  state.editLicensesForm.comment = Object.freeze(initialStateHelper(''));
};

const componentDetailsLicenseDetectionsTileSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    toggleShowEditLicensesPopover: toggleBooleanProp('showEditLicensesPopover'),
    setLicenseFormStatus,
    setLicenseFormScope,
    setLicenseFormComment,
    resetEditLicensesFormFields,
    resetSubmitMaskState: pathSetConst(['editLicensesForm', 'submitMaskState'], null),
  },
  extraReducers: {
    [load.pending]: propSet('loading', true),
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
    [saveEditLicensesForm.pending]: saveEditLicensesFormRequested,
    [saveEditLicensesForm.fulfilled]: saveEditLicensesFormFulfilled,
    [saveEditLicensesForm.rejected]: saveEditLicensesFormFailed,
    [deleteLicenseOverride.pending]: saveEditLicensesFormRequested,
    [deleteLicenseOverride.fulfilled]: saveEditLicensesFormFulfilled,
    [deleteLicenseOverride.rejected]: saveEditLicensesFormFailed,
  },
});

export default componentDetailsLicenseDetectionsTileSlice.reducer;
export const actions = {
  ...componentDetailsLicenseDetectionsTileSlice.actions,
  load,
  saveEditLicensesForm,
  deleteLicenseOverride,
};
