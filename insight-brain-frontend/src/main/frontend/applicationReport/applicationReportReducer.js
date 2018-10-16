/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { identity, inc, lensPath, pick, pipe, reduceBy, reject, set, sum, values } from 'ramda';

import {
  LOAD_REPORT_FAILED,
  LOAD_REPORT_FULFILLED,
  LOAD_REPORT_REQUESTED,
  SET_AGGREGATE_REPORT_ENTRIES,
  SET_FILTERING,
  SET_SORTING
} from './applicationReportActions';

import { aggregateReportEntries, filterReportEntries, sortReportEntries } from './applicationReportService.new';

const initState = {
  loading: false,
  loadError: null,
  aggregate: false,
  sortCol: 'policyThreatLevel',
  sortReversed: true,
  filters: {},
  selectedReport: null
};

export default function(state = initState, {type, payload}) {
  switch (type) {
    case LOAD_REPORT_REQUESTED:
      return {...state, loading: true, loadError: null, selectedReport: null};

    case LOAD_REPORT_FULFILLED:
      return updateDisplayedEntries({
        ...state,
        loading: false,
        selectedReport: {...payload, ...getViolationCountsPerThreatLevel(payload.allEntries)}
      });

    case LOAD_REPORT_FAILED:
      return {...state, loading: false, loadError: payload};

    case SET_AGGREGATE_REPORT_ENTRIES:
      return updateDisplayedEntries({...state, aggregate: payload});

    case SET_FILTERING:
      return updateDisplayedEntries({...state, filters: payload});

    case SET_SORTING:
      return updateDisplayedEntries({...state, ...pick(['sortCol', 'sortReversed'], payload)});

    default:
      return state;
  }
}

function updateDisplayedEntries(state) {
  const { selectedReport, sortCol, sortReversed, aggregate, filters } = state,
      { allEntries } = selectedReport,
      processEntries = pipe(
          aggregate ? aggregateReportEntries : identity,
          filterReportEntries(filters),
          sortReportEntries(sortCol, sortReversed)
      ),
      newDisplayedEntries = processEntries(allEntries);

  return set(lensPath(['selectedReport', 'displayedEntries']), newDisplayedEntries, state);
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
