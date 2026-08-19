/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { contains, keys } from 'ramda';

import { payloadParamActionCreator } from '../util/reduxUtil';
import { getDashboardStageUrl, getActionStageUrl, getCliStageUrl } from '../util/CLMLocation';

export const FETCH_STAGE_TYPES_REQUESTED = 'FETCH_STAGE_TYPES_REQUESTED';
export const FETCH_STAGE_TYPES_FULFILLED = 'FETCH_STAGE_TYPES_FULFILLED';
export const FETCH_STAGE_TYPES_FAILED = 'FETCH_STAGE_TYPES_FAILED';

const urlsByPurpose = {
  dashboard: getDashboardStageUrl(),
  action: getActionStageUrl(),
  cli: getCliStageUrl(),
};

export const validPurposes = keys(urlsByPurpose);

/**
 * Fetches the stage types for the specified purpose _if they are not already loaded_
 */
export function fetchStageTypes(purpose) {
  return function (dispatch, getState) {
    if (!contains(purpose, validPurposes)) {
      throw new TypeError(`purpose must be one of ${validPurposes}`);
    }

    if (!getState().stages[purpose].stageTypes) {
      dispatch(fetchStageTypesRequested(purpose));

      return axios
        .get(urlsByPurpose[purpose])
        .then(({ data }) => {
          dispatch(fetchStageTypesFulfilled(purpose, data));
        })
        .catch((error) => {
          dispatch(fetchStageTypesFailed(purpose, error));
        });
    } else {
      return Promise.resolve();
    }
  };
}

const fetchStageTypesRequested = payloadParamActionCreator(FETCH_STAGE_TYPES_REQUESTED);

const fetchStageTypesFulfilled = (purpose, data) => ({
  type: FETCH_STAGE_TYPES_FULFILLED,
  payload: {
    purpose,
    data,
  },
});

const fetchStageTypesFailed = (purpose, error) => ({
  type: FETCH_STAGE_TYPES_FAILED,
  payload: {
    purpose,
    error,
  },
});
