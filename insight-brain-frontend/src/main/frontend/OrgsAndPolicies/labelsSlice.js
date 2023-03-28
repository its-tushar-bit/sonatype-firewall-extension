/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { prop, curryN, isEmpty, isNil, any, reject, propEq, find, findIndex, equals } from 'ramda';
import { createAsyncThunk, createSlice, unwrapResult } from '@reduxjs/toolkit';
import { nxTextInputStateHelpers, combineValidationErrors } from '@sonatype/react-shared-components';
import { selectRouterSlice, selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getApplicableLabelsUrl, getLabelsUrl, getDeleteLabelsUrl } from 'MainRoot/util/CLMLocation';
import { pathSet, propSet } from 'MainRoot/util/jsUtil';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import {
  selectLabelsCurrentLabel,
  selectLabelsIsEditMode,
  selectPrevOwnerType,
  selectPrevOwnerId,
} from 'MainRoot/OrgsAndPolicies/labelsSelectors';
import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';
import { selectOwnerProperties } from './orgsAndPoliciesSelectors';
import {
  validateNonEmpty,
  validateNameCharacters,
  validateMaxLength,
  validateDoubleWhitespace,
} from 'MainRoot/util/validationUtil';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { rscToAngularColorMap } from './utility/util';

const { initialState: rscInitialState, userInput } = nxTextInputStateHelpers;

const REDUCER_NAME = 'labels';

export const initialState = {
  applicableLabels: null,
  inheritedLabelsOpen: null,
  loadError: null,
  submitError: null,
  deleteError: null,
  success: null,
  loading: false,
  currentLabel: {
    color: Object.values(rscToAngularColorMap)[0],
    description: rscInitialState(''),
    label: rscInitialState('', validateNonEmpty),
  },
  serverCurrentLabel: null,
  siblings: [],
  submitMaskState: null,
  deleteMaskState: null,
  isDirty: false,
  ownerType: null,
  ownerId: null,
  validationError: null,
};

const loadApplicableLabelsRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadApplicableLabelsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.applicableLabels = payload;
  const newInheritedLabelsOpen = {};
  payload.forEach((labels) => {
    newInheritedLabelsOpen[labels.ownerId] = true;
  });
  state.inheritedLabelsOpen = newInheritedLabelsOpen;
};

const loadApplicableLabelsFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadLabelsEditorRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadLabelsEditorFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.submitError = null;
  state.deleteError = null;
  const label = {
    ...payload.currentLabel,
    description: rscInitialState(payload?.currentLabel?.description ?? ''),
    label: rscInitialState(payload?.currentLabel?.label ?? '', validateNonEmpty),
  };

  state.currentLabel = label;
  state.serverCurrentLabel = payload.currentLabel;
  if (payload.siblings) {
    state.siblings = payload.siblings;
  }
};

const loadLabelsEditorFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const saveLabelPending = (state) => {
  state.submitError = null;
  state.submitMaskState = false;
};

const saveLabelFulfilled = (state, { payload }) => {
  state.submitError = null;
  state.isDirty = false;
  state.submitMaskState = true;

  if (payload.isEditMode) {
    const index = findIndex(propEq('id', payload.label.id), state.siblings);
    state.siblings[index] = payload.label;
    const label = {
      ...payload.label,
      description: rscInitialState(payload?.label?.description ?? ''),
      label: rscInitialState(payload?.label?.label ?? '', validateNonEmpty),
    };
    state.currentLabel = label;
    state.serverCurrentLabel = payload.label;
  } else {
    state.currentLabel = initialState.currentLabel;
    state.serverCurrentLabel = initialState.currentLabel;
    state.siblings.push(payload.label);
  }
};

const resetDeleteModalState = (state) => {
  state.deleting = null;
  state.success = null;
  state.errorState = null;
};

const saveLabelFailed = (state, { payload }) => {
  state.submitError = Messages.getHttpErrorMessage(payload);
  state.submitMaskState = null;
};

const removeLabelPending = (state) => {
  state.deleteError = null;
  state.deleteMaskState = false;
};

const removeLabelFulfilled = (state, { payload }) => {
  state.isDirty = false;
  state.deleteMaskState = true;
  state.deleteError = null;
  state.currentLabel = initialState.currentLabel;
  state.serverCurrentLabel = initialState.currentLabel;
  state.siblings = reject(propEq('id', payload))(state.siblings);
};

