/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { always, equals, prop, find, map } from 'ramda';
import {
  initialState as initialStateHelper,
  userInput,
} from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import {
  getBaseLicenseOverrideUrl,
  getComponentMultiLicensesUrl,
  getDeleteLicenseOverrideUrl,
  getLicenseOverrideUrl,
  getLicensesWithSyntheticFilterUrl,
} from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { propSet } from 'MainRoot/util/jsUtil';
import { toggleBooleanProp } from 'MainRoot/util/reduxUtil';
import { selectComponentDetailsRequestData } from 'MainRoot/componentDetails/overview/overviewSelectors';
import { selectEditLicensesForm } from './licenseDetectionsTileSelectors';
import { selectSelectedComponent } from 'MainRoot/applicationReport/applicationReportSelectors';
import { pathSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { isOverriddenOrSelected } from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LegalTabUtils';
import { SELECT_COMPONENT } from 'MainRoot/applicationReport/applicationReportActions';
import { selectFirewallComponentDetailsPageRouteParams } from 'MainRoot/firewall/firewallSelectors';
import { selectIsFirewallOrRepository } from 'MainRoot/reduxUiRouter/routerSelectors';
import { loadComponentLicenses } from 'MainRoot/firewall/firewallActions';

const REDUCER_NAME = 'componentDetailsLicenseDetectionsTile';

export const initialState = {
  licenseOverride: null,
  declaredLicenses: null,
  effectiveLicenses: null,
  observedLicenses: null,
  selectableLicenses: null,
  allLicenses: null,
  hiddenObservedLicenses: false,
  supportAlpObservedLicenses: false,
  loading: false,
  loadError: null,
  showEditLicensesPopover: false,
  editLicensesForm: {
    scope: null,
    comment: Object.freeze(initialStateHelper('')),
    licenseIds: [],
    status: null,
    isDirty: false,
    submitError: null,
    submitMaskState: null,
    fieldsPristineState: null,
    showUnsavedChangesModal: false,
  },
};

const getInitialFormFieldsFromLicenseOverride = (licenseOverride) => {
  const scope = getScopeWithLicenseOverride(licenseOverride) ?? licenseOverride[0] ?? null;
  return {
    scope,
    status: scope?.licenseOverride?.status ?? null,
    licenseIds: isOverriddenOrSelected(scope?.licenseOverride?.status) ? scope?.licenseOverride?.licenseIds : [],
    comment: initialStateHelper(scope?.licenseOverride?.comment ?? ''),
  };
};

const startTimerToResetMaskAndReloadTileData = (dispatch) => {
  setTimeout(() => {
    dispatch(actions.resetSubmitMaskState());
    dispatch(actions.load());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
};

const startFirewallTimerToResetMaskAndReloadTileData = (dispatch, repositoryId, componentIdentifier) => {
  setTimeout(() => {
    dispatch(actions.resetSubmitMaskState());
    dispatch(loadComponentLicenses(repositoryId, componentIdentifier));
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
};

const getScopeWithLicenseOverride = find(prop('licenseOverride'));

const loadFulfilled = (state, { payload }) => {
  const {
    licenseOverride,
    declaredLicenses,
    effectiveLicenses,
    observedLicenses,
    selectableLicenses,
    allLicenses,
    hiddenObservedLicenses,
    supportAlpObservedLicenses,
  } = payload;
  state.licenseOverride = licenseOverride ?? null;
  state.declaredLicenses = declaredLicenses ?? null;
  state.effectiveLicenses = effectiveLicenses ?? null;
  state.observedLicenses = observedLicenses ?? null;
  state.selectableLicenses = selectableLicenses ?? null;
  state.allLicenses = allLicenses ?? null;
  state.hiddenObservedLicenses = hiddenObservedLicenses ?? false;
  state.supportAlpObservedLicenses = supportAlpObservedLicenses ?? false;
  state.loading = false;
  state.loadError = null;

  const { scope, licenseIds, status, comment } = getInitialFormFieldsFromLicenseOverride(licenseOverride);
  state.editLicensesForm.licenseIds = licenseIds;
  state.editLicensesForm.isDirty = false;
  state.editLicensesForm.scope = scope;
  state.editLicensesForm.status = status;
  state.editLicensesForm.comment = comment;
  state.editLicensesForm.fieldsPristineState = {
    comment: '',
    scope,
    status,
    licenseIds,
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
      getComponentMultiLicensesUrl({
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
      const allLicenses = map(({ id, shortDisplayName }) => ({ id, displayName: shortDisplayName }), results[0].data);
      const {
        declaredLicenses,
        observedLicenses,
        effectiveLicenses,
        selectableLicenses,
        hiddenObservedLicenses,
        supportAlpObservedLicenses,
      } = results[1].data;
      const licenseOverride = results[2].data.licenseOverridesByOwner;
      return {
        licenseOverride,
        declaredLicenses,
        effectiveLicenses,
        observedLicenses,
        selectableLicenses,
        allLicenses,
        hiddenObservedLicenses,
        supportAlpObservedLicenses,
      };
    })
    .catch(rejectWithValue);
});

/**
 * Checks if a form is dirty by comparing its current values with the pristine fields
 * @param {State} state the state to check if it's dirty
 */
const isFormDirty = (editLicensesFormState) => {
  const { comment, status, scope, licenseIds, fieldsPristineState } = editLicensesFormState;

  const currentFields = {
    status,
    scope,
    comment: comment.value,
    licenseIds,
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

const setLicenseFormLicenseIds = (state, { payload }) =>
  setIsDirtyFlag({
    ...state,
    editLicensesForm: {
      ...state.editLicensesForm,
      licenseIds: payload,
    },
  });

const saveEditLicensesForm = createAsyncThunk(
  `${REDUCER_NAME}/saveEditLicensesForm`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const { status, comment, scope, licenseIds } = selectEditLicensesForm(getState());
    const { isRepositoryComponent, component, componentIdentifier } = getComponentInfo(getState);
    const { ownerType, ownerId } = scope;
    const payloadLicenseIds = isOverriddenOrSelected(status) ? licenseIds : [];
    const url = getBaseLicenseOverrideUrl(ownerType, ownerId),
      payload = {
        id: null,
        licenseIds: payloadLicenseIds,
        componentIdentifier,
        status,
        comment: comment.value || '',

        ownerId,
      };
    return axios
      .post(url, payload)
      .then(() => {
        if (isRepositoryComponent) {
          startFirewallTimerToResetMaskAndReloadTileData(
            dispatch,
            component.repositoryId,
            component.componentIdentifier
          );
        } else {
          startTimerToResetMaskAndReloadTileData(dispatch);
        }
      })
      .catch((error) => {
        return rejectWithValue(error);
      });
  }
);

const getComponentInfo = (getState) => {
  let componentIdentifier, component;
  const isRepositoryComponent = selectIsFirewallOrRepository(getState());
  if (isRepositoryComponent) {
    component = selectFirewallComponentDetailsPageRouteParams(getState());
    componentIdentifier = JSON.parse(component?.componentIdentifier);
  } else {
    component = selectSelectedComponent(getState());
    componentIdentifier = component?.componentIdentifier;
  }
  return { isRepositoryComponent, component, componentIdentifier };
};

const deleteLicenseOverride = createAsyncThunk(
  `${REDUCER_NAME}/deleteLicenseOverride`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const { scope } = selectEditLicensesForm(getState());
    const { ownerType, ownerId, licenseOverride } = scope;
    const { isRepositoryComponent, component } = getComponentInfo(getState);

    if (!licenseOverride) {
      return startTimerToResetMaskAndReloadTileData(dispatch);
    }

    return axios
      .delete(getDeleteLicenseOverrideUrl(ownerType, ownerId, licenseOverride.id))
      .then(() => {
        if (isRepositoryComponent) {
          startFirewallTimerToResetMaskAndReloadTileData(
            dispatch,
            component.repositoryId,
            component.componentIdentifier
          );
        } else {
          startTimerToResetMaskAndReloadTileData(dispatch);
        }
      })
      .catch((error) => {
        return rejectWithValue(error);
      });
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

  state.editLicensesForm = {
    ...state.editLicensesForm,
    ...getInitialFormFieldsFromLicenseOverride(licenseOverride),
    isDirty: false,
    submitError: null,
    showUnsavedChangesModal: false,
  };
};

const setShowUnsavedChangesModal = (state, { payload }) => {
  state.editLicensesForm.showUnsavedChangesModal = payload;
};

const componentDetailsLicenseDetectionsTileSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    toggleShowEditLicensesPopover: toggleBooleanProp('showEditLicensesPopover'),
    setLicenseFormStatus,
    setLicenseFormScope,
    setLicenseFormComment,
    setLicenseFormLicenseIds,
    resetEditLicensesFormFields,
    resetSubmitMaskState: pathSetConst(['editLicensesForm', 'submitMaskState'], null),
    setShowUnsavedChangesModal,
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
    [SELECT_COMPONENT]: always(initialState),
  },
});

export default componentDetailsLicenseDetectionsTileSlice.reducer;
export const actions = {
  ...componentDetailsLicenseDetectionsTileSlice.actions,
  load,
  saveEditLicensesForm,
  deleteLicenseOverride,
};
