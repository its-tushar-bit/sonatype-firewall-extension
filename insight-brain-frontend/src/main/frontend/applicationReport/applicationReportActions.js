/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { map, pick } from 'ramda';
import axios from 'axios';

import { createReportEntries, createRawDataEntries } from './applicationReportService';
import {
  mappedPayloadParamActionCreator,
  noPayloadActionCreator,
  payloadParamActionCreator,
  httpErrorMessageActionCreator,
} from '../util/reduxUtil';
import {
  getDependenciesUrl,
  getReportBomUrl,
  getReportDataUrl,
  getReportLicenseUrl,
  getReportMetadataUrl,
  getReportPartialMatchedUrl,
  getReportPolicyThreatsUrl,
  getReportReevaluateUrl,
  getReportSecurityUrl,
  getReportUnknownJsUrl,
} from '../util/CLMLocation';
import {
  selectRouterCurrentParams,
  selectIsPrioritiesPageContainer,
  selectPrioritiesPageContainerName,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectSelectedReport, selectReportParameters } from './applicationReportSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';

export const LOAD_REPORT_REQUESTED = 'LOAD_REPORT_REQUESTED';
export const LOAD_REPORT_FULFILLED = 'LOAD_REPORT_FULFILLED';
export const LOAD_REPORT_FAILED = 'LOAD_REPORT_FAILED';
export const LOAD_REPORT_UNNECESSARY = 'LOAD_REPORT_UNNECESSARY';
export const LOAD_REPORT_RAW_DATA_REQUESTED = 'LOAD_REPORT_RAW_DATA_REQUESTED';
export const LOAD_REPORT_RAW_DATA_FULFILLED = 'LOAD_REPORT_RAW_DATA_FULFILLED';
export const LOAD_REPORT_RAW_DATA_FAILED = 'LOAD_REPORT_RAW_DATA_FAILED';
export const LOAD_REPORT_RAW_DATA_UNNECESSARY = 'LOAD_REPORT_RAW_DATA_UNNECESSARY';
export const LOAD_COMMON_DATA_FULFILLED = 'LOAD_COMMON_DATA_FULFILLED';
export const LOAD_COMMON_DATA_FAILED = 'LOAD_COMMON_DATA_FAILED';
export const LOAD_COMMON_DATA_UNNECESSARY = 'LOAD_COMMON_DATA_UNNECESSARY';
export const LOAD_REPORT_ALL_DATA_REQUESTED = 'LOAD_REPORT_ALL_DATA_REQUESTED';
export const TOGGLE_AGGREGATE_REPORT_ENTRIES = 'TOGGLE_AGGREGATE_REPORT_ENTRIES';
export const SET_REPORT_PARAMETERS = 'SET_REPORT_PARAMETERS';
export const SELECT_ROOT_ANCESTOR = 'SELECT_ROOT_ANCESTOR';
export const UNSELECT_ROOT_ANCESTOR = 'UNSELECT_ROOT_ANCESTOR';
export const REEVALUATE_REPORT_REQUESTED = 'REEVALUATE_REPORT_REQUESTED';
export const REEVALUATE_REPORT_FULFILLED = 'REEVALUATE_REPORT_FULFILLED';
export const REEVALUATE_REPORT_FAILED = 'REEVALUATE_REPORT_FAILED';
export const REEVALUATE_REPORT_CANCELLED = 'REEVALUATE_REPORT_CANCELLED';
export const GENERATE_VULNERABILITY_ENTRIES = 'GENERATE_VULNERABILITY_ENTRIES';
export const SET_SORTING_PARAMETERS = 'SET_SORTING_PARAMETERS';
export const SET_RAW_SORTING_PARAMETERS = 'SET_RAW_SORTING_PARAMETERS';
export const SELECT_COMPONENT = 'SELECT_COMPONENT';
export const APPLICATION_REPORT_TOGGLE_FILTER_SIDEBAR = 'APPLICATION_REPORT_TOGGLE_FILTER_SIDEBAR';
export const OPEN_INNERSOURCE_PRODUCER_REPORT_MODAL = 'OPEN_INNERSOURCE_PRODUCER_REPORT_MODAL';
export const CLOSE_INNERSOURCE_PRODUCER_REPORT_MODAL = 'CLOSE_INNERSOURCE_PRODUCER_REPORT_MODAL';
export const OPEN_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL = 'OPEN_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL';
export const CLOSE_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL = 'CLOSE_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL';
export const TOGGLE_SHOW_FILTER_POPOVER = 'TOGGLE_SHOW_FILTER_POPOVER';

