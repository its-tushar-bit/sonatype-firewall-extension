/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getLicenseLegalComponentUrl, getOwnerHierarchyUrl } from '../util/CLMLocation';
import { payloadParamActionCreator } from '../util/reduxUtil';
import { processOwnerHierarchy } from '../util/hierarchyUtil';

export const ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED = 'ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED';
export const ADVANCED_LEGAL_LOAD_COMPONENT_FAILED = 'ADVANCED_LEGAL_LOAD_COMPONENT_FAILED';

export const ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED = 'ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED';
export const ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED = 'ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED';

const loadComponentFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
const loadComponentFailed = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_COMPONENT_FAILED);

const loadAvailableScopesFulfilled = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED);
const loadAvailableScopesFailed = payloadParamActionCreator(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED);

export function loadComponent(orgOrApp, ownerId, hash) {
  return (dispatch) => {
    return axios
      .get(getLicenseLegalComponentUrl(orgOrApp, ownerId, hash))
      .then(({ data }) => {
        dispatch(loadComponentFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadComponentFailed(error));
      });
  };
}

export function loadAvailableScopes(ownerType, ownerId) {
  return (dispatch) => {
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
