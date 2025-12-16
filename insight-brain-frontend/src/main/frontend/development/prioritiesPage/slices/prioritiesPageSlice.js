/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import { isNil, keys, propEq } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getCreatePullRequestUrl, getPrioritiesPageTableData, getVersionGraphUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectPrioritiesPageSlice } from 'MainRoot/development/prioritiesPage/selectors/prioritiesPageSelectors';
import { selectApplicationReportMetaData } from 'MainRoot/applicationReport/applicationReportSelectors';
import { pollPRStatus } from 'MainRoot/manualPullRequest/pollPRStatus';
import { actions as createPRModalActions } from 'MainRoot/manualPullRequest/createPRModalSlice';
import { getAsyncRecommendationsPrioritiesPage } from 'MainRoot/componentDetails/overview/riskRemediation/recommendedVersionsUtils';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';
import { AUTOMATED_REMEDIATION_STATUS } from 'MainRoot/constants/automatedRemediationStatus';
import { fetchDefaultBranchName } from 'MainRoot/util/branchNameUtil';

export const PRIORITIES_PAGE_REDUCER_NAME = 'prioritiesPage';

export const TABLE_PAGE_SIZE = 15;

const loadTableDataRequested = (state) => {
  return {
    ...state,
    priorities: null,
    loadingTableData: true,
    loadErrorTableData: null,
  };
};

const loadTableDataFulfilled = (state, { payload }) => {
  const {
    priorities: { total, page, pageSize, pageCount, results },
    publicAppId,
    scanId,
    scanIdFromLatestBuildStageEvaluation,
    hasAutoWaiversConfigured,
  } = payload;
  return {
    ...state,
    priorities: results,
    loadingTableData: false,
    loadErrorTableData: null,
    pageSize,
    pageCount,
    page,
    total,
    publicAppId,
    scanId,
    scanIdFromLatestBuildStageEvaluation,
    hasAutoWaiversConfigured,
  };
};

const loadTableDataFailed = (state, { payload }) => {
  return {
    ...state,
    priorities: null,
    loadingTableData: false,
    loadErrorTableData: Messages.getHttpErrorMessage(payload),
  };
};

const loadTableData = createAsyncThunk(
  `${PRIORITIES_PAGE_REDUCER_NAME}/loadTableData`,
  (_, { dispatch, getState, rejectWithValue }) => {
    const state = getState();
    const { publicAppId, scanId } = selectRouterCurrentParams(state);
    const tableDataUrl = getPrioritiesPageTableData(publicAppId, scanId);
    const { page, componentNameFilter, filterOnPolicyActions } = selectPrioritiesPageSlice(state);

    return axios
      .get(tableDataUrl, {
        params: { pageSize: TABLE_PAGE_SIZE, page, componentNameFilter, filterOnPolicyActions },
      })
      .then(({ data }) => {
        return dispatch(loadBranchName()).then(() => ({ ...data, publicAppId, scanId }));
      })
      .catch(rejectWithValue);
  }
);

const openCreatePRModal = createAsyncThunk(
  `${PRIORITIES_PAGE_REDUCER_NAME}/openCreatePRModal`,
  ({ componentHash, targetVersion, isDirectDependency }, { dispatch, getState }) => {
    const state = getState();
    const { scanId } = selectRouterCurrentParams(state);
    const prioritiesPage = selectPrioritiesPageSlice(state);
    const { recommendations, branchName } = prioritiesPage;
    const component = prioritiesPage.priorities.find(propEq('componentHash', componentHash));
    const { displayName, componentIdentifier } = component;

    const currentVersion = componentIdentifier?.coordinates?.version;
    const recommendation = recommendations[componentHash];
    const name = recommendation.componentDisplayName || component.displayName.name;
    const breakingChangesCount = recommendation.remediation?.breakingChangesCount;

    dispatch(
      createPRModalActions.openModal({
        name: name,
        fullName: displayName,
        currentVersion: currentVersion,
        targetVersion: targetVersion,
        breakingChangesCount: breakingChangesCount,
        defaultBranch: branchName,
        scanId: scanId,
        identificationSource: 'Sonatype',
        componentHash: componentHash,
        componentIdentifier: componentIdentifier,
        isDirectDependency,
      })
    );
  }
);

const openCreatePRModalFulfilled = (state, action) => {
  const { componentHash } = action.meta.arg;
  state.visibleCreatePRModalComponentHash = componentHash;
};

