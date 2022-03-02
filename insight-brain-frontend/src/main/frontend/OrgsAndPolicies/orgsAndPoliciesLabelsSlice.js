/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { prop, clone, curryN, isEmpty, isNil, any, reject, propEq, filter } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { selectRouterSlice, selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { Messages } from 'MainRoot/util/CommonServices';
import { getApplicableLabelsUrl, getLabelsUrl, getDeleteLabelsUrl } from 'MainRoot/util/CLMLocation';
import { pathSet, propSet } from 'MainRoot/util/jsUtil';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import {
  selectLabelsCurrentLabel,
  selectLabelsIsEditMode,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesLabelsSelectors';
import { actions as rootActions } from './orgsAndPoliciesRootSlice';
import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';
import { selectOwnerProperties } from './orgsAndPoliciesSelectors';

const REDUCER_NAME = 'orgsAndPoliciesLabels';

export const initialState = {
  applicableLabels: null,
  loadError: null,
  submitError: null,
  errorState: null,
  deleting: false,
  success: null,
  loading: false,
  currentLabel: {
    color: null,
    description: null,
    label: null,
  },
  serverCurrentLabel: null,
  siblings: [],
  isDirty: false,
};

const loadApplicableLabelsRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadApplicableLabelsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.applicableLabels = payload.labelsByOwner;
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
  state.currentLabel = payload.currentLabel;
  state.serverCurrentLabel = payload.currentLabel;
  state.siblings = payload.siblings;
};

const loadLabelsEditorFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const saveLabelFulfilled = (state, { payload }) => {
  state.submitError = null;
  state.isDirty = false;

  if (payload.isEditMode) {
    state.currentLabel = payload.label;
    state.serverCurrentLabel = payload.label;
  } else {
    state.currentLabel = initialState.currentLabel;
    state.serverCurrentLabel = initialState.currentLabel;
  }

  state.siblings.push(payload.label);
};

const saveLabelFailed = (state, { payload }) => {
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const removeLabelFulfilled = (state, { payload }) => {
  state.isDirty = false;
  state.success = true;
  state.deleting = null;
  state.errorState = null;
  state.currentLabel = initialState.currentLabel;
  state.serverCurrentLabel = initialState.currentLabel;
  state.siblings = reject(propEq('id', payload))(state.siblings);
};

const removeLabelFailed = (state, { payload }) => {
  state.deleting = false;
  state.errorState = Messages.getHttpErrorMessage(payload);
};

const resetDeleteModalState = (state) => {
  state.deleting = null;
  state.success = null;
  state.errorState = null;
};

const goToCreateLabel = createAsyncThunk(`${REDUCER_NAME}/goToCreateLabel`, (_, { getState, dispatch }) => {
  const router = selectRouterSlice(getState());
  const { to, params } = deriveEditRoute(router, 'create-label');

  dispatch(stateGo(to, params));
});

const goToEditLabel = createAsyncThunk(`${REDUCER_NAME}/goToEditLabel`, (labelId, { getState, dispatch }) => {
  const router = selectRouterSlice(getState());
  const { to, params } = deriveEditRoute(router, 'label', { labelId });

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
      .then(({ payload }) => {
        if (payload?.labelsByOwner) {
          const data = clone(payload);

          data.labelsByOwner.forEach((labels, idx) => {
            labels.inherited = idx > 0;
          });

          dispatch(rootActions.updatedOwnerHandler(data.labelsByOwner[0].ownerName));
          return data;
        }

        return rejectWithValue(payload);
      })
      .catch(rejectWithValue);
  }
);

const loadLabelsEditor = createAsyncThunk(
  `${REDUCER_NAME}/loadLabelsEditor`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const isEditMode = selectLabelsIsEditMode(getState());
    const labelsPromise = isEditMode ? dispatch(loadLabels()) : Promise.resolve({});

    dispatch(actions.resetIsDirty());

    return Promise.all([dispatch(loadApplicableLabelsByOwner()), labelsPromise])
      .then(([{ payload: applicableLabels }, currentLabel]) => {
        let siblings = [];

        if (applicableLabels?.labelsByOwner) {
          applicableLabels?.labelsByOwner.forEach((owner) => {
            siblings = siblings.concat(owner.labels);
          });

          const { labelId } = selectRouterCurrentParams(getState());
          const match = filter(propEq('id', labelId))(currentLabel?.payload || []);

          if (isEmpty(match) && isEditMode) {
            return rejectWithValue('Unable to locate label.');
          }

          return {
            siblings,
            currentLabel: match[0],
          };
        }

        return rejectWithValue(applicableLabels);
      })
      .catch(rejectWithValue);
  }
);

const saveLabel = createAsyncThunk(`${REDUCER_NAME}/saveLabel`, ({ setPristine }, { getState, rejectWithValue }) => {
  const state = getState();
  const { ownerType, ownerId } = selectOwnerProperties(state);
  const isEditMode = selectLabelsIsEditMode(getState());
  const label = selectLabelsCurrentLabel(state);

  setPristine();

  return axios[isEditMode ? 'put' : 'post'](getLabelsUrl(ownerType, ownerId), label)
    .then(({ data }) => {
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
      dispatch(actions.resetIsDirty());
      dispatch(goToCreateLabel());

      return label.id;
    })
    .catch(rejectWithValue);
});

const computeIsDirty = (state) => {
  const { currentLabel, serverCurrentLabel } = state;
  const validatableFields = ['color', 'label', 'description'];

  const isDirty = isNil(serverCurrentLabel)
    ? any((prop) => !isEmpty(currentLabel[prop]), validatableFields)
    : any((prop) => currentLabel[prop] !== serverCurrentLabel[prop], validatableFields);

  return propSet('isDirty', isDirty, state);
};

const setTextInput = curryN(3, function setTextInput(fieldName, state, { payload }) {
  return computeIsDirty(pathSet(['currentLabel', fieldName], payload, state));
});

const orgsAndPoliciesLabelsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setLabelDescription: setTextInput('description'),
    setLabelColor: setTextInput('color'),
    setLabelName: setTextInput('label'),
    resetIsDirty: propSet('isDirty', false),
    resetDeleteModalState,
  },
  extraReducers: {
    [loadApplicableLabels.pending]: loadApplicableLabelsRequested,
    [loadApplicableLabels.fulfilled]: loadApplicableLabelsFulfilled,
    [loadApplicableLabels.rejected]: loadApplicableLabelsFailed,

    [saveLabel.pending]: propSet('submitError', null),
    [saveLabel.fulfilled]: saveLabelFulfilled,
    [saveLabel.rejected]: saveLabelFailed,

    [removeLabel.pending]: propSet('deleting', true),
    [removeLabel.fulfilled]: removeLabelFulfilled,
    [removeLabel.rejected]: removeLabelFailed,

    [loadLabelsEditor.pending]: loadLabelsEditorRequested,
    [loadLabelsEditor.fulfilled]: loadLabelsEditorFulfilled,
    [loadLabelsEditor.rejected]: loadLabelsEditorFailed,
  },
});

export default orgsAndPoliciesLabelsSlice.reducer;
export const actions = {
  ...orgsAndPoliciesLabelsSlice.actions,
  loadLabels,
  loadApplicableLabels,
  loadLabelsEditor,
  saveLabel,
  removeLabel,
  goToEditLabel,
  goToCreateLabel,
  loadApplicableLabelsByOwner,
};