const removeLabelFailed = (state, { payload }) => {
  state.deleteMaskState = null;
  state.deleteError = Messages.getHttpErrorMessage(payload);
};

const clearDeleteError = (state) => {
  state.deleteError = null;
};

const setCurrentOwnerProps = (state, { payload }) => {
  state.ownerType = payload.ownerType;
  state.ownerId = payload.ownerId;
};

const goToCreateLabel = createAsyncThunk(`${REDUCER_NAME}/goToCreateLabel`, (_, { getState, dispatch }) => {
  const router = selectRouterSlice(getState());
  const { to, params } = deriveEditRoute(router, 'create-label');

  dispatch(stateGo(to, params));
});

const loadLabels = createAsyncThunk(`${REDUCER_NAME}/loadLabels`, (_, { getState, rejectWithValue }) => {
  const { ownerType, ownerId } = selectOwnerProperties(getState());
  return axios.get(getLabelsUrl(ownerType, ownerId)).then(prop('data')).catch(rejectWithValue);
});

const loadApplicableLabelsByOwner = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicableLabelsByOwner`,
  (_, { getState, rejectWithValue }) => {
    const { ownerType, ownerId } = selectOwnerProperties(getState());
    return axios.get(getApplicableLabelsUrl(ownerType, ownerId)).then(prop('data')).catch(rejectWithValue);
  }
);

const loadApplicableLabels = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicableLabels`,
  (_, { rejectWithValue, dispatch }) => {
    return dispatch(loadApplicableLabelsByOwner())
      .then((applicableLabelsPayload) => {
        const { labelsByOwner = [] } = unwrapResult(applicableLabelsPayload);

        labelsByOwner.forEach((labels, idx) => {
          labels.inherited = idx > 0;
        });

        return labelsByOwner;
      })
      .catch(rejectWithValue);
  }
);

const toggleInheritedLabelsOpen = (state, { payload }) => {
  state.inheritedLabelsOpen[payload] = !state.inheritedLabelsOpen[payload];
};

