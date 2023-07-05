/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { sortItemsByFields, sortWaiversByFields } from '../../util/sortUtils';
import {
  getApplicationRisks,
  getComponentRisks,
  getNewestRisks,
  getWaivers,
  DASHBOARD_PAGE_SIZE,
} from '../services/dashboard.data.service';
import dashboardServicesModule from '../services/module';
import { isNil, partial } from 'ramda';
import {
  APPLICATIONS_RESULTS_TYPE,
  COMPONENTS_RESULTS_TYPE,
  VIOLATIONS_RESULTS_TYPE,
  WAIVERS_RESULTS_TYPE,
} from 'MainRoot/dashboard/results/dashboardResultsTypes';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

export const LOAD_RESULTS_REQUESTED = 'LOAD_RESULTS_REQUESTED';
export const LOAD_RESULTS_FULFILLED = 'LOAD_RESULTS_FULFILLED';
export const LOAD_RESULTS_FAILED = 'LOAD_RESULTS_FAILED';
export const SORT_RESULTS_REQUESTED = 'SORT_RESULTS_REQUESTED';
export const SORT_RESULTS_FULFILLED = 'SORT_RESULTS_FULFILLED';
export const RESET_ALL_TABS = 'RESET_ALL_TABS';
export const DASHBOARD_SET_PAGE = 'DASHBOARD_SET_PAGE';

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

export function setPage(resultsType, page) {
  return (dispatch) => {
    dispatch({
      type: DASHBOARD_SET_PAGE,
      payload: { resultsType, page },
    });

    return dispatch(loadResults(resultsType));
  };
}

export const loadViolationResults = partial(loadResults, [VIOLATIONS_RESULTS_TYPE]);
export const loadComponentResults = partial(loadResults, [COMPONENTS_RESULTS_TYPE]);
export const loadApplicationResults = partial(loadResults, [APPLICATIONS_RESULTS_TYPE]);
export const loadWaiverResults = partial(loadResults, [WAIVERS_RESULTS_TYPE]);

export const setViolationsPage = partial(setPage, [VIOLATIONS_RESULTS_TYPE]);

function sortResults(resultsType, sortFields) {
  return (dispatch, getState) => {
    dispatch({
      type: SORT_RESULTS_REQUESTED,
      payload: { resultsType, sortFields },
    });

    const dashboardState = getState().dashboard;
    const results = dashboardState[resultsType].results;
    const numResults = dashboardState[resultsType].numResults;
    if (!results || numResults > DASHBOARD_PAGE_SIZE) {
      return dispatch(loadResults(resultsType));
    } else {
      // use sortWaiversByFields only for waivers in case expiryTime prop is null
      const sortByType = {
        waivers: sortWaiversByFields,
        default: sortItemsByFields,
      };

      // sort results in frontend
      const sorted = sortByType[resultsType]
        ? sortByType[resultsType](sortFields, results)
        : sortByType.default(sortFields, results);
      dispatch(sortResultsFulfilled(resultsType, sorted));
      return Promise.resolve();
    }
  };
}

export const sortViolationResults = partial(sortResults, [VIOLATIONS_RESULTS_TYPE]);
export const sortComponentResults = partial(sortResults, [COMPONENTS_RESULTS_TYPE]);
export const sortApplicationResults = partial(sortResults, [APPLICATIONS_RESULTS_TYPE]);
export const sortWaiversResults = partial(sortResults, [WAIVERS_RESULTS_TYPE]);

function sortResultsFulfilled(resultsType, results) {
  return {
    type: SORT_RESULTS_FULFILLED,
    payload: { resultsType, results },
  };
}

function fetchResults(resultsType, state) {
  const sortFields = state.dashboard[resultsType].sortFields;
  let pageFromParams = selectRouterCurrentParams(state)?.page;
  if (!isNil(pageFromParams)) {
    pageFromParams--;
  }
  const page = state.dashboard[resultsType].page ?? pageFromParams ?? 0;
  const serviceMethod = getServiceMethod(resultsType);
  return serviceMethod(state.dashboardFilter.appliedFilter, sortFields, page);
}

function getServiceMethod(resultsType) {
  switch (resultsType) {
    case 'violations':
      return getNewestRisks;

    case 'components':
      return getComponentRisks;

    case 'applications':
      return getApplicationRisks;

    case 'waivers':
      return getWaivers;

    default:
      throw new Error('dashboard results is not supported for ' + resultsType);
  }
}

export default angular
  .module('dashboardResultsActionsModule', [dashboardServicesModule.name])
  .value('dashboardResultsActions', { loadResults, sortResults });
