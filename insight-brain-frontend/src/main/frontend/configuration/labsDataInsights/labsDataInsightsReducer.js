/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createReducerFromActionMap } from '../../util/reduxUtil';
import {
  LABS_DATA_INSIGHTS_REQUESTED,
  LABS_DATA_INSIGHTS_FULFILLED,
  LABS_DATA_INSIGHTS_FAILED,
} from './labsDataInsightsActions';

const initialState = {
  viewState: {
    loadingLabsDataInsights: false,
    errorMessage: null,
  },
};

function loadLabsDataInsightsRequested() {
  return {
    viewState: {
      errorMessage: null,
      loadingLabsDataInsights: true,
    },
  };
}

function loadLabsDataInsightsFulfilled(state) {
  return {
    ...state,
    viewState: {
      errorMessage: null,
      loadingLabsDataInsights: false,
    },
  };
}

function loadingLabsDataInsightsFailed(payload, state) {
  return {
    ...state,
    viewState: {
      errorMessage: payload,
      loadingLabsDataInsights: false,
    },
  };
}

const reducerActionMap = {
  [LABS_DATA_INSIGHTS_REQUESTED]: loadLabsDataInsightsRequested,
  [LABS_DATA_INSIGHTS_FULFILLED]: loadLabsDataInsightsFulfilled,
  [LABS_DATA_INSIGHTS_FAILED]: loadingLabsDataInsightsFailed,
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
