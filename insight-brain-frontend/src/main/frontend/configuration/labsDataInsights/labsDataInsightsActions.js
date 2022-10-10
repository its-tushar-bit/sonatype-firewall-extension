/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';

export const LABS_DATA_INSIGHTS_REQUESTED = 'LABS_DATA_INSIGHTS_REQUESTED';
export const LABS_DATA_INSIGHTS_FULFILLED = 'LABS_DATA_INSIGHTS_FULFILLED';
export const LABS_DATA_INSIGHTS_FAILED = 'LABS_DATA_INSIGHTS_FAILED';

export function loadLabsDataInsights() {
  return function (dispatch) {
    dispatch(loadLabsDataInsightsRequested());
    const script = document.createElement('script');
    const currentUrl = window.location.href;
    const basePath = currentUrl.substring(0, currentUrl.indexOf('/assets/'));
    window.labsBasePath = basePath; // this value is used by data insights
    script.src = basePath + '/rest/labs?command=getJS&values=bootstrapLab.js';
    script.async = true;
    script.addEventListener('load', success, false);
    script.addEventListener('error', error, false);
    function success() {
      dispatch(loadLabsDataInsightsFulfilled());
    }
    function error() {
      dispatch(loadLabsDataInsightsFailed('Failed to load data insights. Please try again.'));
    }
    document.body.appendChild(script);
  };
}

const loadLabsDataInsightsRequested = noPayloadActionCreator(LABS_DATA_INSIGHTS_REQUESTED);
const loadLabsDataInsightsFulfilled = noPayloadActionCreator(LABS_DATA_INSIGHTS_FULFILLED);
const loadLabsDataInsightsFailed = payloadParamActionCreator(LABS_DATA_INSIGHTS_FAILED);
