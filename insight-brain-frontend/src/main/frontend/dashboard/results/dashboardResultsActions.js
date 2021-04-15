/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { sortItemsByFields } from '../../util/sortUtils';
import {
  getNewestRisks,
  getApplicationRisks,
  getComponentRisks,
  MAX_RESULTS,
} from '../services/dashboard.data.service';
import dashboardServicesModule from '../services/module';

export const LOAD_RESULTS_REQUESTED = 'LOAD_RESULTS_REQUESTED';
export const LOAD_RESULTS_FULFILLED = 'LOAD_RESULTS_FULFILLED';
export const LOAD_RESULTS_FAILED = 'LOAD_RESULTS_FAILED';
export const SORT_RESULTS_REQUESTED = 'SORT_RESULTS_REQUESTED';
export const SORT_RESULTS_FULFILLED = 'SORT_RESULTS_FULFILLED';

function loadResultsFulfilled(resultsType, results, numResults, classyBrew) {
  return {
    type: LOAD_RESULTS_FULFILLED,
    payload: { resultsType, results, numResults, classyBrew },
  };
}

function loadResultsFailed(resultsType, error) {
  return {
    type: LOAD_RESULTS_FAILED,
    payload: { resultsType, error },
  };
}

export function loadResults(resultsType) {
  return (dispatch, getState) => {
    dispatch({
      type: LOAD_RESULTS_REQUESTED,
      payload: resultsType,
    });

    return fetchResults(resultsType, getState())
      .then((data) => {
        const { results, numResults, classyBrew } = data;
        dispatch(loadResultsFulfilled(resultsType, results, numResults, classyBrew));
      })
      .catch((error) => {
        dispatch(loadResultsFailed(resultsType, error));
        return Promise.reject(error);
      });
  };
}

export function sortResults(resultsType, sortFields) {
  return (dispatch, getState) => {
    dispatch({
      type: SORT_RESULTS_REQUESTED,
      payload: { resultsType, sortFields },
    });

    const dashboardState = getState().dashboard;
    const results = dashboardState[resultsType].results;
    const numResults = dashboardState[resultsType].numResults;
    if (!results || numResults > MAX_RESULTS) {
      return dispatch(loadResults(resultsType));
    } else {
      // sort results in frontend
      const sorted = sortItemsByFields(sortFields, results);
      dispatch(sortResultsFulfilled(resultsType, sorted));
      return Promise.resolve();
    }
  };
}

function sortResultsFulfilled(resultsType, results) {
  return {
    type: SORT_RESULTS_FULFILLED,
    payload: { resultsType, results },
  };
}

function fetchResults(resultsType, state) {
  const sortFields = state.dashboard[resultsType].sortFields;
  const serviceMethod = getServiceMethod(resultsType);
  return serviceMethod(state.dashboardFilter.appliedFilter, sortFields);
}

function getServiceMethod(resultsType) {
  switch (resultsType) {
    case 'violations':
      return getNewestRisks;

    case 'components':
      return getComponentRisks;

    case 'applications':
      return getApplicationRisks;

    default:
      throw new Error('dashboard results is not supported for ' + resultsType);
  }
}

export default angular
  .module('dashboardResultsActionsModule', [dashboardServicesModule.name])
  .value('dashboardResultsActions', { loadResults, sortResults });
