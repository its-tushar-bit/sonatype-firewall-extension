/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  both,
  curryN,
  findIndex,
  inc,
  lensPath,
  pipe,
  propEq,
  reduceBy,
  reject,
  set,
  values,
  sum,
  indexBy,
  map,
  path,
  assoc,
} from 'ramda';
import {
  LOAD_COMMON_DATA_FAILED,
  LOAD_COMMON_DATA_FULFILLED,
  LOAD_COMMON_DATA_UNNECESSARY,
  LOAD_REPORT_FAILED,
  LOAD_REPORT_FULFILLED,
  LOAD_REPORT_REQUESTED,
  LOAD_REPORT_UNNECESSARY,
  LOAD_REPORT_RAW_DATA_FAILED,
  LOAD_REPORT_RAW_DATA_FULFILLED,
  LOAD_REPORT_RAW_DATA_REQUESTED,
  LOAD_REPORT_RAW_DATA_UNNECESSARY,
  LOAD_REPORT_ALL_DATA_REQUESTED,
  SET_AGGREGATE_REPORT_ENTRIES,
  SET_SUBSTRING_FIELD_FILTER,
  SET_RAW_DATA_SUBSTRING_FIELD_FILTER,
  SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER,
  SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER,
  SET_EXACT_VALUE_FILTER,
  SET_REPORT_PARAMETERS,
  REEVALUATE_REPORT_REQUESTED,
  REEVALUATE_REPORT_FULFILLED,
  REEVALUATE_REPORT_FAILED,
  REEVALUATE_REPORT_CANCELLED,
  SET_SORTING,
  SELECT_ROOT_ANCESTOR,
  UNSELECT_ROOT_ANCESTOR,
  GENERATE_VULNERABILITY_ENTRIES,
  SET_SORTING_PARAMETERS,
  SET_RAW_SORTING_PARAMETERS,
  SELECT_COMPONENT,
  APPLICATION_REPORT_TOGGLE_FILTER_SIDEBAR,
  OPEN_INNERSOURCE_PRODUCER_REPORT_MODAL,
  CLOSE_INNERSOURCE_PRODUCER_REPORT_MODAL,
  OPEN_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL,
  CLOSE_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL,
  TOGGLE_TREE_PATH,
  SET_DEPENDENCY_TREE_ROUTER_PARAMS,
  RESET_DEPENDENCY_TREE_ROUTER_PARAMS,
  SET_DEPENDENCY_TREE_SEARCH_TERM,
  EXPAND_ALL_DEPENDENCY_TREE_NODES,
  COLLAPSE_ALL_DEPENDENCY_TREE_NODES,
} from './applicationReportActions';
import { populateDependencyNodeKeys as populateEntryNodeKeys } from 'MainRoot/applicationReport/DependencyInfoGenerator';
import {
  deepReduce,
  extendDependencyTreeData,
  filterDependencyTree,
  filterDependencyTreeBySearchTerm,
} from 'MainRoot/DependencyTree/dependencyTreeUtil';

import { sortItemsByFields } from '../util/sortUtils';

import {
  aggregateReportEntries,
  filterReportEntries,
  getVulnerabilities,
  extendRawDataWithKey,
} from './applicationReportService';
import { getKey, isNilOrEmpty, pathSet } from '../util/jsUtil';

