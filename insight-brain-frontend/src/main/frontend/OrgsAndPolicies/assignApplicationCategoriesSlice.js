/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { append, curryN, equals, find, ifElse, map, omit, prop, propEq, without } from 'ramda';

import { getApplicableOrganizationCategories, getApplicationCategoriesUrl } from '../util/CLMLocation';
import { selectAppliedCategories } from './assignApplicationCategoriesSelectors';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { propSet } from '../util/jsUtil';
import { selectEntityId } from './orgsAndPoliciesSelectors';
import { selectRouterSlice } from '../reduxUiRouter/routerSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { deriveEditRoute } from './utility/util';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';

const REDUCER_NAME = 'applicationCategories/assign';

export const initialState = {
  loadingApplicableCategories: false,
  loadApplicableCategoriesError: null,
  applicableCategories: [],
  loadingAppliedCategories: false,
  loadAppliedCategoriesError: null,
  appliedCategories: [],
  originalAppliedCategories: [],
  isDirty: false,
  submitError: null,
  submitMaskState: null,
};

const loadApplicableCategoriesRequested = (state) => {
  state.loadingApplicableCategories = true;
  state.loadApplicableCategoriesError = null;
};

const loadApplicableCategoriesFulfilled = (state, { payload }) => {
  state.loadingApplicableCategories = false;
  state.applicableCategories = payload;
  state.isDirty = false;
};

const loadApplicableCategoriesFailed = (state, { payload }) => {
  state.loadingApplicableCategories = false;
  state.loadApplicableCategoriesError = Messages.getHttpErrorMessage(payload);
};

const loadApplicableCategories = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicableCategories`,
  (_, { rejectWithValue, getState, dispatch }) => {
    const entityId = selectEntityId(getState());
    dispatch(loadAppliedCategories());
    return axios.get(getApplicableOrganizationCategories(entityId)).then(prop('data')).catch(rejectWithValue);
  }
);

const loadAppliedCategoriesRequested = (state) => {
  state.loadingAppliedCategories = true;
  state.loadAppliedCategoriesError = null;
};

const loadAppliedCategoriesFulfilled = (state, { payload }) => {
  state.loadingAppliedCategories = false;
  // The service returns an extra field 'nameLowercaseNoWhitespace' that messes with the isDirty Validation so it is cleaned up here
  const appliedCategories = map(omit(['nameLowercaseNoWhitespace']), payload);
  state.appliedCategories = appliedCategories;
  state.originalAppliedCategories = appliedCategories;
  state.isDirty = false;
};

const loadAppliedCategoriesFailed = (state, { payload }) => {
  state.loadingAppliedCategories = false;
  state.loadAppliedCategoriesError = Messages.getHttpErrorMessage(payload);
};

const loadAppliedCategories = createAsyncThunk(
  `${REDUCER_NAME}/loadAppliedCategories`,
  (_, { rejectWithValue, getState }) => {
    const entityId = selectEntityId(getState());
    return axios.get(getApplicationCategoriesUrl(entityId)).then(prop('data')).catch(rejectWithValue);
  }
);

const saveAppliedCategoriesRequested = (state) => {
  state.submitError = null;
  state.submitMaskState = false;
};

const saveAppliedCategoriesFulfilled = (state) => {
  state.originalAppliedCategories = state.appliedCategories;
  state.isDirty = false;
  state.submitMaskState = true;
};

const saveAppliedCategoriesFailed = (state, { payload }) => {
  state.submitError = Messages.getHttpErrorMessage(payload);
  state.submitMaskState = null;
};

const saveAppliedCategories = createAsyncThunk(
  `${REDUCER_NAME}/saveAppliedCategories`,
  (_, { rejectWithValue, getState, dispatch }) => {
    const state = getState();
    const entityId = selectEntityId(state);
    const appliedCategories = selectAppliedCategories(state);
    return axios
      .put(getApplicationCategoriesUrl(entityId), appliedCategories)
      .then(({ data }) => {
        dispatch(loadAppliedCategories);
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone);
        return data;
      })
      .catch(rejectWithValue);
  }
);

const updateAppliedCategories = curryN(2, function updateAppliedCategories(state, { payload }) {
  const newPayload = omit(['isApplied'])(payload);
  const newAppliedCategories = ifElse(
    find(propEq('id', newPayload.id)),
    without([newPayload]),
    append(newPayload)
  )(state.appliedCategories);
  return computeIsDirty(propSet('appliedCategories', newAppliedCategories, state));
});

const computeIsDirty = (state) => {
  const { appliedCategories, originalAppliedCategories } = state;

  const isDirty = !equals(appliedCategories, originalAppliedCategories);

  return propSet('isDirty', isDirty, state);
};

const goToEditCategories = createAsyncThunk(`${REDUCER_NAME}/goToEditCategories`, (_, { getState, dispatch }) => {
  const router = selectRouterSlice(getState());
  const { to, params } = deriveEditRoute(router, 'category');

  dispatch(stateGo(to, params));
});

const assignApplicationCategoriesSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    updateAppliedCategories,
    saveMaskTimerDone: propSet('submitMaskState', null),
  },
  extraReducers: {
    [loadApplicableCategories.pending]: loadApplicableCategoriesRequested,
    [loadApplicableCategories.fulfilled]: loadApplicableCategoriesFulfilled,
    [loadApplicableCategories.rejected]: loadApplicableCategoriesFailed,
    [loadAppliedCategories.pending]: loadAppliedCategoriesRequested,
    [loadAppliedCategories.fulfilled]: loadAppliedCategoriesFulfilled,
    [loadAppliedCategories.rejected]: loadAppliedCategoriesFailed,
    [saveAppliedCategories.pending]: saveAppliedCategoriesRequested,
    [saveAppliedCategories.fulfilled]: saveAppliedCategoriesFulfilled,
    [saveAppliedCategories.rejected]: saveAppliedCategoriesFailed,
  },
});

export default assignApplicationCategoriesSlice.reducer;
export const actions = {
  ...assignApplicationCategoriesSlice.actions,
  loadApplicableCategories,
  loadAppliedCategories,
  saveAppliedCategories,
  goToEditCategories,
};
