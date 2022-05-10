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
  submitLoading: false,
  submitError: null,
};

const loadApplicableCategoriesRequested = (state) => {
  state.loadingApplicableCategories = true;
  state.loadApplicableCategoriesError = null;
};

const loadApplicableCategoriesFulfilled = (state, { payload }) => {
  state.loadingApplicableCategories = false;
  state.applicableCategories = payload;
};

const loadApplicableCategoriesFailed = (state, { payload }) => {
  state.loadingApplicableCategories = false;
  state.loadApplicableCategoriesError = Messages.getHttpErrorMessage(payload);
};

const loadApplicableCategories = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicableCategories`,
  (_, { rejectWithValue, getState }) => {
    const entityId = selectEntityId(getState());
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
  state.submitLoading = true;
  state.submitError = null;
};

const saveAppliedCategoriesFulfilled = (state) => {
  state.submitLoading = false;
  state.originalAppliedCategories = state.appliedCategories;
  state.isDirty = false;
};

const saveAppliedCategoriesFailed = (state, { payload }) => {
  state.submitLoading = false;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const saveAppliedCategories = createAsyncThunk(
  `${REDUCER_NAME}/saveAppliedCategories`,
  ({ onSaveAppliedCategories }, { rejectWithValue, getState }) => {
    const state = getState();
    const entityId = selectEntityId(state);
    const appliedCategories = selectAppliedCategories(state);
    return axios
      .put(getApplicationCategoriesUrl(entityId), appliedCategories)
      .then((categories) => {
        onSaveAppliedCategories();
        return categories.data;
      })
      .catch(rejectWithValue);
  }
);

const updateAppliedCategories = curryN(2, function updateAppliedCategories(state, { payload }) {
  const newAppliedCategories = ifElse(
    find(propEq('id', payload.id)),
    without([payload]),
    append(payload)
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
