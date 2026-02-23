/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { getReportAuditLogUrl } from '../../../../main/frontend/util/CLMLocation';
import * as auditLogActions from '../../../../main/frontend/componentDetails/auditLog/auditLogActions';
import * as sortUtils from '../../../../main/frontend/util/sortUtils';

import 'TestRoot/SpecUtil';

const {
  AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED,
  AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED,
  AUDIT_LOG_LOAD_AUDIT_LOG_FAILED,
  AUDIT_LOG_SORT_AUDIT_LOG_REQUESTED,
  AUDIT_LOG_SORT_AUDIT_LOG_FULFILLED,
  loadAuditLogForComponent,
  sortAuditLog,
} = auditLogActions;

describe('auditLogActions', function () {
  let store, state, mockAxiosCalls, url, mockAppId, mockReportId, mockComponentHash, mockComponent, mockResponse;

  beforeEach(function () {
    mockAppId = 'appId';
    mockReportId = 'reportId';
    mockComponentHash = 'my-component-hash';
    mockComponent = { name: 'My Component', hash: mockComponentHash };

    state = {
      router: {
        currentParams: {
          publicId: mockAppId,
          scanId: mockReportId,
          hash: mockComponentHash,
        },
      },
      auditLog: {
        isLoading: false,
        auditRecords: [],
        error: null,
        appliedSort: null,
      },
      applicationReport: {
        selectedReport: {
          allEntries: [mockComponent],
          displayedEntries: [mockComponent],
        },
      },
    };
    store = SpecUtil.mockReduxStore(state);
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
    url = getReportAuditLogUrl(mockAppId, mockReportId, mockComponent);

    mockResponse = {
      data: {
        aaData: [
          {
            action: 'Selected',
            comment: 'AAA',
            detail: 'License as LGPL-3.0',
            time: 1622046959734,
            user: 'admin',
          },
          {
            action: 'Acknowledged',
            comment: 'BBB',
            detail: 'License as LGPL-3.0',
            time: 1622046959850,
            user: 'admin',
          },
        ],
      },
    };
  });

  describe('loadAuditLogForComponent', function () {
    it('immediately dispatches AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED action', () => {
      store.dispatch(loadAuditLogForComponent());

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()).toHaveActionType(AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED);
    });

    it('sends a GET request to the appropriate url', () => {
      jest.spyOn(axios, 'get').mockReturnValue(Promise.resolve());
      store.dispatch(loadAuditLogForComponent());

      expect(axios.get).toHaveBeenCalledWith(url);
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()).toHaveActionType(AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED);
    });

    it('dispatches AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED after a succesfull response', (done) => {
      mockAxiosCalls({
        get: {
          [url]: Promise.resolve(mockResponse),
        },
      });
      store.dispatch(loadAuditLogForComponent()).then(() => {
        expect(store.getActions().length).toBe(4);
        expect(store.getActions()).toHaveAction({
          type: AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED,
          payload: mockResponse.data.aaData,
        });
        done();
      });
    });

    it('dispatches AUDIT_LOG_LOAD_AUDIT_LOG_FAILED after a failed reponse', (done) => {
      mockAxiosCalls({
        get: {
          [url]: () => Promise.reject('error'),
        },
      });
      store.dispatch(loadAuditLogForComponent()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(store.getActions()).toHaveAction({
          type: AUDIT_LOG_LOAD_AUDIT_LOG_FAILED,
          payload: 'error',
        });
        done();
      });
    });

    it('sorts if response is not an empty array', (done) => {
      mockAxiosCalls({
        get: {
          [url]: Promise.resolve(mockResponse),
        },
      });
      store.dispatch(loadAuditLogForComponent()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(4);
        expect(actions).toHaveActionsInOrder([
          {
            type: AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED,
            payload: mockResponse.data.aaData,
          },
          {
            type: AUDIT_LOG_SORT_AUDIT_LOG_REQUESTED,
            payload: '-time',
          },
          {
            type: AUDIT_LOG_SORT_AUDIT_LOG_FULFILLED,
            payload: [],
          },
        ]);
        done();
      });
    });

    it('does not calls sortAuditLog if response is an empty array', (done) => {
      mockAxiosCalls({
        get: {
          [url]: Promise.resolve({ data: { aaData: [] } }),
        },
      });
      store.dispatch(loadAuditLogForComponent()).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()).toHaveAction({
          type: AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED,
          payload: [],
        });
        done();
      });
    });
  });

  describe('sortAuditLog', function () {
    beforeEach(() => {
      jest.spyOn(sortUtils, 'sortItemsByFields');
    });

    it('immediately dispatches AUDIT_LOG_SORT_AUDIT_LOG_REQUESTED action', () => {
      store.dispatch(sortAuditLog());

      expect(store.getActions()).toHaveAction({
        type: AUDIT_LOG_SORT_AUDIT_LOG_REQUESTED,
        payload: '-time',
      });
    });

    it('calls sortItemsByField with the auditRecords from state', () => {
      // Set records on store
      const auditRecords = mockResponse.data.aaData;
      const nuState = {
        ...state,
        auditLog: {
          ...state.auditLog,
          auditRecords,
        },
      };
      let store = SpecUtil.mockReduxStore(nuState);

      store.dispatch(sortAuditLog());
      expect(sortUtils.sortItemsByFields).toHaveBeenCalledWith(['-time'], auditRecords);
      expect(store.getActions()).toHaveAction({
        type: AUDIT_LOG_SORT_AUDIT_LOG_FULFILLED,
        payload: sortUtils.sortItemsByFields(['-time'], auditRecords),
      });

      // reset store
      store = SpecUtil.mockReduxStore(nuState);
      store.dispatch(sortAuditLog('-comment'));
      expect(sortUtils.sortItemsByFields).toHaveBeenCalledWith(['-comment'], auditRecords);
      expect(store.getActions()).toHaveAction({
        type: AUDIT_LOG_SORT_AUDIT_LOG_FULFILLED,
        payload: sortUtils.sortItemsByFields(['-comment'], auditRecords),
      });
    });
  });
});