// To be used for filters that are done by substring matching, as opposed to matching a discrete set of values
export const SET_SUBSTRING_FIELD_FILTER = 'SET_SUBSTRING_FIELD_FILTER';
export const SET_EXACT_VALUE_FILTER = 'SET_EXACT_VALUE_FILTER';
export const SET_RAW_DATA_SUBSTRING_FIELD_FILTER = 'SET_RAW_DATA_SUBSTRING_FIELD_FILTER';
export const SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER = 'SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER';
export const SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER = 'SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER';
export const SET_SORTING = 'SET_SORTING';

export function setReportParameters(
  appId,
  scanId,
  isUnknownJs,
  embeddable,
  policyViolationId,
  componentHash,
  tabId,
  isNotFiltered
) {
  return {
    type: SET_REPORT_PARAMETERS,
    payload: { appId, scanId, isUnknownJs, embeddable, policyViolationId, componentHash, tabId, isNotFiltered },
  };
}

export function setSortingParameters(key, sortFields, dir) {
  return {
    type: SET_SORTING_PARAMETERS,
    payload: { key, sortFields, dir },
  };
}

export function setRawSortingParameters(key, sortFields, dir) {
  return {
    type: SET_RAW_SORTING_PARAMETERS,
    payload: { key, sortFields, dir },
  };
}

export function fetchCommonData(forceClearMetadata = false) {
  return (dispatch, getState) => {
    const state = getState();
    const { bomData, unknownJsData, metadata } = state.applicationReport;
    const reportParameters = selectReportParameters(state);
    const { appId, scanId, isUnknownJs } = reportParameters;

    if (forceClearMetadata || !metadata || !bomData || (!unknownJsData && isUnknownJs)) {
      const promises = [axios.get(getReportBomUrl(appId, scanId)), axios.get(getReportMetadataUrl(appId, scanId))];

      if (isUnknownJs) {
        promises.push(axios.get(getReportUnknownJsUrl(appId, scanId)));
      }

      return Promise.all(promises)
        .then((results) => {
          const bomResult = results[0].data || undefined;
          const metadataResult = results[1].data;
          const unknownJsResult = (isUnknownJs && results[2].data) || undefined;

          return dispatch(
            loadCommonDataFulfilled({
              bomData: bomResult,
              metadata: metadataResult,
              unknownJsData: unknownJsResult,
            })
          );
        })
        .catch((error) => {
          if (error !== 'XC Report') {
            dispatch(loadCommonDataFailed(error));
          }

          return Promise.reject(error);
        });
    }

    return Promise.resolve(dispatch(loadCommonDataUnnecessary()));
  };
}

export function fetchReportData(forceReload = true) {
  return (dispatch, getState) => {
    const state = getState();
    const { bomData, unknownJsData, selectedReport } = state.applicationReport;
    const reportParameters = selectReportParameters(state);
    const { appId, scanId } = reportParameters;

    if (forceReload || !selectedReport) {
      const promises = [
        axios.get(getReportPolicyThreatsUrl(appId, scanId)),
        axios.get(getReportDataUrl(appId, scanId)),
        axios.get(getReportPartialMatchedUrl(appId, scanId)),
        axios.get(getDependenciesUrl(appId, scanId)),
      ];

      return Promise.all(promises)
        .then((results) => {
          const policyResult = results[0].data || undefined;
          const dataResult = results[1].data;
          const partialMatches = results[2].data || undefined;
          const dependencies = results[3].data;

          const allEntries = createReportEntries(policyResult, bomData, unknownJsData, partialMatches, dependencies);
          const reportVersion = (policyResult && policyResult.version) || null;
          return dispatch(
            loadReportFulfilled({
              allEntries: allEntries.policies,
              isInnerSourceEnabled: allEntries.isInnerSourceEnabled,
              reportVersion,
              dependencies,
              ...dataResult,
            })
          );
        })
        .catch((error) => {
          dispatch(loadReportFailed(error));
        });
    } else {
      return Promise.resolve(dispatch(loadReportUnnecessary()));
    }
  };
}

