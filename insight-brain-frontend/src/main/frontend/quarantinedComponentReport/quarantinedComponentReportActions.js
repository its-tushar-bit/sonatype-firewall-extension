/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import { Messages } from '../util/CommonServices';

import { getQuarantinedComponentUrl } from '../util/CLMLocation';

export const QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_REQUESTED =
  'QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_REQUESTED';
export const QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_FULFILLED =
  'QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_FULFILLED';
export const QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_FAILED = 'QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_FAILED';

const loadComponentRequested = noPayloadActionCreator(QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_REQUESTED);
const loadComponentFulfilled = payloadParamActionCreator(QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_FULFILLED);
const loadComponentFailed = payloadParamActionCreator(QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_FAILED);

export function loadComponent(token) {
  return function (dispatch) {
    dispatch(loadComponentRequested());
    return axios
      .get(getQuarantinedComponentUrl(token))
      .then(({ data }) => {
        dispatch(loadComponentFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadComponentFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}
