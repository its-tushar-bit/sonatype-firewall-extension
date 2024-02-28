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
import * as authorizationUtil from '../../../../../main/frontend/util/authorizationUtil';
import * as actionsModule from '../../../../../main/frontend/configuration/license/productLicenseActions';
import * as jsUtil from '../../../../../main/frontend/util/jsUtil';
import {
  getLicenseDetailsUrl,
  getLicenseSummaryUrl,
  getLicenseUploadUrl,
} from '../../../../../main/frontend/util/CLMLocation';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';

describe('productLicenseActions', () => {
  let load, store, updateLicense, uninstallLicense;
  const axiosMock = axiosMockAdapter();
  const licenseSummaryUrl = getLicenseSummaryUrl();
  const licenseDetailsUrl = getLicenseDetailsUrl();
  const licenseUploadUrl = getLicenseUploadUrl();

  beforeEach(() => {
    jest.spyOn(authorizationUtil, 'getPermissions');
    jest.spyOn(jsUtil, 'getDaysFromNow').mockReturnValue(1);
    ({ load: load, updateLicense: updateLicense, uninstallLicense: uninstallLicense } = actionsModule);
  });

  describe('load', () => {
    describe('success', () => {
      beforeEach(() => {
        authorizationUtil.getPermissions.mockReturnValue({ length: 0 });
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
        const mockResponse = {};
        axiosMock.onGet(licenseSummaryUrl).reply(200, mockResponse);
        store = SpecUtil.mockReduxStore();
        store.dispatch(load()).then(() => {
          const [, { type, payload }] = store.getActions();
          expect(type).toBe(PRODUCT_LICENSE_LOAD_FULFILLED);
          expect(payload).toEqual({ ...mockResponse });
          done();
        });
      });
    });

    describe('success as admin', () => {
      beforeEach(() => {
        authorizationUtil.getPermissions.mockReturnValue({ length: 1 });
      });

      it(`dispatches a PRODUCT_LICENSE_LOAD_REQUESTED action`, () => {
        store = SpecUtil.mockReduxStore();
        store.dispatch(load());
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: PRODUCT_LICENSE_LOAD_REQUESTED,
        });
      });

      it(`dispatches a PRODUCT_LICENSE_LOAD_FULFILLED action`, (done) => {
        const mockResponse = { expiryTimestamp: 1 };

        axiosMock.onGet(licenseDetailsUrl).reply(200, mockResponse);
        store = SpecUtil.mockReduxStore();
        store.dispatch(load()).then(() => {
          const [, { type, payload }] = store.getActions();
          expect(type).toBe(PRODUCT_LICENSE_LOAD_FULFILLED);
          expect(payload).toEqual({ ...mockResponse, daysToExpiration: 1 });
          done();
        });
      });
    });

    describe('fail', () => {
      beforeEach(() => {
        authorizationUtil.getPermissions.mockReturnValue({ length: 0 });
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
        axiosMock.onGet(licenseSummaryUrl).reply(500);
        store = SpecUtil.mockReduxStore();
        store.dispatch(load()).then(() => {
          const [, { type }] = store.getActions();
          expect(type).toBe(PRODUCT_LICENSE_LOAD_FAILED);
          done();
        });
      });

      it(`dispatches a ${PRODUCT_LICENSE_INVALID} action`, (done) => {
        axiosMock.onGet(licenseSummaryUrl).reply(402);
        store = SpecUtil.mockReduxStore();
        store.dispatch(load()).then(() => {
          const [, { type }] = store.getActions();
          expect(type).toBe(PRODUCT_LICENSE_INVALID);
          done();
        });
      });
    });

    describe('fail as admin', () => {
      beforeEach(() => {
        authorizationUtil.getPermissions.mockImplementation(() => Promise.reject('some error'));
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
        axiosMock.onGet(licenseSummaryUrl).reply(500);
        store = SpecUtil.mockReduxStore();
        store.dispatch(load()).then(() => {
          const [, { type }] = store.getActions();
          expect(type).toBe(PRODUCT_LICENSE_LOAD_FAILED);
          done();
        });
      });

      it(`dispatches a ${PRODUCT_LICENSE_INVALID} action`, (done) => {
        axiosMock.onGet(licenseDetailsUrl).reply(402);
        authorizationUtil.getPermissions.mockReturnValue({ length: 1 });
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
        axiosMock.onPost(licenseUploadUrl, expect.objectContaining(form)).reply(200);
        store.dispatch(updateLicense(license)).then(() => {
          const [, { type }] = store.getActions();
          expect(type).toBe(PRODUCT_LICENSE_UPDATE_LICENSE_FULFILLED);
          done();
        });
      });

      it(`dispatches a ${PRODUCT_LICENSE_UPDATE_LICENSE_FULFILLED} action`, (done) => {
        axiosMock.onPost(licenseUploadUrl).reply(200);
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

      it(`dispatches a ${PRODUCT_LICENSE_UPDATE_LICENSE_FAILED} because of service failures`, (done) => {
        const error = 'some error happened';
        axiosMock.onPost(licenseUploadUrl).reply(500, error);
        store.dispatch(updateLicense()).then(() => {
          const [, { type, payload }] = store.getActions();
          expect(type).toBe(PRODUCT_LICENSE_UPDATE_LICENSE_FAILED);
          expect(payload).toBe(error);
          done();
        });
      });
    });
  });

  describe('Uninstall license', () => {
    describe('success', () => {
      let actions;
      beforeEach(() => {
        axiosMock.onDelete(licenseUploadUrl).reply(200);

        store = SpecUtil.mockReduxStore();
        actions = store.getActions();
      });

      it(`dispatches a PRODUCT_LICENSE_UNINSTALL_LICENSE_REQUESTED action`, () => {
        store.dispatch(uninstallLicense());
        expect(actions[0].type).toBe(PRODUCT_LICENSE_UNINSTALL_LICENSE_REQUESTED);
      });

      it(`dispatches a PRODUCT_LICENSE_UNINSTALL_LICENSE_FULFILLED action`, (done) => {
        store.dispatch(uninstallLicense()).then(() => {
          const [, { type: actionType }] = actions;
          expect(actionType).toBe(PRODUCT_LICENSE_UNINSTALL_LICENSE_FULFILLED);
          done();
        });
      });

      it(`dispatches a PRODUCT_LICENSE_UNINSTALL_MASK_TIMER_DONE action`, (done) => {
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
        axiosMock.onDelete(licenseUploadUrl).reply(500, error);
        store = SpecUtil.mockReduxStore();
        actions = store.getActions();
      });

      it(`dispatches a PRODUCT_LICENSE_UNINSTALL_LICENSE_REQUESTED action`, () => {
        store.dispatch(uninstallLicense());
        expect(actions[0].type).toBe(PRODUCT_LICENSE_UNINSTALL_LICENSE_REQUESTED);
      });

      it(`dispatches a PRODUCT_LICENSE_UNINSTALL_LICENSE_FAIL action`, () => {
        store.dispatch(uninstallLicense()).then(() => {
          const [, { type: actionType, payload: actionPayload }] = actions;
          expect(actionType).toBe(PRODUCT_LICENSE_UNINSTALL_LICENSE_FAIL);
          expect(actionPayload).toBe(error);
        });
      });
    });
  });
});
