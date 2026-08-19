/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import axios from 'axios';
import {
  getOwnerHierarchyUrl,
  getReportMetadataUrl,
  getTransitiveViolationsUrl,
  getWaiveTransitiveViolationsUrl,
} from '../util/CLMLocation';
import { processOwnerHierarchy } from '../util/hierarchyUtil';

export const TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_REQUESTED =
  'TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_REQUESTED';
export const TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FULFILLED =
  'TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FULFILLED';
export const TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FAILED = 'TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FAILED';

const loadAvailableScopesRequested = noPayloadActionCreator(TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_REQUESTED);
const loadAvailableScopesFulfilled = payloadParamActionCreator(TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FULFILLED);
const loadAvailableScopesFailed = payloadParamActionCreator(TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FAILED);

export function loadAvailableScopes(ownerType, ownerId) {
  return (dispatch) => {
    dispatch(loadAvailableScopesRequested());

    return axios
      .get(getOwnerHierarchyUrl(ownerType, ownerId))
      .then(({ data }) => {
        dispatch(loadAvailableScopesFulfilled(processOwnerHierarchy(data)));
      })
      .catch((error) => {
        dispatch(loadAvailableScopesFailed(error));
      });
  };
}

export const TRANSITIVE_VIOLATIONS_LOAD_REQUESTED = 'TRANSITIVE_VIOLATIONS_LOAD_REQUESTED';
export const TRANSITIVE_VIOLATIONS_LOAD_FULFILLED = 'TRANSITIVE_VIOLATIONS_LOAD_FULFILLED';
export const TRANSITIVE_VIOLATIONS_LOAD_FAILED = 'TRANSITIVE_VIOLATIONS_LOAD_FAILED';

export const TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_REQUESTED =
  'TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_REQUESTED';
export const TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FULFILLED =
  'TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FULFILLED';
export const TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FAILED = 'TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FAILED';

const loadReportMetadataRequested = noPayloadActionCreator(TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_REQUESTED);
const loadReportMetadataFulfilled = payloadParamActionCreator(TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FULFILLED);
const loadReportMetadataFailed = payloadParamActionCreator(TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FAILED);

export function loadReportMetadata(applicationPublicId, scanId) {
  return (dispatch) => {
    dispatch(loadReportMetadataRequested());

    return axios
      .get(getReportMetadataUrl(applicationPublicId, scanId))
      .then(({ data }) => {
        dispatch(loadReportMetadataFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadReportMetadataFailed(error));
      });
  };
}

const loadTransitiveViolationsRequested = noPayloadActionCreator(TRANSITIVE_VIOLATIONS_LOAD_REQUESTED);
const loadTransitiveViolationsFulfilled = payloadParamActionCreator(TRANSITIVE_VIOLATIONS_LOAD_FULFILLED);
const loadTransitiveViolationsFailed = payloadParamActionCreator(TRANSITIVE_VIOLATIONS_LOAD_FAILED);

export function loadTransitiveViolations(ownerType, ownerId, scanId, hash) {
  return (dispatch) => {
    dispatch(loadTransitiveViolationsRequested());

    return axios
      .get(getTransitiveViolationsUrl(ownerType, ownerId, scanId, hash))
      .then(({ data }) => {
        dispatch(loadTransitiveViolationsFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadTransitiveViolationsFailed(error));
      });
  };
}

export const TRANSITIVE_VIOLATIONS_SET_SORTING_PARAMETERS = 'TRANSITIVE_VIOLATIONS_SET_SORTING_PARAMETERS';

export const setSortingParameters = payloadParamActionCreator(TRANSITIVE_VIOLATIONS_SET_SORTING_PARAMETERS);

export const TRANSITIVE_VIOLATIONS_SET_FILTERING_PARAMETERS = 'TRANSITIVE_VIOLATIONS_SET_FILTERING_PARAMETERS';

export const setFilteringParameters = payloadParamActionCreator(TRANSITIVE_VIOLATIONS_SET_FILTERING_PARAMETERS);

export const TRANSITIVE_VIOLATIONS_TOGGLE_REQUEST_WAIVE = 'TRANSITIVE_VIOLATIONS_TOGGLE_REQUEST_WAIVE';

export const toggleRequestWaiveTransitiveViolations = noPayloadActionCreator(
  TRANSITIVE_VIOLATIONS_TOGGLE_REQUEST_WAIVE
);

export const TRANSITIVE_VIOLATIONS_TOGGLE_WAIVE = 'TRANSITIVE_VIOLATIONS_TOGGLE_WAIVE';

export const toggleWaiveTransitiveViolations = noPayloadActionCreator(TRANSITIVE_VIOLATIONS_TOGGLE_WAIVE);

export const TRANSITIVE_VIOLATION_WAIVERS_LOAD_REQUESTED = 'TRANSITIVE_VIOLATION_WAIVERS_LOAD_REQUESTED';
export const TRANSITIVE_VIOLATION_WAIVERS_LOAD_FULFILLED = 'TRANSITIVE_VIOLATION_WAIVERS_LOAD_FULFILLED';
export const TRANSITIVE_VIOLATION_WAIVERS_LOAD_FAILED = 'TRANSITIVE_VIOLATION_WAIVERS_LOAD_FAILED';

const loadTransitiveViolationWaiversRequested = noPayloadActionCreator(TRANSITIVE_VIOLATION_WAIVERS_LOAD_REQUESTED);
const loadTransitiveViolationWaiversFulfilled = payloadParamActionCreator(TRANSITIVE_VIOLATION_WAIVERS_LOAD_FULFILLED);
const loadTransitiveViolationWaiversFailed = payloadParamActionCreator(TRANSITIVE_VIOLATION_WAIVERS_LOAD_FAILED);

export function loadTransitiveViolationWaivers(ownerId, scanId, hash) {
  return (dispatch) => {
    dispatch(loadTransitiveViolationWaiversRequested());

    return axios
      .get(getWaiveTransitiveViolationsUrl(ownerId, scanId, hash))
      .then(({ data }) => {
        dispatch(loadTransitiveViolationWaiversFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadTransitiveViolationWaiversFailed(error));
      });
  };
}

export const TRANSITIVE_VIOLATIONS_TOGGLE_VIEW_WAIVERS = 'TRANSITIVE_VIOLATIONS_TOGGLE_VIEW_WAIVERS';

export const toggleViewTransitiveViolationWaivers = noPayloadActionCreator(TRANSITIVE_VIOLATIONS_TOGGLE_VIEW_WAIVERS);
