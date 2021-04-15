/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import {
  LEGAL_APPLICATION_DETAILS_LOAD_APP_FAILED,
  LEGAL_APPLICATION_DETAILS_LOAD_APP_FULFILLED,
  LEGAL_APPLICATION_DETAILS_LOAD_APP_REQUESTED,
  LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FAILED,
  LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FULFILLED,
  LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_REQUESTED,
  LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FAILED,
  LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FULFILLED,
  LEGAL_APPLICATION_DETAILS_LOAD_STAGE_REQUESTED,
  loadApplication,
} from '../../../../main/frontend/legal/application/legalApplicationDetailsActions';
import {
  getApplicationUrl,
  getActionStageUrl,
  getLegalDashboardApplicationUrl,
} from '../../../../main/frontend/util/CLMLocation';

describe('legalApplicationDetailsActions', function () {
  describe('loadApplication', function () {
    const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

    const initialState = {
      application: {
        name: null,
        error: null,
        loading: false,
      },
      stageType: {
        name: null,
        error: null,
        loading: false,
      },
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
          [getApplicationUrl(applicationPublicId)]: Promise.resolve({
            data: 'result application',
          }),
          [getActionStageUrl()]: Promise.resolve({ data: stageTypes }),
        },
        post: {
          [getLegalDashboardApplicationUrl(
            applicationPublicId
          )]: Promise.resolve({ data: 'result components' }),
        },
      });

      store
        .dispatch(loadApplication(applicationPublicId, stageTypeId))
        .then(() => {
          expect(store.getActions().length).toBe(6);
          expect(store.getActions()[1]).toEqual({
            type: LEGAL_APPLICATION_DETAILS_LOAD_APP_FULFILLED,
            payload: 'result application',
          });
          expect(store.getActions()[2]).toEqual({
            type: LEGAL_APPLICATION_DETAILS_LOAD_STAGE_REQUESTED,
          });
          expect(store.getActions()[3]).toEqual({
            type: LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FULFILLED,
            payload: 'Stage Type',
          });
          expect(store.getActions()[4]).toEqual({
            type: LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_REQUESTED,
          });
          expect(store.getActions()[5]).toEqual({
            type: LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FULFILLED,
            payload: 'result components',
          });
          done();
        });

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: LEGAL_APPLICATION_DETAILS_LOAD_APP_REQUESTED,
      });
    });

    it('handles failure to load data for the application', function (done) {
      const store = SpecUtil.mockReduxStore(initialState);
      const applicationPublicId = 'app-id';
      const stageTypeId = 'stage-type-id';

      mockAxiosCalls({
        get: {
          [getApplicationUrl(applicationPublicId)]: Promise.reject(
            'error application'
          ),
        },
      });

      store
        .dispatch(loadApplication(applicationPublicId, stageTypeId))
        .catch(() => {
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1]).toEqual({
            type: LEGAL_APPLICATION_DETAILS_LOAD_APP_FAILED,
            payload: 'error application',
          });
          done();
        });
    });

    it('handles failure due to stage type id missing', function (done) {
      const store = SpecUtil.mockReduxStore(initialState);
      const applicationPublicId = 'app-id';

      mockAxiosCalls({
        get: {
          [getApplicationUrl(applicationPublicId)]: Promise.resolve({
            data: 'result application',
          }),
        },
      });

      store.dispatch(loadApplication(applicationPublicId)).catch(() => {
        expect(store.getActions().length).toBe(4);
        expect(store.getActions()[1]).toEqual({
          type: LEGAL_APPLICATION_DETAILS_LOAD_APP_FULFILLED,
          payload: 'result application',
        });
        expect(store.getActions()[2]).toEqual({
          type: LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FAILED,
          payload: 'stageTypeId is mandatory.',
        });
        expect(store.getActions()[3]).toEqual({
          type: LEGAL_APPLICATION_DETAILS_LOAD_APP_FAILED,
          payload: 'stageTypeId is mandatory.',
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
          [getApplicationUrl(applicationPublicId)]: Promise.resolve({
            data: 'result application',
          }),
          [getActionStageUrl()]: Promise.reject('error stage type'),
        },
      });

      store
        .dispatch(loadApplication(applicationPublicId, stageTypeId))
        .catch(() => {
          expect(store.getActions().length).toBe(5);
          expect(store.getActions()[2]).toEqual({
            type: LEGAL_APPLICATION_DETAILS_LOAD_STAGE_REQUESTED,
          });
          expect(store.getActions()[3]).toEqual({
            type: LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FAILED,
            payload: 'error stage type',
          });
          expect(store.getActions()[4]).toEqual({
            type: LEGAL_APPLICATION_DETAILS_LOAD_APP_FAILED,
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
          [getApplicationUrl(applicationPublicId)]: Promise.resolve({
            data: 'result application',
          }),
          [getActionStageUrl()]: Promise.resolve({ data: stageTypes }),
        },
        post: {
          [getLegalDashboardApplicationUrl(
            applicationPublicId
          )]: Promise.reject('error components'),
        },
      });

      store
        .dispatch(loadApplication(applicationPublicId, stageTypeId))
        .catch(() => {
          expect(store.getActions().length).toBe(7);
          expect(store.getActions()[5]).toEqual({
            type: LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FAILED,
            payload: 'error components',
          });
          expect(store.getActions()[6]).toEqual({
            type: LEGAL_APPLICATION_DETAILS_LOAD_APP_FAILED,
            payload: 'error components',
          });
          done();
        });
    });
  });
});
