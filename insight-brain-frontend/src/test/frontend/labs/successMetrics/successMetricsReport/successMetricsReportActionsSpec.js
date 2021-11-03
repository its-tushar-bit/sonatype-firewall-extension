/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import {
  getSuccessMetricsChartDataUrl,
  getSuccessMetricsComponentCountsUrl,
  getSuccessMetricsConfigUrl,
  getSuccessMetricsReportsUrl,
  getSuccessMetricsReportUrl,
} from 'MainRoot/util/CLMLocation';
import {
  SUCCESS_METRICS_REPORT_LOAD_REQUESTED,
  SUCCESS_METRICS_REPORT_LOAD_FAILED,
  SUCCESS_METRICS_REPORT_LOAD_FULFILLED,
  SUCCESS_METRICS_DISABLED_MESSAGE,
  SUCCESS_METRICS_REPORT_DELETE_REQUESTED,
  SUCCESS_METRICS_REPORT_DELETE_FULFILLED,
  SUCCESS_METRICS_REPORT_DELETE_FAILED,
  SUCCESS_METRICS_DELETE_MASK_TIMER_DONE,
  load,
  deleteReport,
} from 'MainRoot/labs/successMetrics/successMetricsReport/SuccessMetricsReportActions';
import { STATE_GO } from 'MainRoot/reduxUiRouter/routerActions';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

