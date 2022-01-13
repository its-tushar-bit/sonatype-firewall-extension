/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { always, curryN, prop, sortWith, ascend, lensPath, over, not } from 'ramda';
import { enableMapSet } from 'immer';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { stateGo } from '../reduxUiRouter/routerActions';
import { loadReportIfNeeded } from '../applicationReport/applicationReportActions';
import { selectDependencyTreeData, selectSelectedComponent } from '../applicationReport/applicationReportSelectors';
import {
  getComponentLabels,
  setProprietaryMatchers,
  getApplicableLabelsUrl,
  getApplicableLabelScopesUrl,
  getSaveLabelScopeUrl,
  removeLabel,
} from '../util/CLMLocation';
import { selectComponentDetailsRequestData } from './overview/overviewSelectors';
import { Messages } from '../util/CommonServices';
import { toggleBooleanProp } from '../util/reduxUtil';
import { pathSet, pathSetConst, propSet } from 'MainRoot/util/reduxToolkitUtil';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { SELECT_COMPONENT } from 'MainRoot/applicationReport/applicationReportActions';
import {
  selectComponentDetails,
  selectIsApplicableLabelsLoading,
  selectIsLabelsLoading,
} from './componentDetailsSelectors';
import { getDependencyTreeSubset } from 'MainRoot/DependencyTree/dependencyTreeUtil';

const HTTP_CLIENT_CLOSED_REQUEST = 499;

const REDUCER_NAME = 'componentDetails';
export const VISIT_ANCESTOR_ACTION = REDUCER_NAME + '/visitAncestors';
export const RETURN_TO_OFFSPRING = REDUCER_NAME + '/backToOffspring';
const COMPONENT_DETAILS_OVERVIEW_ROUTE_NAME = 'applicationReport.componentDetails.overview';
enableMapSet();

