/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  REACT_APP_REPORT_LOAD_METADATA_FAILED,
  REACT_APP_REPORT_LOAD_METADATA_FULFILLED,
  REACT_APP_REPORT_LOAD_METADATA_REQUESTED,
  loadReportMetadata
} from '../../../../main/frontend/applicationReport/react/reportActions';

describe('reportActions', () => {
  let store, mockState;

  beforeEach(() => {
    store = SpecUtil.mockReduxStore(mockState);
  });

  describe('loadReportMetadata', () => {
    it('handles http request failure', (done) => {
      const error = {};
      spyOn(axios, 'get').and.returnValue(Promise.reject(error));

      store.dispatch(loadReportMetadata('invalidAppId', 'invalidScanId')).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[1].type).toEqual(REACT_APP_REPORT_LOAD_METADATA_FAILED);
        expect(store.getActions()[1].payload).toBe(error);
        done();
      });

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: REACT_APP_REPORT_LOAD_METADATA_REQUESTED
      });
    });

    describe('when app Id and scan Id are provided', () => {
      it('requests the metadata details and loads them as details', (done) => {
        const expectedURL = '/rest/report/appId/scanId/metadata',
            expectedJSON = {
              reportTitle: 'Title',
              application: {
                id: 'id',
                publicId: 'publicId'
              }
            };

        spyOn(axios, 'get').and.returnValue(Promise.resolve({
          status: 200,
          data: expectedJSON
        }));

        store.dispatch(loadReportMetadata('appId', 'scanId')).then(() => {
          expect(axios.get).toHaveBeenCalledWith(expectedURL);
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1]).toEqual({
            type: REACT_APP_REPORT_LOAD_METADATA_FULFILLED,
            payload: expectedJSON
          });
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0]).toEqual({
          type: REACT_APP_REPORT_LOAD_METADATA_REQUESTED
        });
      });
    });

  });

});
