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
import { selectDependencyTreeData } from '../applicationReport/applicationReportSelectors';
import {
  getComponentLabels,
  setProprietaryMatchers,
  getApplicableLabelsUrl,
  getApplicableLabelScopesUrl,
  getSaveLabelScopeUrl,
  removeLabel,
} from '../util/CLMLocation';
import { selectComponentDetailsRequestData } from './overview/overviewSelectors';
import { Messages } from '../utilAngular/CommonServices';
import { toggleBooleanProp } from '../util/reduxUtil';
import { processOwnerHierarchy } from 'MainRoot/util/hierarchyUtil';
import { pathSet, pathSetConst, propSet } from 'MainRoot/util/reduxToolkitUtil';
import {
  selectRouterCurrentParams,
  selectIsFirewallOrRepository,
  selectIsPrioritiesPageContainer,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { SELECT_COMPONENT } from 'MainRoot/applicationReport/applicationReportActions';
import {
  selectComponentDetails,
  selectIsApplicableLabelsLoading,
  selectIsLabelsLoading,
} from './componentDetailsSelectors';
import { getDependencyTreeSubset } from 'MainRoot/DependencyTree/dependencyTreeUtil';
import { selectPrioritiesPageContainerName } from '../reduxUiRouter/routerSelectors';

const HTTP_CLIENT_CLOSED_REQUEST = 499;

const REDUCER_NAME = 'componentDetails';

enableMapSet();

export const initialState = Object.freeze({
  pendingLoads: new Set(),
  isSavingLabelScope: false,
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

const mutatePendingLoads = curryN(3, (setMutator, loads, state) => {
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

const onTabChange = (tabId) => {
  return (dispatch, getState) => {
    const { hash } = selectRouterCurrentParams(getState());
    const isPrioritiesPageContainer = selectIsPrioritiesPageContainer(getState());
    const prioritiesPageContainerName = selectPrioritiesPageContainerName(getState());

    if (isPrioritiesPageContainer) {
      return dispatch(stateGo(`${prioritiesPageContainerName}.componentDetails.${tabId}`, { hash }));
    }
    return dispatch(stateGo(`applicationReport.componentDetails.${tabId}`, { hash }));
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

const loadComponentLabelsFullfilled = (state, { payload }) => {
  const labelsByOwner = payload?.data?.labelsByOwner || [];
  const labels = flattenLabelsToSingleArray(labelsByOwner);
  return unsetPendingLoads(['labels'], {
    ...state,
    labels: labels,
    loadError: null,
  });
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

let loadFirewallComponentDetailsLabelsCancelToken = null;
const loadFirewallComponentDetailsLabels = createAsyncThunk(
  `${REDUCER_NAME}/loadFirewallComponentDetailsLabels`,
  (_, { dispatch, getState }) => {
    const isPending = selectIsLabelsLoading(getState());

    if (isPending) {
      loadFirewallComponentDetailsLabelsCancelToken?.cancel(HTTP_CLIENT_CLOSED_REQUEST);
    }

    loadFirewallComponentDetailsLabelsCancelToken = axios.CancelToken.source();

    dispatch(loadFirewallComponentDetailsLabelsWithCancelToken(loadFirewallComponentDetailsLabelsCancelToken.token));
  }
);

let loadComponentDetailsCancelToken = null;
const loadComponentDetails = createAsyncThunk(`${REDUCER_NAME}/loadComponentDetails`, (_, { dispatch, getState }) => {
  const isPending = selectIsLabelsLoading(getState());

  if (isPending) {
    loadComponentDetailsCancelToken?.cancel(HTTP_CLIENT_CLOSED_REQUEST);
  }

  loadComponentDetailsCancelToken = axios.CancelToken.source();

  dispatch(loadComponentDetailsWithCancelToken(loadComponentDetailsCancelToken.token));
});

const loadFirewallComponentDetailsLabelsWithCancelToken = createAsyncThunk(
  `${REDUCER_NAME}/loadFirewallComponentDetailsLabelsWithCancelToken`,
  (cancelToken, { getState, rejectWithValue }) => {
    const currentState = getState();
    const { repositoryId, componentHash } = currentState.router.currentParams;
    return axios
      .get(getComponentLabels(repositoryId, componentHash, 'repository'), { cancelToken })
      .then((results) => {
        return results;
      })
      .catch(rejectWithValue);
  }
);

const loadComponentDetailsWithCancelToken = createAsyncThunk(
  `${REDUCER_NAME}/loadComponentDetailsWithCancelToken`,
  (cancelToken, { getState, dispatch, rejectWithValue }) => {
    return dispatch(loadReportIfNeeded())
      .then(() => {
        const currentState = getState();
        const { publicId, hash } = currentState.router.currentParams;
        return axios.get(getComponentLabels(publicId, hash, 'application'), { cancelToken });
      })
      .then((results) => {
        const currentState = getState();
        const dependencyTree = selectDependencyTreeData(currentState);
        const { hash } = selectComponentDetails(currentState);

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

const firewallLoadApplicableLabels = createAsyncThunk(
  `${REDUCER_NAME}/firewallLoadApplicableLabels`,
  (_, { getState, dispatch }) => {
    const isPending = selectIsApplicableLabelsLoading(getState());

    if (isPending) {
      loadApplicableLabelsCancelToken?.cancel(HTTP_CLIENT_CLOSED_REQUEST);
    }

    loadApplicableLabelsCancelToken = axios.CancelToken.source();

    dispatch(firewallLoadApplicableLabelsWithCancelToken(loadApplicableLabelsCancelToken.token));
  }
);

const loadApplicableLabelsWithCancelToken = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicableLabelsWithCancelToken`,
  (cancelToken, { getState, rejectWithValue }) => {
    const currentState = getState();
    const { publicId } = currentState.router.currentParams;
    return axios.get(getApplicableLabelsUrl('application', publicId), { cancelToken }).catch(rejectWithValue);
  }
);

const firewallLoadApplicableLabelsWithCancelToken = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicableLabelsWithCancelToken`,
  (cancelToken, { getState, rejectWithValue }) => {
    const { repositoryId } = getState().router.currentParams;
    return axios.get(getApplicableLabelsUrl('repository', repositoryId), { cancelToken }).catch(rejectWithValue);
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
    const currentState = getState();
    const { componentDetails, router } = currentState;
    const { id: labelId } = componentDetails.selectedLabelDetails;
    const { publicId, repositoryId } = router.currentParams;
    if (selectIsFirewallOrRepository(currentState)) {
      return axios.get(getApplicableLabelScopesUrl('repository', repositoryId, labelId)).catch(rejectWithValue);
    } else {
      return axios.get(getApplicableLabelScopesUrl('application', publicId, labelId)).catch(rejectWithValue);
    }
  }
);

const loadApplicableLabelScopesRequested = (state) => {
  return setPendingLoads(['applicableLabelScopes'], state);
};

const loadApplicableLabelScopesFulfilled = (state, { payload }) => {
  return unsetPendingLoads(['applicableLabelScopes'], {
    ...state,
    applicableLabelScopes: processOwnerHierarchy(payload.data).reverse(),
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
    const { hash, componentHash } = router.currentParams;
    const { labelScopeType, labelScopeId } = componentDetails.labelScopeToSave;
    const currentHash = hash || componentHash;

    return axios
      .post(getSaveLabelScopeUrl(labelScopeType, labelScopeId, currentHash), payload)
      .then((results) => {
        setTimeout(() => {
          dispatch(actions.cancelApplyLabelModal());
          dispatch(actions.resetApplyLabelMaskState(null));
          dispatch(actions.resetLabelModalMaskState(null));
          if (selectIsFirewallOrRepository(getState())) {
            dispatch(actions.loadFirewallComponentDetailsLabels());
          } else {
            dispatch(actions.loadComponentDetails());
          }
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
    const { hash, componentHash } = getState().router.currentParams;
    return axios
      .delete(removeLabel(ownerType, ownerId, hash || componentHash, id))
      .then(() => {
        setTimeout(() => {
          dispatch(actions.toggleShowRemoveLabelModal());
          if (selectIsFirewallOrRepository(getState())) {
            dispatch(loadFirewallComponentDetailsLabels());
          } else {
            dispatch(loadComponentDetails());
          }
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

    [loadFirewallComponentDetailsLabels.pending]: loadComponentDetailsRequested,
    [loadFirewallComponentDetailsLabelsWithCancelToken.fulfilled]: loadComponentLabelsFullfilled,
    [loadFirewallComponentDetailsLabelsWithCancelToken.rejected]: loadComponentDetailsFailed,

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
  loadFirewallComponentDetailsLabels,
  loadComponentDetailsWithCancelToken,
  onTabChange,
  loadApplicableLabels,
  firewallLoadApplicableLabels,
  loadApplicableLabelsWithCancelToken,
  removeAppliedLabel,
  handleRemoveLabelTag,
  loadApplicableLabelScopes,
  saveApplyLabelScope,
  setLabelScopeToSaveAction,
};
