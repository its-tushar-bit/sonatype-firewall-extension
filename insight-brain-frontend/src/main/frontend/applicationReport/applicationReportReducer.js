/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {LOAD_REPORT_FAILED, LOAD_REPORT_FULFILLED, LOAD_REPORT_REQUESTED} from './applicationReportActions';

const initState = {
  loading: false,
  loadError: null,
  selectedReport: null
};

export default function(state = initState, {type, payload}) {
  switch (type) {
    case LOAD_REPORT_REQUESTED:
      return {...state, loading: true, loadError: null, selectedReport: null};

    case LOAD_REPORT_FULFILLED:
      return {...state, loading: false, selectedReport: payload};

    case LOAD_REPORT_FAILED:
      return {...state, loading: false, loadError: payload};

    default:
      return state;
  }
}