const initState = Object.freeze({
  pendingLoads: new Set(),
  filterSidebarOpen: false,
  reevaluating: false,
  loadError: null,
  reevaluationError: null,
  aggregate: true,
  sortFields: Object.freeze(['-policyThreatLevel', 'policyName', 'derivedComponentName']),

  // map from field name to Set of allowed values
  // example: { policyThreatLevel: new Set([1, 5, 6, 7]) }
  exactValueFilters: Object.freeze({}),
  reportRawData: null,

  // map from field name to string to use for substring matching
  // example: { policyName: 'security', derivedComponentName: 'foo' }
  substringFilters: Object.freeze({}),
  rawDataSubstringFilters: Object.freeze({}),

  // numeric filters to allow matching for numeric values
  rawDataNumericFilters: Object.freeze({}),

  selectedReport: null,
  selectedComponentIndex: null,
  selectedRootAncestor: null,
  policyTypeFilterEnabled: true,
  isUnknownJs: false,

  vulnerabilities: null,
  vulnerabilitiesPageEnabled: true,
  isInnerSourceEnabled: false,
  sortConfiguration: {
    key: 'policyThreatLevel',
    sortFields: ['-policyThreatLevel', 'policyName', 'derivedComponentName'],
    dir: 'desc',
  },
  rawSortConfiguration: {
    key: 'derivedComponentName',
    sortFields: ['derivedComponentName', 'licenseSortKey', 'securityCode', '-cvssScore'],
    dir: 'asc',
  },
  selectedComponent: null,
  dependencyTree: null,
  dependencyTreePageRouterParams: null,
  dependencyTreeSearchTerm: '',
  displayedDependencyTree: null,
  reevaluateMaskState: null,
});

