/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  PRODUCT_LICENSE_INVALID,
  PRODUCT_LICENSE_LOAD_FAILED,
  PRODUCT_LICENSE_LOAD_FULFILLED,
  PRODUCT_LICENSE_LOAD_REQUESTED,
  PRODUCT_LICENSE_UPDATE_LICENSE_FAILED,
  PRODUCT_LICENSE_UPDATE_LICENSE_FULFILLED,
  PRODUCT_LICENSE_UPDATE_LICENSE_REQUESTED,
  PRODUCT_LICENSE_UNINSTALL_LICENSE_FAIL,
  PRODUCT_LICENSE_UNINSTALL_LICENSE_FULFILLED,
  PRODUCT_LICENSE_UNINSTALL_LICENSE_REQUESTED,
  PRODUCT_LICENSE_UNINSTALL_MASK_TIMER_DONE,
} from '../../../../../main/frontend/configuration/license/productLicenseActions';
import axios from 'axios';
import { getLicenseSummaryUrl, getLicenseUploadUrl } from '../../../../../main/frontend/util/CLMLocation';

describe('productLicenseActions', () => {
  let checkPermissionsSpy, load, store, updateLicense, uninstallLicense;
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const licenseSummaryUrl = getLicenseSummaryUrl();
  const licenseUploadUrl = getLicenseUploadUrl();

  beforeEach(() => {
    checkPermissionsSpy = jasmine.createSpy('checkPermissions');
    const actionsModule = require('inject-loader!../../../../../../src/main/frontend/configuration/license/productLicenseActions')(
      {
        '../../util/authorizationUtil': {
          checkPermissions: checkPermissionsSpy,
        },
        '../../util/jsUtil': {
          getDaysFromNow: () => 1,
        },
      }
    );

    ({ load: load, updateLicense: updateLicense, uninstallLicense: uninstallLicense } = actionsModule);
  });

  describe('load', () => {
    describe('success', () => {
      beforeEach(() => {
        checkPermissionsSpy.and.returnValue(Promise.resolve());
      });

      it(`dispatches a ${PRODUCT_LICENSE_LOAD_REQUESTED} action`, () => {
        store = SpecUtil.mockReduxStore();
        store.dispatch(load());
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: PRODUCT_LICENSE_LOAD_REQUESTED,
        });
      });

      it(`dispatches a ${PRODUCT_LICENSE_LOAD_FULFILLED} action`, (done) => {
        const mockResponse = { data: { expiryTimestamp: '' } };
        mockAxiosCalls({
          get: {
            [licenseSummaryUrl]: Promise.resolve(mockResponse),
          },
        });
        store = SpecUtil.mockReduxStore();
        store.dispatch(load()).then(() => {
          const [, { type, payload }] = store.getActions();
          expect(type).toBe(PRODUCT_LICENSE_LOAD_FULFILLED);
          expect(payload).toEqual({ ...mockResponse.data, daysToExpiration: 1 });
          done();
        });
      });
    });

    describe('fail', () => {
      beforeEach(() => {
        const actionsModule = require('inject-loader!../../../../../../src/main/frontend/configuration/license/productLicenseActions')(
          {
            '../../util/authorizationUtil': {
              checkPermissions: () => Promise.reject('some error'),
            },
            '../../util/jsUtil': {
              getDaysFromNow: () => 1,
            },
          }
        );
        ({ load: load, updateLicense: updateLicense, uninstallLicense: uninstallLicense } = actionsModule);
      });

      it(`dispatches a ${PRODUCT_LICENSE_LOAD_REQUESTED} action`, () => {
        store = SpecUtil.mockReduxStore();
        store.dispatch(load());
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: PRODUCT_LICENSE_LOAD_REQUESTED,
        });
      });

      it(`dispatches a ${PRODUCT_LICENSE_LOAD_FAILED} action because of insufficient permissions`, (done) => {
        store = SpecUtil.mockReduxStore();
        store.dispatch(load()).then(() => {
          const [, { type }] = store.getActions();
          expect(type).toBe(PRODUCT_LICENSE_LOAD_FAILED);
          done();
        });
      });

      it(`dispatches a ${PRODUCT_LICENSE_LOAD_FAILED} action because of service failures`, (done) => {
        checkPermissionsSpy.and.returnValue(Promise.resolve());
        mockAxiosCalls({
          get: {
            [licenseSummaryUrl]: Promise.reject({ response: {} }),
          },
        });
        store = SpecUtil.mockReduxStore();
        store.dispatch(load()).then(() => {
          const [, { type }] = store.getActions();
          expect(type).toBe(PRODUCT_LICENSE_LOAD_FAILED);
          done();
        });
      });

      it(`dispatches a ${PRODUCT_LICENSE_INVALID} action`, (done) => {
        const actionsModule = require('inject-loader!../../../../../../src/main/frontend/configuration/license/productLicenseActions')(
          {
            '../../util/authorizationUtil': {
              checkPermissions: () => Promise.resolve(),
            },
            '../../util/jsUtil': {
              getDaysFromNow: () => 1,
            },
          }
        );
        ({ load: load, updateLicense: updateLicense, uninstallLicense: uninstallLicense } = actionsModule);
        mockAxiosCalls({
          get: {
            [licenseSummaryUrl]: Promise.reject({ response: { status: 402 } }),
          },
        });
        store = SpecUtil.mockReduxStore();
        store.dispatch(load()).then(() => {
          const [, { type }] = store.getActions();
          expect(type).toBe(PRODUCT_LICENSE_INVALID);
          done();
        });
      });
    });
  });

  describe('updateLicense', () => {
    describe('success', () => {
      beforeEach(() => {
        checkPermissionsSpy.and.returnValue(Promise.resolve());
        store = SpecUtil.mockReduxStore();
      });

      it(`dispatches a ${PRODUCT_LICENSE_UPDATE_LICENSE_REQUESTED} action`, () => {
        store.dispatch(updateLicense({}));
        const [{ type }] = store.getActions();
        expect(type).toBe(PRODUCT_LICENSE_UPDATE_LICENSE_REQUESTED);
      });

      it(`adds the license file to a form and send it to the service`, (done) => {
        const license = {};
        const form = new FormData();
        form.append('file', license);
        const spy = spyOn(axios, 'post').and.returnValue(Promise.resolve({}));
        store.dispatch(updateLicense(license)).then(() => {
          expect(spy).toHaveBeenCalledWith(getLicenseUploadUrl(), form);
          done();
        });
      });

      it(`dispatches a ${PRODUCT_LICENSE_UPDATE_LICENSE_FULFILLED} action`, (done) => {
        spyOn(axios, 'post').and.returnValue(Promise.resolve());
        store.dispatch(updateLicense()).then(() => {
          const [, { type }] = store.getActions();
          expect(type).toBe(PRODUCT_LICENSE_UPDATE_LICENSE_FULFILLED);
          done();
        });
      });
    });

    describe('fail', () => {
      beforeEach(() => {
        store = SpecUtil.mockReduxStore();
      });

      it(`dispatches a ${PRODUCT_LICENSE_UPDATE_LICENSE_REQUESTED} action`, () => {
        store.dispatch(updateLicense());
        const [{ type }] = store.getActions();
        expect(type).toBe(PRODUCT_LICENSE_UPDATE_LICENSE_REQUESTED);
      });

      it(`dispatches a ${PRODUCT_LICENSE_UPDATE_LICENSE_FAILED} because of service failures`, () => {
        const error = 'some error happened';
        mockAxiosCalls({
          post: {
            [licenseUploadUrl]: Promise.reject(error),
          },
        });
        store.dispatch(updateLicense()).then(() => {
          const [, { type, payload }] = store.getActions();
          expect(type).toBe(PRODUCT_LICENSE_UPDATE_LICENSE_FAILED);
          expect(payload).toBe(error);
        });
      });
    });
  });

  describe('Uninstall license', () => {
    describe('success', () => {
      let actions;
      beforeEach(() => {
        mockAxiosCalls({
          del: {
            [licenseUploadUrl]: Promise.resolve({}),
          },
        });

        store = SpecUtil.mockReduxStore();
        actions = store.getActions();
      });

      it(`dispatches a ${PRODUCT_LICENSE_UNINSTALL_LICENSE_REQUESTED} action`, () => {
        store.dispatch(uninstallLicense());
        expect(actions[0].type).toBe(PRODUCT_LICENSE_UNINSTALL_LICENSE_REQUESTED);
      });

      it(`dispatches a ${PRODUCT_LICENSE_UNINSTALL_LICENSE_FULFILLED} action`, (done) => {
        store.dispatch(uninstallLicense()).then(() => {
          const [, { type: actionType }] = actions;
          expect(actionType).toBe(PRODUCT_LICENSE_UNINSTALL_LICENSE_FULFILLED);
          done();
        });
      });

      it(`dispatches a ${PRODUCT_LICENSE_UNINSTALL_MASK_TIMER_DONE} action`, (done) => {
        store.dispatch(uninstallLicense()).then(() => {
          const [, , { type: actionType }] = actions;
          expect(actionType).toBe(PRODUCT_LICENSE_UNINSTALL_MASK_TIMER_DONE);
          done();
        });
      });
    });

    describe('fail', () => {
      let actions;
      const error = 'some error happened';

      beforeEach(() => {
        mockAxiosCalls({
          del: {
            [licenseUploadUrl]: Promise.reject(error),
          },
        });
        store = SpecUtil.mockReduxStore();
        actions = store.getActions();
      });

      it(`dispatches a ${PRODUCT_LICENSE_UNINSTALL_LICENSE_REQUESTED} action`, () => {
        store.dispatch(uninstallLicense());
        expect(actions[0].type).toBe(PRODUCT_LICENSE_UNINSTALL_LICENSE_REQUESTED);
      });

      it(`dispatches a ${PRODUCT_LICENSE_UNINSTALL_LICENSE_FAIL} action`, () => {
        store.dispatch(uninstallLicense()).then(() => {
          const [, { type: actionType, payload: actionPayload }] = actions;
          expect(actionType).toBe(PRODUCT_LICENSE_UNINSTALL_LICENSE_FAIL);
          expect(actionPayload).toBe(error);
        });
      });
    });
  });
});
