/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createReducerFromActionMap } from '../util/reduxUtil';
import {
  QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FAILED,
  QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FULFILLED,
  QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED,
} from './quarantinedComponentReportActions';

const initialState = Object.freeze({
  viewState: Object.freeze({
    loadError: null,
    componentOverview: {
      componentOverviewLoading: true,
      componentDisplayName: '',
      isQuarantined: false,
      quarantinedPolicyViolationsCount: '',
      repositoryName: '',
      quarantinedDate: '',
      cataloguedDate: '',
    },
  }),
});

const loadComponentOverviewRequested = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    componentOverview: {
      ...state.viewState.componentOverview,
      componentOverviewLoading: true,
    },
  },
});

const loadComponentOverviewFulfilled = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadError: null,
    componentOverview: {
      ...payload,
      componentOverviewLoading: false,
    },
  },
});

const loadComponentOverviewFailed = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadError: state.viewState.loadError || payload,
    componentOverview: {
      ...state.viewState.componentOverview,
      componentOverviewLoading: false,
    },
  },
});

const reducerActionMap = {
  [QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FAILED]: loadComponentOverviewFailed,
  [QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FULFILLED]: loadComponentOverviewFulfilled,
  [QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED]: loadComponentOverviewRequested,
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
