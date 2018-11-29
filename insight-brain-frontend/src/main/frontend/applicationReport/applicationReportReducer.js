/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  identity,
  inc,
  lensPath,
  pipe,
  reduceBy,
  reject,
  set,
  sum,
  values
} from 'ramda';

import {
  LOAD_REPORT_FAILED,
  LOAD_REPORT_FULFILLED,
  LOAD_REPORT_REQUESTED,
  SET_AGGREGATE_REPORT_ENTRIES,
  SET_SUBSTRING_FIELD_FILTER,
  REEVALUATE_REPORT_REQUESTED,
  REEVALUATE_REPORT_FULFILLED,
  REEVALUATE_REPORT_FAILED,
  REEVALUATE_REPORT_CANCELLED,
  SET_SORTING,
  SELECT_COMPONENT
} from './applicationReportActions';

import { aggregateReportEntries, filterReportEntries, sortReportEntries } from './applicationReportService';
import { pathSet } from '../util/jsUtil';

const initState = {
  loading: false,
  reevaluating: false,
  loadError: null,
  reevaluationError: null,
  aggregate: true,
  sortFields: ['-policyThreatLevel', 'policyName', 'derivedComponentName'],

  // map from field name to array of allowed values
  // example: { policyThreatLevel: [1, 5, 6, 7] }
  exactValueFilters: {},

  // map from field name to string to use for substring matching
  // example: { policyName: 'security', derivedComponentName: 'foo' }
  substringFilters: {},
  selectedReport: null,
  selectedComponentIndex: null
};

export default function(state = initState, {type, payload}) {
  switch (type) {
    case LOAD_REPORT_REQUESTED:
      return {...state, loading: true, loadError: null, selectedReport: null};

    case LOAD_REPORT_FULFILLED:
      return updateDisplayedEntries({
        ...state,
        loading: false,
        isUnknownJs: payload.isUnknownJs,
        selectedReport: {...(payload.report), ...getViolationCountsPerThreatLevel(payload.report.allEntries)}
      });

    case REEVALUATE_REPORT_REQUESTED:
      return {...state, reevaluating: true, reevaluationError: null};

    case REEVALUATE_REPORT_FULFILLED:
    case REEVALUATE_REPORT_CANCELLED:
      return {...state, reevaluating: false, reevaluationError: null};

    case LOAD_REPORT_FAILED:
      return {...state, loading: false, loadError: payload};

    case REEVALUATE_REPORT_FAILED:
      return {...state, reevaluating: false, reevaluationError: payload};

    case SET_AGGREGATE_REPORT_ENTRIES:
      return updateDisplayedEntries({...state, aggregate: payload});

    case SET_SUBSTRING_FIELD_FILTER: {
      const { fieldName, filterString } = payload;

      return updateDisplayedEntries(pathSet(['substringFilters', fieldName], filterString, state));
    }

    case SET_SORTING:
      return updateDisplayedEntries({...state, sortFields: payload});

    case SELECT_COMPONENT:
      return {...state, selectedComponentIndex: payload};

    default:
      return state;
  }
}

/**
 * Update the `displayedEntries` field on the state based on `allEntries` and the various sorting, filtering,
 * and aggregation settings stored in the state
 */
function updateDisplayedEntries(state) {
  const { selectedReport, sortFields, aggregate, exactValueFilters, substringFilters } = state;

  if (selectedReport) {
    const { allEntries } = selectedReport,
        processEntries = pipe(
            aggregate ? aggregateReportEntries : identity,
            filterReportEntries(exactValueFilters, substringFilters),
            sortReportEntries(sortFields)
        ),
        newDisplayedEntries = processEntries(allEntries);

    return set(lensPath(['selectedReport', 'displayedEntries']), newDisplayedEntries, state);
  }
  else {
    return state;
  }
}

function getViolationCountsPerThreatLevel(entries) {
  const zeroCounts = {'criticalViolationCount': 0, 'severeViolationCount': 0, 'moderateViolationCount': 0};
  const groupByThreatLevel = v => v.policyThreatLevel >= 8 ? 'criticalViolationCount' : v.policyThreatLevel >= 4 ?
    'severeViolationCount' : v.policyThreatLevel >= 2 ? 'moderateViolationCount' : undefined;
  const reduceToCountsByThreatLevel = reduceBy(inc, 0)(groupByThreatLevel);
  const rejectIgnored = reject(v => v.grandfathered || v.waived || v.policyThreatLevel < 2);
  const nonZeroCounts = pipe(rejectIgnored, reduceToCountsByThreatLevel)(entries);
  const nonLowViolationCount = sum(values(nonZeroCounts));
  return {...zeroCounts, ...nonZeroCounts, nonLowViolationCount};
}