const createPR = createAsyncThunk(
  `${PRIORITIES_PAGE_REDUCER_NAME}/createPR`,
  ({ componentHash, targetVersion, isDirectDependency }, { getState, rejectWithValue }) => {
    const state = getState();
    const { application } = selectApplicationReportMetaData(state);
    const { scanId } = selectRouterCurrentParams(state);
    const prioritiesPage = selectPrioritiesPageSlice(state);
    const component = prioritiesPage.priorities.find(propEq('componentHash', componentHash));
    const { componentIdentifier } = component;

    return axios
      .post(getCreatePullRequestUrl(), {
        applicationId: application.id,
        scanId: scanId,
        targetVersion: targetVersion,
        identificationSource: 'Sonatype',
        componentIdentifier: componentIdentifier,
        isDirectDependency: isDirectDependency,
      })
      .catch((error) => rejectWithValue(error));
  }
);

const createPRPending = (state, action) => {
  const { componentHash } = action.meta.arg;
  state.recommendations[componentHash].automatedRemediationStatus = {
    status: AUTOMATED_REMEDIATION_STATUS.PULL_REQUEST_CREATION_PENDING,
  };
};

const createPRFailed = (state, action) => {
  const { componentHash } = action.meta.arg;
  const error = action.payload.response?.data?.errorMessage || Messages.getHttpErrorMessage(action.payload);
  state.recommendations[componentHash].automatedRemediationStatus = {
    status: AUTOMATED_REMEDIATION_STATUS.PULL_REQUEST_CREATION_FAILED,
    reason: error,
  };
};

const loadRecommendations = createAsyncThunk(
  `${PRIORITIES_PAGE_REDUCER_NAME}/loadRecommendations`,
  (requestData, { rejectWithValue }) => {
    const { actualVersion, ...requestParams } = requestData;
    return axios
      .get(getVersionGraphUrl(requestParams))
      .then(({ data }) => {
        const remediation = getAsyncRecommendationsPrioritiesPage(
          data.remediation,
          actualVersion,
          requestData.stageId,
          data.allVersions
        );
        return {
          [requestData.hash]: {
            remediation: remediation,
            automatedRemediationStatus: data.automatedRemediationStatus,
            componentDisplayName:
              (data.allVersions && data.allVersions.length > 0 && data.allVersions[0].displayName?.name) ||
              (data.remediation.suggestedVersionChange?.data.component.displayName?.split(':')[1] || '').trim(),
          },
        };
      })
      .catch(rejectWithValue);
  }
);

const loadRecommendationsRequested = (state, { meta }) => {
  return {
    ...state,
    recommendations: {
      ...state.recommendations,
      [meta.arg.hash]: {
        loading: true,
        error: null,
        remediation: null,
      },
    },
  };
};

const loadRecommendationsFulfilled = (state, { payload }) => {
  const hash = keys(payload)[0];
  return {
    ...state,
    recommendations: {
      ...state.recommendations,
      [hash]: { loading: false, error: null, ...payload[hash] },
    },
  };
};

const loadRecommendationsFailed = (state, { payload, meta }) => {
  return {
    ...state,
    recommendations: {
      ...state.recommendations,
      [meta.arg.hash]: {
        loading: false,
        error: Messages.getHttpErrorMessage(payload),
        remediation: null,
      },
    },
  };
};

const loadBranchName = createAsyncThunk(
  `${PRIORITIES_PAGE_REDUCER_NAME}/loadBranchName`,
  (_, { getState, rejectWithValue }) => {
    const state = getState();
    const prioritiesPage = selectPrioritiesPageSlice(state);

    if (prioritiesPage.branchName) {
      return prioritiesPage.branchName;
    }

    return fetchDefaultBranchName(state).catch(rejectWithValue);
  }
);

const loadBranchNameFulfilled = (state, { payload }) => {
  state.branchName = payload;
};

const startPRStatusPolling = createAsyncThunk(
  `${PRIORITIES_PAGE_REDUCER_NAME}/handlePRCreated`,
  async ({ id, componentHash }, { rejectWithValue, signal }) => {
    try {
      const prStatus = await pollPRStatus(id, signal);
      return {
        componentHash,
        automatedRemediationStatus: prStatus,
      };
    } catch (error) {
      rejectWithValue(error);
    }
  }
);

const startPRStatusPollingPending = (state, action) => {
  const { componentHash } = action.meta.arg;
  state.recommendations[componentHash].automatedRemediationStatus = {
    status: AUTOMATED_REMEDIATION_STATUS.PULL_REQUEST_CREATION_PENDING,
  };
};

const startPRStatusPollingFulfilled = (state, { payload }) => {
  const { componentHash, automatedRemediationStatus } = payload;
  state.recommendations[componentHash].automatedRemediationStatus = automatedRemediationStatus;
};

