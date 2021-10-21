/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createReducerFromActionMap } from '../util/reduxUtil';
import { always } from 'ramda';
import {
  QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_FAILED,
  QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_FULFILLED,
  QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_REQUESTED,
} from './quarantinedComponentReportActions';

const initialState = Object.freeze({
  viewState: Object.freeze({
    loadError: null,
    dataLoading: true,
    repositoryComponentId: '',
  }),
});

const loadComponentFulfilled = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    dataLoading: false,
    repositoryComponentId: payload.repositoryComponentId,
  },
});

const loadComponentFailed = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadError: payload,
    dataLoading: false,
    repositoryComponentId: '',
  },
});

const reducerActionMap = {
  [QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_REQUESTED]: always(initialState),
  [QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_FULFILLED]: loadComponentFulfilled,
  [QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_FAILED]: loadComponentFailed,
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