export default function applicationReportReducer(state = initState, { type, payload }) {
  switch (type) {
    case SET_REPORT_PARAMETERS:
      return setReportParameters(state, payload);

    case SET_SORTING_PARAMETERS:
      return setSortingParameters(state, payload);

    case SET_RAW_SORTING_PARAMETERS:
      return updateRawDataDisplayedEntries(pathSet(['rawSortConfiguration'], payload, state));

    case LOAD_REPORT_REQUESTED:
      return setPendingLoads(['common', 'policy'], {
        ...state,
        loadError: null,
        selectedReport: null,
      });

    case LOAD_REPORT_FULFILLED:
      return setExtendedTreeData(setSelectedReport(state, payload), payload?.dependencies || {});

    case LOAD_COMMON_DATA_FULFILLED: {
      const { bomData, metadata, unknownJsData } = payload;
      return unsetPendingLoads(['common'], {
        ...state,
        bomData,
        metadata,
        unknownJsData,
      });
    }

    case LOAD_REPORT_RAW_DATA_REQUESTED:
      return setPendingLoads(['common', 'raw'], {
        ...state,
        loadError: null,
        reportRawData: null,
      });

    case LOAD_REPORT_RAW_DATA_FULFILLED:
      return setRawDataInformation(state, payload);

    case LOAD_REPORT_ALL_DATA_REQUESTED:
      return setPendingLoads(['common', 'raw', 'policy'], state);

    case REEVALUATE_REPORT_REQUESTED:
      return { ...state, reevaluating: true, reevaluationError: null, reevaluateMaskState: false };

    case REEVALUATE_REPORT_FULFILLED:
    case REEVALUATE_REPORT_CANCELLED:
      return { ...state, reevaluating: false, reevaluationError: null, reevaluateMaskState: true };

    case LOAD_REPORT_FAILED:
      return unsetPendingLoads(['policy'], { ...state, loadError: payload });

    case LOAD_REPORT_RAW_DATA_FAILED:
      return unsetPendingLoads(['raw'], { ...state, loadError: payload });

    case LOAD_COMMON_DATA_FAILED:
      return unsetPendingLoads(['common'], { ...state, loadError: payload });

    case REEVALUATE_REPORT_FAILED:
      return { ...state, reevaluating: false, reevaluationError: payload, reevaluateMaskState: null };

    case LOAD_COMMON_DATA_UNNECESSARY:
      return unsetPendingLoads(['common'], state);

    case LOAD_REPORT_RAW_DATA_UNNECESSARY:
      return unsetPendingLoads(['raw'], state);

    case LOAD_REPORT_UNNECESSARY:
      return unsetPendingLoads(['policy'], state);

    case SET_AGGREGATE_REPORT_ENTRIES:
      return updateDisplayedEntries({ ...state, aggregate: payload });

    case SET_EXACT_VALUE_FILTER: {
      const { fieldName, allowedValues } = payload;

      return updateDisplayedEntries(pathSet(['exactValueFilters', fieldName], allowedValues, state));
    }

    case SET_SUBSTRING_FIELD_FILTER: {
      const { fieldName, filterString } = payload;

      return updateDisplayedEntries(pathSet(['substringFilters', fieldName], filterString, state));
    }

    case SET_RAW_DATA_SUBSTRING_FIELD_FILTER: {
      const { fieldName, filterString } = payload;
      return updateRawDataDisplayedEntries(pathSet(['rawDataSubstringFilters', fieldName], filterString, state));
    }

    case SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER: {
      const { fieldName, filterValue } = payload;
      return updateRawDataDisplayedEntries(pathSet(['rawDataNumericFilters', fieldName, 0], filterValue, state));
    }

    case SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER: {
      const { fieldName, filterValue } = payload;
      return updateRawDataDisplayedEntries(pathSet(['rawDataNumericFilters', fieldName, 1], filterValue, state));
    }

    case SET_SORTING:
      return updateDisplayedEntries({ ...state, sortFields: payload });

    case SELECT_ROOT_ANCESTOR:
      return { ...state, selectedRootAncestor: payload };

    case UNSELECT_ROOT_ANCESTOR:
      return { ...state, selectedRootAncestor: null };

    case GENERATE_VULNERABILITY_ENTRIES:
      return generateVulnerabilityEntries(state);

    case SELECT_COMPONENT:
      return setSelectedComponent(state, payload);

    case APPLICATION_REPORT_TOGGLE_FILTER_SIDEBAR:
      return { ...state, filterSidebarOpen: payload };

    case OPEN_INNERSOURCE_PRODUCER_REPORT_MODAL:
      return pathSet(['selectedComponent', 'showInnerSourceProducerReportModal'], true, state);

    case CLOSE_INNERSOURCE_PRODUCER_REPORT_MODAL:
      return pathSet(['selectedComponent', 'showInnerSourceProducerReportModal'], false, state);

    case OPEN_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL:
      return pathSet(['selectedComponent', 'showInnerSourceProducerPermissionsModal'], true, state);

    case CLOSE_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL:
      return pathSet(['selectedComponent', 'showInnerSourceProducerPermissionsModal'], false, state);

    case TOGGLE_TREE_PATH:
      return setIsOpenAtTreePath(state, payload);

    case SET_DEPENDENCY_TREE_ROUTER_PARAMS:
      return { ...state, dependencyTreePageRouterParams: payload };

    case RESET_DEPENDENCY_TREE_ROUTER_PARAMS:
      return { ...state, dependencyTreePageRouterParams: null };

    case SET_DEPENDENCY_TREE_SEARCH_TERM:
      return applyDependencyTreeSearchTermFilter(pathSet(['dependencyTreeSearchTerm'], payload, state));

    case EXPAND_ALL_DEPENDENCY_TREE_NODES:
      return setExpansionStatusForAllNodesInDependencyTree(state, true);

    case COLLAPSE_ALL_DEPENDENCY_TREE_NODES:
      return setExpansionStatusForAllNodesInDependencyTree(state, false);

    default:
      return state;
  }
}

function setReportParameters(state, payload) {
  return {
    ...initState,
    reportParameters: payload,
  };
}

function setSelectedComponent(state, payload) {
  return {
    ...state,
    selectedComponent: payload.component,
    selectedComponentIndex: payload.componentIndex,
    selectedRootAncestor: null,
  };
}

function setSortingParameters(state, payload) {
  return {
    ...state,
    sortConfiguration: payload,
  };
}