export const checkIfLoadRecommendationsNeeded = (requestData) => async (dispatch, getState) => {
  const state = getState();
  const { recommendations } = selectPrioritiesPageSlice(state);
  const { hash } = requestData;

  if (!isNil(recommendations[hash]?.remediation)) {
    return { [hash]: recommendations[hash] };
  }

  return dispatch(loadRecommendations(requestData));
};

const setPage = (state, { payload }) => {
  return {
    ...state,
    page: payload + 1,
    loadingTableData: true,
  };
};

const restoreSavedPagination = (state) => {
  const { savedPage, savedPageCount } = state;
  if (savedPage !== null && savedPageCount !== null) {
    return {
      ...state,
      page: savedPage,
      pageCount: savedPageCount,
      savedPage: null,
      savedPageCount: null,
    };
  }
  return state;
};

const resetState = (state) => {
  return restoreSavedPagination({
    ...state,
    priorities: null,
    loadingTableData: false,
    loadErrorTableData: null,
    loadingMetadata: false,
    loadErrorMetaData: null,
    recommendations: {},
    page: 1,
    pageCount: 1,
    branchName: null,
  });
};

const isValidSavePaginationTransition = (router) => {
  const { fromState, toState } = router || {};
  return (
    fromState?.name?.includes('prioritiesPage') &&
    toState?.name?.includes('componentDetailsPageWithinPrioritiesPageContainer')
  );
};

const savePagination = (state, { payload }) => {
  if (isValidSavePaginationTransition(payload)) {
    return {
      ...state,
      savedPage: state.page,
      savedPageCount: state.pageCount,
    };
  }
};

const setComponentNameFilter = (state, { payload }) => {
  return {
    ...state,
    componentNameFilter: payload,
  };
};

const setFilterOnPolicyActions = (state, { payload }) => {
  return {
    ...state,
    filterOnPolicyActions: payload,
  };
};

const setHasDefaultFilters = (state, { payload }) => {
  return {
    ...state,
    hasDefaultFilters: payload,
  };
};

const setIntegrationType = (state, { payload }) => {
  return {
    ...state,
    integrationType: payload,
  };
};

const setHasUserInteractedWithFilter = (state, { payload }) => {
  return {
    ...state,
    hasUserInteractedWithFilter: payload,
  };
};

const prioritiesPageSlice = createSlice({
  name: PRIORITIES_PAGE_REDUCER_NAME,
  initialState: initialState(),
  reducers: {
    resetState,
    setPage,
    setComponentNameFilter,
    setFilterOnPolicyActions,
    setHasDefaultFilters,
    savePagination,
    setIntegrationType,
    setHasUserInteractedWithFilter,
  },
  extraReducers: {
    [loadBranchName.fulfilled]: loadBranchNameFulfilled,
    [loadTableData.pending]: loadTableDataRequested,
    [loadTableData.fulfilled]: loadTableDataFulfilled,
    [loadTableData.rejected]: loadTableDataFailed,
    [loadRecommendations.pending]: loadRecommendationsRequested,
    [loadRecommendations.fulfilled]: loadRecommendationsFulfilled,
    [loadRecommendations.rejected]: loadRecommendationsFailed,
    [openCreatePRModal.fulfilled]: openCreatePRModalFulfilled,
    [createPR.rejected]: createPRFailed,
    [createPR.pending]: createPRPending,
    [startPRStatusPolling.fulfilled]: startPRStatusPollingFulfilled,
    [startPRStatusPolling.pending]: startPRStatusPollingPending,
    [UI_ROUTER_ON_FINISH]: savePagination,
  },
});

function initialState() {
  return {
    priorities: null,
    loadingTableData: false,
    loadErrorTableData: null,
    loadingMetadata: false,
    loadErrorMetaData: null,
    recommendations: {},
    pageSize: TABLE_PAGE_SIZE,
    pageCount: 1,
    page: 1,
    total: null,
    publicAppId: null,
    scanId: null,
    componentNameFilter: '',
    filterOnPolicyActions: false,
    hasDefaultFilters: true,
    savedPage: null,
    savedPageCount: null,
    integrationType: null,
    branchName: null,
    visibleCreatePRModalComponentHash: null,
    hasUserInteractedWithFilter: false,
  };
}

export default prioritiesPageSlice.reducer;

export const actions = {
  ...prioritiesPageSlice.actions,
  loadTableData,
  loadRecommendations,
  openCreatePRModal,
  createPR,
  checkIfLoadRecommendationsNeeded,
  loadBranchName,
  startPRStatusPolling,
};
