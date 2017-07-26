/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
var licenseGroupModule = angular.module('LicenseThreatGroup',
    ['Stores', 'CLMAppLocation']);

licenseGroupModule.service('licenseGroupStore', [
  'CLMAppLocations', 'CachedStore', function(CLMAppLocations, CachedStore) {
    var licenseGroupStoreTemplate = {
      id: 'id',
      template: { id: null, ownerId: null, name: null, threatLevel: 5 },
      getUrl: CLMAppLocations.getLicenseGroupsUrl,
      relationalConfigs: {
        'licenses': {
          id: 'licenseId',
          template: { id: null, licenseId: null },
          url: CLMAppLocations.getLicenseGroupLicensesUrl
        }
      }
    };

    return CachedStore.get(licenseGroupStoreTemplate);
  }
]);

licenseGroupModule.service('licenseStore', [
  'CLMLocations', 'StoreFactory', function(CLMLocations, StoreFactory) {
    var licenseStore = StoreFactory.getStore({
      id: 'id',
      url: CLMLocations.getLicensesUrl()
    });
    return licenseStore;
  }
]);