describe('successMetricsReportActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  describe('load', () => {
    describe('when success metrics is disabled', () => {
      it('dispatch SUCCESS_METRICS_REPORT_LOAD_REQUESTED and SUCCESS_METRICS_REPORT_LOAD_FAILED ', (done) => {
        mockAxiosCalls({
          get: {
            [getSuccessMetricsConfigUrl()]: Promise.resolve({ data: { enabled: false } }),
          },
        });

        const store = SpecUtil.mockReduxStore();
        store.dispatch(load('101')).then(() => {
          const actions = store.getActions();
          expect(actions).toHaveActionsInOrder([
            { type: SUCCESS_METRICS_REPORT_LOAD_REQUESTED },
            { type: SUCCESS_METRICS_REPORT_LOAD_FAILED, payload: SUCCESS_METRICS_DISABLED_MESSAGE },
          ]);
          done();
        });
      });
    });

    describe('when success metrics is enabled', () => {
      it('dispatch SUCCESS_METRICS_REPORT_LOAD_REQUESTED and SUCCESS_METRICS_REPORT_LOAD_FULFILLED ', (done) => {
        const reportId = '101';
        const includeLatestData = true;
        const reportName = 'test101';
        const chartData = { testProp: true };
        const reports = [
          { id: reportId, name: reportName, includeLatestData, scope: {} },
          { id: '202', name: 'test202', includeLatestData: false },
        ];
        const componentCounts = {};

        mockAxiosCalls({
          get: {
            [getSuccessMetricsConfigUrl()]: Promise.resolve({ data: { enabled: true } }),
            [getSuccessMetricsChartDataUrl(reportId)]: Promise.resolve({ data: chartData }),
            [getSuccessMetricsReportsUrl()]: Promise.resolve({ data: reports }),
            [getSuccessMetricsComponentCountsUrl(reportId)]: Promise.resolve({ data: componentCounts }),
          },
        });

        const store = SpecUtil.mockReduxStore();
        store.dispatch(load(reportId)).then(() => {
          const actions = store.getActions();
          expect(actions[0]).toEqual({ type: SUCCESS_METRICS_REPORT_LOAD_REQUESTED });
          expect(actions[1]).toEqual({
            type: SUCCESS_METRICS_REPORT_LOAD_FULFILLED,
            payload: {
              ...chartData,
              componentCounts,
              reportName,
              isSingleApplicationReport: false,
              includeLatestData,
            },
          });
          done();
        });
      });
    });

    describe('fires SUCCESS_METRICS_REPORT_LOAD_FAILED', () => {
      const errorMessage = 'fetch failed';
      const reportId = '101';
      let store;
      beforeEach(() => {
        store = SpecUtil.mockReduxStore();
      });

      it('when getSuccessMetricsConfigUrl fetch fails', (done) => {
        mockAxiosCalls({
          get: {
            [getSuccessMetricsConfigUrl()]: () => Promise.reject(errorMessage),
            [getSuccessMetricsChartDataUrl(reportId)]: Promise.resolve(),
            [getSuccessMetricsReportsUrl()]: Promise.resolve(),
            [getSuccessMetricsComponentCountsUrl(reportId)]: Promise.resolve(),
          },
        });

        store.dispatch(load('101')).then(() => {
          const actions = store.getActions();
          expect(actions).toHaveActionsInOrder([
            { type: SUCCESS_METRICS_REPORT_LOAD_REQUESTED },
            { type: SUCCESS_METRICS_REPORT_LOAD_FAILED, payload: errorMessage },
          ]);
          done();
        });
      });

      it('when getSuccessMetricsChartDataUrl fetch fails', (done) => {
        mockAxiosCalls({
          get: {
            [getSuccessMetricsConfigUrl()]: Promise.resolve({ data: { enabled: true } }),
            [getSuccessMetricsChartDataUrl(reportId)]: () => Promise.reject(errorMessage),
            [getSuccessMetricsReportsUrl()]: Promise.resolve(),
            [getSuccessMetricsComponentCountsUrl(reportId)]: Promise.resolve(),
          },
        });

        store.dispatch(load('101')).then(() => {
          const actions = store.getActions();
          expect(actions).toHaveActionsInOrder([
            { type: SUCCESS_METRICS_REPORT_LOAD_REQUESTED },
            { type: SUCCESS_METRICS_REPORT_LOAD_FAILED, payload: errorMessage },
          ]);
          done();
        });
      });

      it('when getSuccessMetricsReportsUrl fetch fails', (done) => {
        mockAxiosCalls({
          get: {
            [getSuccessMetricsConfigUrl()]: Promise.resolve({ data: { enabled: true } }),
            [getSuccessMetricsChartDataUrl(reportId)]: Promise.resolve(),
            [getSuccessMetricsReportsUrl()]: () => Promise.reject(errorMessage),
            [getSuccessMetricsComponentCountsUrl(reportId)]: Promise.resolve(),
          },
        });

        store.dispatch(load('101')).then(() => {
          const actions = store.getActions();
          expect(actions).toHaveActionsInOrder([
            { type: SUCCESS_METRICS_REPORT_LOAD_REQUESTED },
            { type: SUCCESS_METRICS_REPORT_LOAD_FAILED, payload: errorMessage },
          ]);
          done();
        });
      });

      it('when getSuccessMetricsComponentCountsUrl fetch fails', (done) => {
        mockAxiosCalls({
          get: {
            [getSuccessMetricsConfigUrl()]: Promise.resolve({ data: { enabled: true } }),
            [getSuccessMetricsChartDataUrl(reportId)]: Promise.resolve(),
            [getSuccessMetricsReportsUrl()]: Promise.resolve(),
            [getSuccessMetricsComponentCountsUrl(reportId)]: () => Promise.reject(errorMessage),
          },
        });

        store.dispatch(load('101')).then(() => {
          const actions = store.getActions();
          expect(actions).toHaveActionsInOrder([
            { type: SUCCESS_METRICS_REPORT_LOAD_REQUESTED },
            { type: SUCCESS_METRICS_REPORT_LOAD_FAILED, payload: errorMessage },
          ]);
          done();
        });
      });
    });
  });

  describe('deleteReport', () => {
    let store;
    beforeEach(() => {
      store = SpecUtil.mockReduxStore();
    });

    it('fires SUCCESS_METRICS_DELETE_REQUESTED, SUCCESS_METRICS_DELETE_FULFILLED, SUCCESS_METRICS_DELETE_MASK_TIMER_DONE and STATE_GO actions on success', (done) => {
      mockAxiosCalls({
        del: {
          [getSuccessMetricsReportUrl('101')]: Promise.resolve({ data: 'success' }),
        },
      });

      store.dispatch(deleteReport('101')).then(() => {
        setTimeout(function () {
          expect(store.getActions()).toHaveActionsInOrder([
            { type: SUCCESS_METRICS_REPORT_DELETE_REQUESTED },
            { type: SUCCESS_METRICS_REPORT_DELETE_FULFILLED },
            { type: SUCCESS_METRICS_DELETE_MASK_TIMER_DONE },
            {
              type: STATE_GO,
              payload: {
                to: 'labs.successMetrics',
                params: undefined,
                options: undefined,
              },
            },
          ]);
          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });
    });

    it('fires SUCCESS_METRICS_DELETE_FAILED action on error', (done) => {
      mockAxiosCalls({
        del: {
          [getSuccessMetricsReportUrl('404')]: () => Promise.reject({ response: 'failed to delete report' }),
        },
      });

      store.dispatch(deleteReport('404')).then(() => {
        expect(store.getActions()).toHaveActionsInOrder([
          { type: SUCCESS_METRICS_REPORT_DELETE_REQUESTED },
          {
            type: SUCCESS_METRICS_REPORT_DELETE_FAILED,
            payload: 'failed to delete report',
          },
        ]);
        done();
      });
    });
  });
});
