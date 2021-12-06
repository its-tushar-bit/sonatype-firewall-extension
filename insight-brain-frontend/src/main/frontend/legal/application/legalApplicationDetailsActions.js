/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import { find, propEq } from 'ramda';
import { getLegalDashboardApplicationUrl, getApplicationUrl, getActionStageUrl } from '../../util/CLMLocation';
import { payloadParamActionCreator, noPayloadActionCreator } from '../../util/reduxUtil';

export const LEGAL_APPLICATION_DETAILS_LOAD_APP_REQUESTED = 'LEGAL_APPLICATION_DETAILS_LOAD_APP_REQUESTED';
export const LEGAL_APPLICATION_DETAILS_LOAD_APP_FULFILLED = 'LEGAL_APPLICATION_DETAILS_LOAD_APP_FULFILLED';
export const LEGAL_APPLICATION_DETAILS_LOAD_APP_FAILED = 'LEGAL_APPLICATION_DETAILS_LOAD_APP_FAILED';

export const LEGAL_APPLICATION_DETAILS_LOAD_STAGE_REQUESTED = 'LEGAL_APPLICATION_DETAILS_LOAD_STAGE_REQUESTED';
export const LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FULFILLED = 'LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FULFILLED';
export const LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FAILED = 'LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FAILED';

export const LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_REQUESTED =
  'LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_REQUESTED';
export const LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FULFILLED =
  'LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FULFILLED';
export const LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FAILED = 'LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FAILED';

export const LEGAL_APPLICATION_DETAILS_APPLY_FILTERS = 'LEGAL_APPLICATION_DETAILS_APPLY_FILTERS';

const legalApplicationDetailsLoadAppRequested = noPayloadActionCreator(LEGAL_APPLICATION_DETAILS_LOAD_APP_REQUESTED);
const legalApplicationDetailsLoadAppFulfilled = payloadParamActionCreator(LEGAL_APPLICATION_DETAILS_LOAD_APP_FULFILLED);
const legalApplicationDetailsLoadAppFailed = payloadParamActionCreator(LEGAL_APPLICATION_DETAILS_LOAD_APP_FAILED);

const legalApplicationDetailsLoadStageRequested = noPayloadActionCreator(
  LEGAL_APPLICATION_DETAILS_LOAD_STAGE_REQUESTED
);
const legalApplicationDetailsLoadStageFulfilled = payloadParamActionCreator(
  LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FULFILLED
);
const legalApplicationDetailsLoadStageFailed = payloadParamActionCreator(LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FAILED);

const legalApplicationDetailsLoadComponentsRequested = noPayloadActionCreator(
  LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_REQUESTED
);
const legalApplicationDetailsLoadComponentsFulfilled = payloadParamActionCreator(
  LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FULFILLED
);
const legalApplicationDetailsLoadComponentsFailed = payloadParamActionCreator(
  LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FAILED
);

const legalApplicationDetailsApplyFilters = noPayloadActionCreator(LEGAL_APPLICATION_DETAILS_APPLY_FILTERS);

export function loadApplication(applicationPublicId, stageTypeId) {
  return (dispatch) => {
    dispatch(legalApplicationDetailsLoadAppRequested());

    return axios
      .get(getApplicationUrl(applicationPublicId))
      .then((response) => {
        dispatch(legalApplicationDetailsLoadAppFulfilled(response.data));
        return dispatch(loadStageType(stageTypeId));
      })
      .then(() => dispatch(loadComponents(applicationPublicId, stageTypeId)))
      .catch((error) => {
        dispatch(legalApplicationDetailsLoadAppFailed(error));
        return Promise.reject(error);
      });
  };
}

function loadStageType(stageTypeId) {
  return (dispatch) => {
    if (!stageTypeId) {
      dispatch(legalApplicationDetailsLoadStageFailed('stageTypeId is mandatory.'));
      return Promise.reject('stageTypeId is mandatory.');
    }
    dispatch(legalApplicationDetailsLoadStageRequested());

    return axios
      .get(getActionStageUrl())
      .then((response) => {
        const stageType = find(propEq('stageTypeId', stageTypeId), response.data);
        if (stageType) {
          return dispatch(legalApplicationDetailsLoadStageFulfilled(stageType.stageName));
        }
        return Promise.reject(`${stageTypeId} is not a valid stage type ID.`);
      })
      .catch((error) => {
        dispatch(legalApplicationDetailsLoadStageFailed(error));
        return Promise.reject(error);
      });
  };
}

function loadComponents(applicationPublicId, stageTypeId) {
  return (dispatch) => {
    dispatch(legalApplicationDetailsLoadComponentsRequested());

    return axios
      .post(getLegalDashboardApplicationUrl(applicationPublicId), {
        stageTypeIds: [stageTypeId],
      })
      .then((response) => dispatch(legalApplicationDetailsLoadComponentsFulfilled(response.data)))
      .then(() => dispatch(legalApplicationDetailsApplyFilters()))
      .catch((error) => {
        dispatch(legalApplicationDetailsLoadComponentsFailed(error));
        return Promise.reject(error);
      });
  };
}
