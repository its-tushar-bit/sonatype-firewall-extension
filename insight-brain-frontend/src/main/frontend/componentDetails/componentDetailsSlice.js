/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { curryN, reduce, prop, sortWith, ascend } from 'ramda';
import { enableMapSet } from 'immer';

import { stateGo } from '../reduxUiRouter/routerActions';
import { loadReportIfNeeded } from '../applicationReport/applicationReportActions';
import { selectComponentDetails } from './componentDetailsSelectors';
import { selectSelectedComponent } from '../applicationReport/applicationReportSelectors';
import { selectComponentDetailsRequestData } from './overview/overviewSelectors';
import { getComponentLabels, setProprietaryMatchers, getApplicableLabels } from '../util/CLMLocation';
import { Messages } from '../util/CommonServices';
import { toggleBooleanProp } from '../util/reduxUtil';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { pathSet, pathSetConst } from 'MainRoot/util/reduxToolkitUtil';

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
  applicableLabels: [],
  loadError: null,
  applicableLabelsLoadError: null,
  showMatchersPopover: false,
  setProprietaryMatchers: {
    submitMaskState: null,
    submitError: null,
    data: { pathnames: [], regex: '' },
  },
});

const mutatePendingLoads = curryN(3, function mutatePendingLoads(setMutator, loads, state) {
  const { pendingLoads } = state;
  const newPendingLoads = new Set(pendingLoads);

  loads.forEach(setMutator(newPendingLoads));

  return { ...state, pendingLoads: newPendingLoads };
});

const setPendingLoads = mutatePendingLoads((set) => (val) => set.add(val));
const unsetPendingLoads = mutatePendingLoads((set) => (val) => set.delete(val));

const flattenLabelsToSingleArray = (labels) =>
  reduce((accumulator, currentValue) => [...accumulator, ...currentValue.labels], [], labels);

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
  const labelsByOwner = payload.data.labelsByOwner;
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
    return dispatch(loadReportIfNeeded())
      .then(() => {
        const { publicId, hash } = getState().router.currentParams;
        return axios.get(getComponentLabels(publicId, hash));
      })
      .then((results) => results)
      .catch(rejectWithValue);
  }
);

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch(actions.resetSubmitMaskState());
    dispatch(actions.toggleShowMatchersPopover());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const addProprietaryMatchersRequested = (state) => {
  state.setProprietaryMatchers.submitMaskState = false;
  state.setProprietaryMatchers.submitError = null;
};

const addProprietaryMatchersFulfilled = (state) => {
  state.setProprietaryMatchers.submitMaskState = true;
  state.setProprietaryMatchers.submitError = null;
  state.setProprietaryMatchers.data.regex = '';
};

const addProprietaryMatchersFailed = (state, { payload }) => {
  state.setProprietaryMatchers.submitMaskState = null;
  state.setProprietaryMatchers.submitError = Messages.getHttpErrorMessage(payload);
};

const addProprietaryMatchers = createAsyncThunk(
  `${REDUCER_NAME}/addProprietaryMatchers`,
  (data = { paths: [] }, { dispatch, getState, rejectWithValue }) => {
    const { ownerId } = selectComponentDetailsRequestData(getState());
    return axios
      .post(setProprietaryMatchers(ownerId), data)
      .then((results) => {
        startSubmitMaskSuccessTimer(dispatch);
        return results;
      })
      .catch(rejectWithValue);
  }
);

const loadApplicableLabelsRequested = (state) => {
  return setPendingLoads(['applicableLabels'], state);
};

const loadApplicableLabelsFulfilled = (state, { payload }) => {
  const sortAlphabetically = sortWith([ascend(prop('label'))]);
  return unsetPendingLoads(['applicableLabels'], {
    ...state,
    applicableLabels: sortAlphabetically(flattenLabelsToSingleArray(payload.data.labelsByOwner)),
    applicableLabelsLoadError: null,
  });
};

const loadApplicableLabelsFailed = (state, { payload }) => {
  return unsetPendingLoads(['applicableLabels'], {
    ...state,
    applicableLabelsLoadError: Messages.getHttpErrorMessage(payload),
  });
};

const loadApplicableLabels = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicableLabels`,
  (_, { getState, rejectWithValue }) => {
    const { publicId } = getState().router.currentParams;
    return axios
      .get(getApplicableLabels('application', publicId))
      .then((result) => result)
      .catch(rejectWithValue);
  }
);

const componentDetailsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    visitAncestors,
    backToOffspring,
    toggleShowMatchersPopover: toggleBooleanProp('showMatchersPopover'),
    resetSubmitMaskState: pathSetConst(['setProprietaryMatchers', 'submitMaskState'], null),
    resetSubmitError: pathSetConst(['setProprietaryMatchers', 'submitError'], null),
    setComponentMatchersData: pathSet(['setProprietaryMatchers', 'data']),
  },
  extraReducers: {
    [loadComponentDetails.pending]: loadComponentDetailsRequested,
    [loadComponentDetails.fulfilled]: loadComponentDetailsFulfilled,
    [loadComponentDetails.rejected]: loadComponentDetailsFailed,
    [addProprietaryMatchers.pending]: addProprietaryMatchersRequested,
    [addProprietaryMatchers.fulfilled]: addProprietaryMatchersFulfilled,
    [addProprietaryMatchers.rejected]: addProprietaryMatchersFailed,
    [loadApplicableLabels.pending]: loadApplicableLabelsRequested,
    [loadApplicableLabels.fulfilled]: loadApplicableLabelsFulfilled,
    [loadApplicableLabels.rejected]: loadApplicableLabelsFailed,
  },
});

export default componentDetailsSlice.reducer;
export const actions = {
  ...componentDetailsSlice.actions,
  addProprietaryMatchers,
  loadComponentDetails,
  onTabChange,
  visitAncestorAction,
  backToOffspringAction,
  loadApplicableLabels,
};
