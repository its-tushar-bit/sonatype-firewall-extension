/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import {noPayloadActionCreator, payloadParamActionCreator} from '../util/reduxUtil';
import {getFirewallConfigUrl} from '../util/CLMLocation';

export const FIREWALL_LOAD_STATUS_REQUESTED = 'FIREWALL_LOAD_STATUS_REQUESTED';
export const FIREWALL_LOAD_STATUS_FULFILLED = 'FIREWALL_LOAD_STATUS_FULFILLED';
export const FIREWALL_LOAD_STATUS_FAILED = 'FIREWALL_LOAD_STATUS_FAILED';

const loadStatusRequested = noPayloadActionCreator(FIREWALL_LOAD_STATUS_REQUESTED);
const loadStatusFulfilled = payloadParamActionCreator(FIREWALL_LOAD_STATUS_FULFILLED);
const loadStatusFailed = payloadParamActionCreator(FIREWALL_LOAD_STATUS_FAILED);

export function loadStatus() {
  return function(dispatch) {
    dispatch(loadStatusRequested());
    return axios.get(getFirewallConfigUrl())
        .then(({data}) => {
          dispatch(loadStatusFulfilled(data));
        })
        .catch(error => {
          dispatch(loadStatusFailed(error));
        });
  };
}