const loadApplicableLabelsByOwnerIfNeeded = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicableLabelsByOwnerIfNeeded`,
  (_, { getState, dispatch }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);
    const prevOwnerType = selectPrevOwnerType(state);
    const prevOwnerId = selectPrevOwnerId(state);

    if (!equals(ownerType, prevOwnerType) || !equals(prevOwnerId, ownerId)) {
      dispatch(actions.setCurrentOwnerProps({ ownerType, ownerId }));
      return dispatch(loadApplicableLabelsByOwner()).then(unwrapResult);
    }

    return Promise.resolve({});
  }
);

const loadLabelsEditor = createAsyncThunk(
  `${REDUCER_NAME}/loadLabelsEditor`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const isEditMode = selectLabelsIsEditMode(getState());
    const labelsPromise = isEditMode ? dispatch(loadLabels()) : Promise.resolve({});

    dispatch(actions.resetIsDirty());

    return Promise.all([dispatch(loadApplicableLabelsByOwnerIfNeeded()), labelsPromise])
      .then(([applicableLabelsPayload, currentLabel]) => {
        const { labelsByOwner } = unwrapResult(applicableLabelsPayload);
        const labels = unwrapResult(currentLabel);
        let siblings = [];

        if (labelsByOwner) {
          labelsByOwner.forEach((owner) => {
            siblings = siblings.concat(owner.labels);
          });
        } else {
          siblings = [];
        }

        const { labelId } = selectRouterCurrentParams(getState());
        const match = find(propEq('id', labelId), labels || []);

        if (!match && isEditMode) {
          return rejectWithValue('Unable to locate label.');
        }

        return {
          siblings,
          currentLabel: isEditMode
            ? match
            : { color: Object.values(rscToAngularColorMap)[0], description: '', label: '' },
        };
      })
      .catch(rejectWithValue);
  }
);

const saveLabel = createAsyncThunk(`${REDUCER_NAME}/saveLabel`, (_, { getState, rejectWithValue, dispatch }) => {
  const state = getState();
  const { ownerType, ownerId } = selectOwnerProperties(state);

  const isEditMode = selectLabelsIsEditMode(getState());
  const label = selectLabelsCurrentLabel(state);

  const newLabel = { ...label, description: label.description.trimmedValue, label: label.label.trimmedValue };

  return axios[isEditMode ? 'put' : 'post'](getLabelsUrl(ownerType, ownerId), newLabel)
    .then(({ data }) => {
      startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone);
      return {
        label: data,
        isEditMode,
      };
    })
    .catch(rejectWithValue);
});

const removeLabel = createAsyncThunk(`${REDUCER_NAME}/removeLabel`, (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const { ownerType, ownerId } = selectOwnerProperties(state);
  const label = selectLabelsCurrentLabel(state);

  return axios
    .delete(getDeleteLabelsUrl(ownerType, ownerId, label.id))
    .then(() => {
      startSaveMaskSuccessTimer(dispatch, actions.deleteMaskTimerDone);
      dispatch(actions.resetIsDirty());
      dispatch(goToCreateLabel());

      return label.id;
    })
    .catch(rejectWithValue);
});

const computeIsDirty = (state) => {
  const { currentLabel, serverCurrentLabel } = state;
  const computedCurrentLabel = {
    ...currentLabel,
    description: currentLabel?.description.trimmedValue,
    label: currentLabel?.label.trimmedValue,
  };
  const validatableFields = ['color', 'label', 'description'];

  const isDirty = isNil(serverCurrentLabel)
    ? any((prop) => !isEmpty(currentLabel[prop]), validatableFields)
    : any((prop) => computedCurrentLabel[prop] !== serverCurrentLabel[prop], validatableFields);

  return propSet('isDirty', isDirty, state);
};

const duplicationValidator = (state, value) => {
  const exists = any(
    (item) => item.label?.toLowerCase() === value.toLowerCase() && item.id !== state.currentLabel?.id,
    state.siblings
  );

  return exists ? 'Name is already in use' : null;
};

const labelNameValidator = (state, val) =>
  combineValidationErrors(
    validateNameCharacters(val),
    validateNonEmpty(val.trim()),
    validateDoubleWhitespace(val),
    validateMaxLength(50, val),
    duplicationValidator(state, val.trim())
  );

const setTextInput = curryN(3, function setTextInput(fieldName, validationFunc, state, { payload }) {
  return computeIsDirty(
    pathSet(
      ['currentLabel', fieldName],
      fieldName === 'label'
        ? userInput(() => validationFunc(state, payload), payload)
        : userInput(validationFunc, payload),
      state
    )
  );
});

const setColorInput = curryN(2, function setTextInput(state, { payload }) {
  return computeIsDirty(pathSet(['currentLabel', 'color'], payload, state));
});

const labelsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setLabelDescription: setTextInput('description', validateMaxLength(255)),
    setLabelColor: setColorInput,
    setLabelName: setTextInput('label', labelNameValidator),
    resetIsDirty: propSet('isDirty', false),
    setCurrentOwnerProps,
    saveMaskTimerDone: propSet('submitMaskState', null),
    deleteMaskTimerDone: propSet('deleteMaskState', null),
    resetDeleteModalState,
    clearDeleteError,
    toggleInheritedLabelsOpen,
  },
  extraReducers: {
    [loadApplicableLabels.pending]: loadApplicableLabelsRequested,
    [loadApplicableLabels.fulfilled]: loadApplicableLabelsFulfilled,
    [loadApplicableLabels.rejected]: loadApplicableLabelsFailed,

    [saveLabel.pending]: saveLabelPending,
    [saveLabel.fulfilled]: saveLabelFulfilled,
    [saveLabel.rejected]: saveLabelFailed,

    [removeLabel.pending]: removeLabelPending,
    [removeLabel.fulfilled]: removeLabelFulfilled,
    [removeLabel.rejected]: removeLabelFailed,

    [loadLabelsEditor.pending]: loadLabelsEditorRequested,
    [loadLabelsEditor.fulfilled]: loadLabelsEditorFulfilled,
    [loadLabelsEditor.rejected]: loadLabelsEditorFailed,
  },
});

export default labelsSlice.reducer;
export const actions = {
  ...labelsSlice.actions,
  loadLabels,
  loadApplicableLabels,
  loadLabelsEditor,
  saveLabel,
  removeLabel,
  goToCreateLabel,
  loadApplicableLabelsByOwner,
};
