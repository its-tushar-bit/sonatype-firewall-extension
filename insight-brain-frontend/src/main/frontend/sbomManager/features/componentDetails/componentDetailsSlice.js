/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { convertToWaiverViolationFormat as convertToViolationDetailsFormat } from 'MainRoot/util/waiverUtils';

import axios from 'axios';
import {
  always,
  any,
  compose,
  cond,
  equals,
  find,
  findIndex,
  includes,
  keys,
  pickBy,
  pipe,
  prop,
  propEq,
  T,
  toPairs,
  values,
  without,
} from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import {
  getApplicationSummaryUrl,
  getSbomComponentDependencyTreeUrl,
  getSbomComponentDetailsUrl,
  getSbomVulnerabibilityAnalysisReferenceData,
  getSbomVulnerabilityAnnotationUrl,
  getSbomPolicyViolationReportUrl,
  getVulnerabilityJsonDetailUrl,
  getVulnerabilityOverrideUrl,
  getBillOfMaterialsComponentsUrl,
} from 'MainRoot/util/CLMLocation';
import { checkPermissions } from 'MainRoot/util/authorizationUtil';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';
import { propSet, propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectSbomComponentDetails } from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSelector';
import { COMPONENTS_PER_PAGE } from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsComponentsTile/billOfMaterialsComponentsTileSlice';

const REDUCER_NAME = 'sbomComponentDetailsPage';

export const SORT_BY_FIELDS = Object.freeze({
  cvssScore: 'cvssScore',
  analysisStatus: 'analysisStatus',
});

export const SORT_DIRECTION = Object.freeze({
  ASC: 'asc',
  DESC: 'desc',
  DEFAULT: null,
});

export const TAB_INDICES = Object.freeze({
  VULNERABILTIY: 0,
  POLICY_VIOLATION: 1,
});

export const defaultSortConfiguration = Object.freeze({
  sortBy: SORT_BY_FIELDS.cvssScore,
  sortDirection: SORT_DIRECTION.DESC,
});

export const sbomPolicyViolationsInitialState = Object.freeze({
  loading: true,
  error: null,
  policy: null,
});

export const policyViolationDetailsDrawerInitialState = Object.freeze({
  loading: true,
  error: null,
  showDrawer: false,
  policyViolationId: null,
  violationDetails: null,
});

export const initialState = {
  loading: true,
  loadError: null,
  loadingDependencyTree: false,
  loadDependencyTreeError: null,
  loadingVulnerabilityDetail: false,
  loadVulnerabilityDetailError: null,
  submitMaskStateForVexAnnotationForm: null,
  loadSaveVexAnnotationFormError: null,
  loadingVulnerabilityAnalysisReferenceData: false,
  loadVulnerabilityAnalysisReferenceDataError: null,
  activeTabIndex: TAB_INDICES.VULNERABILTIY,
  publicAppId: null,
  componentDetails: null,
  disclosedVulnerabilitiesSortConfiguration: { ...defaultSortConfiguration },
  additionalVulnerabilitiesSortConfiguration: { ...defaultSortConfiguration },
  dependencyTreeSubset: null,
  vulnerabilityDetails: null,
  vulnerabilityAnalysisReferenceData: {
    responses: [],
    justifications: [],
    states: [],
  },
  selectedIssueForActions: null,
  deleteMaskState: null,
  deleteError: null,
  showDeleteModal: false,

  // policy-violations
  policyViolationDetailsDrawer: { ...policyViolationDetailsDrawerInitialState },
  sbomPolicyViolations: { ...sbomPolicyViolationsInitialState },
  componentDetailsPaginationData: null,
};

const setActiveTabIndex = (state, { payload }) => {
  state.activeTabIndex = payload;
};

const setSelectedIssueForActions = (state, { payload }) => {
  if (state.selectedIssueForActions === payload) {
    state.selectedIssueForActions = null;
  } else {
    state.selectedIssueForActions = payload;
  }
};

