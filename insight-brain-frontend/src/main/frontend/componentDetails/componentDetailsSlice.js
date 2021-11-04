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
import { selectSelectedComponent } from '../applicationReport/applicationReportSelectors';
import {
  getComponentLabels,
  setProprietaryMatchers,
  getApplicableLabelsUrl,
  getApplicableLabelScopesUrl,
  getSaveLabelScopeUrl,
} from '../util/CLMLocation';
import { selectComponentDetailsRequestData } from './overview/overviewSelectors';
import { Messages } from '../util/CommonServices';
import { toggleBooleanProp } from '../util/reduxUtil';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { pathSet, pathSetConst, propSet } from 'MainRoot/util/reduxToolkitUtil';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

const REDUCER_NAME = 'componentDetails';
export const VISIT_ANCESTOR_ACTION = REDUCER_NAME + '/visitAncestors';
export const RETURN_TO_OFFSPRING = REDUCER_NAME + '/backToOffspring';
const COMPONENT_DETAILS_OVERVIEW_ROUTE_NAME = 'applicationReport.componentDetails.overview';
enableMapSet();

const initialState = Object.freeze({
  pendingLoads: new Set(),
  isVisitingAncestor: false,
  isSavingLabelScope: false,
  offspring: null,
  labels: [],
  applicableLabels: [],
  applicableLabelScopes: [],
  loadError: null,
  showApplyLabelModal: false,
  selectedLabelDetails: {},
  labelScopeToSave: {},
  applicableLabelsLoadError: null,
  applicableLabelScopesLoadError: null,
  saveLabelScopeError: null,
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

const flattenLabelsToSingleArray = (labelsByOwner) => {
  let flattenedLabelsArray = [];
  labelsByOwner.forEach(function (labelOwner) {
    labelOwner.labels.forEach(function (label) {
      label.ownerType = labelOwner.ownerType;
      label.ownerId = labelOwner.ownerId;
      flattenedLabelsArray.push(label);
    });
  });
  return flattenedLabelsArray;
};

const flattenScopesToSingleArray = (topLevelScope) => {
  let flattenedScopesArray = [topLevelScope];
  topLevelScope.children.forEach(function (childScope) {
    flattenedScopesArray.push(childScope);
    if (childScope.children) {
      childScope.children.forEach(function (nextChild) {
        flattenedScopesArray.push(nextChild);
      });
    }
  });
  return flattenedScopesArray;
};

const onTabChange = (tabId) => {
  return (dispatch, getState) => {
    const { hash } = selectRouterCurrentParams(getState());
    return dispatch(stateGo(`applicationReport.componentDetails.${tabId}`, { hash }));
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

const loadApplicableLabels = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicableLabels`,
  (_, { getState, rejectWithValue }) => {
    const { publicId } = getState().router.currentParams;
    return axios.get(getApplicableLabelsUrl('application', publicId)).catch(rejectWithValue);
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

const loadApplicableLabelScopes = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicableLabelScopes`,
  (_, { getState, rejectWithValue }) => {
    const { componentDetails, router } = getState();
    const { id } = componentDetails.selectedLabelDetails;
    const { publicId } = router.currentParams;
    return axios.get(getApplicableLabelScopesUrl('application', publicId, id)).catch(rejectWithValue);
  }
);

const loadApplicableLabelScopesRequested = (state) => {
  return setPendingLoads(['applicableLabelScopes'], state);
};

const loadApplicableLabelScopesFulfilled = (state, { payload }) => {
  return unsetPendingLoads(['applicableLabelScopes'], {
    ...state,
    applicableLabelScopes: flattenScopesToSingleArray(payload.data),
    applicableLabelScopesLoadError: null,
  });
};

const loadApplicableLabelScopesFailed = (state, { payload }) => {
  return unsetPendingLoads(['applicableLabelScopes'], {
    ...state,
    applicableLabelScopesLoadError: Messages.getHttpErrorMessage(payload),
  });
};

const saveApplyLabelScope = createAsyncThunk(
  `${REDUCER_NAME}/saveApplyLabelScope`,
  (_, { dispatch, getState, rejectWithValue }) => {
    const { componentDetails, router } = getState();
    const payload = componentDetails.selectedLabelDetails;
    const { hash } = router.currentParams;
    const { labelScopeType, labelScopeId } = componentDetails.labelScopeToSave;

    return axios
      .post(getSaveLabelScopeUrl(labelScopeType, labelScopeId, hash), payload)
      .then((results) => {
        dispatch(actions.cancelApplyLabelModal());
        dispatch(actions.loadComponentDetails());
        return results;
      })
      .catch(rejectWithValue);
  }
);

const saveApplyLabelScopeRequested = (state) => {
  return setPendingLoads(['isSavingLabelScope'], state);
};

const saveApplyLabelScopeFulfilled = (state) => {
  return unsetPendingLoads(['isSavingLabelScope'], {
    ...state,
    saveLabelScopeError: null,
  });
};

const saveApplyLabelScopeFailed = (state, { payload }) => {
  return unsetPendingLoads(['isSavingLabelScope'], {
    ...state,
    saveLabelScopeError: Messages.getHttpErrorMessage(payload),
  });
};

const handleAddLabelTag = (labelDetails, ownerType) => {
  return (dispatch) => {
    dispatch(actions.setSelectedLabelDetails(labelDetails));
    if (ownerType === 'application') {
      dispatch(actions.setLabelScopeToSaveAction());
      dispatch(actions.saveApplyLabelScope());
    } else {
      dispatch(actions.showApplyLabelModalAction());
    }
  };
};

const setLabelScopeToSaveAction = (labelScope) => {
  return (dispatch, getState) => {
    const { publicId } = getState().router.currentParams;
    if (labelScope === undefined) {
      labelScope = { labelScopeType: 'application', labelScopeId: publicId };
    }
    dispatch(actions.setLabelScopeToSave(labelScope));
  };
};

const cancelApplyLabelModal = (state) => {
  return {
    ...state,
    showApplyLabelModal: false,
  };
};

const showApplyLabelModalAction = (state) => {
  return {
    ...state,
    showApplyLabelModal: true,
    labelScopeToSave: {},
  };
};

const componentDetailsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    visitAncestors,
    backToOffspring,
    showApplyLabelModalAction,
    cancelApplyLabelModal,
    toggleShowMatchersPopover: toggleBooleanProp('showMatchersPopover'),
    resetSubmitMaskState: pathSetConst(['setProprietaryMatchers', 'submitMaskState'], null),
    resetSubmitError: pathSetConst(['setProprietaryMatchers', 'submitError'], null),
    setComponentMatchersData: pathSet(['setProprietaryMatchers', 'data']),
    setLabelScopeToSave: propSet('labelScopeToSave'),
    setSelectedLabelDetails: propSet('selectedLabelDetails'),
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
    [loadApplicableLabelScopes.pending]: loadApplicableLabelScopesRequested,
    [loadApplicableLabelScopes.fulfilled]: loadApplicableLabelScopesFulfilled,
    [loadApplicableLabelScopes.rejected]: loadApplicableLabelScopesFailed,
    [saveApplyLabelScope.pending]: saveApplyLabelScopeRequested,
    [saveApplyLabelScope.fulfilled]: saveApplyLabelScopeFulfilled,
    [saveApplyLabelScope.rejected]: saveApplyLabelScopeFailed,
  },
});

export default componentDetailsSlice.reducer;
export const actions = {
  ...componentDetailsSlice.actions,
  addProprietaryMatchers,
  handleAddLabelTag,
  loadComponentDetails,
  onTabChange,
  visitAncestorAction,
  backToOffspringAction,
  loadApplicableLabels,
  loadApplicableLabelScopes,
  saveApplyLabelScope,
  setLabelScopeToSaveAction,
};
