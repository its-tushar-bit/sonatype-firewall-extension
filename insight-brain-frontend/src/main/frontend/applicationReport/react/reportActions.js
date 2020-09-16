/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { getReportMetadataUrl } from '../../util/CLMLocation';
import axios from 'axios';

export const REACT_APP_REPORT_LOAD_METADATA_REQUESTED = 'REACT_APP_REPORT_LOAD_METADATA_REQUESTED';
export const REACT_APP_REPORT_LOAD_METADATA_FULFILLED = 'REACT_APP_REPORT_LOAD_METADATA_FULFILLED';
export const REACT_APP_REPORT_LOAD_METADATA_FAILED = 'REACT_APP_REPORT_LOAD_METADATA_FAILED';

const loadMetadataRequested = noPayloadActionCreator(REACT_APP_REPORT_LOAD_METADATA_REQUESTED);
const loadFulfilledMetadata = payloadParamActionCreator(REACT_APP_REPORT_LOAD_METADATA_FULFILLED);
const loadMetadataFailed = payloadParamActionCreator(REACT_APP_REPORT_LOAD_METADATA_FAILED);

export function loadReportMetadata(appId, scanId) {
  return (dispatch) => {
    if (!appId) {
      return dispatch(loadMetadataFailed('Missing app id.'));
    }
    if (!scanId) {
      return dispatch(loadMetadataFailed('Missing scan id.'));
    }
    dispatch(loadMetadataRequested());

    const url = getReportMetadataUrl(appId, scanId);

    return axios.get(url)
        .then(results => {
          dispatch(loadFulfilledMetadata(results.data));
        })
        .catch(error => {
          dispatch(loadMetadataFailed(error));
        });
  };
}
