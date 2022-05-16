/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getConditionTypeUrl, getConditionValueTypeUrl } from 'MainRoot/util/CLMLocation';
import { selectOwnerProperties } from './orgsAndPoliciesSelectors';

import { Messages } from 'MainRoot/utilAngular/CommonServices';

const REDUCER_NAME = 'constraint';

export const initialState = {
  loading: false,
  isDirty: false,
  loadError: null,
  conditionTypes: null,
  conditionTypesMap: null,
  editConstraintMap: {},
};

const loadConstraintRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadConstraintFulfilled = (state, { payload }) => {
  state.loading = false;
  state.conditionTypes = payload.conditionTypes;
  state.conditionTypesMap = payload.conditionTypesMap;
  state.editConstraintMap = payload.editConstraintMap;
};

const loadConstraintFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const updateEditConstraintId = (state, { payload }) => {
  state.editConstraintMap[payload] = true;
};

const loadConstraint = createAsyncThunk(
  `${REDUCER_NAME}/loadConstraint`,
  ({ isNewPolicy, constraints }, { getState, rejectWithValue }) => {
    const { ownerType, ownerId } = selectOwnerProperties(getState());
    const promises = [axios.get(getConditionTypeUrl()), axios.get(getConditionValueTypeUrl(ownerType, ownerId))];

    return Promise.all(promises)
      .then(([{ data: allConditionTypes }, { data: allConditionValues }]) => {
        const typeValues = {};
        const conditionTypesMap = {};
        const editConstraintMap = {};

        allConditionValues.forEach((typeValue) => {
          typeValues[typeValue.id] = typeValue;
        });

        allConditionTypes.forEach((type) => {
          type.valueType = type.valueTypeId ? typeValues[type.valueTypeId] : null;
          conditionTypesMap[type.id] = type;
        });

        if (isNewPolicy) {
          editConstraintMap[constraints[0].id] = true;
        }

        return {
          conditionTypes: allConditionTypes.filter((type) => type.enabled),
          conditionTypesMap,
          editConstraintMap,
        };
      })
      .catch(rejectWithValue);
  }
);

const constraintSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    updateEditConstraintId,
  },
  extraReducers: {
    [loadConstraint.pending]: loadConstraintRequested,
    [loadConstraint.fulfilled]: loadConstraintFulfilled,
    [loadConstraint.rejected]: loadConstraintFailed,
  },
});

export const actions = {
  ...constraintSlice.actions,
  loadConstraint,
};

export default constraintSlice.reducer;
