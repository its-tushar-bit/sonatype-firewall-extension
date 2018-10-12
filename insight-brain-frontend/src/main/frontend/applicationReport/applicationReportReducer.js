/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { identity, lensPath, pick, pipe, set } from 'ramda';

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
  return {
    moderateViolationCount: entries.filter(between(2, 4)).length,
    severeViolationCount: entries.filter(between(4, 8)).length,
    criticalViolationCount: entries.filter(between(8, 11)).length,
    nonLowViolationCount: entries.filter(between(2, 11)).length
  };
}

function between(from, to) {
  return v => v.policyThreatLevel >= from && v.policyThreatLevel < to;
}
