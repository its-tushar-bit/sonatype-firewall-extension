/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import CLMContextLocationModule from '../utilAngular/CLMContextLocation';
import storesModule from '../utilAngular/Stores';

var licenseGroupModule = angular.module('LicenseThreatGroup', [storesModule.name, CLMContextLocationModule.name]);

licenseGroupModule.service('licenseGroupStore', [
  'CLMContextLocations',
  'CachedStore',
  function (CLMContextLocations, CachedStore) {
    var licenseGroupStoreTemplate = {
      id: 'id',
      template: { id: null, ownerId: null, name: null, threatLevel: 5 },
      getUrl: CLMContextLocations.getLicenseGroupsUrl,
      relationalConfigs: {
        licenses: {
          id: 'licenseId',
          template: { id: null, licenseId: null },
          url: CLMContextLocations.getLicenseGroupLicensesUrl,
        },
      },
    };

    return CachedStore.get(licenseGroupStoreTemplate);
  },
]);

licenseGroupModule.service('licenseStore', [
  'CLMLocations',
  'StoreFactory',
  function (CLMLocations, StoreFactory) {
    var licenseStore = StoreFactory.getStore({
      id: 'id',
      url: CLMLocations.getLicensesUrl(),
    });
    return licenseStore;
  },
]);

export default licenseGroupModule;
