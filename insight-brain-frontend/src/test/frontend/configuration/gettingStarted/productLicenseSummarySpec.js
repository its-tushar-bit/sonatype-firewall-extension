/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import ProductLicenseSummary from '../../../../main/frontend/configuration/gettingStarted/components/ProductLicenseSummary';
import { getDaysFromNow, getExpiryDate } from '../../../../main/frontend/util/jsUtil';
import * as enzymeUtils from '../../enzymeUtils';

describe('productLicenseSummary', function () {
  let initialProps, getShallow;

  beforeEach(() => {
    initialProps = {
      license: {
        productEdition: 'Lifecycle',
        fingerprint: '99c9cd6be744c30439b4260010bf14d7e2c3013a',
        expiryTimestamp: 1627862400000,
        licensedUsersToDisplay: null,
        applicationLimitToDisplay: null,
        applicationCountToDisplay: null,
        firewallUsersToDisplay: null,
        contactName: 'Nick Cook',
        contactCompany: 'Sonatype Inc',
        contactEmail: 'ncook@sonatype.com',
        products: [
          'Nexus Lifecycle',
          'Nexus Firewall',
          'Nexus Firewall for Artifactory',
          'Nexus Advanced Development Pack',
        ],
      },
    };
    getShallow = enzymeUtils.getShallowComponent(ProductLicenseSummary, initialProps);
  });

  describe('loaded', function () {
    describe('daysToExpiration', function () {
      it('is set to zero if expiryTimestamp is today', function () {
        const anHourFromNow = new Date().getTime() + 1000 * 60 * 60;
        const daysFromNow = getDaysFromNow(anHourFromNow);

        const newProps = {
          license: {
            ...initialProps.license,
            expiryTimestamp: anHourFromNow,
          },
        };

        const shallowComponent = getShallow(newProps);
        const daysToExpiration = shallowComponent.find('#license-days-to-expiration');

        expect(daysToExpiration).toExist();
        expect(daysToExpiration).toHaveText(daysFromNow.toString());
      });

      it('is set to 1 if expiryTimestamp is tomorrow', function () {
        const aDayFromNow = new Date().getTime() + 1000 * 60 * 60 * 25;

        const newProps = {
          license: {
            ...initialProps.license,
            expiryTimestamp: aDayFromNow,
          },
        };

        const shallowComponent = getShallow(newProps);
        const daysToExpiration = shallowComponent.find('#license-days-to-expiration');

        expect(daysToExpiration).toExist();
        expect(daysToExpiration).toHaveText('1');
      });

      it('is set to 2 if expiryTimestamp is day after tomorrow', function () {
        const aDayFromNow = new Date().getTime() + 1000 * 60 * 60 * 49;

        const newProps = {
          license: {
            ...initialProps.license,
            expiryTimestamp: aDayFromNow,
          },
        };

        const shallowComponent = getShallow(newProps);
        const daysToExpiration = shallowComponent.find('#license-days-to-expiration');

        expect(daysToExpiration).toExist();
        expect(daysToExpiration).toHaveText('2');
      });
    });

    describe('expirationDate', function () {
      it('is set to same day if expirationDate is today', function () {
        const anHourFromNow = new Date().getTime() + 1000 * 60 * 60;
        const daysFromNow = getExpiryDate(anHourFromNow);

        const newProps = {
          license: {
            ...initialProps.license,
            expiryTimestamp: anHourFromNow,
          },
        };

        const shallowComponent = getShallow(newProps);
        const licenseExpiryDate = shallowComponent.find('#license-expiry-date');

        expect(licenseExpiryDate).toExist();
        expect(licenseExpiryDate).toHaveText(daysFromNow.toString());
      });

      it('is set to 1 if expirationDate is tomorrow', function () {
        const aDayFromNow = new Date().getTime() + 1000 * 60 * 60 * 25;
        const daysFromNow = getExpiryDate(aDayFromNow);

        const newProps = {
          license: {
            ...initialProps.license,
            expiryTimestamp: aDayFromNow,
          },
        };

        const shallowComponent = getShallow(newProps);
        const licenseExpiryDate = shallowComponent.find('#license-expiry-date');

        expect(licenseExpiryDate).toExist();
        expect(licenseExpiryDate).toHaveText(daysFromNow.toString());
      });

      it('is set to 2 if expirationDate is day after tomorrow', function () {
        const aDayFromNow = new Date().getTime() + 1000 * 60 * 60 * 49;
        const daysFromNow = getExpiryDate(aDayFromNow);

        const newProps = {
          license: {
            ...initialProps.license,
            expiryTimestamp: aDayFromNow,
          },
        };

        const shallowComponent = getShallow(newProps);
        const licenseExpiryDate = shallowComponent.find('#license-expiry-date');

        expect(licenseExpiryDate).toExist();
        expect(licenseExpiryDate).toHaveText(daysFromNow.toString());
      });
    });

    describe('fingerprint', function () {
      it('fingerprint alphanumeric id exists', function () {
        const newProps = {
          license: {
            ...initialProps.license,
            fingerprint: 'randomFingerPrint',
          },
        };

        const shallowComponent = getShallow(newProps);
        const licenseFingerprint = shallowComponent.find('#license-fingerprint');

        expect(licenseFingerprint).toExist();
        expect(licenseFingerprint).toHaveText('randomFingerPrint');
      });
    });

    describe('licenseType', function () {
      it('shows inside License Type list', function () {
        const newProps = {
          license: {
            ...initialProps.license,
            products: [
              'Nexus Lifecycle',
              'Nexus Firewall',
              'Nexus Firewall for Artifactory',
              'Nexus Advanced Development Pack',
            ],
          },
        };

        const shallowComponent = getShallow(newProps);
        const productsList = shallowComponent.find('#license-products');
        const productsListChildren = shallowComponent.find('#license-products .nx-read-only__data');

        expect(productsList).toExist();
        expect(productsListChildren.at(0).text()).toEqual('Nexus Lifecycle');
        expect(productsListChildren.at(1).text()).toEqual('Nexus Firewall');
        expect(productsListChildren.at(2).text()).toEqual('Nexus Firewall for Artifactory');
        expect(productsListChildren.at(3).text()).toEqual('Nexus Advanced Development Pack');
      });
    });

    describe('licensed developers is showed', function () {
      it('containing 300 licensed users and 100 firewall users', function () {
        const newProps = {
          license: {
            ...initialProps.license,
            firewallUsersToDisplay: 100,
            licensedUsersToDisplay: 300,
          },
        };

        const shallowComponent = getShallow(newProps);
        const licensedDevelopers = shallowComponent.find('#license-licensed-developers');
        const developersByType = licensedDevelopers.find('div');

        expect(licensedDevelopers).toExist();
        expect(developersByType.at(0).find('.nx-read-only__label').text()).toEqual('Lifecycle');
        expect(developersByType.at(0).find('.nx-read-only__data').text()).toEqual('300');
        expect(developersByType.at(1).find('.nx-read-only__label').text()).toEqual('Firewall');
        expect(developersByType.at(1).find('.nx-read-only__data').text()).toEqual('100');
      });

      it('containing 101 licensed users and no firewall users', function () {
        const newProps = {
          license: {
            ...initialProps.license,
            licensedUsersToDisplay: 101,
          },
        };

        const shallowComponent = getShallow(newProps);
        const licensedDevelopers = shallowComponent.find('#license-licensed-developers');

        expect(licensedDevelopers).toExist();
        expect(licensedDevelopers.text()).toBe('101');
      });
    });

    describe('license application limit', function () {
      it('is showed with proper text', function () {
        const appLimit = 404;
        const appCount = 101;
        const newProps = {
          license: {
            ...initialProps.license,
            applicationLimitToDisplay: appLimit,
            applicationCountToDisplay: appCount,
          },
        };

        const shallowComponent = getShallow(newProps);
        const licenseLimit = shallowComponent.find('#license-application-limit');

        expect(licenseLimit).toExist();
        expect(licenseLimit.text()).toEqual(`${appLimit} (${appCount} in use)`);
      });
    });
  });
});