const updateCurrentPage = (state, { payload }) => {
  if (state.componentDetailsPaginationData) {
    const { pagesData } = state.componentDetailsPaginationData;
    const pageWithHash = pipe(
      toPairs,
      find(([, items]) => any(propEq('hash', payload), items))
    )(pagesData);

    if (pageWithHash) {
      const pageNumber = pageWithHash[0];
      state.componentDetailsPaginationData.pagination.currentPage = Number(pageNumber);
    }
  }
};

const loadComponentDependencyTreeDataRequested = (state) => {
  state.loadingDependencyTree = true;
  state.loadDependencyTreeError = null;
};

const loadComponentDependencyTreeDataFulfilled = (state, { payload }) => {
  state.loadingDependencyTree = false;
  state.loadDependencyTreeError = null;
  state.dependencyTreeSubset = payload;
};

const loadComponentDependencyTreeDataRejected = (state, { payload }) => {
  state.loadingDependencyTree = false;
  state.loadDependencyTreeError = payload.response.data;
  state.dependencyTreeSubset = null;
};

const loadComponentDetailsRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadComponentDetailsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.componentDetails = payload;
};

const loadComponentDetailsFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = payload.response.data;
  state.componentDetails = null;
};

const loadVulnerabilityDetailsRequested = (state) => {
  state.loadingVulnerabilityDetail = true;
  state.loadVulnerabilityDetailError = null;
};

const loadVulnerabilityDetailsFulfilled = (state, { payload }) => {
  state.loadingVulnerabilityDetail = false;
  state.loadVulnerabilityDetailError = null;
  state.vulnerabilityDetails = payload;
};

const loadVulnerabilityDetailsRejected = (state, { payload }) => {
  state.loadingVulnerabilityDetail = false;
  state.loadVulnerabilityDetailError = payload.response.data;
  state.vulnerabilityDetails = null;
};

const saveVexAnnotationRequested = function (state) {
  state.submitMaskStateForVexAnnotationForm = false;
  state.loadSaveVexAnnotationFormError = null;
};

const saveVexAnnotationFulfilled = function (state) {
  state.submitMaskStateForVexAnnotationForm = true;
  state.loadSaveVexAnnotationFormError = null;
};

const saveVexAnnotationRejected = function (state, { payload }) {
  state.submitMaskStateForVexAnnotationForm = null;
  state.loadSaveVexAnnotationFormError = payload.message;
};

const deleteVexAnnotationRequested = function (state) {
  state.deleteMaskState = false;
  state.deleteError = null;
};

const deleteVexAnnotationRejected = function (state, { payload }) {
  state.deleteMaskState = null;
  state.deleteError = payload.message;
};

const deleteVexAnnotationFulfilled = function (state) {
  state.deleteMaskState = true;
  state.deleteError = null;
};

const copyVexAnnotationRequested = function (state) {
  state.copyMaskState = false;
  state.copyError = null;
};

const copyVexAnnotationRejected = function (state, { payload }) {
  state.copyMaskState = null;
  state.copyError = payload.message;
};

const copyVexAnnotationFulfilled = function (state) {
  state.copyMaskState = true;
  state.copyError = null;
};

const getVulnerabilityAnalysisReferenceDataRequested = function (state) {
  state.loadingVulnerabilityAnalysisReferenceData = true;
  state.loadVulnerabilityAnalysisReferenceDataError = null;
  state.vulnerabilityAnalysisReferenceData = { responses: [], justifications: [], states: [] };
};
const getVulnerabilityAnalysisReferenceDataFulfilled = function (state, { payload }) {
  state.loadingVulnerabilityAnalysisReferenceData = false;
  state.loadVulnerabilityAnalysisReferenceDataError = null;
  state.vulnerabilityAnalysisReferenceData = { ...payload };
};

const getVulnerabilityAnalysisReferenceDataRejected = function (state, { payload }) {
  state.loadingVulnerabilityAnalysisReferenceData = false;
  state.loadVulnerabilityAnalysisReferenceDataError = payload.response.data;
};

const startMaskSuccessTimer = (dispatch, action) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(dispatch(action()));
    }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
  });
};

