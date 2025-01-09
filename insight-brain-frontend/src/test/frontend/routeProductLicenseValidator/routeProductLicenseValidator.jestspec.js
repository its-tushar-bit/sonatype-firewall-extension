/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import handleOnEnterPermissions from 'MainRoot/routeProductLicenseValidator/RouteProductLicenseValidator';

let mockProductLicenseResponse;
jest.mock('MainRoot/utility/services/ProductLicense', () => ({
  loadIfNotYetLoaded: () => Promise.resolve(mockProductLicenseResponse),
}));

describe('RouteProductLicenseValidator handleOnEnterPermissions', () => {
  let mockStateServiceTargetFn;

  beforeEach(() => {
    mockStateServiceTargetFn = jest.fn().mockImplementation((stateName) => stateName);
  });

  describe('handleOnEnterPermissions is called', () => {
    it(`returns true for all non protected paths`, async () => {
      mockProductLicenseResponse = null;
      expect(await handleOnEnterPermissions(mockStateServiceTargetFn, { name: 'home' })).toEqual(true);
      expect(await handleOnEnterPermissions(mockStateServiceTargetFn, { name: 'root' })).toEqual(true);
      expect(await handleOnEnterPermissions(mockStateServiceTargetFn, { name: 'productlicense' })).toEqual(true);
      expect(await handleOnEnterPermissions(mockStateServiceTargetFn, { name: 'gettingStarted' })).toEqual(true);
      expect(await handleOnEnterPermissions(mockStateServiceTargetFn, { name: 'proxyConfig' })).toEqual(true);
      expect(mockStateServiceTargetFn).not.toHaveBeenCalled();
    });

    it(`calls stateServiceTargetFn handler if productLicense is null for any non protected path`, async () => {
      mockProductLicenseResponse = null;
      const consoleWarnMock = jest.spyOn(console, 'warn').mockImplementation();

      expect(await handleOnEnterPermissions(mockStateServiceTargetFn, { name: 'some-random-path' })).toEqual('home');
      expect(mockStateServiceTargetFn).toHaveBeenCalledWith('home');
      expect(consoleWarnMock).toBeCalled();

      consoleWarnMock.mockRestore();
    });

    describe('the products are not only SBOM Manager', () => {
      it(`calls handleOnEnterPermissions, and returns true`, async () => {
        mockProductLicenseResponse = { products: ['some-random-product', 'Sonatype SBOM Manager'] };
        expect(await handleOnEnterPermissions(mockStateServiceTargetFn, { name: 'some-random-path' })).toEqual(true);
        expect(mockStateServiceTargetFn).not.toHaveBeenCalled();
      });
    });

    describe.each(['Sonatype SBOM Manager', 'Sonatype SBOM Manager SaaS'])(
      'the product is only %s',
      (sbomManagerProductName) => {
        beforeEach(() => {
          mockProductLicenseResponse = { products: [sbomManagerProductName] };
        });

        it(`calls stateServiceTargetFn handler for non SBOM manager state names`, async () => {
          expect(await handleOnEnterPermissions(mockStateServiceTargetFn, { name: 'non-sbom-manager-path' })).toEqual(
            'home'
          );
          expect(mockStateServiceTargetFn).toHaveBeenCalledWith('home');
        });

        it(`returns true for state names starting with sbomManager`, async () => {
          expect(await handleOnEnterPermissions(mockStateServiceTargetFn, { name: 'sbomManager.anything' })).toEqual(
            true
          );
          expect(mockStateServiceTargetFn).not.toHaveBeenCalled();
        });

        test.each([
          ['addRole', true],
          ['addWebhook', true],
          ['administrators', true],
          ['administratorsConfig', true],
          ['administratorsEdit', true],
          ['advancedSearchConfig', true],
          ['baseUrlConfiguration', true],
          ['create-ldap', true],
          ['edit-ldap-connection', true],
          ['edit-ldap-usermapping', true],
          ['editRole', true],
          ['editWebhook', true],
          ['ldap-list', true],
          ['listWebhooks', true],
          ['mailConfig', true],
          ['proxyConfig', true],
          ['rolesList', true],
          ['saml', true],
          ['systemNoticeConfiguration', true],
          ['users', true],
          ['createUser', true],
          ['editUser', true],
        ])('when handleOnEnterPermissions is called with the state name %s returns %s', async (name, expected) => {
          expect(await handleOnEnterPermissions(mockStateServiceTargetFn, { name })).toEqual(expected);
          expect(mockStateServiceTargetFn).not.toHaveBeenCalled();
        });
      }
    );

    describe('the products are firewall only', () => {
      it(`calls handleOnEnterPermissions and stateServiceTargetFn without the firewall prefix in the state name`, async () => {
        mockProductLicenseResponse = {
          products: ['Sonatype Repository Firewall', 'Sonatype Firewall for Artifactory'],
        };
        expect(await handleOnEnterPermissions(mockStateServiceTargetFn, { name: 'some-random-path' })).toEqual('home');
        expect(mockStateServiceTargetFn).toHaveBeenCalled();
      });

      it(`calls handleOnEnterPermissions and returns true with the firewall prefix in the state name`, async () => {
        mockProductLicenseResponse = {
          products: ['Sonatype Repository Firewall', 'Sonatype Firewall for Artifactory'],
        };
        expect(await handleOnEnterPermissions(mockStateServiceTargetFn, { name: 'firewall.some-random-path' })).toEqual(
          true
        );
        expect(mockStateServiceTargetFn).not.toHaveBeenCalled();
      });
    });

    describe('the products are not firewall only', () => {
      it(`calls handleOnEnterPermissions and returns true without the firewall prefix in the state name`, async () => {
        mockProductLicenseResponse = {
          products: ['Sonatype Repository Firewall', 'Sonatype SBOM Manager'],
        };
        expect(await handleOnEnterPermissions(mockStateServiceTargetFn, { name: 'some-random-path' })).toEqual(true);
        expect(mockStateServiceTargetFn).not.toHaveBeenCalled();
      });

      it(`calls handleOnEnterPermissions and returns true with the firewall prefix in the state name`, async () => {
        mockProductLicenseResponse = {
          products: ['Sonatype Repository Firewall', 'Sonatype SBOM Manager'],
        };
        expect(await handleOnEnterPermissions(mockStateServiceTargetFn, { name: 'firewall.some-random-path' })).toEqual(
          true
        );
        expect(mockStateServiceTargetFn).not.toHaveBeenCalled();
      });
    });

    describe('the products not contain firewall', () => {
      it(`calls handleOnEnterPermissions and returns true without the firewall prefix in the state name`, async () => {
        mockProductLicenseResponse = {
          products: ['Sonatype Lifecycle SaaS', 'Sonatype SBOM Manager'],
        };
        expect(await handleOnEnterPermissions(mockStateServiceTargetFn, { name: 'some-random-path' })).toEqual(true);
        expect(mockStateServiceTargetFn).not.toHaveBeenCalled();
      });

      it(`calls handleOnEnterPermissions and stateServiceTargetFn with the firewall prefix in the state name`, async () => {
        mockProductLicenseResponse = {
          products: ['Sonatype Lifecycle SaaS', 'Sonatype SBOM Manager'],
        };
        expect(await handleOnEnterPermissions(mockStateServiceTargetFn, { name: 'firewall.some-random-path' })).toEqual(
          'home'
        );
        expect(mockStateServiceTargetFn).toHaveBeenCalled();
      });
    });
  });
});