export function fetchReportRawData(forceReload = true) {
  return (dispatch, getState) => {
    const state = getState();
    const { bomData, unknownJsData, reportRawData } = state.applicationReport;
    const reportParameters = selectReportParameters(state);
    const { appId, scanId } = reportParameters;

    if (forceReload || !reportRawData) {
      const promises = [axios.get(getReportSecurityUrl(appId, scanId)), axios.get(getReportLicenseUrl(appId, scanId))];

      return Promise.all(promises)
        .then((results) => {
          const securityResult = results[0].data;
          const licenseResult = results[1].data;
          const allEntries = createRawDataEntries(securityResult, licenseResult, bomData, unknownJsData);
          return dispatch(loadReportRawDataFulfilled(allEntries));
        })
        .catch((error) => {
          dispatch(loadReportRawDataFailed(error));
        });
    } else {
      return Promise.resolve(dispatch(loadReportRawDataUnnecessary()));
    }
  };
}

export function loadReport(forceClearMetadata = false) {
  return (dispatch) => {
    dispatch({
      type: LOAD_REPORT_REQUESTED,
    });

    return dispatch(fetchCommonData(forceClearMetadata))
      .then(() => dispatch(fetchReportData(forceClearMetadata)))
      .catch((error) => dispatch(loadReportFailed(error)));
  };
}

export function loadReportRawData() {
  return (dispatch) => {
    dispatch({
      type: LOAD_REPORT_RAW_DATA_REQUESTED,
    });

    // Rejected promised from `fetchCommonData` simply mean not to proceed to the next step, but the
    // error handling has already been done. So just swallow them
    return dispatch(fetchCommonData())
      .then(() => dispatch(fetchReportRawData()))
      .catch(() => {});
  };
}

export function loadReportAllData() {
  return (dispatch) => {
    dispatch({
      type: LOAD_REPORT_ALL_DATA_REQUESTED,
    });
    return dispatch(fetchCommonData())
      .then(() => Promise.all(map(dispatch, [fetchReportRawData(false), fetchReportData(false)])))
      .then(() => dispatch(generateVulnerabilityEntries()))
      .catch(() => {});
  };
}

export function selectComponent(componentIndex) {
  return (dispatch, getState) => {
    const { selectedReport } = getState().applicationReport;
    const component = selectedReport && selectedReport.displayedEntries[componentIndex];
    return Promise.resolve(dispatch(setSelectedComponent({ component, componentIndex })));
  };
}

const loadCommonDataFulfilled = mappedPayloadParamActionCreator(
  LOAD_COMMON_DATA_FULFILLED,
  pick(['bomData', 'metadata', 'unknownJsData'])
);

const loadCommonDataFailed = httpErrorMessageActionCreator(LOAD_COMMON_DATA_FAILED);
const loadCommonDataUnnecessary = noPayloadActionCreator(LOAD_COMMON_DATA_UNNECESSARY);
const loadReportFulfilled = payloadParamActionCreator(LOAD_REPORT_FULFILLED);
const loadReportFailed = httpErrorMessageActionCreator(LOAD_REPORT_FAILED);
const loadReportUnnecessary = httpErrorMessageActionCreator(LOAD_REPORT_UNNECESSARY);
const loadReportRawDataFulfilled = payloadParamActionCreator(LOAD_REPORT_RAW_DATA_FULFILLED);
const loadReportRawDataFailed = httpErrorMessageActionCreator(LOAD_REPORT_RAW_DATA_FAILED);
const loadReportRawDataUnnecessary = httpErrorMessageActionCreator(LOAD_REPORT_RAW_DATA_UNNECESSARY);
const generateVulnerabilityEntries = noPayloadActionCreator(GENERATE_VULNERABILITY_ENTRIES);
const setSelectedComponent = payloadParamActionCreator(SELECT_COMPONENT);
const toggleFilterSidebar = payloadParamActionCreator(APPLICATION_REPORT_TOGGLE_FILTER_SIDEBAR);
export const toggleShowFilterPopover = payloadParamActionCreator(TOGGLE_SHOW_FILTER_POPOVER);
export const setSorting = payloadParamActionCreator(SET_SORTING);