const loadComponentDetails = createAsyncThunk(
  `${REDUCER_NAME}/loadComponentDetails`,
  async ({ internalAppId, sbomVersion, componentHash }, { rejectWithValue }) => {
    try {
      const response = await axios.get(getSbomComponentDetailsUrl(internalAppId, sbomVersion, componentHash));
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const loadComponentDependencyTreeData = createAsyncThunk(
  `${REDUCER_NAME}/loadComponentDependencyTreeData`,
  async ({ hash: componentHash }, { rejectWithValue }) => {
    try {
      const response = await axios.get(getSbomComponentDependencyTreeUrl(componentHash));
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const loadVulnerabilityDetails = createAsyncThunk(
  `${REDUCER_NAME}/loadVulnerabilityDetails`,
  ({ componentIdentifier, vulnerability, extraParams }, { rejectWithValue }) => {
    const { ownerId, hash, isRepositoryComponent, scanId } = extraParams;
    const ownerType = isRepositoryComponent ? 'repository' : 'application';
    const extraQueryParameters = {
      ownerType,
      ownerId,
      scanId,
      identificationSource: vulnerability.source,
    };

    const vulnerabilityJsonDetailUrl = getVulnerabilityJsonDetailUrl(
      vulnerability.refId,
      componentIdentifier,
      extraQueryParameters
    );
    const vulnerabilityOverrideUrl = getVulnerabilityOverrideUrl(ownerType, ownerId, hash, vulnerability);

    return axios
      .all([axios.get(vulnerabilityJsonDetailUrl), axios.get(vulnerabilityOverrideUrl)])
      .then(([{ data: vulnerabilityDetails }, { data: vulnerabilityOverride }]) => {
        if (ownerType === 'application') {
          return axios
            .get(getApplicationSummaryUrl(ownerId))
            .then(({ data }) => {
              return checkPermissions(['WRITE'], ownerType, data.id);
            })
            .then(() => {
              return { ...vulnerabilityDetails, comment: vulnerabilityOverride.comment, hasEditIqPermission: true };
            })
            .catch(() => {
              return { ...vulnerabilityDetails, comment: vulnerabilityOverride.comment };
            });
        } else {
          return checkPermissions(['WRITE'], 'repository', ownerId)
            .then((_) => {
              return {
                ...vulnerabilityDetails,
                comment: vulnerabilityOverride.comment,
                hasEditIqPermission: true,
                _,
              };
            })
            .catch(() => {
              return { ...vulnerabilityDetails, comment: vulnerabilityOverride.comment };
            });
        }
      })
      .catch(rejectWithValue);
  }
);

const saveVexAnnotation = createAsyncThunk(
  `${REDUCER_NAME}/saveVexAnnotation`,
  async ({ internalAppId, sbomVersion, vulnerabilityRefId, vexAnnotationFormData }, { rejectWithValue }) => {
    const urlSaveUpdate = getSbomVulnerabilityAnnotationUrl(internalAppId, sbomVersion, vulnerabilityRefId);
    try {
      const response = await axios.put(urlSaveUpdate, vexAnnotationFormData);
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const deleteVexAnnotation = createAsyncThunk(
  `${REDUCER_NAME}/deleteVexAnnotation`,
  async ({ internalAppId, sbomVersion, vulnerabilityRefId, componentLocator }, { rejectWithValue, dispatch }) => {
    const url = getSbomVulnerabilityAnnotationUrl(internalAppId, sbomVersion, vulnerabilityRefId);
    return axios
      .delete(url, { data: componentLocator })
      .then(() => {
        startMaskSuccessTimer(dispatch, actions.deleteMaskTimerDone).then(() =>
          dispatch(actions.setShowDeleteModal(false))
        );
        dispatch(actions.loadComponentDetails({ internalAppId, sbomVersion, componentHash: componentLocator.hash }));
      })
      .catch((err) => rejectWithValue(err));
  }
);

const copyVexAnnotation = createAsyncThunk(
  `${REDUCER_NAME}/copyVexAnnotation`,
  async ({ internalAppId, sbomVersion, vulnerabilityRefId, vexAnnotationFormData }, { rejectWithValue, dispatch }) => {
    const urlSaveUpdate = getSbomVulnerabilityAnnotationUrl(internalAppId, sbomVersion, vulnerabilityRefId);
    return axios
      .put(urlSaveUpdate, vexAnnotationFormData)
      .then(() => {
        startMaskSuccessTimer(dispatch, actions.copyMaskTimerDone).then(() =>
          dispatch(actions.setShowCopyModal(false))
        );
        dispatch(
          actions.loadComponentDetails({
            internalAppId,
            sbomVersion,
            componentHash: vexAnnotationFormData.componentLocator.hash,
          })
        );
      })
      .catch((err) => rejectWithValue(err));
  }
);

const getVulnerabilityAnalysisReferenceData = createAsyncThunk(
  `${REDUCER_NAME}/loadVulnerabilityAnalysisReferenceData`,
  async (_, { rejectWithValue }) => {
    try {
      const response = await axios.get(getSbomVulnerabibilityAnalysisReferenceData());
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const cycleList = (list, current) => {
  const index = findIndex((item) => item === current, list);
  return list[(index + 1) % list.length];
};

const cycleVulnerabilitiesReducerCreator = (sortConfigurationStatePath) => (
  state,
  { payload: { sortBy: newSortBy } }
) => {
  const { sortBy: currentSortBy, sortDirection: currentSortDirection } = prop(sortConfigurationStatePath, state);
  const { sortBy: defaultSortBy, sortDirection: defaultSortDirection } = defaultSortConfiguration;
  const sortDirections = values(SORT_DIRECTION);

  if (newSortBy === defaultSortBy) {
    const complement = [defaultSortDirection, SORT_DIRECTION.DEFAULT];
    if (newSortBy !== currentSortBy) {
      state[sortConfigurationStatePath].sortDirection = sortDirections[0];
    } else if (includes(currentSortDirection, complement)) {
      state[sortConfigurationStatePath].sortDirection = cycleList(
        without(complement, sortDirections),
        currentSortDirection
      );
    } else if (includes(cycleList(sortDirections, currentSortDirection), complement)) {
      state[sortConfigurationStatePath].sortDirection = defaultSortDirection;
    }
    state[sortConfigurationStatePath].sortBy = newSortBy;
  } else {
    const nextDirection =
      newSortBy !== currentSortBy ? sortDirections[0] : cycleList(sortDirections, currentSortDirection);
    state[sortConfigurationStatePath] =
      nextDirection === SORT_DIRECTION.DEFAULT
        ? { ...defaultSortConfiguration }
        : { sortBy: newSortBy, sortDirection: nextDirection };
  }
};

const cycleDisclosedVulnerabilitiesSortDirection = cycleVulnerabilitiesReducerCreator(
  'disclosedVulnerabilitiesSortConfiguration'
);

const cycleAdditionalVulnerabilitiesSortDirection = cycleVulnerabilitiesReducerCreator(
  'additionalVulnerabilitiesSortConfiguration'
);

const showPolicyViolationDetailsDrawer = (state, { payload }) => {
  if (payload) {
    const policy = state.sbomPolicyViolations.policy;
    const violations = policy.allViolations ? policy.allViolations : policy.activeViolations;
    const findPolicyViolationDetails = (policyViolationId) =>
      find(propEq('policyViolationId', policyViolationId))(violations);
    const formattedViolationDetails = convertToViolationDetailsFormat(findPolicyViolationDetails(payload));

    state.policyViolationDetailsDrawer.showDrawer = true;
    state.policyViolationDetailsDrawer.policyViolationId = payload;
    state.policyViolationDetailsDrawer.violationDetails = formattedViolationDetails;
  }
};

const hidePolicyViolationDetailsDrawer = (state) => {
  state.policyViolationDetailsDrawer = { ...policyViolationDetailsDrawerInitialState };
};

const loadSbomPolicyViolations = createAsyncThunk(
  `${REDUCER_NAME}/loadSbomPolicyViolationReport`,
  async ({ applicationPublicId, sbomVersion, componentRef, fileCoordinateId, hash }, { rejectWithValue }) =>
    axios
      .get(getSbomPolicyViolationReportUrl(applicationPublicId, sbomVersion, componentRef, fileCoordinateId, hash))
      .then((response) => response.data)
      .catch((error) => rejectWithValue(error))
);

const loadSbomPolicyViolationsRequested = (state) => {
  state.sbomPolicyViolations = { ...sbomPolicyViolationsInitialState };
};

const loadSbomPolicyViolationsFulfilled = (state, { payload }) => {
  state.sbomPolicyViolations = Object.freeze({
    loading: false,
    error: null,
    policy: payload,
  });
};

const loadSbomPolicyViolationsRejected = (state, { payload }) => {
  state.sbomPolicyViolations = Object.freeze({
    loading: false,
    error: Messages.getHttpErrorMessage(payload),
    policy: null,
  });
};

const loadInternalAppIdRequested = (state) => {
  state.loadingInternalAppId = true;
  state.errorInternalAppId = null;
  state.applicationName = null;
};

const loadInternalAppIdFulfilled = (state, { payload }) => {
  state.loadingInternalAppId = false;
  state.errorInternalAppId = null;
  state.internalAppId = payload.id;
  state.applicationName = payload.name;
};

const loadInternalAppIdRejected = (state, { payload }) => {
  state.loadingInternalAppId = false;
  state.errorInternalAppId = payload.response.data;
  state.internalAppId = null;
  state.publicApplicationId = null;
};

const loadComponentsRequested = (state) => {
  state.loadingComponents = true;
  state.errorLoadingComponents = null;
};

const loadComponentsFulfilled = (state, { payload }) => {
  state.loadingComponents = false;
  state.errorLoadingComponents = null;
  state.componentDetailsPaginationData = {
    ...state.componentDetailsPaginationData,
    pagesData: {
      ...state.componentDetailsPaginationData.pagesData,
      [state.componentDetailsPaginationData.pagination.nextPage]: payload.results,
    },
  };
};

const loadComponentsRejected = (state, { payload }) => {
  state.loadingComponents = false;
  state.errorLoadingComponents = payload.response.data;
};

const loadInternalAppId = createAsyncThunk(
  `${REDUCER_NAME}/loadInternalAppId`,
  async (publicApplicationId, { rejectWithValue }) =>
    axios
      .get(getApplicationSummaryUrl(publicApplicationId))
      .then((response) => response.data)
      .catch((error) => rejectWithValue(error))
);

const loadComponents = createAsyncThunk(
  `${REDUCER_NAME}/loadComponents`,
  async ({ internalAppId, sbomVersion, pageToQuery }, { getState, rejectWithValue }) => {
    const state = getState();
    const { componentDetailsPaginationData } = selectSbomComponentDetails(state);
    const { sortConfiguration, filterConfiguration, componentNameSearchFromState } = componentDetailsPaginationData;
    const pickKeysWithTrueValue = compose(
      keys,
      pickBy((v) => !!v)
    );

    const sortDirection = cond([
      [equals(SORT_DIRECTION.ASC), always(true)],
      [equals(SORT_DIRECTION.DESC), always(false)],
      [T, always(null)],
    ])(sortConfiguration.sortDirection);
    return axios
      .get(
        getBillOfMaterialsComponentsUrl(
          internalAppId,
          sbomVersion,
          pageToQuery + 1,
          COMPONENTS_PER_PAGE,
          sortConfiguration.sortBy,
          sortDirection,
          pickKeysWithTrueValue(filterConfiguration.vulnerabilityThreatLevels),
          pickKeysWithTrueValue(filterConfiguration.dependencyTypes),
          componentNameSearchFromState
        )
      )
      .then((response) => response.data)
      .catch((err) => rejectWithValue(err));
  }
);

const sbomComponentDetailsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setActiveTabIndex,
    clearFormSubmitMask: function (state) {
      state.submitMaskStateForVexAnnotationForm = null;
    },
    setFormErrorSaveMessage: (state, { payload }) => {
      state.loadSaveVexAnnotationFormError = payload;
    },
    cycleDisclosedVulnerabilitiesSortDirection,
    cycleAdditionalVulnerabilitiesSortDirection,
    setSelectedIssueForActions,
    setShowDeleteModal: compose(propSetConst('deleteError', null), propSet('showDeleteModal')),
    deleteMaskTimerDone: propSetConst('deleteMaskState', null),
    setShowCopyModal: compose(propSetConst('copyError', null), propSet('showCopyModal')),
    copyMaskTimerDone: propSetConst('copyMaskState', null),
    showPolicyViolationDetailsDrawer,
    hidePolicyViolationDetailsDrawer,
    setComponentDetailsPaginationData: (state, { payload }) => {
      state.componentDetailsPaginationData = payload;
    },
    setComponentsNextPage: (state, { payload }) => {
      state.componentDetailsPaginationData.pagination.nextPage = payload;
    },
    updateCurrentPage,
  },
  extraReducers: {
    [loadComponentDetails.pending]: loadComponentDetailsRequested,
    [loadComponentDetails.fulfilled]: loadComponentDetailsFulfilled,
    [loadComponentDetails.rejected]: loadComponentDetailsFailed,
    [loadComponentDependencyTreeData.pending]: loadComponentDependencyTreeDataRequested,
    [loadComponentDependencyTreeData.fulfilled]: loadComponentDependencyTreeDataFulfilled,
    [loadComponentDependencyTreeData.rejected]: loadComponentDependencyTreeDataRejected,
    [loadVulnerabilityDetails.pending]: loadVulnerabilityDetailsRequested,
    [loadVulnerabilityDetails.fulfilled]: loadVulnerabilityDetailsFulfilled,
    [loadVulnerabilityDetails.rejected]: loadVulnerabilityDetailsRejected,
    [saveVexAnnotation.pending]: saveVexAnnotationRequested,
    [saveVexAnnotation.fulfilled]: saveVexAnnotationFulfilled,
    [saveVexAnnotation.rejected]: saveVexAnnotationRejected,
    [deleteVexAnnotation.pending]: deleteVexAnnotationRequested,
    [deleteVexAnnotation.fulfilled]: deleteVexAnnotationFulfilled,
    [deleteVexAnnotation.rejected]: deleteVexAnnotationRejected,
    [copyVexAnnotation.pending]: copyVexAnnotationRequested,
    [copyVexAnnotation.fulfilled]: copyVexAnnotationFulfilled,
    [copyVexAnnotation.rejected]: copyVexAnnotationRejected,
    [getVulnerabilityAnalysisReferenceData.pending]: getVulnerabilityAnalysisReferenceDataRequested,
    [getVulnerabilityAnalysisReferenceData.fulfilled]: getVulnerabilityAnalysisReferenceDataFulfilled,
    [getVulnerabilityAnalysisReferenceData.rejected]: getVulnerabilityAnalysisReferenceDataRejected,
    [loadSbomPolicyViolations.pending]: loadSbomPolicyViolationsRequested,
    [loadSbomPolicyViolations.fulfilled]: loadSbomPolicyViolationsFulfilled,
    [loadSbomPolicyViolations.rejected]: loadSbomPolicyViolationsRejected,
    [loadInternalAppId.pending]: loadInternalAppIdRequested,
    [loadInternalAppId.fulfilled]: loadInternalAppIdFulfilled,
    [loadInternalAppId.rejected]: loadInternalAppIdRejected,
    [loadComponents.pending]: loadComponentsRequested,
    [loadComponents.fulfilled]: loadComponentsFulfilled,
    [loadComponents.rejected]: loadComponentsRejected,
    [UI_ROUTER_ON_FINISH]: (state) => {
      const savedPaginationData = state.componentDetailsPaginationData;
      return {
        ...initialState,
        componentDetailsPaginationData: savedPaginationData,
      };
    },
  },
});

export const actions = {
  ...sbomComponentDetailsSlice.actions,
  loadComponentDetails,
  loadComponentDependencyTreeData,
  loadVulnerabilityDetails,
  loadSbomPolicyViolations,
  getVulnerabilityAnalysisReferenceData,
  saveVexAnnotation,
  deleteVexAnnotation,
  copyVexAnnotation,
  loadInternalAppId,
  loadComponents,
};

export default sbomComponentDetailsSlice.reducer;
