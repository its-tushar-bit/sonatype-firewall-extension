/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import {
  LEGAL_APPLICATION_DETAILS_APPLY_FILTERS,
  LEGAL_APPLICATION_DETAILS_LOAD_DATA_FAILED,
  LEGAL_APPLICATION_DETAILS_LOAD_DATA_FULFILLED,
  LEGAL_APPLICATION_DETAILS_LOAD_DATA_REQUESTED,
  fetchLegalApplicationDetailsData,
} from '../../../../main/frontend/legal/application/legalApplicationDetailsActions';
import {
  getApplicationLegalReviewerUrl,
  getActionStageUrl,
  getLegalDashboardApplicationUrl,
} from '../../../../main/frontend/util/CLMLocation';

import 'TestRoot/SpecUtil';

describe('legalApplicationDetailsActions', function () {
  describe('fetchLegalApplicationDetailsData', function () {
    const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

    const initialState = {
      loading: false,
      error: null,
      applicationName: null,
      stageName: null,
      components: {
        results: [],
        error: null,
        loading: false,
      },
    };

    it('loads data for the application, stage type and components', function (done) {
      const store = SpecUtil.mockReduxStore(initialState);
      const applicationPublicId = 'app-id';
      const stageTypeId = 'stage-type-id';
      const stageTypes = [
        {
          stageTypeId: stageTypeId,
          stageName: 'Stage Type',
        },
      ];

      mockAxiosCalls({
        get: {
          [getApplicationLegalReviewerUrl(applicationPublicId)]: Promise.resolve({
            data: 'result application',
          }),
          [getActionStageUrl()]: Promise.resolve({ data: stageTypes }),
        },
        post: {
          [getLegalDashboardApplicationUrl(applicationPublicId)]: Promise.resolve({ data: 'result components' }),
        },
      });

      store.dispatch(fetchLegalApplicationDetailsData(applicationPublicId, stageTypeId)).then(() => {
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[0]).toEqual({
          type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_REQUESTED,
        });
        expect(store.getActions()[1]).toEqual({
          type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_FULFILLED,
          payload: {
            application: 'result application',
            stageName: 'Stage Type',
            components: 'result components',
          },
        });
        expect(store.getActions()[2]).toEqual({ type: LEGAL_APPLICATION_DETAILS_APPLY_FILTERS });
        done();
      });

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_REQUESTED,
      });
    });

    it('handles failure to load data for the application', function (done) {
      const store = SpecUtil.mockReduxStore(initialState);
      const applicationPublicId = 'app-id';
      const stageTypeId = 'stage-type-id';

      mockAxiosCalls({
        get: {
          [getApplicationLegalReviewerUrl(applicationPublicId)]: () => Promise.reject('error application'),
        },
      });

      store.dispatch(fetchLegalApplicationDetailsData(applicationPublicId, stageTypeId)).catch(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[0]).toEqual({
          type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_REQUESTED,
        });
        expect(store.getActions()[1]).toEqual({
          type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_FAILED,
          payload: 'error application',
        });
        done();
      });
    });

    it('handles failure due to stage type id missing', function (done) {
      const store = SpecUtil.mockReduxStore(initialState);
      const applicationPublicId = 'app-id';

      store.dispatch(fetchLegalApplicationDetailsData(applicationPublicId)).catch(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[0]).toEqual({
          type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_REQUESTED,
        });
        expect(store.getActions()[1]).toEqual({
          type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_FAILED,
          payload: 'stageTypeId is mandatory.',
        });
        done();
      });
    });

    it('handles failure due to stage type id is not a valid', function (done) {
      const store = SpecUtil.mockReduxStore(initialState);
      const applicationPublicId = 'app-id';
      const stageTypes = [
        {
          stageTypeId: 'stage-type-id',
          stageName: 'Stage Type',
        },
      ];

      mockAxiosCalls({
        get: {
          [getApplicationLegalReviewerUrl(applicationPublicId)]: Promise.resolve({
            data: 'result application',
          }),
          [getActionStageUrl()]: Promise.resolve({ data: stageTypes }),
        },
        post: {
          [getLegalDashboardApplicationUrl(applicationPublicId)]: Promise.resolve({ data: 'result components' }),
        },
      });

      store.dispatch(fetchLegalApplicationDetailsData(applicationPublicId, 'stage-type-id-no-valid')).catch(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[0]).toEqual({
          type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_REQUESTED,
        });
        expect(store.getActions()[1]).toEqual({
          type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_FAILED,
          payload: 'stage-type-id-no-valid is not a valid stage type ID.',
        });
        done();
      });
    });

    it('handles failure to load data for the stage type', function (done) {
      const store = SpecUtil.mockReduxStore(initialState);
      const applicationPublicId = 'app-id';
      const stageTypeId = 'stage-type-id';

      mockAxiosCalls({
        get: {
          [getApplicationLegalReviewerUrl(applicationPublicId)]: Promise.resolve({
            data: 'result application',
          }),
          [getActionStageUrl()]: () => Promise.reject('error stage type'),
        },
      });

      store.dispatch(fetchLegalApplicationDetailsData(applicationPublicId, stageTypeId)).catch(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[0]).toEqual({
          type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_REQUESTED,
        });
        expect(store.getActions()[1]).toEqual({
          type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_FAILED,
          payload: 'error stage type',
        });
        done();
      });
    });

    it('handles failure to load data for components', function (done) {
      const store = SpecUtil.mockReduxStore(initialState);
      const applicationPublicId = 'app-id';
      const stageTypeId = 'stage-type-id';
      const stageTypes = [
        {
          stageTypeId: stageTypeId,
          stageName: 'Stage Type',
        },
      ];

      mockAxiosCalls({
        get: {
          [getApplicationLegalReviewerUrl(applicationPublicId)]: Promise.resolve({
            data: 'result application',
          }),
          [getActionStageUrl()]: Promise.resolve({ data: stageTypes }),
        },
        post: {
          [getLegalDashboardApplicationUrl(applicationPublicId)]: () => Promise.reject('error components'),
        },
      });

      store.dispatch(fetchLegalApplicationDetailsData(applicationPublicId, stageTypeId)).catch(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[0]).toEqual({
          type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_REQUESTED,
        });
        expect(store.getActions()[1]).toEqual({
          type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_FAILED,
          payload: 'error components',
        });
        done();
      });
    });
  });
});