function setSelectedReport(state, report) {
  const newState = pipe(
    updateDisplayedEntries,
    unsetPendingLoads(['policy'])
  )({
    ...state,
    policyTypeFilterEnabled: report.reportVersion && report.reportVersion >= 4,
    vulnerabilitiesPageEnabled: !!(report.reportVersion && report.reportVersion >= 5),
    selectedReport: {
      ...report,
      ...getViolationCountsPerThreatLevel(report.allEntries),
    },
    isInnerSourceEnabled: report.isInnerSourceEnabled,
  });

  // if there is selected component, update selectedComponentIndex
  if (state.selectedReport && state.selectedComponentIndex != null) {
    const selectedComponent = state.selectedReport.displayedEntries[state.selectedComponentIndex];
    const findPredicate = state.aggregate
      ? propEq('hash', selectedComponent.hash)
      : both(propEq('hash', selectedComponent.hash), propEq('policyName', selectedComponent.policyName));
    const selectedComponentIndex = findIndex(findPredicate, newState.selectedReport.displayedEntries);
    if (selectedComponentIndex >= 0) {
      return { ...newState, selectedComponentIndex };
    }
  }
  return newState;
}

function setRawDataInformation(state, rawDataEntries) {
  return pipe(
    updateRawDataDisplayedEntries,
    unsetPendingLoads(['raw'])
  )({
    ...state,
    reportRawData: {
      allEntries: rawDataEntries,
    },
  });
}

function generateVulnerabilityEntries(state) {
  const { reportRawData, selectedReport } = state;

  if (reportRawData == null || selectedReport == null) {
    return state;
  } else {
    const rawDataEntries = reportRawData.allEntries,
      policyEntries = selectedReport.allEntries,
      vulnerabilityEntries = getVulnerabilities(policyEntries, rawDataEntries);

    return {
      ...state,
      vulnerabilities: sortItemsByFields(
        ['violationSortState', '-policyThreatLevel', '-cvssScore', 'securityCode', 'derivedComponentName'],
        vulnerabilityEntries
      ),
    };
  }
}

/**
 * Update the `displayedEntries` field on the reportRawData section of the state
 * based on `allEntries` and the sorting passed-in, if any.
 */
function updateRawDataDisplayedEntries(state) {
  const { reportRawData, rawSortConfiguration, rawDataSubstringFilters, rawDataNumericFilters } = state;
  if (reportRawData) {
    const { allEntries } = reportRawData;
    const processEntries = pipe(
      filterReportEntries(null, rawDataSubstringFilters, rawDataNumericFilters),
      sortItemsByFields(rawSortConfiguration.sortFields),
      extendRawDataWithKey
    );
    const newDisplayedEntries = processEntries(allEntries);

    return pathSet(['reportRawData', 'displayedEntries'], newDisplayedEntries, state);
  } else {
    return state;
  }
}

/**
 * Calculates `aggregatedEntries` field for the selectedReport section of the state, and
 * updates the `displayedEntries` field on the selectedReport section of the state.
 * Both of these operations based on `allEntries` and the various sorting, filtering, and aggregation settings stored in the state
 */
function updateDisplayedEntries(state) {
  let { selectedReport, sortFields, aggregate, exactValueFilters, substringFilters } = state;
  if (selectedReport) {
    const { allEntries } = selectedReport,
      filterAndSortEntries = pipe(
        filterReportEntries(exactValueFilters, substringFilters, null),
        sortItemsByFields(sortFields)
      ),
      processAggregatedEntries = pipe(aggregateReportEntries, filterAndSortEntries),
      aggregatedEntries = processAggregatedEntries(allEntries),
      newDisplayedEntries = aggregate ? aggregatedEntries : filterAndSortEntries(allEntries),
      // create `aggregatedEntries` prop to be used for navigation
      stateWithAggregatedEntries = set(lensPath(['selectedReport', 'aggregatedEntries']), aggregatedEntries, state);

    return set(lensPath(['selectedReport', 'displayedEntries']), newDisplayedEntries, stateWithAggregatedEntries);
  } else {
    return state;
  }
}

