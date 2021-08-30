/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { compose } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { stateGo } from '../../../reduxUiRouter/routerActions';
import { Messages } from '../../../util/CommonServices';
import { noPayloadActionCreator, payloadParamActionCreator } from '../../../util/reduxUtil';
import {
  getApplicationsUrl,
  getSuccessMetricsChartDataUrl,
  getSuccessMetricsComponentCountsUrl,
  getSuccessMetricsConfigUrl,
  getSuccessMetricsReportsUrl,
  getSuccessMetricsReportUrl,
} from '../../../util/CLMLocation';
import { checkPermissions } from '../../../util/authorizationUtil';

export const SUCCESS_METRICS_REPORT_LOAD_REQUESTED = 'SUCCESS_METRICS_REPORT_LOAD_REQUESTED';
export const SUCCESS_METRICS_REPORT_LOAD_FAILED = 'SUCCESS_METRICS_REPORT_LOAD_FAILED';
export const SUCCESS_METRICS_REPORT_LOAD_FULFILLED = 'SUCCESS_METRICS_REPORT_LOAD_FULFILLED';

export const SUCCESS_METRICS_REPORT_DELETE_REQUESTED = 'SUCCESS_METRICS_REPORT_DELETE_REQUESTED';
export const SUCCESS_METRICS_REPORT_DELETE_FULFILLED = 'SUCCESS_METRICS_REPORT_DELETE_FULFILLED';
export const SUCCESS_METRICS_REPORT_DELETE_FAILED = 'SUCCESS_METRICS_REPORT_DELETE_FAILED';
export const SUCCESS_METRICS_DELETE_MASK_TIMER_DONE = 'SUCCESS_METRICS_DELETE_MASK_TIMER_DONE';

//LOAD
const loadRequested = noPayloadActionCreator(SUCCESS_METRICS_REPORT_LOAD_REQUESTED);
const loadFulfilled = payloadParamActionCreator(SUCCESS_METRICS_REPORT_LOAD_FULFILLED);
const loadFailed = payloadParamActionCreator(SUCCESS_METRICS_REPORT_LOAD_FAILED);

// DELETE
const deleteRequested = noPayloadActionCreator(SUCCESS_METRICS_REPORT_DELETE_REQUESTED);
const deleteFulfilled = noPayloadActionCreator(SUCCESS_METRICS_REPORT_DELETE_FULFILLED);
const deleteFailed = payloadParamActionCreator(SUCCESS_METRICS_REPORT_DELETE_FAILED);
const deleteMaskTimerDone = noPayloadActionCreator(SUCCESS_METRICS_DELETE_MASK_TIMER_DONE);

export const SUCCESS_METRICS_DISABLED_MESSAGE = 'Success metrics have been disabled by your system administrator.';

export const load = (successMetricsReportId) => {
  return (dispatch) => {
    dispatch(loadRequested());

    const loadChartData = () => {
      const dataChartRequest = axios.get(getSuccessMetricsChartDataUrl(successMetricsReportId));
      const userReportsRequest = axios.get(getSuccessMetricsReportsUrl());
      const componentCountsRequest = axios.get(getSuccessMetricsComponentCountsUrl(successMetricsReportId));
      return Promise.all([dataChartRequest, userReportsRequest, componentCountsRequest])
        .then(([{ data: chartData }, { data: reports }, { data: componentCounts }]) => {
          const report = reports.find((rep) => rep.id === successMetricsReportId);
          const reportName = report.name;
          const includeLatestData = report.includeLatestData;

          const isSingleApplicationReport =
            !!(report && report.scope.applicationIds && report.scope.applicationIds.length === 1) &&
            (!report.scope.organizationIds || report.scope.organizationIds.length === 0);

          if (isSingleApplicationReport)
            return axios.get(getApplicationsUrl()).then(({ data }) => {
              const application = data.find((app) => app.id === report.scope.applicationIds[0]);

              const singleApplicationName = application ? application.name : null;

              return dispatch(
                loadFulfilled({
                  ...chartData,
                  reportName,
                  isSingleApplicationReport,
                  includeLatestData,
                  singleApplicationName,
                  componentCounts,
                })
              );
            });

          return dispatch(
            loadFulfilled({
              ...chartData,
              reportName,
              isSingleApplicationReport,
              includeLatestData,
              componentCounts,
            })
          );
        })
        .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
    };
    return checkPermissions(['CONFIGURE_SYSTEM'])
      .then(() =>
        axios
          .get(getSuccessMetricsConfigUrl())
          .then(({ data }) => {
            if (data.enabled) return loadChartData();
            else return dispatch(loadFailed(SUCCESS_METRICS_DISABLED_MESSAGE));
          })
          .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage))
      )
      .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
  };
};

const startDeleteMaskSuccessTimer = (dispatch) => {
  setTimeout(() => {
    dispatch(deleteMaskTimerDone());
    dispatch(stateGo('labs.successMetrics'));
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
};

export const deleteReport = (reportId) => {
  return (dispatch) => {
    dispatch(deleteRequested());
    return axios
      .delete(getSuccessMetricsReportUrl(reportId))
      .then(() => {
        dispatch(deleteFulfilled());
        startDeleteMaskSuccessTimer(dispatch);
      })
      .catch(compose(dispatch, deleteFailed, Messages.getHttpErrorMessage));
  };
};