export const toggleAggregateReportEntries = noPayloadActionCreator(TOGGLE_AGGREGATE_REPORT_ENTRIES);

export function setStringFieldFilter(fieldName, filterString) {
  return {
    type: SET_SUBSTRING_FIELD_FILTER,
    payload: { fieldName, filterString },
  };
}

export function setRawDataStringFieldFilter(fieldName, filterString) {
  return {
    type: SET_RAW_DATA_SUBSTRING_FIELD_FILTER,
    payload: { fieldName, filterString },
  };
}

export function setRawDataNumericMaxFilter(fieldName, filterValue) {
  return {
    type: SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER,
    payload: { fieldName, filterValue },
  };
}

export function setRawDataNumericMinFilter(fieldName, filterValue) {
  return {
    type: SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER,
    payload: { fieldName, filterValue },
  };
}

export function setExactValueFilter(fieldName, allowedValues) {
  return {
    type: SET_EXACT_VALUE_FILTER,
    payload: { fieldName, allowedValues },
  };
}

export const selectRootAncestor = payloadParamActionCreator(SELECT_ROOT_ANCESTOR);
export const unselectRootAncestor = noPayloadActionCreator(UNSELECT_ROOT_ANCESTOR);

export function reevaluateReport(skipAutoWaivers = false) {
  return (dispatch, getState) => {
    const state = getState();
    const reportParameters = selectReportParameters(state);
    const { appId, scanId } = reportParameters;

    dispatch({
      type: REEVALUATE_REPORT_REQUESTED,
    });

    const reevaluateUrl = getReportReevaluateUrl(appId, scanId) + (skipAutoWaivers ? '?skipAutoWaivers=true' : '');

    return axios
      .post(reevaluateUrl)
      .then(() => {
        dispatch(reevaluateReportFulfilled());
        return dispatch(loadReport(true));
      })
      .catch((error) => {
        dispatch(reevaluateReportFailed(error));
      });
  };
}

const reevaluateReportFulfilled = noPayloadActionCreator(REEVALUATE_REPORT_FULFILLED);
const reevaluateReportFailed = httpErrorMessageActionCreator(REEVALUATE_REPORT_FAILED);
export const reevaluateReportCancelled = noPayloadActionCreator(REEVALUATE_REPORT_CANCELLED);

export const openInnerSourceProducerReportModal = noPayloadActionCreator(OPEN_INNERSOURCE_PRODUCER_REPORT_MODAL);
export const closeInnerSourceProducerReportModal = noPayloadActionCreator(CLOSE_INNERSOURCE_PRODUCER_REPORT_MODAL);
export const openInnerSourceProducerPermissionsModal = noPayloadActionCreator(
  OPEN_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL
);
export const closeInnerSourceProducerPermissionsModal = noPayloadActionCreator(
  CLOSE_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL
);

export function loadReportIfNeeded() {
  return (dispatch, getState) => {
    const state = getState();
    const currentParams = selectRouterCurrentParams(state);
    const selectedReport = selectSelectedReport(state);
    const reportParameters = selectReportParameters(state);
    const isReportInMemory = selectedReport && !!(currentParams?.scanId === reportParameters?.scanId);

    if (isReportInMemory) {
      // if report is in memory resolve promise with it
      return Promise.resolve(dispatch(loadReportUnnecessary()));
    }
    // if report is not in memory, send request
    return dispatch(loadReport());
  };
}

export const TOGGLE_TREE_PATH = 'DEPENDENCY_TREE_TOGGLE_TREE_PATH';
export const toggleTreePathAction = payloadParamActionCreator(TOGGLE_TREE_PATH);

