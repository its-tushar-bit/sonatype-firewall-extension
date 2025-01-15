/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  sortWaiversByFields,
  caseInsensitiveComparator,
  defaultComparator,
  sortItemsByFieldsWithComparator,
} from '../../util/sortUtils';
import axios from 'axios';
import {
  getApplicationRisks,
  getComponentRisks,
  getNewestRisks,
  getWaivers,
  getWaiversAndAutoWaivers,
} from '../services/dashboard.data.service';
import { isNil, partial } from 'ramda';
import {
  APPLICATIONS_RESULTS_TYPE,
  COMPONENTS_RESULTS_TYPE,
  VIOLATIONS_RESULTS_TYPE,
  WAIVERS_RESULTS_TYPE,
} from 'MainRoot/dashboard/results/dashboardResultsTypes';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { getProductFeaturesUrl } from 'MainRoot/util/CLMLocation';

export const LOAD_RESULTS_REQUESTED = 'LOAD_RESULTS_REQUESTED';
export const LOAD_RESULTS_FULFILLED = 'LOAD_RESULTS_FULFILLED';
export const LOAD_RESULTS_FAILED = 'LOAD_RESULTS_FAILED';
export const SORT_RESULTS_REQUESTED = 'SORT_RESULTS_REQUESTED';
export const SORT_RESULTS_FULFILLED = 'SORT_RESULTS_FULFILLED';
export const RESET_ALL_TABS = 'RESET_ALL_TABS';
export const DASHBOARD_SET_PAGE = 'DASHBOARD_SET_PAGE';

function loadResultsFulfilled(resultsType, results, hasNextPage, classyBrew) {
  return {
    type: LOAD_RESULTS_FULFILLED,
    payload: { resultsType, results, hasNextPage, classyBrew },
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
        const { results, hasNextPage, classyBrew } = data;
        dispatch(loadResultsFulfilled(resultsType, results, hasNextPage, classyBrew));
      })
      .catch((error) => {
        dispatch(loadResultsFailed(resultsType, error));
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

export function setNextPage(resultsType) {
  return (dispatch, getState) => {
    const nextPage = getState().dashboard[resultsType].page + 1;

    return dispatch(setPage(resultsType, nextPage));
  };
}

export function setPreviousPage(resultsType) {
  return (dispatch, getState) => {
    const previousPage = getState().dashboard[resultsType].page - 1;

    return dispatch(setPage(resultsType, previousPage));
  };
}

export const loadViolationResults = partial(loadResults, [VIOLATIONS_RESULTS_TYPE]);
export const loadComponentResults = partial(loadResults, [COMPONENTS_RESULTS_TYPE]);
export const loadApplicationResults = partial(loadResults, [APPLICATIONS_RESULTS_TYPE]);
export const loadWaiverResults = partial(loadResults, [WAIVERS_RESULTS_TYPE]);

export const setNextViolationsPage = partial(setNextPage, [VIOLATIONS_RESULTS_TYPE]);
export const setPreviousViolationsPage = partial(setPreviousPage, [VIOLATIONS_RESULTS_TYPE]);
export const setNextComponentsPage = partial(setNextPage, [COMPONENTS_RESULTS_TYPE]);
export const setPreviousComponentsPage = partial(setPreviousPage, [COMPONENTS_RESULTS_TYPE]);
export const setNextApplicationsPage = partial(setNextPage, [APPLICATIONS_RESULTS_TYPE]);
export const setPreviousApplicationsPage = partial(setPreviousPage, [APPLICATIONS_RESULTS_TYPE]);
export const setNextWaiversPage = partial(setNextPage, [WAIVERS_RESULTS_TYPE]);
export const setPreviousWaiversPage = partial(setPreviousPage, [WAIVERS_RESULTS_TYPE]);

function sortResults(resultsType, sortFields) {
  return (dispatch, getState) => {
    dispatch({
      type: SORT_RESULTS_REQUESTED,
      payload: { resultsType, sortFields },
    });

    const dashboardState = getState().dashboard;
    const results = dashboardState[resultsType].results;
    const hasMultiplePages = dashboardState[resultsType].hasMultiplePages;
    if (!results || hasMultiplePages) {
      return dispatch(loadResults(resultsType));
    } else {
      // sort results in frontend
      // use sortWaiversByFields only for waivers in case expiryTime prop is null
      const sorted =
        resultsType === WAIVERS_RESULTS_TYPE
          ? sortWaiversByFields(sortFields, results)
          : sortItemsByFieldsWithComparator(
              (a, b) => {
                if (typeof a === 'string' && typeof b === 'string') {
                  return caseInsensitiveComparator(a, b);
                }
                return defaultComparator(a, b);
              },
              sortFields,
              results
            );
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

async function fetchResults(resultsType, state) {
  const features = await axios
    .get(getProductFeaturesUrl())
    .then((response) => response.data)
    .catch((error) => {
      console.error('Error fetching product features:', error);
      return [];
    });

  const autoWaiversFeature = features.includes('auto-waivers');
  const sortFields = state.dashboard[resultsType].sortFields;
  let pageFromParams = selectRouterCurrentParams(state)?.page;
  if (!isNil(pageFromParams)) {
    pageFromParams--;
  }
  const page = state.dashboard[resultsType].page ?? pageFromParams ?? 0;
  const serviceMethod = getServiceMethod(resultsType, autoWaiversFeature);
  return serviceMethod(state.dashboardFilter.appliedFilter, sortFields, page);
}

function getServiceMethod(resultsType, autoWaiversFeature) {
  switch (resultsType) {
    case 'violations':
      return getNewestRisks;

    case 'components':
      return getComponentRisks;

    case 'applications':
      return getApplicationRisks;

    case 'waivers':
      return autoWaiversFeature ? getWaiversAndAutoWaivers : getWaivers;

    default:
      throw new Error('dashboard results is not supported for ' + resultsType);
  }
}
