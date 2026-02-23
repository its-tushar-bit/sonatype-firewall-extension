/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  successMetricsListReducer as reducer,
  load,
  initialState,
  SUCCESS_METRICS_DISABLED_MESSAGE,
} from 'MainRoot/labs/successMetrics/successMetricsReportListSlice';
import { getSuccessMetricsConfigUrl, getSuccessMetricsReportsUrl } from '../../../../main/frontend/util/CLMLocation';

import 'TestRoot/SpecUtil';

describe('successMetricsReportListState', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  describe('action', () => {
    const reports = [
      { id: '101', name: 'test 101' },
      { id: '202', name: 'test 202' },
      { id: '303', name: 'test 303' },
    ];
    describe('load', () => {
      it('when successful, dispatch pending and fulfilled actions', (done) => {
        mockAxiosCalls({
          get: {
            [getSuccessMetricsConfigUrl()]: Promise.resolve({ data: { enabled: true } }),
            [getSuccessMetricsReportsUrl()]: Promise.resolve({ data: reports }),
          },
        });

        const store = SpecUtil.mockReduxStore();

        store.dispatch(load()).then(() => {
          const actions = store.getActions();
          expect(actions[0].type).toBe('successMetricsList/load/pending');
          expect(actions[1].type).toBe('successMetricsList/load/fulfilled');
          expect(actions[1].payload).toBe(reports);

          done();
        });
      });
      it('when success metrics config is disabled, dispatch pending and rejected actions', (done) => {
        mockAxiosCalls({
          get: {
            [getSuccessMetricsConfigUrl()]: Promise.resolve({ data: { enabled: false } }),
            [getSuccessMetricsReportsUrl()]: Promise.resolve({ data: reports }),
          },
        });

        const store = SpecUtil.mockReduxStore();

        store.dispatch(load()).then(() => {
          const actions = store.getActions();
          expect(actions[0].type).toBe('successMetricsList/load/pending');
          expect(actions[1].type).toBe('successMetricsList/load/rejected');
          expect(actions[1].payload).toBe(SUCCESS_METRICS_DISABLED_MESSAGE);

          done();
        });
      });
      it('when getSuccessMetricsConfigUrl returns error, dispatch pending and rejected actions', (done) => {
        const errorMsg = 'error message';
        mockAxiosCalls({
          get: {
            [getSuccessMetricsConfigUrl()]: () => Promise.reject(errorMsg),
            [getSuccessMetricsReportsUrl()]: Promise.resolve({ data: reports }),
          },
        });

        const store = SpecUtil.mockReduxStore();

        store.dispatch(load()).then(() => {
          const actions = store.getActions();
          expect(actions[0].type).toBe('successMetricsList/load/pending');
          expect(actions[1].type).toBe('successMetricsList/load/rejected');
          expect(actions[1].payload).toBe(errorMsg);

          done();
        });
      });
      it('when getSuccessMetricsReportsUrl returns error, dispatch pending and rejected actions', (done) => {
        const errorMsg = 'error message';
        mockAxiosCalls({
          get: {
            [getSuccessMetricsConfigUrl()]: Promise.resolve({ data: { enabled: true } }),
            [getSuccessMetricsReportsUrl()]: () => Promise.reject(errorMsg),
          },
        });

        const store = SpecUtil.mockReduxStore();

        store.dispatch(load()).then(() => {
          const actions = store.getActions();
          expect(actions[0].type).toBe('successMetricsList/load/pending');
          expect(actions[1].type).toBe('successMetricsList/load/rejected');
          expect(actions[1].payload).toBe(errorMsg);

          done();
        });
      });
    });
  });
  describe('reducer', () => {
    describe('successMetricsList/load/pending action', () => {
      let newState;
      beforeAll(() => {
        newState = reducer(initialState, { type: 'successMetricsList/load/pending' });
      });
      it('sets loading to true', () => {
        expect(newState.loading).toBe(true);
      });
    });
    describe('successMetricsList/load/rejected action', () => {
      let newState, errorMgs;
      beforeAll(() => {
        errorMgs = 'error message';
        newState = reducer(
          { ...initialState, loading: true },
          { type: 'successMetricsList/load/rejected', payload: errorMgs }
        );
      });
      it('sets loading to false', () => {
        expect(newState.loading).toBe(false);
      });
      it('sets loadError to payload', () => {
        expect(newState.loadError).toBe(errorMgs);
      });
    });
    describe('successMetricsList/load/fulfilled action', () => {
      let newState, reports;
      beforeAll(() => {
        reports = [
          { id: '101', name: 'test 101' },
          { id: '202', name: 'test 202' },
          { id: '303', name: 'test 303' },
        ];
        newState = reducer(
          { ...initialState, loading: true },
          { type: 'successMetricsList/load/fulfilled', payload: reports }
        );
      });
      it('sets loading to false', () => {
        expect(newState.loading).toBe(false);
      });
      it('sets loadError to null', () => {
        expect(newState.loadError).toBe(null);
      });
      it('sets reports to payload', () => {
        expect(newState.reports).toBe(reports);
      });
    });
    describe('successMetricsList/newReport action', () => {
      let newState, initialReports, newReport;
      beforeAll(() => {
        newReport = { id: '404', name: 'test 404' };
        initialReports = [
          { id: '101', name: 'test 101' },
          { id: '202', name: 'test 202' },
          { id: '303', name: 'test 303' },
        ];
        newState = reducer(
          { ...initialState, reports: initialReports },
          { type: 'successMetricsList/newReport', payload: newReport }
        );
      });
      it('add new report to reports', () => {
        expect(newState.reports.length).toBe(4);
        expect(newState.reports[3]).toBe(newReport);
      });
      it('sets isAddModalOpen to false', () => {
        expect(newState.isAddModalOpen).toBe(false);
      });
    });
    describe('successMetricsList/toggleAddModal action', () => {
      let newState;
      beforeAll(() => {
        newState = reducer(initialState, { type: 'successMetricsList/toggleAddModal' });
      });
      it('sets isAddModalOpen to true', () => {
        expect(newState.isAddModalOpen).toBe(true);
      });
    });
  });
});