function getViolationCountsPerThreatLevel(entries) {
  const zeroCounts = {
    criticalViolationCount: 0,
    severeViolationCount: 0,
    moderateViolationCount: 0,
  };
  const groupByThreatLevel = (v) =>
    v.policyThreatLevel >= 8
      ? 'criticalViolationCount'
      : v.policyThreatLevel >= 4
      ? 'severeViolationCount'
      : v.policyThreatLevel >= 2
      ? 'moderateViolationCount'
      : undefined;
  const reduceToCountsByThreatLevel = reduceBy(inc, 0)(groupByThreatLevel);
  const rejectIgnored = reject((v) => v.grandfathered || v.waived || v.policyThreatLevel < 2);
  const nonZeroCounts = pipe(rejectIgnored, reduceToCountsByThreatLevel)(entries);
  const nonLowViolationCount = sum(values(nonZeroCounts));
  return { ...zeroCounts, ...nonZeroCounts, nonLowViolationCount };
}

const mutatePendingLoads = curryN(3, function mutatePendingLoads(setMutator, loads, state) {
  const { pendingLoads } = state,
    newPendingLoads = new Set(pendingLoads);

  loads.forEach(setMutator(newPendingLoads));

  return { ...state, pendingLoads: newPendingLoads };
});

const setPendingLoads = mutatePendingLoads((set) => (val) => set.add(val)),
  unsetPendingLoads = mutatePendingLoads((set) => (val) => set.delete(val));

function setExtendedTreeData(state, dependencies) {
  if (isNilOrEmpty(dependencies?.dependencyTree?.children)) {
    return state;
  }

  const entriesByKey = map(populateEntryNodeKeys, state.selectedReport?.aggregatedEntries);
  const indexedEntries = indexBy(getKey, entriesByKey);
  const filteredDependencyTree = filterDependencyTree(dependencies.dependencyTree, indexedEntries);
  const dependencyTreeWithExtendedData = extendDependencyTreeData(filteredDependencyTree, indexedEntries);

  return {
    ...state,
    dependencyTree: dependencyTreeWithExtendedData,
    displayedDependencyTree: dependencyTreeWithExtendedData,
  };
}

function setIsOpenAtTreePath(state, treePath) {
  const { dependencyTree, displayedDependencyTree } = state;
  const node = path(treePath, displayedDependencyTree);
  const isOpen = !node.isOpen;
  const setIsOpenAtPath = (tree, path) => pathSet([...path, 'isOpen'], isOpen, tree);

  return {
    ...state,
    dependencyTree: setIsOpenAtPath(dependencyTree, node?.originalTreePath),
    displayedDependencyTree: setIsOpenAtPath(displayedDependencyTree, treePath),
  };
}

function uncollapseDisplayedDependencyTree(state, treePath) {
  const { displayedDependencyTree } = state;

  return {
    ...state,
    displayedDependencyTree: pathSet([...treePath, 'isOpen'], true, displayedDependencyTree),
  };
}

function applyDependencyTreeSearchTermFilter(state) {
  const { dependencyTree, dependencyTreeSearchTerm } = state;

  if (!dependencyTreeSearchTerm) return assoc('displayedDependencyTree', dependencyTree, state);

  const visibleDependencyTree = filterDependencyTreeBySearchTerm(dependencyTree, dependencyTreeSearchTerm);
  const stateWithUpdatedDependencyTree = assoc('displayedDependencyTree', visibleDependencyTree, state);
  const uncollapseBranch = (state, node) => uncollapseDisplayedDependencyTree(state, node.treePath);
  const stateWithUncollapsedTree = deepReduce(uncollapseBranch, stateWithUpdatedDependencyTree, visibleDependencyTree);

  return stateWithUncollapsedTree;
}

function setExpansionStatusForAllNodesInDependencyTree(state, isOpen) {
  const { dependencyTree, displayedDependencyTree } = state;
  const updateExpansionStatus = (tree, node) => pathSet(node.treePath, { ...node, isOpen }, tree);

  return {
    ...state,
    dependencyTree: deepReduce(updateExpansionStatus, [], dependencyTree),
    displayedDependencyTree: deepReduce(updateExpansionStatus, [], displayedDependencyTree),
  };
}
