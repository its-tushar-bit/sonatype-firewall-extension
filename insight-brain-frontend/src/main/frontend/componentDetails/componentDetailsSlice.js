/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { curryN, reduce } from 'ramda';
import { enableMapSet } from 'immer';

import { stateGo } from '../reduxUiRouter/routerActions';
import { loadReport } from '../applicationReport/applicationReportActions';
import { selectComponentDetails } from './componentDetailsSelectors';
import { selectSelectedComponent } from '../applicationReport/applicationReportSelectors';
import { getComponentLabels } from '../util/CLMLocation';
import { Messages } from '../util/CommonServices';

const REDUCER_NAME = 'componentDetails';
export const VISIT_ANCESTOR_ACTION = REDUCER_NAME + '/visitAncestors';
export const RETURN_TO_OFFSPRING = REDUCER_NAME + '/backToOffspring';
const COMPONENT_DETAILS_OVERVIEW_ROUTE_NAME = 'applicationReport.componentDetails.overview';
enableMapSet();

const initialState = Object.freeze({
  pendingLoads: new Set(),
  isVisitingAncestor: false,
  offspring: null,
  labels: [],
  loadError: null,
});

const mutatePendingLoads = curryN(3, function mutatePendingLoads(setMutator, loads, state) {
  const { pendingLoads } = state;
  const newPendingLoads = new Set(pendingLoads);

  loads.forEach(setMutator(newPendingLoads));

  return { ...state, pendingLoads: newPendingLoads };
});

const setPendingLoads = mutatePendingLoads((set) => (val) => set.add(val));
const unsetPendingLoads = mutatePendingLoads((set) => (val) => set.delete(val));

const onTabChange = (tabId) => {
  return (dispatch, getState) => {
    const componentDetails = selectComponentDetails(getState());
    return dispatch(stateGo(`applicationReport.componentDetails.${tabId}`, { hash: componentDetails.hash }));
  };
};

const visitAncestorAction = (hash) => {
  return (dispatch, getState) => {
    const state = getState();
    const component = selectSelectedComponent(state);

    dispatch(
      actions.visitAncestors({
        offspring: {
          derivedComponentName: component.derivedComponentName,
          hash: component.hash,
        },
      })
    );
    dispatch(stateGo(COMPONENT_DETAILS_OVERVIEW_ROUTE_NAME, { hash }));
  };
};

const backToOffspringAction = (hash) => {
  return (dispatch) => {
    dispatch(actions.backToOffspring());
    dispatch(stateGo(COMPONENT_DETAILS_OVERVIEW_ROUTE_NAME, { hash }));
  };
};

const visitAncestors = (state, { payload }) => {
  return {
    ...state,
    offspring: payload.offspring,
    isVisitingAncestor: true,
  };
};

const backToOffspring = (state) => {
  return {
    ...state,
    offspring: null,
    isVisitingAncestor: false,
  };
};

const loadComponentDetailsRequested = (state) => {
  return setPendingLoads(['labels'], state);
};

const loadComponentDetailsFulfilled = (state, { payload }) => {
  const labelsByOwner = payload[0].data.labelsByOwner;
  const labels = reduce((accumulator, currentValue) => [...accumulator, ...currentValue.labels], [], labelsByOwner);
  return unsetPendingLoads(['labels'], {
    ...state,
    labels: labels,
    loadError: null,
  });
};

function loadComponentDetailsFailed(state, { payload }) {
  return unsetPendingLoads(['labels'], { ...state, loadError: Messages.getHttpErrorMessage(payload) });
}

const loadComponentDetails = createAsyncThunk(
  `${REDUCER_NAME}/loadComponentDetails`,
  (_, { dispatch, getState, rejectWithValue }) => {
    const { publicId, hash } = getState().router.currentParams;
    const promises = [axios.get(getComponentLabels(publicId, hash)), dispatch(loadReport(true))];

    return Promise.all(promises)
      .then((results) => results)
      .catch(rejectWithValue);
  }
);

const componentDetailsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    visitAncestors,
    backToOffspring,
  },
  extraReducers: {
    [loadComponentDetails.pending]: loadComponentDetailsRequested,
    [loadComponentDetails.fulfilled]: loadComponentDetailsFulfilled,
    [loadComponentDetails.rejected]: loadComponentDetailsFailed,
  },
});

export default componentDetailsSlice.reducer;
export const actions = {
  ...componentDetailsSlice.actions,
  loadComponentDetails,
  onTabChange,
  visitAncestorAction,
  backToOffspringAction,
};
