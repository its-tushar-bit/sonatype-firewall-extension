/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { getReportAuditLogUrl } from '../../../../main/frontend/util/CLMLocation';
import {
  AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED,
  AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED,
  AUDIT_LOG_LOAD_AUDIT_LOG_FAILED,
  loadAuditLogForComponent,
} from '../../../../main/frontend/componentDetails/auditLog/auditLogActions';

describe('auditLogActions', function () {
  let store, mockAxiosCalls, url, mockAppId, mockReportId, mockSelectedComponent, mockResponse;

  beforeEach(function () {
    mockAppId = 'appId';
    mockReportId = 'reportId';
    mockSelectedComponent = { derivedComponentName: 'componentName' };

    const state = {
      router: {
        currentParams: {
          publicId: mockAppId,
          scanId: mockReportId,
        },
      },
      auditLog: {
        isLoading: false,
        auditRecords: [],
        error: null,
      },
      applicationReport: {
        selectedComponent: mockSelectedComponent,
      },
    };
    store = SpecUtil.mockReduxStore(state);
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
    url = getReportAuditLogUrl(mockAppId, mockReportId, mockSelectedComponent);

    mockResponse = {
      data: {
        aaData: [
          {
            hash: 'hash1',
          },
          {
            hash: 'hash2',
          },
        ],
      },
    };
  });

  describe('loadAuditLogForComponent', function () {
    it('immediately dispatches AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED action', () => {
      store.dispatch(loadAuditLogForComponent());

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED);
    });

    it('sends a GET request to the appropriate url', () => {
      spyOn(axios, 'get').and.returnValue(Promise.resolve());
      store.dispatch(loadAuditLogForComponent());

      expect(axios.get).toHaveBeenCalledWith(url);
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toBe(AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED);
    });

    it('dispatches AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED after a succesfull response', (done) => {
      mockAxiosCalls({
        get: {
          [url]: Promise.resolve(mockResponse),
        },
      });
      store.dispatch(loadAuditLogForComponent()).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[1]).toEqual({
          type: AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED,
          payload: mockResponse.data.aaData,
        });
        done();
      });
    });

    it('dispatches AUDIT_LOG_LOAD_AUDIT_LOG_FAILED after a failed reponse', (done) => {
      mockAxiosCalls({
        get: {
          [url]: Promise.reject('error'),
        },
      });
      store.dispatch(loadAuditLogForComponent()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(store.getActions()[1]).toEqual({
          type: AUDIT_LOG_LOAD_AUDIT_LOG_FAILED,
          payload: 'error',
        });
        done();
      });
    });
  });
});
