/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import reducer, { actions } from 'MainRoot/report/react/reportsSlice';
import { getApplicationSummariesUrl } from 'MainRoot/util/CLMLocation';

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
    const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
      applicationSummariesUrlPayload = [
        {
          id: '2e340b54c696423f8e228423f6a9d5b9',
          publicId: 'a-org-app',
          name: 'a-org-app',
          organizationId: '93ade64c3e0d4549b326fc5264fd2d65',
          organizationName: 'a-org',
        },
        {
          id: '7f3726e9cc9c4137893b4910ef1380fe',
          publicId: 'apptest1',
          name: 'apptest',
          organizationId: '373a0f41024f4c5ebe93a20464599c4f',
          organizationName: 'org-test very long name to an org in this world but it happens',
        },
        {
          id: '31db96d8cc624113a756aa02f3ff8ed4',
          publicId: 'apptestb',
          name: 'apptestB',
          organizationId: '053de74d5513477094250cf7143ea453',
          organizationName: 'my-org',
        },
        {
          id: '5bee9f16c0ca4f87af587f2adb039548',
          publicId: 'b-org-app',
          name: 'b-org-app',
          organizationId: 'e6542935d99a418daaf0a75dfc436d8c',
          organizationName: 'b-org',
        },
      ];

    let store;
    let state = Object.freeze({
      reports: {
        appFilter: filterValue,
        applicationsInformationList: [],
        pages: 1,
      },
    });

    beforeEach(() => {
      store = SpecUtil.mockReduxStore(state);
    });

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

    // these tests are going to be optimesed in CLM-21521
    it('dispatches a reports/setFilter and reports/loadReports/fulfilled actions', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicationSummariesUrl(filterValue, 'APP_NAME_ASC', 1, 50)]: Promise.resolve({
            data: applicationSummariesUrlPayload,
          }),
        },
      });

      store.dispatch(actions.filterReports(filterValue));

      expect(store.getActions().length).toBe(1);

      setTimeout(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(3);
        expect(actions[0].type).toBe('reports/setFilter');
        expect(actions[0].payload).toBe(filterValue);
        expect(actions[1].type).toBe('reports/loadReports/pending');
        expect(actions[1].payload).toBeUndefined();
        expect(actions[2].type).toBe('reports/loadReports/fulfilled');
        expect(actions[2].payload).not.toBeUndefined();
        done();
      }, 1000);
    });

    // these tests are going to be optimesed in CLM-21521
    it('dispatches a reports/setFilter and reports/loadReports/rejected actions', (done) => {
      const error = 'error';
      mockAxiosCalls({
        get: {
          [getApplicationSummariesUrl(filterValue, 'APP_NAME_ASC', 1, 50)]: () => Promise.reject({ error }),
        },
      });

      store.dispatch(actions.filterReports(filterValue));

      expect(store.getActions().length).toBe(1);

      setTimeout(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(3);
        expect(actions[0].type).toBe('reports/setFilter');
        expect(actions[0].payload).toBe(filterValue);
        expect(actions[1].type).toBe('reports/loadReports/pending');
        expect(actions[1].payload).toBeUndefined();
        expect(actions[2].type).toBe('reports/loadReports/rejected');
        expect(actions[2].payload).not.toBeUndefined();
        done();
      }, 1000);
    });
  });
});
