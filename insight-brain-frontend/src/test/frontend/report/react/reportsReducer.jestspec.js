/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { actions } from 'MainRoot/report/react/reportsSlice';

import 'TestRoot/SpecUtil';

describe('Reports reducer', () => {
  describe('reports/loadStages/pending', () => {
    it('sets loading and loadError property', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'test',
      });

      const { loading, loadError } = reducer(state, {
        type: 'reports/loadStages/pending',
      });

      expect(loading).toEqual(true);
      expect(loadError).toEqual(null);
    });
  });

  describe('reports/loadStages/fulfilled', () => {
    it('sets stages property', () => {
      const state = Object.freeze({
        stages: [],
      });

      const payload = [{ test: 'test' }];

      const { stages } = reducer(state, {
        type: 'reports/loadStages/fulfilled',
        payload,
      });

      expect(stages).toEqual(payload);
    });
  });

  describe('reports/loadStages/rejected', () => {
    it('sets loading and loadError property', () => {
      const state = Object.freeze({
        loading: false,
        loadError: null,
      });

      const payload = 'Error';

      const { loading, loadError } = reducer(state, {
        type: 'reports/loadStages/rejected',
        payload,
      });

      expect(loading).toEqual(false);
      expect(loadError).toEqual('Error');
    });
  });

  describe('reports/loadContactName/pending', () => {
    it('sets loading and loadError property', () => {
      const publicId = 'CDPAPPGO';

      const state = Object.freeze({
        applicationsInformationList: [
          {
            publicId: publicId,
            contact: {
              error: null,
            },
          },
        ],
        loadingPublicIds: new Set(),
      });

      const { loadingPublicIds } = reducer(state, {
        type: 'reports/loadContactName/pending',
        meta: { arg: publicId },
      });

      expect(loadingPublicIds.has(publicId)).toEqual(true);
    });
  });

  describe('reports/loadContactName/fulfilled', () => {
    it('sets stages property', () => {
      const publicId1 = 'CDPAPPGO';
      const publicId2 = 'TestAppId2';

      const state = Object.freeze({
        applicationsInformationList: [
          {
            publicId: publicId1,
            contact: {
              internalName: 'admin',
              displayName: null,
            },
          },
          {
            publicId: publicId2,
            contact: {
              internalName: 'otheruser',
              displayName: null,
            },
          },
        ],
        loadingPublicIds: new Set(),
      });

      const payload = { appPublicId: publicId1, publicId: publicId1, contact: { displayName: 'test name' } };

      const { applicationsInformationList } = reducer(state, {
        type: 'reports/loadContactName/fulfilled',
        payload,
        meta: { arg: publicId1 },
      });

      expect(applicationsInformationList[0].contact.displayName).toEqual(payload.contact.displayName);
    });
  });

  describe('reports/loadContactName/rejected', () => {
    it('sets loading and loadError property', () => {
      const publicId = 'CDPAPPGO';

      const state = Object.freeze({
        applicationsInformationList: [
          {
            publicId: 'CDPAPPGO',
            contact: {
              error: null,
            },
          },
        ],
        loadingPublicIds: new Set([publicId]),
      });

      const { applicationsInformationList, loadingPublicIds } = reducer(state, {
        type: 'reports/loadContactName/rejected',
        meta: { arg: publicId },
      });

      expect(loadingPublicIds.has(publicId)).toEqual(false);
      expect(applicationsInformationList[0].contact.error).toEqual(true);
    });
  });

  describe('reports/loadStagesAndReports/pending', () => {
    it('sets loading, loadError, applicationsInformationList and pages property', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'test',
        applicationsInformationList: ['test'],
        pages: 99999,
      });

      const { loading, loadError, applicationsInformationList, pages } = reducer(state, {
        type: 'reports/loadStagesAndReports/pending',
      });

      expect(loading).toEqual(true);
      expect(loadError).toEqual(null);
      expect(applicationsInformationList).toEqual([]);
      expect(pages).toEqual(1);
    });
  });

  describe('reports/loadStagesAndReports/fulfilled', () => {
    it('sets loading and loadError property', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'test',
      });

      const { loading, loadError } = reducer(state, {
        type: 'reports/loadStagesAndReports/fulfilled',
      });

      expect(loading).toEqual(false);
      expect(loadError).toEqual(null);
    });
  });

  describe('reports/loadStagesAndReports/rejected', () => {
    it('sets loading and loadError property', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const payload = 'Error';

      const { loading, loadError } = reducer(state, {
        type: 'reports/loadStagesAndReports/rejected',
        payload,
      });

      expect(loading).toEqual(false);
      expect(loadError).toEqual('Error');
    });
  });

  describe('reports/loadReports/pending', () => {
    it('sets loading, loadError and hasMoreResults property', () => {
      const state = Object.freeze({
        loading: false,
        loadError: null,
        hasMoreResults: false,
      });

      const { loading, loadError, hasMoreResults } = reducer(state, {
        type: 'reports/loadReports/pending',
      });

      expect(loading).toEqual(true);
      expect(loadError).toEqual(null);
      expect(hasMoreResults).toEqual(true);
    });
  });

  describe('reports/loadReports/fulfilled', () => {
    it('sets loading, loadError, applicationsInformationList and hasMoreResults property', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'test',
        appliedSort: 'xyz',
        applicationsInformationList: [],
        hasMoreResults: true,
      });

      const payload = [{ test: 'test' }];

      const { loading, loadError, appliedSort, applicationsInformationList, hasMoreResults } = reducer(state, {
        type: 'reports/loadReports/fulfilled',
        payload,
      });

      expect(loading).toEqual(false);
      expect(loadError).toEqual(null);
      expect(appliedSort).toEqual('xyz');
      expect(applicationsInformationList).toEqual(payload);
      expect(hasMoreResults).toEqual(false);
    });

    it('sets hasMoreResults to true when there are more than 50 reports', () => {
      const state = Object.freeze({
        loading: false,
        loadError: null,
        applicationsInformationList: [],
        hasMoreResults: false,
      });

      const payload = [];

      for (let i = 0; i < 50; i++) {
        payload.push({ test: 'test' });
      }

      const { hasMoreResults } = reducer(state, {
        type: 'reports/loadReports/fulfilled',
        payload,
      });

      expect(hasMoreResults).toEqual(true);
    });
  });

  describe('reports/loadReports/rejected', () => {
    it('sets loading and loadError property', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const payload = 'Error';

      const { loading, loadError } = reducer(state, {
        type: 'reports/loadReports/rejected',
        payload,
      });

      expect(loading).toEqual(false);
      expect(loadError).toEqual('Error');
    });
  });

  describe('reports/sortReports', () => {
    const state = {
      appliedSort: null,
      loading: false,
      pages: null,
    };

    it('set appliedSort, loading and pages properties', () => {
      const { appliedSort, loading, pages } = reducer(state, {
        type: 'reports/sortReports',
        payload: 'name',
      });

      expect(appliedSort).toEqual('name');
      expect(pages).toEqual(1);
      expect(loading).toEqual(true);
    });

    it('set pages to 1 when applying sort', () => {
      state.pages = 10;

      const { appliedSort, pages } = reducer(state, {
        type: 'reports/sortReports',
        payload: 'xyz',
      });

      expect(appliedSort).toEqual('xyz');
      expect(pages).toEqual(1);
    });
  });

  describe('filterReports', () => {
    const filterValue = 'test';

    it('set appFilter and pages to 1 when setting filter', () => {
      const state = Object.freeze({
        appFilter: filterValue,
        pages: 10,
      });

      const { appFilter, pages } = reducer(state, {
        type: 'reports/setFilter',
        payload: 'xyz',
      });

      expect(appFilter).toEqual('xyz');
      expect(pages).toEqual(1);
    });

    it('dispatches a loadReports action after a debounced time', (done) => {
      const state = Object.freeze({
        reports: {
          appFilter: filterValue,
          applicationsInformationList: [],
          pages: 1,
        },
      });
      const store = SpecUtil.mockReduxStore(state);
      const loadReportsSpy = jest.spyOn(actions, 'loadReports').mockResolvedValue({});

      store.dispatch(actions.filterReports('filterValue'));
      expect(store.getActions().length).toBe(1);
      expect(loadReportsSpy).not.toHaveBeenCalled();

      setTimeout(() => {
        const storeActions = store.getActions();
        expect(storeActions.length).toBe(2);
        expect(loadReportsSpy).toHaveBeenCalled();
        done();
      }, 600);
    });
  });
});