export const SET_DEPENDENCY_TREE_ROUTER_PARAMS = 'SET_DEPENDENCY_TREE_ROUTER_PARAMS';
export const setDependencyTreeRouterParams = payloadParamActionCreator(SET_DEPENDENCY_TREE_ROUTER_PARAMS);

export const RESET_DEPENDENCY_TREE_ROUTER_PARAMS = 'RESET_DEPENDENCY_TREE_ROUTER_PARAMS';
export const resetDependencyTreeRouterParams = noPayloadActionCreator(RESET_DEPENDENCY_TREE_ROUTER_PARAMS);

export const setDependencyTreeRouterParamsForBackButton = () => {
  return (dispatch, getState) => {
    const currentRouterParams = selectRouterCurrentParams(getState());
    dispatch(setDependencyTreeRouterParams(pick(['publicId', 'scanId'], currentRouterParams)));
  };
};

export const SET_DEPENDENCY_TREE_SEARCH_TERM = 'SET_DEPENDENCY_TREE_SEARCH_TERM';
export const setDependencyTreeSearchTerm = payloadParamActionCreator(SET_DEPENDENCY_TREE_SEARCH_TERM);

export const EXPAND_ALL_DEPENDENCY_TREE_NODES = 'EXPAND_ALL_DEPENDENCY_TREE_NODES';
export const expandAllDependencyTreeNodes = noPayloadActionCreator(EXPAND_ALL_DEPENDENCY_TREE_NODES);

export const COLLAPSE_ALL_DEPENDENCY_TREE_NODES = 'COLLAPSE_ALL_DEPENDENCY_TREE_NODES';
export const collapseAllDependencyTreeNodes = noPayloadActionCreator(COLLAPSE_ALL_DEPENDENCY_TREE_NODES);

export const goToDependencyTreePage = (hash) => {
  return (dispatch, getState) => {
    const { publicId, scanId } = selectRouterCurrentParams(getState());
    const isPrioritiesPageContainer = selectIsPrioritiesPageContainer(getState());
    const prioritiesPageContainerName = selectPrioritiesPageContainerName(getState());

    if (isPrioritiesPageContainer) {
      dispatch(stateGo(`${prioritiesPageContainerName}.dependencyTree`, { hash, publicId, scanId }));
    } else {
      dispatch(stateGo('applicationReport.dependencyTree', { hash, publicId, scanId }));
    }
  };
};

export const goToComponentDetailsPage = (hash) => {
  return (dispatch, getState) => {
    const { publicId, scanId } = selectRouterCurrentParams(getState());
    const isPrioritiesPageContainer = selectIsPrioritiesPageContainer(getState());
    const prioritiesPageContainerName = selectPrioritiesPageContainerName(getState());

    if (isPrioritiesPageContainer) {
      dispatch(
        stateGo(`${prioritiesPageContainerName}.componentDetailsFromReport.overview`, { hash, publicId, scanId })
      );
    } else {
      dispatch(stateGo('applicationReport.componentDetails', { hash, publicId, scanId }));
    }
  };
};

export default function applicationReportActions() {
  return {
    setReportParameters,
    loadReport,
    loadReportRawData,
    loadReportAllData,
    reevaluateReport,
    reevaluateReportCancelled,
    toggleAggregateReportEntries,
    setStringFieldFilter,
    setExactValueFilter,
    setRawDataStringFieldFilter,
    setRawDataNumericMaxFilter,
    setRawDataNumericMinFilter,
    setSorting,
    selectComponent,
    selectRootAncestor,
    unselectRootAncestor,
    setSortingParameters,
    setRawSortingParameters,
    toggleFilterSidebar,
    openInnerSourceProducerReportModal,
    closeInnerSourceProducerReportModal,
    openInnerSourceProducerPermissionsModal,
    closeInnerSourceProducerPermissionsModal,
    loadReportIfNeeded,
    toggleTreePathAction,
    setDependencyTreeRouterParams,
    resetDependencyTreeRouterParams,
    setDependencyTreeRouterParamsForBackButton,
    setDependencyTreeSearchTerm,
    toggleShowFilterPopover,
    expandAllDependencyTreeNodes,
    collapseAllDependencyTreeNodes,
    goToComponentDetailsPage,
    goToDependencyTreePage,
  };
}
