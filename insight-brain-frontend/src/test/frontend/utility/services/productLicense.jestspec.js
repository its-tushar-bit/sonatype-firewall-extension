/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { clearLoadedProductLicensePromise, loadIfNotYetLoaded } from 'MainRoot/utility/services/ProductLicense';
import * as authorizationUtil from 'MainRoot/util/authorizationUtil';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getLicenseDetailsUrl, getLicenseSummaryUrl } from 'MainRoot/util/CLMLocation';

describe('productLicense', () => {
  let axiosMock, licenseDetailsUrl, licenseSummaryUrl;
  beforeAll(() => {
    licenseSummaryUrl = getLicenseSummaryUrl();
    licenseDetailsUrl = getLicenseDetailsUrl();
    axiosMock = axiosMockAdapter();
    clearLoadedProductLicensePromise();
  });

  afterEach(() => {
    clearLoadedProductLicensePromise();
  });

  beforeEach(() => {
    jest.spyOn(authorizationUtil, 'getPermissions');
  });

  describe('loadIfNotYetLoaded is called', () => {
    describe('getPermissions returns the CONFIGURE_SYSTEM permission', () => {
      beforeEach(() => {
        authorizationUtil.getPermissions.mockReturnValue({ length: 1 });
      });

      it(`resolves successfully when get license is successful`, async () => {
        const expectedResponse = { products: ['product-001', 'product-002'] };
        axiosMock.onGet(licenseDetailsUrl).replyOnce(200, expectedResponse);

        const response = await loadIfNotYetLoaded();
        expect(response).toEqual(expectedResponse);
      });

      it(`resolves the same result when get license is successful and its called multiple times`, async () => {
        const expectedResponse = { products: ['product-003', 'product-004'] };
        axiosMock.onGet(licenseDetailsUrl).replyOnce(200, expectedResponse);

        let response = await loadIfNotYetLoaded();
        expect(response).toEqual(expectedResponse);

        response = await loadIfNotYetLoaded();
        expect(response).toEqual(expectedResponse);
      });

      it(`rejects when get license is un-successful`, async () => {
        axiosMock.onGet(licenseDetailsUrl).replyOnce(500);

        await expect(loadIfNotYetLoaded()).rejects.toThrow(new Error('Request failed with status code 500'));
      });

      it(`retries to get license summary for successive calls after an un-successful result`, async () => {
        axiosMock.onGet(licenseDetailsUrl).replyOnce(500);

        await expect(loadIfNotYetLoaded()).rejects.toThrow(new Error('Request failed with status code 500'));

        const expectedResponse = { products: ['product-005', 'product-006'] };
        axiosMock.onGet(licenseDetailsUrl).replyOnce(200, expectedResponse);

        let response = await loadIfNotYetLoaded();
        expect(response).toEqual(expectedResponse);
      });
    });

    describe('getPermissions returns no permissions', () => {
      beforeEach(() => {
        authorizationUtil.getPermissions.mockReturnValue({ length: 0 });
      });

      it(`resolves successfully when get license summary is successful`, async () => {
        const expectedResponse = { products: ['product-005', 'product-006'] };
        axiosMock.onGet(licenseSummaryUrl).replyOnce(200, expectedResponse);

        const response = await loadIfNotYetLoaded();
        expect(response).toEqual(expectedResponse);
      });

      it(`resolves the same result when get license summary is successful and its called multiple times`, async () => {
        const expectedResponse = { products: ['product-007', 'product-08'] };
        axiosMock.onGet(licenseSummaryUrl).replyOnce(200, expectedResponse);

        let response = await loadIfNotYetLoaded();
        expect(response).toEqual(expectedResponse);

        response = await loadIfNotYetLoaded();
        expect(response).toEqual(expectedResponse);
      });

      it(`rejects when get license summary is un-successful`, async () => {
        axiosMock.onGet(licenseSummaryUrl).replyOnce(500);

        await expect(loadIfNotYetLoaded()).rejects.toThrow(new Error('Request failed with status code 500'));
      });

      it(`retries to get license summary for successive calls after an un-successful result`, async () => {
        axiosMock.onGet(licenseSummaryUrl).replyOnce(500);

        await expect(loadIfNotYetLoaded()).rejects.toThrow(new Error('Request failed with status code 500'));

        const expectedResponse = { products: ['product-009', 'product-010'] };
        axiosMock.onGet(licenseSummaryUrl).replyOnce(200, expectedResponse);

        let response = await loadIfNotYetLoaded();
        expect(response).toEqual(expectedResponse);
      });
    });

    describe('getPermissions returns 401 permission', () => {
      beforeEach(() => {
        const errorResponse = new Error('Request failed');
        errorResponse.status = 401;
        authorizationUtil.getPermissions.mockRejectedValue(errorResponse);
      });

      it(`resolves successfully when get license summary is successful`, async () => {
        const expectedResponse = { products: ['product-011', 'product-012'] };
        axiosMock.onGet(licenseSummaryUrl).replyOnce(200, expectedResponse);

        const response = await loadIfNotYetLoaded();
        expect(response).toEqual(expectedResponse);
      });

      it(`retries to get license summary for successive calls when get license summary is successful`, async () => {
        const expectedResponse = { products: ['product-013', 'product-014'] };
        axiosMock.onGet(licenseSummaryUrl).replyOnce(200, expectedResponse);

        const response = await loadIfNotYetLoaded();
        expect(response).toEqual(expectedResponse);

        const expectedResponseTwo = { products: ['product-015', 'product-016'] };
        axiosMock.onGet(licenseSummaryUrl).replyOnce(200, expectedResponseTwo);

        const responseTwo = await loadIfNotYetLoaded();
        expect(responseTwo).toEqual(expectedResponseTwo);
      });

      it(`rejects when get license summary is un-successful`, async () => {
        axiosMock.onGet(licenseSummaryUrl).replyOnce(500);

        await expect(loadIfNotYetLoaded()).rejects.toThrow(new Error('Request failed with status code 500'));
      });

      it(`retries to get license summary for successive calls after an un-successful result`, async () => {
        axiosMock.onGet(licenseSummaryUrl).replyOnce(500);

        await expect(loadIfNotYetLoaded()).rejects.toThrow(new Error('Request failed with status code 500'));

        const expectedResponse = { products: ['product-009', 'product-010'] };
        axiosMock.onGet(licenseSummaryUrl).replyOnce(200, expectedResponse);

        let response = await loadIfNotYetLoaded();
        expect(response).toEqual(expectedResponse);
      });
    });

    describe('getPermissions returns an error', () => {
      let errorResponse;

      beforeEach(() => {
        errorResponse = new Error('Request failed');
        authorizationUtil.getPermissions.mockRejectedValue(errorResponse);
      });

      it(`returns rejected getPermissions promise`, async () => {
        await expect(loadIfNotYetLoaded()).rejects.toThrow(new Error('Request failed'));
      });

      it(`returns rejected promise and the next loadIfNotYetLoaded attempts to fetch new data`, async () => {
        await expect(loadIfNotYetLoaded()).rejects.toThrow(new Error('Request failed'));

        authorizationUtil.getPermissions.mockReturnValue({ length: 0 });

        const expectedResponse = { products: ['product-007', 'product-008'] };
        axiosMock.onGet(licenseSummaryUrl).replyOnce(200, expectedResponse);

        const response = await loadIfNotYetLoaded();
        expect(response).toEqual(expectedResponse);
      });
    });
  });
});
