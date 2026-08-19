/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Mock the dependencies before importing actions
jest.mock('../../../../main/frontend/util/authorizationUtil', () => ({
  getPermissions: jest.fn(),
}));

import {
  GETTING_STARTED_LOAD_REQUESTED,
  GETTING_STARTED_LOAD_FULFILLED,
  GETTING_STARTED_LOAD_FAILED,
  load,
} from 'MainRoot/configuration/gettingStarted/gettingStartedActions';
import '../../SpecUtil';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getPermissions } from '../../../../main/frontend/util/authorizationUtil';
import { getIsHdsReachable } from 'MainRoot/util/CLMLocation';
import * as productLicenseActions from 'MainRoot/configuration/license/productLicenseActions';

describe('gettingStartedReducerActions', () => {
  let loadIfNotYetLoadedSpy, store;
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });
  const isHdsReachable = getIsHdsReachable();

  beforeEach(() => {
    jest.clearAllMocks();
    getPermissions.mockClear();
    loadIfNotYetLoadedSpy = jest.spyOn(productLicenseActions, 'load');
  });

  describe('load', () => {
    describe('success', () => {
      let mockLicenseResponse;

      beforeEach(() => {
        mockLicenseResponse = { payload: { expiryTimestamp: '' } };
        getPermissions.mockReturnValue(Promise.resolve(['CONFIGURE_SYSTEM', 'ADD_APPLICATION']));
        loadIfNotYetLoadedSpy.mockImplementation(() => () => Promise.resolve(mockLicenseResponse));
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
        axiosMock.onGet(isHdsReachable).reply(200, {});
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
        getPermissions.mockImplementation(() => Promise.reject(errorMsg));
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
        getPermissions.mockReturnValue(Promise.resolve(['CONFIGURE_SYSTEM', 'ADD_APPLICATION']));
        loadIfNotYetLoadedSpy.mockImplementation(() => () => Promise.reject({ response: {} }));
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
