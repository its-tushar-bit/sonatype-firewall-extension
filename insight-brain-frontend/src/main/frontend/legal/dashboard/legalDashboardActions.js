/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { getLegalDashboardApplicationsUrl } from '../../util/CLMLocation';

export const LOAD_LEGAL_RESULTS_REQUESTED = 'LOAD_LEGAL_RESULTS_REQUESTED';
export const LOAD_LEGAL_RESULTS_FULFILLED = 'LOAD_LEGAL_RESULTS_FULFILLED';
export const LOAD_LEGAL_RESULTS_FAILED = 'LOAD_LEGAL_RESULTS_FAILED';

function loadResultsFulfilled(resultsType, results) {
  return {
    type: LOAD_LEGAL_RESULTS_FULFILLED,
    payload: { resultsType, results }
  };
}

function loadResultsFailed(resultsType, error) {
  return {
    type: LOAD_LEGAL_RESULTS_FAILED,
    payload: { resultsType, error }
  };
}

export function loadResults(resultsType) {
  return (dispatch, getState) => {
    dispatch({
      type: LOAD_LEGAL_RESULTS_REQUESTED,
      payload: resultsType
    });

    return fetchResults(resultsType, getState())
        .then(payload => {
          dispatch(loadResultsFulfilled(resultsType, payload.data));
        })
        .catch(error => {
          dispatch(loadResultsFailed(resultsType, error));
          return Promise.reject(error);
        });
  };
}

function fetchResults(resultsType, state) {
  const { applications, organizations, stages, categories } = state.legalDashboardFilter.appliedFilter;
  const applicationFilter = {
    applicationIds: Array.from(applications),
    organizationIds: Array.from(organizations),
    stageTypeIds: Array.from(stages),
    tagIds: Array.from(categories)
  };

  const serviceMethod = getServiceMethod(resultsType);
  return axios.post(serviceMethod(), applicationFilter);
}

function getServiceMethod(resultsType) {
  switch (resultsType) {
    case 'applications':
      return getLegalDashboardApplicationsUrl;

    default:
      throw new Error('retrieving legal dashboard results is not supported for ' + resultsType);
  }
}