export const initialState = Object.freeze({
  pendingLoads: new Set(),
  isVisitingAncestor: false,
  isSavingLabelScope: false,
  offspring: null,
  labels: [],
  applicableLabels: [],
  applicableLabelScopes: [],
  loadError: null,
  showApplyLabelModal: false,
  applyLabelMaskState: null,
  removeLabelMaskState: null,
  labelModalMaskState: null,
  selectedLabelDetails: {},
  selectedLabelOwnerType: '',
  labelScopeToSave: {},
  applicableLabelsLoadError: null,
  removeAppliedLabelError: null,
  showRemoveLabelModal: false,
  applicableLabelScopesLoadError: null,
  saveLabelScopeError: null,
  showMatchersPopover: false,
  setProprietaryMatchers: {
    submitMaskState: null,
    submitError: null,
    data: { pathnames: [], regex: '' },
  },
  dependencyTreeSubset: null,
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

const handleRemoveLabelTag = (labelDetails, ownerType) => {
  return (dispatch) => {
    dispatch(actions.toggleShowRemoveLabelModal());
    dispatch(actions.setSelectedLabelDetails({ ...labelDetails, ownerType }));
  };
};

const loadComponentDetailsRequested = (state) => {
  return setPendingLoads(['labels'], state);
};

const loadComponentDetailsFulfilled = (state, { payload }) => {
  const labelsByOwner = payload.data.labelsByOwner;
  const labels = flattenLabelsToSingleArray(labelsByOwner);
  return unsetPendingLoads(['labels'], {
    ...state,
    labels: labels,
    loadError: null,
    dependencyTreeSubset: getDependencyTreeSubset(payload.dependencyTree, payload.hash),
  });
};

function loadComponentDetailsFailed(state, { payload }) {
  if (payload.message === HTTP_CLIENT_CLOSED_REQUEST) {
    return {
      ...state,
      labels: [],
    };
  }
  return unsetPendingLoads(['labels'], { ...state, loadError: Messages.getHttpErrorMessage(payload) });
}

let loadComponentDetailsCancelToken = null;
const loadComponentDetails = createAsyncThunk(`${REDUCER_NAME}/loadComponentDetails`, (_, { dispatch, getState }) => {
  const isPending = selectIsLabelsLoading(getState());

  if (isPending) {
    loadComponentDetailsCancelToken?.cancel(HTTP_CLIENT_CLOSED_REQUEST);
  }

  loadComponentDetailsCancelToken = axios.CancelToken.source();

  dispatch(loadComponentDetailsWithCancelToken(loadComponentDetailsCancelToken.token));
});

const loadComponentDetailsWithCancelToken = createAsyncThunk(
  `${REDUCER_NAME}/loadComponentDetailsWithCancelToken`,
  (cancelToken, { getState, dispatch, rejectWithValue }) => {
    return dispatch(loadReportIfNeeded())
      .then(() => {
        const { publicId, hash } = getState().router.currentParams;
        return axios.get(getComponentLabels(publicId, hash), { cancelToken });
      })
      .then((results) => {
        const dependencyTree = selectDependencyTreeData(getState());
        const { hash } = selectComponentDetails(getState());

        return {
          ...results,
          dependencyTree,
          hash,
        };
      })
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

let loadApplicableLabelsCancelToken = null;
const loadApplicableLabels = createAsyncThunk(`${REDUCER_NAME}/loadApplicableLabels`, (_, { getState, dispatch }) => {
  const isPending = selectIsApplicableLabelsLoading(getState());

  if (isPending) {
    loadApplicableLabelsCancelToken?.cancel(HTTP_CLIENT_CLOSED_REQUEST);
  }

  loadApplicableLabelsCancelToken = axios.CancelToken.source();

  dispatch(loadApplicableLabelsWithCancelToken(loadApplicableLabelsCancelToken.token));
});

const loadApplicableLabelsWithCancelToken = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicableLabelsWithCancelToken`,
  (cancelToken, { getState, rejectWithValue }) => {
    const { publicId } = getState().router.currentParams;

    return axios.get(getApplicableLabelsUrl('application', publicId), { cancelToken }).catch(rejectWithValue);
  }
);

const loadApplicableLabelsRequested = (state) => {
  return setPendingLoads(['applicableLabels'], {
    ...state,
    applicableLabels: [],
  });
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
  if (payload.message === HTTP_CLIENT_CLOSED_REQUEST) {
    return {
      ...state,
      applicableLabels: [],
    };
  }
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

/**
 * Save selected label w/ scope action and reducers
 */
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
        setTimeout(() => {
          dispatch(actions.cancelApplyLabelModal());
          dispatch(actions.resetApplyLabelMaskState(null));
          dispatch(actions.resetLabelModalMaskState(null));
          dispatch(actions.loadComponentDetails());
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        return results;
      })
      .catch(rejectWithValue);
  }
);

const saveApplyLabelScopeRequested = (state) => {
  return setPendingLoads(['isSavingLabelScope'], {
    ...state,
    applyLabelMaskState: state.selectedLabelOwnerType === 'application' ? false : null,
    labelModalMaskState: state.selectedLabelOwnerType === 'application' ? null : false,
  });
};

const saveApplyLabelScopeFulfilled = (state) => {
  return unsetPendingLoads(['isSavingLabelScope'], {
    ...state,
    saveLabelScopeError: null,
    applyLabelMaskState: state.selectedLabelOwnerType === 'application' ? true : null,
    labelModalMaskState: state.selectedLabelOwnerType === 'application' ? null : true,
  });
};

const saveApplyLabelScopeFailed = (state, { payload }) => {
  return unsetPendingLoads(['isSavingLabelScope'], {
    ...state,
    saveLabelScopeError: Messages.getHttpErrorMessage(payload),
    applyLabelMaskState: null,
    labelModalMaskState: null,
  });
};

/**
 * Remove applied label action and reducers
 */
const removeAppliedLabel = createAsyncThunk(
  `${REDUCER_NAME}/removeLabel`,
  ({ ownerType, ownerId, id }, { dispatch, getState, rejectWithValue }) => {
    const { hash } = getState().router.currentParams;
    return axios
      .delete(removeLabel(ownerType, ownerId, hash, id))
      .then(() => {
        setTimeout(() => {
          dispatch(actions.toggleShowRemoveLabelModal());
          dispatch(loadComponentDetails());
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      })
      .catch(rejectWithValue);
  }
);

const removeAppliedLabelRequested = (state) => {
  return setPendingLoads(['removeAppliedLabel'], {
    ...state,
    removeAppliedLabelError: null,
    removeLabelMaskState: false,
  });
};

const removeAppliedLabelFulfilled = (state) => {
  return unsetPendingLoads(['removeAppliedLabel'], {
    ...state,
    selectedLabelDetails: {},
    removeAppliedLabelError: null,
    removeLabelMaskState: true,
  });
};

const removeAppliedLabelFailed = (state, { payload }) => {
  return unsetPendingLoads(['removeAppliedLabel'], {
    ...state,
    removeLabelMaskState: null,
    removeAppliedLabelError: Messages.getHttpErrorMessage(payload),
  });
};

const handleAddLabelTag = (labelDetails, ownerType) => {
  return (dispatch) => {
    dispatch(actions.setSelectedLabelDetails(labelDetails));
    dispatch(actions.setSelectedLabelOwnerType(ownerType));
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

const toggleShowRemoveLabelModal = (state) => {
  return toggleBooleanProp('showRemoveLabelModal')({
    ...state,
    removeLabelMaskState: null,
    removeAppliedLabelError: null,
  });
};

const toggleIsOpenAtTreePathAction = (state, { payload }) => {
  const treePathIsOpenLens = lensPath([...payload, 'isOpen']);
  const currentSubset = state.dependencyTreeSubset;

  state.dependencyTreeSubset = over(treePathIsOpenLens, not, currentSubset);
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
    resetApplyLabelMaskState: propSet('applyLabelMaskState'),
    resetRemoveLabelMaskState: propSet('removeLabelMaskState'),
    resetLabelModalMaskState: propSet('labelModalMaskState'),
    resetSubmitError: pathSetConst(['setProprietaryMatchers', 'submitError'], null),
    setComponentMatchersData: pathSet(['setProprietaryMatchers', 'data']),
    toggleShowRemoveLabelModal: toggleShowRemoveLabelModal,
    setLabelScopeToSave: propSet('labelScopeToSave'),
    setSelectedLabelDetails: propSet('selectedLabelDetails'),
    setSelectedLabelOwnerType: propSet('selectedLabelOwnerType'),
    toggleIsOpenAtTreePathAction,
  },
  extraReducers: {
    [loadComponentDetails.pending]: loadComponentDetailsRequested,
    [loadComponentDetailsWithCancelToken.fulfilled]: loadComponentDetailsFulfilled,
    [loadComponentDetailsWithCancelToken.rejected]: loadComponentDetailsFailed,

    [addProprietaryMatchers.pending]: addProprietaryMatchersRequested,
    [addProprietaryMatchers.fulfilled]: addProprietaryMatchersFulfilled,
    [addProprietaryMatchers.rejected]: addProprietaryMatchersFailed,

    [loadApplicableLabelsWithCancelToken.pending]: loadApplicableLabelsRequested,
    [loadApplicableLabelsWithCancelToken.fulfilled]: loadApplicableLabelsFulfilled,
    [loadApplicableLabelsWithCancelToken.rejected]: loadApplicableLabelsFailed,

    [removeAppliedLabel.pending]: removeAppliedLabelRequested,
    [removeAppliedLabel.fulfilled]: removeAppliedLabelFulfilled,
    [removeAppliedLabel.rejected]: removeAppliedLabelFailed,
    [loadApplicableLabelScopes.pending]: loadApplicableLabelScopesRequested,
    [loadApplicableLabelScopes.fulfilled]: loadApplicableLabelScopesFulfilled,
    [loadApplicableLabelScopes.rejected]: loadApplicableLabelScopesFailed,
    [saveApplyLabelScope.pending]: saveApplyLabelScopeRequested,
    [saveApplyLabelScope.fulfilled]: saveApplyLabelScopeFulfilled,
    [saveApplyLabelScope.rejected]: saveApplyLabelScopeFailed,
    [SELECT_COMPONENT]: always(initialState),
  },
});

export default componentDetailsSlice.reducer;
export const actions = {
  ...componentDetailsSlice.actions,
  addProprietaryMatchers,
  handleAddLabelTag,
  loadComponentDetails,
  loadComponentDetailsWithCancelToken,
  onTabChange,
  visitAncestorAction,
  backToOffspringAction,
  loadApplicableLabels,
  loadApplicableLabelsWithCancelToken,
  removeAppliedLabel,
  handleRemoveLabelTag,
  loadApplicableLabelScopes,
  saveApplyLabelScope,
  setLabelScopeToSaveAction,
};
