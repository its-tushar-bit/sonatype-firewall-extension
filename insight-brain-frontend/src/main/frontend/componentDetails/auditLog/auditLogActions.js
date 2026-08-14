/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { getHrcReportAuditLogUrl, getReportAuditLogUrl } from '../../util/CLMLocation';
import { httpErrorMessageActionCreator, noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { sortItemsByFields } from '../../util/sortUtils';
import { selectSelectedComponent } from '../../applicationReport/applicationReportSelectors';
import { unwrapReportEntry } from '../../applicationReport/reportEntryUtils';

export const AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED = 'AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED';
export const AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED = 'AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED';
export const AUDIT_LOG_LOAD_AUDIT_LOG_FAILED = 'AUDIT_LOG_LOAD_AUDIT_LOG_FAILED';
export const AUDIT_LOG_SORT_AUDIT_LOG_REQUESTED = 'AUDIT_LOG_SORT_AUDIT_LOG_REQUESTED';
export const AUDIT_LOG_SORT_AUDIT_LOG_FULFILLED = 'AUDIT_LOG_SORT_AUDIT_LOG_FULFILLED';

const loadAuditLogRequested = noPayloadActionCreator(AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED);
const loadAuditLogFulfilled = payloadParamActionCreator(AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED);
const loadAuditLogFailed = httpErrorMessageActionCreator(AUDIT_LOG_LOAD_AUDIT_LOG_FAILED);

export function loadAuditLogForComponent() {
  return (dispatch, getState) => {
    dispatch(loadAuditLogRequested());
    const state = getState();
    const selectedComponent = selectSelectedComponent(state);

    const {
      router: {
        currentParams: { publicId, hrcId, scanId },
      },
    } = state;
    const url = hrcId
      ? getHrcReportAuditLogUrl(hrcId, scanId, selectedComponent)
      : getReportAuditLogUrl(publicId, scanId, selectedComponent);

    return axios
      .get(url)
      .then(({ data }) => {
        // HRC audit log endpoint returns the ReportEntry wrapper {name, time, buf};
        // unwrap it so downstream code sees the same shape as the application response.
        const unwrapped = hrcId ? unwrapReportEntry(data) : data;
        const response = (unwrapped && unwrapped.aaData) || [];
        dispatch(loadAuditLogFulfilled(response));
        if (response && response.length) {
          dispatch(sortAuditLog());
        }
      })
      .catch((error) => {
        dispatch(loadAuditLogFailed(error));
      });
  };
}

const sortAuditLogRequested = payloadParamActionCreator(AUDIT_LOG_SORT_AUDIT_LOG_REQUESTED);
const sortAuditLogFulfilled = payloadParamActionCreator(AUDIT_LOG_SORT_AUDIT_LOG_FULFILLED);

export function sortAuditLog(sortField = '-time') {
  return (dispatch, getState) => {
    dispatch(sortAuditLogRequested(sortField));
    const {
      auditLog: { auditRecords },
    } = getState();
    const sortedResults = sortItemsByFields([sortField], auditRecords);
    dispatch(sortAuditLogFulfilled(sortedResults));
  };
}
