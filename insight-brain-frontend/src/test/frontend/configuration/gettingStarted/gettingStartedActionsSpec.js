/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  GETTING_STARTED_LOAD_REQUESTED,
  GETTING_STARTED_LOAD_FULFILLED,
  GETTING_STARTED_LOAD_FAILED,
} from 'MainRoot/configuration/gettingStarted/gettingStartedActions';
import axios from 'axios';
import { getIsHdsReachable } from 'MainRoot/util/CLMLocation';
import * as productLicenseActions from 'MainRoot/configuration/license/productLicenseActions';

describe('gettingStartedReducerActions', () => {
  let getPermissionsSpy, loadIfNotYetLoadedSpy, load, store;
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const isHdsReachable = getIsHdsReachable();

  beforeEach(() => {
    getPermissionsSpy = jasmine.createSpy('getPermissions');
    loadIfNotYetLoadedSpy = spyOn(productLicenseActions, 'load');
    const actionsModule = require('inject-loader!../../../../../src/main/frontend/configuration/gettingStarted/gettingStartedActions')(
      {
        '../../util/authorizationUtil': {
          getPermissions: getPermissionsSpy,
        },
        'MainRoot/configuration/license/productLicenseActions': {
          load: loadIfNotYetLoadedSpy,
        },
      }
    );

    ({ load: load } = actionsModule);
  });

  describe('load', () => {
    describe('success', () => {
      let mockLicenseResponse;

      beforeEach(() => {
        mockLicenseResponse = { payload: { expiryTimestamp: '' } };
        getPermissionsSpy.and.returnValue(Promise.resolve(['CONFIGURE_SYSTEM', 'ADD_APPLICATION']));
        loadIfNotYetLoadedSpy.and.callFake(() => () => Promise.resolve(mockLicenseResponse));
      });

      it(`dispatches a ${GETTING_STARTED_LOAD_REQUESTED} action`, (done) => {
        store = SpecUtil.mockReduxStore();
        store.dispatch(load()).then(() => {
          const actions = store.getActions();
          expect(actions).toHaveAction({
            type: GETTING_STARTED_LOAD_REQUESTED,
          });
          done();
        });
      });

      it(`dispatches a ${GETTING_STARTED_LOAD_FULFILLED} action`, (done) => {
        const mockHdsResponse = { data: {} };
        mockAxiosCalls({
          get: {
            [isHdsReachable]: Promise.resolve(mockHdsResponse),
          },
        });
        store = SpecUtil.mockReduxStore();
        store.dispatch(load()).then(() => {
          const actions = store.getActions();
          const { type, payload } = actions[1];
          expect(type).toBe(GETTING_STARTED_LOAD_FULFILLED);
          expect(payload.license).toEqual({ ...mockLicenseResponse.payload });
          done();
        });
      });
    });

    describe('fail', () => {
      const errorMsg = 'fetch failed';
      beforeEach(() => {
        getPermissionsSpy.and.callFake(() => Promise.reject(errorMsg));
      });

      it(`dispatches a ${GETTING_STARTED_LOAD_REQUESTED} action`, () => {
        store = SpecUtil.mockReduxStore();
        store.dispatch(load());
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: GETTING_STARTED_LOAD_REQUESTED,
        });
      });

      it(`dispatches a ${GETTING_STARTED_LOAD_FAILED} action because of insufficient permissions`, (done) => {
        store = SpecUtil.mockReduxStore();
        store.dispatch(load()).then(() => {
          const [, { type, payload }] = store.getActions();
          expect(type).toBe(GETTING_STARTED_LOAD_FAILED);
          expect(payload).toBe(errorMsg);
          done();
        });
      });

      it(`dispatches a ${GETTING_STARTED_LOAD_FAILED} action because of service failures`, (done) => {
        getPermissionsSpy.and.returnValue(Promise.resolve(['CONFIGURE_SYSTEM', 'ADD_APPLICATION']));
        loadIfNotYetLoadedSpy.and.callFake(() => () => Promise.reject({ response: {} }));
        store = SpecUtil.mockReduxStore();
        store.dispatch(load()).then(() => {
          const [, { type }] = store.getActions();
          expect(type).toBe(GETTING_STARTED_LOAD_FAILED);
          done();
        });
      });
    });
  });
});
