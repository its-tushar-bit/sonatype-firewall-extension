/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import axios from 'axios';
import { getOwnerHierarchyUrl, getTransitiveViolationsUrl } from '../util/CLMLocation';
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
        let payload = {
          values: processOwnerHierarchy(data),
        };
        dispatch(loadAvailableScopesFulfilled(payload));
      })
      .catch((error) => {
        dispatch(loadAvailableScopesFailed(error));
      });
  };
}

export const TRANSITIVE_VIOLATIONS_LOAD_REQUESTED = 'TRANSITIVE_VIOLATIONS_LOAD_REQUESTED';
export const TRANSITIVE_VIOLATIONS_LOAD_FULFILLED = 'TRANSITIVE_VIOLATIONS_LOAD_FULFILLED';
export const TRANSITIVE_VIOLATIONS_LOAD_FAILED = 'TRANSITIVE_VIOLATIONS_LOAD_FAILED';

const loadTransitiveViolationsRequested = noPayloadActionCreator(TRANSITIVE_VIOLATIONS_LOAD_REQUESTED);
const loadTransitiveViolationsFulfilled = payloadParamActionCreator(TRANSITIVE_VIOLATIONS_LOAD_FULFILLED);
const loadTransitiveViolationsFailed = payloadParamActionCreator(TRANSITIVE_VIOLATIONS_LOAD_FAILED);

export function loadTransitiveViolations(ownerType, ownerId, stageTypeId, hash) {
  return (dispatch) => {
    dispatch(loadTransitiveViolationsRequested());

    return axios
      .get(getTransitiveViolationsUrl(ownerType, ownerId, stageTypeId, hash))
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
